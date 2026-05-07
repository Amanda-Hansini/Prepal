import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression
import joblib
import os

DATASET_PATH = "future_gpa_dataset_final.csv"
MODEL_PATH = "gpa_model_engineered.pkl"

def train_model():
    """Trains the Multiple Linear Regression model using only the most impactful features."""
    if not os.path.exists(DATASET_PATH):
        print(f"[Error] '{DATASET_PATH}' not found in the server directory.")
        return None
    
    try:
        print(f"[AI Model] Loading dataset and training 5-factor Multiple Linear Regression model...")
        df = pd.read_csv(DATASET_PATH)
        
        # Most Impactful Features as per user requirement
        features = [
            'Previous_GPA', 'Attendance_Percentage', 
            'Study_Hours_Per_Week', 'Sleep_Hours_Per_Day', 
            'Stress_Level'
        ]
        target = 'Future_GPA'

        # Drop any rows with missing values
        df = df.dropna(subset=features + [target])
        
        X = df[features]
        y = df[target]

        # Train Model
        model = LinearRegression()
        model.fit(X, y)
        
        # Save Model
        joblib.dump(model, MODEL_PATH)
        print("[AI Model] 5-factor Model trained and saved successfully.")
        return model
        
    except Exception as e:
        print(f"Error training model: {e}")
        return None

def predict_future_gpa(previous_gpa, attendance, study_hours, sleep_hours, stress_level):
    """Predicts future GPA using the reduced 5-factor model."""
    if not os.path.exists(MODEL_PATH):
        model = train_model()
    else:
        model = joblib.load(MODEL_PATH)
        
    if model is None:
        return 0.0

    features = [
        'Previous_GPA', 'Attendance_Percentage', 
        'Study_Hours_Per_Week', 'Sleep_Hours_Per_Day', 
        'Stress_Level'
    ]
    
    student_data = [[
        previous_gpa, 
        attendance,
        study_hours, 
        sleep_hours, 
        stress_level
    ]]
    
    student_df = pd.DataFrame(student_data, columns=features)

    # Predict
    predicted_gpa = model.predict(student_df)[0]
    return round(max(0.0, min(4.0, predicted_gpa)), 2)

if __name__ == "__main__":
    train_model()
