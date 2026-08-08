# Appendix M: Live GPA Prediction Calculation (Model A)

To demonstrate how the PrePal system predicts future academic performance, this appendix provides a transparent breakdown of the mathematical operations underlying **Model A (Pre-Semester Prediction)**. 

The system utilizes a Multiple Linear Regression (MLR) algorithm, trained on the `future_gpa_dataset_final.csv` via Google Colab. During training, the model assigns specific weights (coefficients) to each input feature based on its historical impact on GPA, and establishes a baseline (intercept).

## 1. The Machine Learning Formula
The live prediction operates using the standard MLR formula:
**Predicted GPA = Intercept + (w₁ × Feature₁) + (w₂ × Feature₂) + ... + (wₙ × Featureₙ)**

For Model A, the exact weights derived from `gpa_model_a.pkl` are:
* **Intercept (Baseline):** 0.2656
* **Previous_GPA (w₁):** +0.5206
* **Attendance_Percentage (w₂):** +0.0106
* **Study_Hours_Per_Week (w₃):** +0.0145
* **Sleep_Hours_Per_Day (w₄):** +0.0430
* **Stress_Level (w₅):** -0.0342

*Note: Positive weights indicate that an increase in the feature boosts the GPA, whereas a negative weight (Stress Level) indicates that an increase harms the GPA.*

## 2. Example Student Scenario
Consider a student using the app before the semester begins, inputting the following data:
* **Previous Semester CGPA:** 3.80
* **Expected Attendance:** 98%
* **Planned Study Time:** 18 hours per week
* **Average Sleep:** 8 hours per day
* **Self-Reported Stress Level:** 2.0 (Low-Moderate)

## 3. Step-by-Step Live Calculation
The PrePal API receives these inputs via the `/api/predict` endpoint and mathematically applies the weights in real-time:

| Feature | Student Input | Model Weight | Calculation (Input × Weight) | Contribution to GPA |
| :--- | :--- | :--- | :--- | :--- |
| **Baseline (Intercept)** | N/A | N/A | (Fixed Value) | **+0.2656** |
| **Previous GPA** | 3.80 | 0.5206 | 3.80 × 0.5206 | **+1.9783** |
| **Attendance (%)** | 98 | 0.0106 | 98 × 0.0106 | **+1.0388** |
| **Study Hours/Week** | 18 | 0.0145 | 18 × 0.0145 | **+0.2610** |
| **Sleep Hours/Day** | 8 | 0.0430 | 8 × 0.0430 | **+0.3440** |
| **Stress Level (1-5)** | 2.0 | -0.0342 | 2.0 × (-0.0342) | **-0.0684** |

## 4. Final Output Generation
The backend sums all the contributing factors:

`Predicted GPA = 0.2656 + 1.9783 + 1.0388 + 0.2610 + 0.3440 - 0.0684`
`Predicted GPA = 3.8193`

The `ml_model.py` script then rounds this result to two decimal places:
**Final Output shown on App = 3.82**

## Conclusion for the Evaluation Panel
By exposing the formula, the panel can observe that **Previous GPA** and **Attendance** carry the heaviest mathematical influence on the final result, while negative psychological factors like **Stress** actively deduct points from the student's potential score. This aligns with academic literature and demonstrates the direct functionality of the Machine Learning integration in a live environment.
