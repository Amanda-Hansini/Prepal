import re
import os
import glob

def replace_alerts_in_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # If it doesn't have alert, skip
    if 'alert(' not in content:
        return
        
    # We will do some heuristic replacements
    # if it says "failed" or "error" or "required" or "please" -> toast.error
    # else toast.success
    
    def replacer(match):
        message = match.group(1)
        lower_msg = message.lower()
        if any(word in lower_msg for word in ['failed', 'error', 'required', 'please', 'blocked', 'duplicate', 'cannot', 'must be']):
            return f'toast.error({message})'
        else:
            return f'toast.success({message})'

    new_content = re.sub(r'alert\((.*?)\);?', replacer, content)
    
    # Add import toast from 'react-hot-toast' if not present
    if 'toast' in new_content and 'react-hot-toast' not in new_content:
        # insert after first import
        new_content = re.sub(r'(import .*?;?\n)', r'\1import toast from "react-hot-toast";\n', new_content, count=1)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print(f"Updated {filepath}")

for f in glob.glob(r"c:\Users\USER\AndroidStudioProjects\FinalYearProjectNew\prepal-web-admin\src\pages\*.jsx"):
    replace_alerts_in_file(f)
