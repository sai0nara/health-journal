# Health Journal Product Guidelines

## Tone & Voice
- **Clinical & Precise:** Use language that is professional, accurate, and objective. Avoid overly emotional or flowery prose. Communications should feel reliable and medical-grade where appropriate, while remaining accessible.

## UX Principles
- **Frictionless Entry:** Prioritize speed and ease of use for logging events. The goal is to minimize the "time to log" so users remain consistent.
- **Safety Nets & Undo:** Provide immediate 'Undo' actions for potentially destructive or organizational tasks (like archiving/deletion) to reduce user anxiety.
- **Physical Feedback:** Utilize platform-native haptics to provide tactile confirmation of successful gestures and important actions.
- **Offline-First:** Ensure all core functionality (logging, history viewing) works without an internet connection. Sync should happen in the background when connectivity is available.
- **Visual Clarity:** Present health data and insights clearly. Use charts and summaries that are easy to interpret at a glance.

## Design System
- **Material 3 (Native):** Follow the latest Android design standards. Use Material 3 components, dynamic color, and native navigation patterns to provide a familiar and high-quality Android experience.
- **Medical Color System (Semantic Tokens):** All UI colors must be referenced via `MaterialTheme.colorScheme` roles — never absolute colors. Two fixed palettes are defined:
    - **Light ("Medical Standard"):** off-white background (#F8F9FA), pure white surfaces, trust-blue primary (#0A66C2), healing-teal secondary (#20C997).
    - **Dark ("Eye-strain Reduction"):** deep charcoal background (#121212), elevated-gray surfaces (#1E1E1E), slightly desaturated blue primary (#4A90E2) to reduce eye strain.
- **System Theme Follow:** The app follows the OS light/dark preference with instant re-theming; an in-app override and Material You dynamic color are intentionally out of scope.

(Note: the existing "dynamic color" phrase in the Material 3 bullet predates this system; the Medical Color System entry takes precedence.)

## Privacy & Security Communications
- **Cloud-Secure:** Emphasize that user data is securely backed up and synchronized using their personal Google Drive. Communicate the reliability and security of this approach, ensuring users feel confident that their data is safe and accessible across devices.
