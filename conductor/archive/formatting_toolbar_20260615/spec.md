# Specification: Rich Text Formatting Toolbar

## Overview
Implement a visually sophisticated formatting toolbar for the journaling app, following a modern dark mode aesthetic. The toolbar will provide users with intuitive controls for styling their health journal entries.

## Design Pattern (Reference: image_0.png)
- **Aesthetic:** Charcoal/Dark Grey background matching the app's background.
- **Hit States:** Circular, highlighted with semi-transparent grey when active/selected.
- **Layout:** Horizontal stack with specific spacing and vertical divider lines.
- **Positioning:** Fixed at the top of the "Edit Journal Entry" screen.

## Functional Requirements
- **Formatting Options:**
    - `H`: Toggle between Header 1, Header 2, and Plaintext.
    - `B`: Bold text.
    - `I`: Italic text.
    - `U`: Underline text.
    - `List Icon (Ordered/Unordered)`: Toggle lists.
    - `Paperclip`: Trigger the existing media attachment flow.
    - `Link`: Insert/Edit hyperlinks.
    - `Clear`: Remove all formatting from the current selection/line.
- **Data Persistence:** Store formatted content as HTML strings in the Room database.
- **Rendering:** Utilize the existing `HtmlParser` for consistent rendering across screens.
- **Interaction:**
    - High-fidelity feedback for button taps.
    - Seamless transition between active editing and read-only modes.

## UX Behavior
- **Truncation:** Long journal entries in feed views (History/Archive) must be truncated to exactly 3 lines.
- **Indicators:** Use a smooth ellipsis (...) and a "Show More" interaction point to signal additional content.

## Out of Scope
- Custom font families or colors for specific text spans.
- Advanced document formatting (e.g., tables, alignment blocks).
