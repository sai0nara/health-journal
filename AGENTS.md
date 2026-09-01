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
- `wiki/`: LLMwiki vault for persistent knowledge management.
    - `service/`, `modules/`, `tests/`, `integrations/`: knowledge pages.
    - `meta/`: contract (`spec.md`) and operational schema (`schema.md`).
    - `hooks/`: end-of-turn hook scripts; `lint.py` is the vault checker.
- `.opencode/plugins/wiki.js`: registers the vault hooks in opencode.

## Building and Running
The project uses Gradle for build management.

### Key Commands:
- **Build the project:** `JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home ./gradlew build`
- **Install debug build on connected device/emulator:** `JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home ./gradlew installDebug`
- **Run unit tests:** `JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home ./gradlew test`
- **Run instrumented tests:** `JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home ./gradlew connectedAndroidTest`
- **Clean build artifacts:** `JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home ./gradlew clean`

## Development Conventions
- **UI:** Exclusively use Jetpack Compose for building user interfaces.
- **State Management:** Use `StateFlow` in ViewModels to expose UI state.
- **Dependency Management:** Kotlin DSL (`.gradle.kts`) is used for all Gradle configuration files.
- **Data Model:** `JournalEntry` uses UUID-based string IDs for future synchronization compatibility (e.g., Google Drive sync as mentioned in `PRD.md`).
- **Architecture:** Maintain strict separation between the UI, business logic (ViewModel), and data access (Repository).

## Roadmap Highlights (from PRD)
- [x] Basic entry logging and history view.
- [x] Integration with Google Drive for cloud synchronization.
- [ ] Health Connect integration for automatic metric importing (steps, heart rate, sleep).
- [ ] AI-driven health insights based on logs and metrics.
- [ ] Export functionality (CSV/XML).

## Wiki (LLMwiki)
The operational contract for the vault lives in `wiki/meta/schema.md`; the full
contract and rationale is `wiki/meta/spec.md`. Read the operational schema
before modifying code or wiki content.

Three rules govern every change:
1. **Agent owns the vault** — when a change touches source that a wiki page
   cites, update the page and commit both together.
2. **Cite, don't copy** — pages explain durable structure and cite mutable
   values to the owning file via a backticked relative path such as
   `` `app/src/main/java/.../File.kt` ``; never transcribe a value into a page.
3. **The lint exits 0 before the turn ends** — run it before finishing.

Commands:
- Lint: `python3 wiki/lint.py`
- Hook self-tests: `python3 wiki/hooks/test_hooks.py`
- Lint self-tests: `python3 wiki/test_lint.py`

Content boundary: the wiki describes this repo's architecture and behavior.
No secrets, personal data, or issue-tracker references go in wiki pages. New
pages must follow the structure in `wiki/meta/schema.md`. The hooks registered
in `.opencode/plugins/wiki.js` run at the end of each turn: they name pages
affected by changed code and report vault health.
