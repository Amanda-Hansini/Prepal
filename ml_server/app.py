from flask import Flask, request, jsonify
import os

from gpa_calculator import calculate_gpa
from ml_model import predict_model_a, predict_model_b, predict_model_c
from ai_chatbot import generate_ai_chat_response

app = Flask(__name__)

@app.route('/api/predict', methods=['POST'])
def predict():
    try:
        json_data = request.get_json(silent=True) or {}
        student_id = json_data.get('student_id')
        student_type = int(json_data.get('student_type', 1)) # 1: Pre-Sem, 2: Mid-Sem (New), 3: Master Model
        
        print(f"[API] Prediction request for Student: {student_id}, Type: {student_type}")
        
        if not student_id:
            return jsonify({'error': 'student_id is required'}), 400

        # --- 1. EXTRACT QUESTIONNAIRE INPUTS ---
        try:
            attendance = float(json_data.get('attendance', 0.0))
            study_hours = float(json_data.get('study_hours', 0.0))
            sleep_hours = float(json_data.get('sleep_hours', 0.0))
            stress_level = float(json_data.get('stress_level', 3.0))
            cgpa = float(json_data.get('cgpa', 0.0))
        except ValueError:
            return jsonify({'error': 'Questionnaire inputs must be valid numbers'}), 400

        extracted_grades = json_data.get('results', [])
        
        # --- 2. WARNING & ACKNOWLEDGEMENT ENGINE ---
        acknowledgements_required = []

        if cgpa > 0 and cgpa < 2.00:
            acknowledgements_required.append(
                "ACADEMIC WARNING: Your Cumulative GPA is dangerously low. You must score high this semester to avoid academic probation."
            )

        if attendance < 80.0:
            acknowledgements_required.append(
                "DANGER: Your attendance is below the 80% university requirement. You are at high risk of being barred from final exams."
            )

        total_credits = sum(float(mod.get('credits', 0)) for mod in extracted_grades)
        if total_credits > 0:
            min_study_hours = (total_credits * 50) / 15
            if study_hours < min_study_hours:
                acknowledgements_required.append(
                    f"BY-LAW WARNING: You are studying {study_hours} hrs, which is less than the SLQF mandated minimum of {min_study_hours:.1f} hrs for {total_credits} credits. This mathematically lowers your predicted GPA."
                )

        if sleep_hours < 7 or sleep_hours > 9:
            acknowledgements_required.append(
                "HEALTH WARNING: You are deviating from the National Sleep Foundation's 7-9 hour standard, severely impacting cognitive performance and your ML predicted GPA."
            )

        if stress_level >= 4.0:
            acknowledgements_required.append(
                "HEALTH WARNING: Your stress levels are critically high. Consider utilizing campus counseling services to prevent severe academic burnout."
            )

        # --- 3. PROCESS CA MARKS (MID + ASSIGNMENT) ---
        valid_mid_marks = []
        valid_assg_marks = []
        failed_credits = 0

        for mod in extracted_grades:
            mod_name = mod.get('moduleName', 'Unknown Module')
            mod_credits = float(mod.get('credits', 0))
            mid = float(mod.get('mid_mark', 0))
            assg = float(mod.get('assignment_mark', 0))

            ca_total = mid + assg

            if ca_total < 8:
                acknowledgements_required.append(
                    f"DANGER: You scored {ca_total}/40 in {mod_name} continuous assessment. You are barred from the final exam! Speak to your lecturer immediately."
                )
                failed_credits += mod_credits
            else:
                if ca_total < 15:
                    acknowledgements_required.append(
                        f"NOTICE: Your CA marks ({ca_total}/40) for {mod_name} are borderline. You must score exceptionally high in the final exam to secure a good grade."
                    )
                valid_mid_marks.append(mid)
                valid_assg_marks.append(assg)

        # Calculate ML Averages for passed CA modules
        avg_mid = sum(valid_mid_marks) / len(valid_mid_marks) if valid_mid_marks else 0.0
        avg_assg = sum(valid_assg_marks) / len(valid_assg_marks) if valid_assg_marks else 0.0

        # --- 4. MULTIPLE LINEAR REGRESSION PREDICTION ---
        predicted_gpa = 0.0
        
        if student_type == 1:
            predicted_gpa = predict_model_a(cgpa, attendance, study_hours, sleep_hours, stress_level)
        elif student_type == 2:
            predicted_gpa = predict_model_b(avg_mid, avg_assg, attendance, study_hours, sleep_hours, stress_level)
        elif student_type == 3:
            predicted_gpa = predict_model_c(cgpa, avg_mid, avg_assg, attendance, study_hours, sleep_hours, stress_level)
        
        # --- 5. APPLY PENALTIES FOR BARRED MODULES ---
        if total_credits > 0 and failed_credits > 0:
            passed_credits = total_credits - failed_credits
            adjusted_gpa = (predicted_gpa * passed_credits) / total_credits
            predicted_gpa = round(adjusted_gpa, 2)
            eligible = False
        else:
            eligible = True
            
        # --- 6. MOTIVATION TIP ---
        if predicted_gpa >= 3.0: tip = "Excellent! You are on track for outstanding results. Keep it up!"
        elif predicted_gpa >= 2.0: tip = "Good progress. Focus more on weak modules to reach your full potential."
        else: tip = "Your predicted GPA is low. Please review the acknowledgements and organize a strict study schedule."

        return jsonify({
            'student_id': student_id,
            'student_type': student_type,
            'semester_gpa': cgpa,
            'predicted_future_gpa': predicted_gpa,
            'motivation_tip': tip,
            'eligible': eligible,
            'acknowledgements_required': acknowledgements_required
        }), 200

    except Exception as e:
        print(f"[Server Error] {e}")
        return jsonify({'error': str(e)}), 500

@app.route('/api/ai_chat', methods=['POST'])
def ai_chat():
    try:
        json_data = request.get_json(silent=True) or {}
        user_message = json_data.get('user_message', '').strip()
        student_context = json_data.get('student_context', {})
        
        if not user_message:
            return jsonify({'error': 'user_message is required'}), 400
            
        print(f"[API] AI Chat request: {user_message[:50]}...")
        ai_result = generate_ai_chat_response(user_message, student_context)
        return jsonify(ai_result), 200
    except Exception as e:
        print(f"[AI Chat Server Error] {e}")
        return jsonify({'reply': f'Server processing error: {str(e)}', 'status': 'error'}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
