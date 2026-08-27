
[//]: # (10. User should be able to add more than one attachment: common formats. )
[//]: # (And more than one photo. User should be able to save attachments synchronized from the cloud.)
[//]: # (2. add photos)
[//]: # (4. search)
[//]: # (7. allure report with screenshots)
[//]: # (8. add versions to app-debug.apk name and on screen &#40;about app&#41;)
<!-- * Ability to move entry to archive. From archive user can delete any entry, couple or all of them  -->
<!-- * Add different categories/tags: illness, checkup, doctor, exercises. Search by these categories/tags -->
* calendar with events from one day or period 
* graph, trends for every part
* Restore data from backup

<!-- * Body parts measurements  -->
<!-- * change color scheme to more common for medical apps. App should have an ability to get theme settings from the System. Add dark theme. UI defect: user can't see status bar since it is white -->
<!-- * For a text larger than 3 strings should be displayed only first 3 strings. Basic formatting tools, like bold, italic, headers -->
<!-- * User should be able to save attachments in any directory he choise -->
<!-- * Defect: pull the feed down triggers syncronization but the update icon newer dissaper   -->

### 1. Feature Breakdown & Product UX

* **User Value Proposition:** Prevents API key theft, unauthorized usage quota exhaustion, and billing fraud while maintaining uninterrupted access to AI-powered capabilities (e.g., natural language logging, trend analysis, smart recommendations).
* **Key User Flows:**
* **Entry Point:** User triggers an AI feature in the UI (e.g., tapping "Analyze Workout Trends").
* **Action:** The app requests attestation verification from the device, sends the request payload to a lightweight secure Proxy Backend rather than directly to the AI provider, and listens for streamed responses.
* **Success State:** Proxy validates the client request, appends the server-side AI API key, executes the call, and streams the chunked response back to the client UI.
* **Error State:** If client attestation fails, rate limits are exceeded, or the proxy rejects the request, the UI displays a clean fallback message ("Service temporarily unavailable") without exposing underlying service URLs or security credentials.


* **Mobile-Specific Considerations:**
* **Latency & Streaming:** AI LLM queries are high-latency. Use Server-Sent Events (SSE) or WebSockets through the proxy to stream responses token-by-token directly into Jetpack Compose states.
* **Network Resiliency:** Implement automatic retries with exponential backoff on the proxy layer rather than on the device to conserve battery and cellular data.
* **Offline Graceful Degradation:** Disable AI-dependent UI triggers when device network connectivity (`NetworkCapabilities`) is offline, providing local deterministic alternatives where possible.



---

### 2. Architectural Design & Approaches

* **Design Pattern Recommendation:** **BFF (Backend-for-Frontend) Proxy Pattern** integrated with an **MVI / Clean Architecture** layer in Android. The mobile binary remains completely agnostic of the remote AI service credentials.
* **Data Flow:**

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Android Client Binary                         │
│                                                                        │
│  [ UI / Compose ] ──► [ ViewModel ] ──► [ AI Remote DataSource ]       │
└───────────────────────────────────────────────────┬────────────────────┘
                                                    │ 1. Request + Integrity Token
                                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Backend Proxy Gateway                           │
│  (Cloudflare Workers / Firebase Functions / Ktor / AWS Lambda)         │
│                                                                        │
│  1. Verify Play Integrity / App Check                                  │
│  2. Enforce Rate Limiting & User Auth (JWT)                            │
│  3. Inject Environment API Key (Stored in Secrets Manager)             │
└───────────────────────────────────────────────────┬────────────────────┘
                                                    │ 2. Forward Request + Secure Key
                                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     AI Provider (Gemini / OpenAI)                      │
└────────────────────────────────────────────────────────────────────────┘

```

* **State Management:**
Define an explicit state machine inside the ViewModel to reflect attestation, proxy transport, and stream consumption:

```kotlin
sealed interface AiFeatureUiState {
    object Idle : AiFeatureUiState
    object AttestingDevice : AiFeatureUiState
    data class StreamingResponse(val partialText: String) : AiFeatureUiState
    data class Success(val completedText: String) : AiFeatureUiState
    sealed interface Error : AiFeatureUiState {
        object AttestationFailed : Error
        object RateLimitExceeded : Error
        data class NetworkError(val message: String?) : Error
    }
}

```

---

### 3. Recommended Technical Solutions

* **Tech Stack & Libraries:**
* **Device Attestation:** Google **Play Integrity API** (`com.google.android.play:integrity`) or **Firebase App Check** to verify that requests originate strictly from your untampered, genuine app binary installed via Google Play.
* **Networking & Streaming:** **Ktor Client** or **OkHttp** with `ServerSentEvent` support for lightweight asynchronous text streaming over HTTP/2.
* **Proxy Layer Options:** Serverless gateways such as **Cloudflare Workers**, **Firebase Cloud Functions**, or a **Kotlin/Ktor Microservice** deployed behind AWS API Gateway with keys held in AWS Secrets Manager or GCP Secret Manager.


* **Offline & Performance Strategy:**
* **Token Caching:** Cache short-lived signed proxy JWTs or Play Integrity verdict tokens in memory (`StateFlow` / Singleton scope) so subsequent prompt submissions don't trigger re-attestation overhead within the same session window.
* **Response Caching:** Store deterministic AI outputs (e.g., standard summary outputs) in a local **Room Database** indexed by payload hash to prevent redundant network calls.


* **Security & Platform Guardrails:**
* **Anti-Decompilation Reality:** Never rely on NDK native libraries (`.so` files), CMake, obfuscation tools (ProGuard/R8), or `BuildConfig` variables to store API keys. Reverse-engineering tools like JADX and Ghidra extract strings from native binaries within minutes.
* **TLS Certificate Pinning:** Implement Network Security Config with `pin-set` definitions (or OkHttp `CertificatePinner`) between the Android app and your Proxy Backend to prevent Man-In-The-Middle (MITM) proxy inspection.
* **Proxy Sanity Enforcements:** Ensure the proxy enforces strict user authorization (e.g., Firebase Auth / OAuth2 Bearer Tokens), caps maximum token generation limits per user account, and sanitizes input prompts to mitigate prompt injection vulnerabilities before hitting the AI API.


==== Personal card
A complete personal medical card consolidates basic demographics, clinical baseline, medical history, active treatments, and emergency contacts into a single standardized profile.Personal Demographics & IdentificationFull Name:Date of Birth / Age:Sex / Gender:Height:Weight: (Essential alongside height for medication dosing)Race / Ethnicity:Baseline Medical ProfileBlood Type: (e.g., O+, A-, etc.)Allergies: (Medications, food, latex, insect stings — with specific reaction details like anaphylaxis or hives)Active Medications: (Exact drug name, dosage, schedule, and purpose for prescription, OTC, and supplements)Adverse Drug Reactions: (Severe non-allergic side effects such as extreme nausea, cough, or muscle pain)Medical & Family HistoryHereditary & Family Diseases: (Heart disease, diabetes, cancer, or genetic risks in parents/siblings)Chronic Conditions & Diagnoses: (Ongoing diagnoses such as hypertension, asthma, thyroid disease)Surgical & Hospitalization History: (Past procedures, major traumas, dates, and surgical complications)Preventive Health & Baseline DataImmunization Record: (Recent vaccines and boosters, especially Tetanus/Tdap, Hepatitis, Influenza)Typical Baseline Vitals: (Usual resting blood pressure, resting heart rate, $SpO_2$)Lifestyle & Occupational Exposures: (Smoking/vaping status, alcohol frequency, workplace chemical or noise exposures)Emergency & Administrative ContextEmergency Contacts: (Name, relationship, primary phone number)Care Team: (Primary Care Physician, main specialists, preferred hospital network)Insurance & Identification Details: (Health insurance provider, policy/group ID numbers)Advance Directives: (Healthcare proxy name, living will status, organ donor preferences)