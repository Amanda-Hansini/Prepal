from flask import Flask, request, jsonify
import os

from gpa_calculator import calculate_gpa
from ml_model import predict_future_gpa

app = Flask(__name__)

@app.route('/api/predict', methods=['POST'])
def predict():
    try:
        # 1. Get JSON data (Now only accepting JSON for manual entry)
        json_data = request.get_json(silent=True) or {}
        student_id = json_data.get('student_id')
        
        print(f"[API] Prediction request for Student: {student_id}")
        
        if not student_id:
            return jsonify({'error': 'student_id is required'}), 400

        # 2. Extract Questionnaire Inputs (5 Impactful Factors)
        try:
            attendance = float(json_data.get('attendance', 0.0))
            study_hours = float(json_data.get('study_hours', 0.0))
            sleep_hours = float(json_data.get('sleep_hours', 0.0))
            stress_level = float(json_data.get('stress_level', 3.0))
        except ValueError:
            return jsonify({'error': 'Questionnaire inputs must be valid numbers'}), 400

        # 3. Process Manual Results
        extracted_grades = json_data.get('results', [])
        manual_gpa = json_data.get('gpa')
        
        if not extracted_grades:
            return jsonify({'error': 'No results provided'}), 400
        
        # Calculate/Use GPA
        if manual_gpa is not None:
            calc_results = {
                'current_gpa': float(manual_gpa),
                'ab_modules': [], 'mc_modules': [], 'ne_modules': []
            }
        else:
            calc_results = calculate_gpa(extracted_grades)

        sem_gpa = calc_results['current_gpa']
        
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

        # 5. Multiple Linear Regression Prediction (Reduced 5 Factors)
        predicted_gpa = predict_future_gpa(
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
            'ab_modules': calc_results['ab_modules'],
            'mc_modules': calc_results['mc_modules'],
            'ne_modules': calc_results['ne_modules'],
            'eligible': eligible
        }), 200

    except Exception as e:
        print(f"[Server Error] {e}")
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
