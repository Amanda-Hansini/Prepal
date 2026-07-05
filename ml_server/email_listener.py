import firebase_admin
from firebase_admin import credentials, firestore
import os
import time
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

# ==========================================
# 1. SMTP EMAIL CONFIGURATION
# ==========================================
# Gmail is recommended for testing. Follow these steps to configure:
# 1. Go to your Google Account Settings -> Security.
# 2. Enable "2-Step Verification" (if not already enabled).
# 3. Search for "App Passwords" and create a new one (e.g. named "PrePal").
# 4. Copy the generated 16-character code and paste it below.
# ==========================================
SMTP_SERVER = "smtp.gmail.com"
SMTP_PORT = 587  # Use 587 for TLS
SMTP_EMAIL = "hansiniamanda7@gmail.com"  # Your sender Gmail address
SMTP_PASSWORD = "xrvc qdzu zshb tivj"      # Replace with your 16-character App Password

# Initialize Firebase
cred_path = "serviceAccountKey.json"
db = None

if not firebase_admin._apps:
    if os.path.exists(cred_path):
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
        print("[Firebase] Initialized successfully in Email Listener.")
        db = firestore.client()
    else:
        print(f"[Error] '{cred_path}' not found. Place your serviceAccountKey.json inside the 'ml_server' directory.")
        exit(1)
else:
    db = firestore.client()

def send_smtp_email(to_email, subject, body):
    """
    Sends an email using standard SMTP.
    """
    if not SMTP_EMAIL or not SMTP_PASSWORD or "@" not in SMTP_EMAIL:
        raise Exception("SMTP_EMAIL or SMTP_PASSWORD is not configured. Please open email_listener.py and add your details.")

    # Create message
    msg = MIMEMultipart()
    msg['From'] = SMTP_EMAIL
    msg['To'] = to_email
    msg['Subject'] = subject
    msg.attach(MIMEText(body, 'plain'))

    # Connect to SMTP server
    server = smtplib.SMTP(SMTP_SERVER, SMTP_PORT)
    server.starttls()  # Upgrade connection to secure TLS
    server.login(SMTP_EMAIL, SMTP_PASSWORD)
    server.sendmail(SMTP_EMAIL, to_email, msg.as_string())
    server.quit()

def on_snapshot(col_snapshot, changes, read_time):
    """
    Callback triggered when changes occur in the "mail" collection.
    """
    for change in changes:
        if change.type.name == 'ADDED':
            doc_ref = change.document.reference
            doc_data = change.document.to_dict()

            # Skip if already processed or has a delivery state
            if "delivery" in doc_data:
                continue

            to_address = doc_data.get("to")
            message_map = doc_data.get("message", {})
            subject = message_map.get("subject", "PrePal Notification")
            body = message_map.get("text", "")

            if not to_address or "@" not in to_address:
                print(f"[Email Listener] Skipping document {change.document.id} - Invalid recipient email: '{to_address}'")
                continue

            print(f"[Email Listener] Found new email request to: {to_address}. Sending...")

            try:
                # Dispatch Email
                send_smtp_email(to_address, subject, body)
                
                # Update Firestore with SUCCESS state (mirrors Firebase Trigger Email extension)
                doc_ref.update({
                    "delivery": {
                        "state": "SUCCESS",
                        "sentAt": firestore.SERVER_TIMESTAMP,
                        "info": "Sent successfully via Local SMTP Listener"
                    }
                })
                print(f"[Email Listener] Email sent successfully to {to_address}!")

            except Exception as e:
                error_msg = str(e)
                print(f"[Email Listener] Error sending email to {to_address}: {error_msg}")
                
                # Update Firestore with ERROR state
                doc_ref.update({
                    "delivery": {
                        "state": "ERROR",
                        "error": error_msg,
                        "failedAt": firestore.SERVER_TIMESTAMP
                    }
                })

def start_listener():
    """
    Subscribes to real-time changes on both standard 'mail' and legacy '2FA_Emails' collections.
    """
    print("\n==================================================")
    print("      PrePal Local SMTP Email Listener Active     ")
    print("==================================================")
    print(f"Monitoring collections for outgoing emails...")
    print(f"Sender Email Account: {SMTP_EMAIL}")
    print("Listening... Press Ctrl+C to stop.\n")

    # Real-time listener on "mail"
    mail_ref = db.collection("mail")
    mail_ref.on_snapshot(on_snapshot)

    # Keep the main thread alive
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nStopping Local SMTP Email Listener...")

if __name__ == "__main__":
    start_listener()
