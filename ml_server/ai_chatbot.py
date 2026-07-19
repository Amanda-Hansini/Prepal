import os
import requests

MODEL_CANDIDATES = [
    "gemini-2.5-flash",
    "gemini-2.0-flash",
    "gemini-1.5-flash",
    "gemini-1.5-flash-latest",
    "gemini-pro"
]

SYSTEM_INSTRUCTION = (
    "You are PrePal AI, an expert academic tutor, study planner, and student counselor for university students. "
    "Your goal is to help students create personalized study schedules, manage exam stress, improve low module grades, "
    "and solve academic/study issues. "
    "Be encouraging, structured, practical, and clear. Format your responses with markdown, bullet points, and clear sections where appropriate."
)

def generate_ai_chat_response(user_message, student_context=None):
    """
    Generates an AI study planning & counseling response using Gemini API with candidate model fallbacks.
    :param user_message: String user prompt
    :param student_context: Dict containing optional keys like current_gpa, predicted_gpa, stress_level, weak_modules
    :return: dict with 'reply' and 'status'
    """
    raw_api_key = os.environ.get("GEMINI_API_KEY", "")
    api_key = raw_api_key.strip().strip('"').strip("'")
    
    if not api_key:
        return {
            "reply": "⚠️ **GEMINI_API_KEY is not configured.**\n\nPlease set your free Gemini API key from [Google AI Studio](https://aistudio.google.com/) as an environment variable (`GEMINI_API_KEY`) on your Render server dashboard.",
            "status": "warning"
        }
    
    # Build context string if provided
    context_str = ""
    if student_context and isinstance(student_context, dict):
        current_gpa = student_context.get("current_gpa", "N/A")
        predicted_gpa = student_context.get("predicted_gpa", "N/A")
        weak_modules = student_context.get("weak_modules", [])
        stress_level = student_context.get("stress_level", "N/A")
        
        context_str = f"[STUDENT ACADEMIC PROFILE Context: Current GPA: {current_gpa}, Predicted Future GPA: {predicted_gpa}, Reported Stress Level (1-5): {stress_level}"
        if weak_modules:
            context_str += f", Modules needing attention: {', '.join(weak_modules)}"
        context_str += "]\n\n"
        
    full_prompt = f"{SYSTEM_INSTRUCTION}\n\n{context_str}Student Question: {user_message}"
    
    payload = {
        "contents": [
            {
                "parts": [{"text": full_prompt}]
            }
        ]
    }
    headers = {"Content-Type": "application/json"}

    last_error = ""

    # Try candidate models in case one is restricted or 404 on Google AI Studio account
    for model in MODEL_CANDIDATES:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
        try:
            response = requests.post(url, json=payload, headers=headers, timeout=25)
            print(f"[Gemini API Attempt] Model: {model}, Status: {response.status_code}")
            
            if response.status_code == 200:
                res_data = response.json()
                try:
                    reply_text = res_data["candidates"][0]["content"]["parts"][0]["text"]
                    return {"reply": reply_text, "status": "success"}
                except (KeyError, IndexError):
                    return {"reply": "Sorry, I received an unparseable response format from the AI engine.", "status": "error"}
            else:
                last_error = f"Model {model} returned ({response.status_code}): {response.text}"
                # If 404 or 400, continue to next candidate model
                continue

        except Exception as e:
            print(f"[Gemini API Exception for {model}] {e}")
            last_error = str(e)
            continue

    return {
        "reply": f"Sorry, AI service error. Details: {last_error}. Please double-check your Gemini API key on Render.",
        "status": "error"
    }
