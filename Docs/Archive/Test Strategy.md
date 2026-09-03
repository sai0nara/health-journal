Since you are a **Senior AQA Java professional**, I’ve designed this test plan to align with industry standards while focusing on the unique challenges of your "Serverless/Gdrive-based" architecture.

---

## 1. Test Strategy
The testing will follow a **Risk-Based Testing (RBT)** approach. The highest risks are data corruption on Google Drive and permission failures (Health Connect/OAuth2).

### Testing Levels
* **Unit Testing:** Kotlin/JUnit 5 for business logic (CSV/XML parsers, data mapping).
* **Integration Testing:** MockWebServer for AI API and GDrive API responses.
* **End-to-End (E2E):** UI Automator or Espresso for full "Entry-to-Cloud" flows.
* **User Acceptance (UAT):** Verifying the "vibe" and accuracy of AI health advice.

---

## 2. Test Scope & Scenarios

### **A. Functional Testing (The Core)**
| ID | Feature | Test Case | Expected Result |
| :--- | :--- | :--- | :--- |
| **FT-1** | Entry Creation | Create entry with text and photo. | Local Room DB updates; GDrive upload starts. |
| **FT-2** | Health Sync | Trigger manual import from Samsung Health. | Data matches the source app metrics exactly. |
| **FT-3** | Export | Generate XML and CSV files. | Files are readable by Excel/Notepad; data is valid. |
| **FT-4** | AI Advice | Send 7-day log to AI. | API returns context-aware advice; no JSON errors. |

### **B. Cloud & Sync Testing (The "Keep on Gdrive" Rule)**
| ID | Feature | Test Case | Expected Result |
| :--- | :--- | :--- | :--- |
| **ST-1** | Conflict Res. | Edit same entry on two devices simultaneously. | Latest timestamp wins; no data loss. |
| **ST-2** | Offline Mode | Add entry without Wi-Fi/LTE. | Entry saves locally; syncs automatically on reconnect. |
| **ST-3** | Token Exp. | Force OAuth token expiration. | App prompts for re-login or refreshes silently. |

### **C. Edge Cases & Negative Testing**
* **Large Media:** Uploading a 20MB 4K photo on a slow 3G connection.
* **Permission Denial:** User denies Health Connect access (App must not crash; show "Permissions Needed" state).
* **Empty States:** Opening the app for the first time or having zero health data for a specific day.
* **Gdrive Full:** Attempting to sync when the user's Google Drive quota is exceeded.

---

## 3. Automation Plan (AQA Focus)
As a single developer/architect, focus your automation on the most brittle parts:

1.  **API Layer (Contract Testing):** * Verify the AI prompt construction logic.
    * Test the CSV/XML serializers with various edge-case strings (emojis, special characters).
2.  **UI Automation (The "Happy Path"):**
    * One script that: *Logs in -> Creates Entry -> Attaches Photo -> Checks Sync Status.*
3.  **Data Integrity:**
    * A test that writes 100 entries locally and verifies the resulting `data.json` on GDrive is schema-valid.

---

## 4. Test Environment & Tools
* **Physical Devices:** Samsung (for Samsung Health testing) and Pixel (for clean Health Connect testing).
* **Tools:**
    * **Charles Proxy / Fiddler:** To intercept and inspect Google Drive API traffic.
    * **ADB:** To simulate low battery or toggling airplane mode during sync.
    * **Health Connect Toolbox:** A Google-provided app to inject mock health data into the system for testing.

---

## 5. Entry/Exit Criteria
* **Entry:** Successful build of the feature on a staging branch; GDrive API credentials configured.
* **Exit:** 100% of "High" priority test cases passed; No P0/P1 bugs (e.g., data loss, crashes on startup).

---

### **AQA Recommendation:** Since you use **IntelliJ/Cursor**, I suggest setting up a `test-fixtures` directory early on. Use a small set of JSON files that represent different "Health States" (e.g., `high_activity.json`, `no_sleep.json`) to verify that your AI Advice logic handles different inputs predictably.

Would you like me to draft a specific **JUnit 5 template** for your CSV/XML export logic?