# Health Journal Technology Stack

## Core Technologies
- **Language:** [Kotlin](https://kotlinlang.org/) - The primary language for Android development, offering safety and expressiveness.
- **UI Framework:** [Jetpack Compose](https://developer.android.com/compose) - Modern declarative toolkit for building native UI using Material 3.
- **Database:** [Room](https://developer.android.com/training/data-storage/room) - SQLite abstraction layer for robust local data persistence.

## Architecture & Logic
- **Pattern:** MVVM (Model-View-ViewModel) - Ensures separation of concerns between UI, business logic, and data.
- **Data Serialization:** [Gson](https://github.com/google/gson) - Used with Room TypeConverters to persist multi-media collections.
- **Rich Text Rendering:** Native Jetpack Compose `AnnotatedString` combined with a custom `HtmlParser` utility utilizing `android.text.Html` for standard formatting support.
- **Repository Pattern:** Centralizes data access from local and potentially remote sources.
- **Background Tasks:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) - For reliable background synchronization with Google Drive.

## Specialized Integrations
- **Health Data:** [Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect) - Unified platform for accessing and sharing health/fitness data.
- **Cloud Storage:** [Google Drive API](https://developers.google.com/drive/api) - Used for user-controlled cloud backup and synchronization.
- **Authentication:** [Credential Manager](https://developer.android.com/training/sign-in/credential-manager) - Modern API for secure user authentication, including Google Sign-In.
- **Camera:** [CameraX](https://developer.android.com/training/camerax) - Jetpack library for simplified camera integration.

## Testing & Quality
- **Unit Testing:** JUnit 4, MockK - For robust business logic and ViewModel testing.
- **UI Testing:** Jetpack Compose Testing - For verifying UI behavior.
- **Reporting:** [Allure](https://allurereport.org/) - Comprehensive test reporting tool.
