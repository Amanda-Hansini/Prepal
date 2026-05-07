import pandas as pd
import numpy as np

def parse_student_grades_excel(excel_path, student_id):
    """
    Parses an Excel result sheet and extracts grades for the given student ID.
    Looks for the student ID in any row, and then assigns valid grades to the official modules.
    """
    extracted_grades = {}
    print(f"\n[Excel Parser] Reading Excel file for '{student_id}'...")

    official_modules = [
        "Fundamentals of Database Management",
        "Business Management",
        "Introduction to System Development",
        "Web Technologies",
        "Multimedia Technologies",
        "Soft Skills and Entrepreneurship"
    ]

    valid_grades = ["A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "E", "AB", "MC", "NE"]

    try:
        # Read the Excel file, ignoring headers to just search raw data
        df = pd.read_excel(excel_path, header=None)
        
        # Iterate through each row to find the student ID
        for index, row in df.iterrows():
            row_values = [str(val).strip().upper() for val in row.values if pd.notna(val)]
            
            # Check if student ID is in this row
            student_id_upper = str(student_id).strip().upper()
            if any(student_id_upper in val for val in row_values):
                print(f"[Excel Parser] Found student {student_id} in row {index}")
                
                grade_index = 0
                # Start looking for grades in this row
                for val in row_values:
                    # If this value is a valid grade and we still have modules to assign
                    if val in valid_grades and grade_index < len(official_modules):
                        module_name = official_modules[grade_index]
                        extracted_grades[module_name] = val
                        grade_index += 1
                
                # Once we process the row with the student ID, we can stop
                if extracted_grades:
                    return extracted_grades

    except Exception as e:
        print(f"[Excel Parser] Error parsing Excel file: {e}")
        
    return extracted_grades

# Simple test block if run directly
if __name__ == "__main__":
    import sys
    if len(sys.argv) > 2:
        file_path = sys.argv[1]
        s_id = sys.argv[2]
        print(parse_student_grades_excel(file_path, s_id))
    else:
        print("Usage: python excel_parser.py <path_to_excel> <student_id>")
