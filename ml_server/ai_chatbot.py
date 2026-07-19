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
    Generates an AI study planning response supporting Groq API (Llama-3.3 70B) or Google Gemini API.
    """
    groq_key = os.environ.get("GROQ_API_KEY", "").strip().strip('"').strip("'")
    gemini_key = os.environ.get("GEMINI_API_KEY", "").strip().strip('"').strip("'")
    
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

    # 1. Try Groq API first if key is present (Fastest & 100% Free 14,400 req/day)
    if groq_key:
        try:
            url = "https://api.groq.com/openai/v1/chat/completions"
            headers = {
                "Authorization": f"Bearer {groq_key}",
                "Content-Type": "application/json"
            }
            payload = {
                "model": "llama-3.3-70b-versatile",
                "messages": [
                    {"role": "system", "content": SYSTEM_INSTRUCTION},
                    {"role": "user", "content": f"{context_str}Student Question: {user_message}"}
                ],
                "temperature": 0.7
            }
            res = requests.post(url, json=payload, headers=headers, timeout=25)
            if res.status_code == 200:
                data = res.json()
                reply = data["choices"][0]["message"]["content"]
                return {"reply": reply, "status": "success"}
            else:
                print(f"[Groq Error {res.status_code}] {res.text}")
        except Exception as e:
            print(f"[Groq Exception] {e}")

    # 2. Try Gemini API if key is present
    if gemini_key:
        headers = {"Content-Type": "application/json"}
        payload = {
            "contents": [
                {
                    "parts": [{"text": full_prompt}]
                }
            ]
        }
        
        for model in ["gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro"]:
            url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={gemini_key}"
            try:
                res = requests.post(url, json=payload, headers=headers, timeout=25)
                if res.status_code == 200:
                    data = res.json()
                    reply = data["candidates"][0]["content"]["parts"][0]["text"]
                    return {"reply": reply, "status": "success"}
                else:
                    err_json = res.json() if res.headers.get("content-type", "").startswith("application/json") else {}
                    err_msg = err_json.get("error", {}).get("message", res.text)
                    print(f"[Gemini {model} Error] {err_msg}")
                    if "429" in str(res.status_code) or "Quota" in err_msg:
                        continue
            except Exception as e:
                print(f"[Gemini Exception {model}] {e}")

    # Fallback if no keys or all failed
    if not groq_key and not gemini_key:
        return {
            "reply": "⚠️ **AI Key is missing.**\n\nPlease add `GROQ_API_KEY` (Free from console.groq.com) or `GEMINI_API_KEY` on Render Environment variables.",
            "status": "warning"
        }
    
    return {
        "reply": "⚠️ Google Gemini free quota is currently limited (0 limit) for your Google account region. Please get a **100% Free Groq API Key** from [console.groq.com](https://console.groq.com/keys) and add `GROQ_API_KEY` in Render environment variables!",
        "status": "error"
    }
