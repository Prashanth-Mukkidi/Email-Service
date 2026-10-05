# 📎 Email Attachment Service - Testing & User Guide

A Spring Boot 3.x microservice built with **Java 21** that sends emails with **single or multiple file attachments** (PDF, Word, Images, ZIP, etc.) using `MimeMessage` and Gmail SMTP.

---

## 🚀 Step 1: Start the Application

### Option A: From IntelliJ IDEA
1. Open the project folder `d:\Projects\email-service` in **IntelliJ IDEA**.
2. Locate `src/main/java/com/example/email/EmailServiceApplication.java`.
3. Right-click and choose **Run 'EmailServiceApplication'**.
4. You will see: `Tomcat started on port 8086 (http) with context path '/'`.

### Option B: From Command Line / Terminal
```bash
# Using Maven:
mvn spring-boot:run
```

---

## 🛠️ Step 2: Test Using Postman

### 1. Send Email with File Attachment (`multipart/form-data`)

1. **Method:** `POST`
2. **URL:** `http://localhost:8086/api/v1/email/send-with-attachment`
3. Click the **Body** tab.
4. Select **`form-data`** (⚠️ **Do NOT select raw JSON**).
5. Enter the following key-value pairs:

| Key | Type | Value / Example | Notes |
| :--- | :--- | :--- | :--- |
| `to` | **Text** | `recipient@example.com` | Recipient email address (**Required**) |
| `subject` | **Text** | `Invoice & Report Attached` | Subject line (**Required**) |
| `body` | **Text** | `Hello, please find your files attached.` | Email message (**Required**) |
| `html` | **Text** | `false` | Set `true` if your body contains HTML tags |
| `cc` | **Text** | `cc@example.com` | Optional CC recipient(s) |
| `files` | **File** | *(Browse and choose your file)* | **Hover over the Key field, select `File` from the dropdown, and upload a file** |

> 💡 **To attach multiple files:** Add another key named `files` (set to **File** type) and select another file, or select multiple files in the file chooser.

6. Click **Send**.

#### Expected Success Response:
```json
{
  "success": true,
  "message": "Email sent successfully with attachments",
  "data": {
    "recipient": "recipient@example.com",
    "subject": "Invoice & Report Attached",
    "attachmentCount": 1,
    "attachments": [
      "sample-invoice.pdf"
    ]
  },
  "timestamp": "2026-10-05T22:00:00"
}
```

---

### 2. Send Simple JSON Email (Without File Attachment)

For simple text emails without attachments, you can also send raw JSON:

1. **Method:** `POST`
2. **URL:** `http://localhost:8086/api/v1/email/send-simple`
3. **Body:** Select **raw** -> **JSON**
4. Paste:
```json
{
  "to": "recipient@example.com",
  "subject": "Simple Test Email",
  "body": "This is a simple email without attachments.",
  "html": false
}
```

---

### 3. Check Health

- **Method:** `GET`
- **URL:** `http://localhost:8086/api/v1/email/health`

---

## 💻 Step 3: Test Using cURL

```bash
curl -X POST http://localhost:8086/api/v1/email/send-with-attachment \
  -F "to=recipient@example.com" \
  -F "subject=Meeting Notes & File" \
  -F "body=Hello, here is the document." \
  -F "files=@C:/path/to/your/document.pdf"
```

---

## ⚙️ Configuration (`src/main/resources/application.yml`)

The service comes pre-configured with Gmail SMTP. You can override settings using environment variables or in `application.yml`:

```yaml
server:
  port: 8086

spring:
  servlet:
    multipart:
      max-file-size: 15MB
      max-request-size: 30MB

  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:your-email@gmail.com}
    password: ${MAIL_PASSWORD:your-gmail-app-password}
```

> **Note on Gmail:** Always use a 16-character **Google App Password** (generated from [Google App Passwords](https://myaccount.google.com/apppasswords)), not your normal Gmail account password.
