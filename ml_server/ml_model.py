import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression
import joblib
import os

DATASET_PATH = "future_gpa_dataset_final.csv"
MODEL_A_PATH = "gpa_model_a.pkl"
MODEL_B_PATH = "gpa_model_b.pkl"
MODEL_C_PATH = "gpa_model_c.pkl"

def train_models():
    """Trains Model A, Model B, and Model C Multiple Linear Regression models."""
    if not os.path.exists(DATASET_PATH):
        print(f"[Error] '{DATASET_PATH}' not found in the server directory.")
        return None, None, None
    
    try:
        print(f"[AI Model] Loading dataset and training Tri-Model Lifecycle Suite...")
        df = pd.read_csv(DATASET_PATH).dropna()
        
        features_a = ['Previous_GPA', 'Attendance_Percentage', 'Study_Hours_Per_Week', 'Sleep_Hours_Per_Day', 'Stress_Level']
        features_b = ['Mid_Mark', 'Assignment_Mark', 'Attendance_Percentage', 'Study_Hours_Per_Week', 'Sleep_Hours_Per_Day', 'Stress_Level']
        features_c = ['Previous_GPA', 'Mid_Mark', 'Assignment_Mark', 'Attendance_Percentage', 'Study_Hours_Per_Week', 'Sleep_Hours_Per_Day', 'Stress_Level']
        target = 'Future_GPA'

        # Train Model A
        X_a = df[features_a]
        y_a = df[target]
        model_a = LinearRegression()
        model_a.fit(X_a, y_a)
        joblib.dump(model_a, MODEL_A_PATH)
        
        # Train Model B
        X_b = df[features_b]
        y_b = df[target]
        model_b = LinearRegression()
        model_b.fit(X_b, y_b)
        joblib.dump(model_b, MODEL_B_PATH)

        # Train Model C
        X_c = df[features_c]
        y_c = df[target]
        model_c = LinearRegression()
        model_c.fit(X_c, y_c)
        joblib.dump(model_c, MODEL_C_PATH)
        
        print("[AI Model] Model A, Model B, and Model C trained and saved successfully.")
        return model_a, model_b, model_c
        
    except Exception as e:
        print(f"Error training models: {e}")
        return None, None, None

def predict_model_a(cgpa, attendance, study_hours, sleep_hours, stress_level):
    if not os.path.exists(MODEL_A_PATH): model_a, _, _ = train_models()
    else: model_a = joblib.load(MODEL_A_PATH)
    if model_a is None: return 0.0

    student_data = pd.DataFrame([[cgpa, attendance, study_hours, sleep_hours, stress_level]], 
                                columns=['Previous_GPA', 'Attendance_Percentage', 'Study_Hours_Per_Week', 'Sleep_Hours_Per_Day', 'Stress_Level'])
    return round(np.clip(model_a.predict(student_data)[0], 0.0, 4.0), 2)

def predict_model_b(mid_mark, assignment_mark, attendance, study_hours, sleep_hours, stress_level):
    if not os.path.exists(MODEL_B_PATH): _, model_b, _ = train_models()
    else: model_b = joblib.load(MODEL_B_PATH)
    if model_b is None: return 0.0

    student_data = pd.DataFrame([[mid_mark, assignment_mark, attendance, study_hours, sleep_hours, stress_level]], 
                                columns=['Mid_Mark', 'Assignment_Mark', 'Attendance_Percentage', 'Study_Hours_Per_Week', 'Sleep_Hours_Per_Day', 'Stress_Level'])
    return round(np.clip(model_b.predict(student_data)[0], 0.0, 4.0), 2)

def predict_model_c(cgpa, mid_mark, assignment_mark, attendance, study_hours, sleep_hours, stress_level):
    if not os.path.exists(MODEL_C_PATH): _, _, model_c = train_models()
    else: model_c = joblib.load(MODEL_C_PATH)
    if model_c is None: return 0.0

    student_data = pd.DataFrame([[cgpa, mid_mark, assignment_mark, attendance, study_hours, sleep_hours, stress_level]], 
                                columns=['Previous_GPA', 'Mid_Mark', 'Assignment_Mark', 'Attendance_Percentage', 'Study_Hours_Per_Week', 'Sleep_Hours_Per_Day', 'Stress_Level'])
    return round(np.clip(model_c.predict(student_data)[0], 0.0, 4.0), 2)

if __name__ == "__main__":
    train_models()
