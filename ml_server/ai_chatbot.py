import os
import google.generativeai as genai

SYSTEM_INSTRUCTION = (
    "You are PrePal AI, an expert academic tutor, study planner, and student counselor for university students. "
    "Your goal is to help students create personalized study schedules, manage exam stress, improve low module grades, "
    "and solve academic/study issues. "
    "Be encouraging, structured, practical, and clear. Format your responses with markdown, bullet points, and clear sections where appropriate."
)

def generate_ai_chat_response(user_message, student_context=None):
    """
    Generates an AI study planning & counseling response using Google Gemini SDK.
    :param user_message: String user prompt
    :param student_context: Dict containing optional keys like current_gpa, predicted_gpa, stress_level, weak_modules
    :return: dict with 'reply' and 'status'
    """
    raw_api_key = os.environ.get("GEMINI_API_KEY", "")
    api_key = raw_api_key.strip().strip('"').strip("'")
    
    if not api_key:
        return {
            "reply": "⚠️ **GEMINI_API_KEY is not configured on Render.**\n\nPlease set your free Gemini API key from [Google AI Studio](https://aistudio.google.com/) as an environment variable (`GEMINI_API_KEY`) on your Render server dashboard.",
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
        genai.configure(api_key=api_key)
        
        # Try gemini-1.5-flash with fallback to gemini-2.0-flash
        try:
            model = genai.GenerativeModel("gemini-1.5-flash")
            response = model.generate_content(full_prompt)
        except Exception:
            model = genai.GenerativeModel("gemini-2.0-flash")
            response = model.generate_content(full_prompt)

        if response and hasattr(response, 'text') and response.text:
            return {"reply": response.text, "status": "success"}
        else:
            return {"reply": "Sorry, received an empty response from the AI engine.", "status": "error"}

    except Exception as e:
        err_msg = str(e)
        print(f"[Gemini SDK Exception] {err_msg}")
        return {
            "reply": f"Google Gemini API Error: {err_msg}. Please verify your GEMINI_API_KEY in Render environment variables.",
            "status": "error"
        }
