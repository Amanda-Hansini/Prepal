import firebase_admin
from firebase_admin import credentials, firestore
import os

# Initialize Firebase Admin SDK
# Note: You need to generate a private key from Firebase Console -> Project Settings -> Service Accounts -> Generate New Private Key
# and save it as "serviceAccountKey.json" in the ml_server directory.
cred_path = "serviceAccountKey.json"
db = None

if os.path.exists(cred_path):
    cred = credentials.Certificate(cred_path)
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    print("[Firebase] Initialized successfully.")
else:
    print(f"[Firebase] Warning: '{cred_path}' not found. Cannot fetch real module credits.")

GRADE_POINTS = {
    "A+": 4.00, "A": 4.00, "A-": 3.70,
    "B+": 3.30, "B": 3.00, "B-": 2.70,
    "C+": 2.30, "C": 2.00, "C-": 1.70,
    "D+": 1.30, "D": 1.00, "E": 0.00, "F": 0.00
}

SPECIAL_GRADES = ["AB", "MC", "NE"]

def get_module_credit(module_name):
    """
    Fetches the credit value for a given module from Firebase.
    Returns default of 3 if not found or if Firebase is not connected.
    """
    if db is None:
        # Fallback for testing without Firebase
        return 3

    try:
        # Assuming there is a 'Modules' collection where document ID is the module name
        # or the module name is a field. Adjust this query based on your Firebase structure.
        docs = db.collection("Modules").where("moduleName", "==", module_name).stream()
        for doc in docs:
            data = doc.to_dict()
            if "credits" in data:
                return float(data["credits"])
        return 3 # Default if not found
    except Exception as e:
        print(f"Error fetching credit for {module_name}: {e}")
        return 3

def calculate_gpa(extracted_grades):
    """
    Calculates GPA from a list of dictionaries: [ { "module_name": "...", "grade": "...", "grade_point": 4.0, "credits": 3.0 }, ... ]
    Returns a dictionary with calculation results.
    """
    total_points = 0.0
    total_credits = 0.0
    failed_count = 0
    
    ab_modules = []
    mc_modules = []
    ne_modules = []

    # Check if extracted_grades is a dictionary (old format) or list (new format)
    if isinstance(extracted_grades, dict):
        for module, grade in extracted_grades.items():
            grade = grade.strip().upper()
            credit = get_module_credit(module)
            
            if grade in GRADE_POINTS:
                points = GRADE_POINTS[grade]
                total_points += (points * credit)
                total_credits += credit
                if grade == "E": failed_count += 1
            elif grade in SPECIAL_GRADES:
                failed_count += 1
                if grade == "AB": ab_modules.append(module)
                elif grade == "MC": mc_modules.append(module)
                elif grade == "NE": ne_modules.append(module)
    else:
        # Handle new list format
        for item in extracted_grades:
            # Use snake_case keys as sent from Android
            module = item.get('module_name', 'Unknown')
            grade = str(item.get('grade', '')).strip().upper()
            
            # Use provided points/credits if available, otherwise fetch
            points = item.get('grade_point')
            if points is None:
                points = GRADE_POINTS.get(grade, 0.0)
            
            credit = item.get('credits')
            if credit is None:
                credit = get_module_credit(module)
            
            credit = float(credit)
            points = float(points)

            if grade in GRADE_POINTS or grade not in SPECIAL_GRADES:
                if grade not in SPECIAL_GRADES:
                    total_points += (points * credit)
                    total_credits += credit
                    if grade == "E" or grade == "F": failed_count += 1
            
            if grade in SPECIAL_GRADES:
                failed_count += 1
                if grade == "AB": ab_modules.append(module)
                elif grade == "MC": mc_modules.append(module)
                elif grade == "NE": ne_modules.append(module)
                
    # CGPA Formula: Sum of All Quality Points / Total Credit Hours (Sum of all attempted credits)
    gpa = total_points / total_credits if total_credits > 0 else 0.0
    
    return {
        "current_gpa": round(gpa, 2),
        "total_credits_earned": total_credits,
        "failed_count": failed_count,
        "ab_modules": ab_modules,
        "mc_modules": mc_modules,
        "ne_modules": ne_modules
    }

