import os
import requests

GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

SYSTEM_INSTRUCTION = (
    "You are PrePal AI, an expert academic tutor, study planner, and student counselor for university students. "
    "Your goal is to help students create personalized study schedules, manage exam stress, improve low module grades, "
    "and solve academic/study issues. "
    "Be encouraging, structured, practical, and clear. Format your responses with markdown, bullet points, and clear sections where appropriate."
)

def generate_ai_chat_response(user_message, student_context=None):
    """
    Generates an AI study planning & counseling response using Gemini API.
    :param user_message: String user prompt
    :param student_context: Dict containing optional keys like current_gpa, predicted_gpa, stress_level, weak_modules
    :return: dict with 'reply' and 'status'
    """
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        return {
            "reply": "⚠️ **GEMINI_API_KEY is not configured.**\n\nPlease set your free Gemini API key from [Google AI Studio](https://aistudio.google.com/) as an environment variable (`GEMINI_API_KEY`) on your Render server dashboard or local `.env` file.",
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
    
    try:
        url = f"{GEMINI_API_URL}?key={api_key}"
        payload = {
            "contents": [
                {
                    "parts": [{"text": full_prompt}]
                }
            ]
        }
        headers = {"Content-Type": "application/json"}
        
        response = requests.post(url, json=payload, headers=headers, timeout=30)
        
        if response.status_code == 200:
            res_data = response.json()
            try:
                reply_text = res_data["candidates"][0]["content"]["parts"][0]["text"]
                return {"reply": reply_text, "status": "success"}
            except (KeyError, IndexError):
                return {"reply": "Sorry, I received an unparseable response format from the AI engine.", "status": "error"}
        else:
            err_msg = f"AI API Error ({response.status_code}): {response.text}"
            print(f"[AI Chatbot Error] {err_msg}")
            return {
                "reply": f"Sorry, the AI service encountered an error ({response.status_code}). Please verify your Gemini API key.",
                "status": "error"
            }
    except Exception as e:
        print(f"[AI Chatbot Exception] {e}")
        return {"reply": f"Unable to reach AI server: {str(e)}", "status": "error"}
