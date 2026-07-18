import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression
import joblib
import os

DATASET_PATH = "future_gpa_dataset_final.csv"
MODEL_A_PATH = "gpa_model_a.pkl"
MODEL_B_PATH = "gpa_model_b.pkl"

def train_models():
    """Trains both Model A and Model B Multiple Linear Regression models."""
    if not os.path.exists(DATASET_PATH):
        print(f"[Error] '{DATASET_PATH}' not found in the server directory.")
        return None, None
    
    try:
        print(f"[AI Model] Loading dataset and training dual Multiple Linear Regression models...")
        df = pd.read_csv(DATASET_PATH)
        
        # Convert O/L letter grades to numbers if necessary
        grade_mapping = {'A': 4, 'B': 3, 'C': 2, 'S': 1, 'W': 0, 'F': 0}
        if df['OL_Maths'].dtype == object:
            df['OL_Maths'] = df['OL_Maths'].map(grade_mapping).fillna(0)
        if df['OL_English'].dtype == object:
            df['OL_English'] = df['OL_English'].map(grade_mapping).fillna(0)
        
        # Features for Model A (Returning Students)
        features_a = [
            'Previous_GPA', 'Attendance_Percentage', 
            'Study_Hours_Per_Week', 'Sleep_Hours_Per_Day', 
            'Stress_Level'
        ]
        
        # Features for Model B (New Students)
        features_b = [
            'OL_Maths', 'OL_English', 'Attendance_Percentage',
            'Study_Hours_Per_Week', 'Sleep_Hours_Per_Day', 'Stress_Level'
        ]
        
        target = 'Future_GPA'

        # Train Model A
        df_a = df.dropna(subset=features_a + [target])
        X_a = df_a[features_a]
        y_a = df_a[target]
        model_a = LinearRegression()
        model_a.fit(X_a, y_a)
        joblib.dump(model_a, MODEL_A_PATH)
        
        # Train Model B
        df_b = df.dropna(subset=features_b + [target])
        X_b = df_b[features_b]
        y_b = df_b[target]
        model_b = LinearRegression()
        model_b.fit(X_b, y_b)
        joblib.dump(model_b, MODEL_B_PATH)
        
        print("[AI Model] Model A and Model B trained and saved successfully.")
        return model_a, model_b
        
    except Exception as e:
        print(f"Error training models: {e}")
        return None, None

def predict_returning_student(previous_gpa, attendance, study_hours, sleep_hours, stress_level):
    """Predicts future GPA using Model A (Previous GPA)."""
    if not os.path.exists(MODEL_A_PATH):
        model_a, _ = train_models()
    else:
        model_a = joblib.load(MODEL_A_PATH)
        
    if model_a is None:
        return 0.0

    features = [
        'Previous_GPA', 'Attendance_Percentage', 
        'Study_Hours_Per_Week', 'Sleep_Hours_Per_Day', 
        'Stress_Level'
    ]
    
    student_data = pd.DataFrame([[
        previous_gpa, attendance, study_hours, sleep_hours, stress_level
    ]], columns=features)

    predicted_gpa = model_a.predict(student_data)[0]
    return round(max(0.0, min(4.0, predicted_gpa)), 2)

def predict_new_student(ol_maths, ol_english, attendance, study_hours, sleep_hours, stress_level):
    """Predicts future GPA using Model B (O/L Results)."""
    if not os.path.exists(MODEL_B_PATH):
        _, model_b = train_models()
    else:
        model_b = joblib.load(MODEL_B_PATH)
        
    if model_b is None:
        return 0.0

    # Ensure O/L grades are mapped to numbers
    grade_mapping = {'A': 4, 'B': 3, 'C': 2, 'S': 1, 'W': 0, 'F': 0}
    
    ol_m_val = ol_maths if isinstance(ol_maths, (int, float)) else grade_mapping.get(str(ol_maths).upper(), 0)
    ol_e_val = ol_english if isinstance(ol_english, (int, float)) else grade_mapping.get(str(ol_english).upper(), 0)

    features = [
        'OL_Maths', 'OL_English', 'Attendance_Percentage',
        'Study_Hours_Per_Week', 'Sleep_Hours_Per_Day', 'Stress_Level'
    ]
    
    student_data = pd.DataFrame([[
        ol_m_val, ol_e_val, attendance, study_hours, sleep_hours, stress_level
    ]], columns=features)

    predicted_gpa = model_b.predict(student_data)[0]
    return round(max(0.0, min(4.0, predicted_gpa)), 2)

if __name__ == "__main__":
    train_models()
