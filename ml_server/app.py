from flask import Flask, request, jsonify
import os

from gpa_calculator import calculate_gpa
from ml_model import predict_returning_student, predict_new_student
from ai_chatbot import generate_ai_chat_response

app = Flask(__name__)

@app.route('/api/predict', methods=['POST'])
def predict():
    try:
        # 1. Get JSON data (Now only accepting JSON for manual entry)
        json_data = request.get_json(silent=True) or {}
        student_id = json_data.get('student_id')
        student_type = json_data.get('student_type', 1) # 1: Returning, 2: New 1st Sem
        
        print(f"[API] Prediction request for Student: {student_id}, Type: {student_type}")
        
        if not student_id:
            return jsonify({'error': 'student_id is required'}), 400

        # 2. Extract Questionnaire Inputs
        try:
            attendance = float(json_data.get('attendance', 0.0))
            study_hours = float(json_data.get('study_hours', 0.0))
            sleep_hours = float(json_data.get('sleep_hours', 0.0))
            stress_level = float(json_data.get('stress_level', 3.0))
        except ValueError:
            return jsonify({'error': 'Questionnaire inputs must be valid numbers'}), 400

        # 3. Process Modules and GPA
        extracted_grades = json_data.get('results', [])
        manual_gpa = json_data.get('gpa')
        
        if not extracted_grades:
            return jsonify({'error': 'No modules/results provided'}), 400
        
        sem_gpa = 0.0
        calc_results = {'ab_modules': [], 'mc_modules': [], 'ne_modules': []}
        
        if student_type == 1:
            # Model A (Returning student) needs previous GPA
            if manual_gpa is not None:
                calc_results['current_gpa'] = float(manual_gpa)
            else:
                calc_results = calculate_gpa(extracted_grades)
            sem_gpa = calc_results.get('current_gpa', 0.0)

        # 4. Handle 80% Attendance Rule Module-wise
        module_attendances = json_data.get('module_attendances', {})
        failed_modules = []
        total_credits = 0
        failed_credits = 0

        for mod in extracted_grades:
            mod_name = mod.get('moduleName', '')
            try:
                mod_credits = float(mod.get('credits', 0))
            except ValueError:
                mod_credits = 0
            
            total_credits += mod_credits
            
            if mod_name in module_attendances:
                if module_attendances[mod_name] < 80.0:
                    failed_modules.append(mod_name)
                    failed_credits += mod_credits

        # 5. Multiple Linear Regression Prediction
        if student_type == 2:
            ol_maths = json_data.get('ol_maths', 'F')
            ol_english = json_data.get('ol_english', 'F')
            predicted_gpa = predict_new_student(
                ol_maths=ol_maths,
                ol_english=ol_english,
                attendance=attendance,
                study_hours=study_hours,
                sleep_hours=sleep_hours,
                stress_level=stress_level
            )
        else:
            predicted_gpa = predict_returning_student(
                previous_gpa=sem_gpa, 
                attendance=attendance,
                study_hours=study_hours, 
                sleep_hours=sleep_hours, 
                stress_level=stress_level
            )

        # 6. Apply Mathematical Penalty for failed modules
        if total_credits > 0 and failed_credits > 0:
            passed_credits = total_credits - failed_credits
            adjusted_gpa = (predicted_gpa * passed_credits) / total_credits
            predicted_gpa = round(adjusted_gpa, 2)
            
            tip = f"WARNING: You are predicted to fail {', '.join(failed_modules)} due to <80% attendance. Adjusted predicted GPA is {predicted_gpa:.2f}."
            eligible = False
        else:
            eligible = True
            # 7. Motivation Tip
            if predicted_gpa >= 3.0: tip = "Excellent! You are on track for outstanding results. Keep it up!"
            elif predicted_gpa >= 2.0: tip = "Good progress. Focus more on weak modules to reach your full potential."
            else: tip = "Your predicted GPA is low. Organize a strict study schedule and seek academic help. You can improve!"

        return jsonify({
            'student_id': student_id,
            'extracted_grades': extracted_grades,
            'semester_gpa': sem_gpa,
            'predicted_future_gpa': predicted_gpa,
            'motivation_tip': tip,
            'ab_modules': calc_results.get('ab_modules', []),
            'mc_modules': calc_results.get('mc_modules', []),
            'ne_modules': calc_results.get('ne_modules', []),
            'eligible': eligible
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
