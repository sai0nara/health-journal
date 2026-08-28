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

A complete personal medical card consolidates basic demographics, clinical baseline, medical history, active treatments, and emergency contacts into a single standardized profile.

* **Personal Demographics & Identification:**
  * Full Name
  * Date of Birth / Age
  * Sex / Gender
  * Height
  * Weight (essential alongside height for medication dosing)
  * Race / Ethnicity
* **Baseline Medical Profile:**
  * Blood Type (e.g., O+, A-, etc.)
  * Allergies (medications, food, latex, insect stings — with specific reaction details like anaphylaxis or hives)
  * Active Medications (exact drug name, dosage, schedule, and purpose for prescription, OTC, and supplements)
  * Adverse Drug Reactions (severe non-allergic side effects such as extreme nausea, cough, or muscle pain)
* **Medical & Family History:**
  * Hereditary & Family Diseases (heart disease, diabetes, cancer, or genetic risks in parents/siblings)
  * Chronic Conditions & Diagnoses (ongoing diagnoses such as hypertension, asthma, thyroid disease)
  * Surgical & Hospitalization History (past procedures, major traumas, dates, and surgical complications)
* **Preventive Health & Baseline Data:**
  * Immunization Record (recent vaccines and boosters, especially Tetanus/Tdap, Hepatitis, Influenza)
  * Typical Baseline Vitals (usual resting blood pressure, resting heart rate, $SpO_2$)
  * Lifestyle & Occupational Exposures (smoking/vaping status, alcohol frequency, workplace chemical or noise exposures)
* **Emergency & Administrative Context:**
  * Emergency Contacts (name, relationship, primary phone number)
  * Care Team (Primary Care Physician, main specialists, preferred hospital network)
  * Insurance & Identification Details (health insurance provider, policy/group ID numbers)
  * Advance Directives (healthcare proxy name, living will status, organ donor preferences)