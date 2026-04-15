# Health Journal Project Overview

This is an Android health journal application designed for personal event logging and health metric tracking. It leverages modern Android development practices, including Jetpack Compose for the UI, Room for local data persistence, and follows the MVVM (Model-View-ViewModel) architectural pattern.

## Key Technologies
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Database:** Room Persistence Library
- **Navigation:** Jetpack Navigation Compose
- **Architecture:** MVVM with Repository pattern
- **Asynchronous Programming:** Kotlin Coroutines and Flow

## Project Structure
- `app/src/main/java/com/example/healthjournal/`
    - `MainActivity.kt`: Entry point, sets up navigation and provides ViewModels.
    - `data/`: Contains the data layer.
        - `JournalRepository.kt`: Orchestrates data between the local database and the UI.
        - `local/`: Room database components (`JournalDao`, `JournalDatabase`, `JournalEntry`).
    - `ui/`: Contains the UI layer.
        - `screens/`: Individual Compose screens (`AddEntryScreen`, `HistoryScreen`).
        - `theme/`: App-wide Compose themes and styling.
    - `viewmodel/`: Contains ViewModels for business logic and UI state management.
- `Docs/`: Project documentation.
    - `PRD.md`: Product Requirements Document detailing the vision and features.
    - `Plan.md`: Implementation and development plan.
    - `Stories.md`: User stories and functional requirements.

## Building and Running
The project uses Gradle for build management.

### Key Commands:
- **Build the project:** `./gradlew build`
- **Install debug build on connected device/emulator:** `./gradlew installDebug`
- **Run unit tests:** `./gradlew test`
- **Run instrumented tests:** `./gradlew connectedAndroidTest`
- **Clean build artifacts:** `./gradlew clean`

## Development Conventions
- **UI:** Exclusively use Jetpack Compose for building user interfaces.
- **State Management:** Use `StateFlow` in ViewModels to expose UI state.
- **Dependency Management:** Kotlin DSL (`.gradle.kts`) is used for all Gradle configuration files.
- **Data Model:** `JournalEntry` uses UUID-based string IDs for future synchronization compatibility (e.g., Google Drive sync as mentioned in `PRD.md`).
- **Architecture:** Maintain strict separation between the UI, business logic (ViewModel), and data access (Repository).

## Roadmap Highlights (from PRD)
- [x] Basic entry logging and history view.
- [ ] Integration with Google Drive for cloud synchronization.
- [ ] Health Connect integration for automatic metric importing (steps, heart rate, sleep).
- [ ] AI-driven health insights based on logs and metrics.
- [ ] Export functionality (CSV/XML).
