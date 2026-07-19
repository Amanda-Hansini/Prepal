import os
import requests

SYSTEM_INSTRUCTION = (
    "You are PrePal AI, an expert academic tutor, study planner, and student counselor for university students. "
    "Your goal is to help students create personalized study schedules, manage exam stress, improve low module grades, "
    "and solve academic/study issues. "
    "Be encouraging, structured, practical, and clear. Format your responses with markdown, bullet points, and clear sections where appropriate."
)

def generate_ai_chat_response(user_message, student_context=None):
    """
    Generates an AI study planning response using Google Gemini API.
    :param user_message: String user prompt
    :param student_context: Dict containing optional keys like current_gpa, predicted_gpa, stress_level, weak_modules
    :return: dict with 'reply' and 'status'
    """
    raw_api_key = os.environ.get("GEMINI_API_KEY", "")
    api_key = raw_api_key.strip().strip('"').strip("'")
    
    if not api_key:
        return {
            "reply": "⚠️ **GEMINI_API_KEY is missing on Render server.**\n\nPlease add `GEMINI_API_KEY` in your Render Environment Variables dashboard.",
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

    # Use Google Gemini 1.5 Flash (Standard AI Studio Free Model)
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={api_key}"
    
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=25)
        print(f"[Gemini API] Response Code: {response.status_code}")
        
        if response.status_code == 200:
            res_data = response.json()
            try:
                reply_text = res_data["candidates"][0]["content"]["parts"][0]["text"]
                return {"reply": reply_text, "status": "success"}
            except (KeyError, IndexError):
                return {"reply": "Sorry, I received an unparseable response format from Gemini.", "status": "error"}
        else:
            try:
                err_json = response.json()
                err_msg = err_json.get("error", {}).get("message", response.text)
            except Exception:
                err_msg = response.text
                
            print(f"[Gemini API Error Detail] {err_msg}")
            return {
                "reply": f"Google Gemini API Error ({response.status_code}): {err_msg}",
                "status": "error"
            }

    except Exception as e:
        print(f"[Gemini API Exception] {e}")
        return {"reply": f"Unable to connect to AI service: {str(e)}", "status": "error"}
