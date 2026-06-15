# Implementation Plan: Rich Text Formatting Toolbar

## Phase 1: Toolbar UI Component
- [x] Task: Create `RichTextToolbar` composable.
    - [x] Implement dark mode styling (charcoal background).
    - [x] Create circular icon buttons with active state highlights.
    - [x] Add vertical dividers between logical groups.
- [x] Task: Conductor - User Manual Verification 'Phase 1: Toolbar UI Component' (Protocol in workflow.md)

## Phase 2: Rich Text State Management
- [ ] Task: Integrate a rich text state controller (e.g., using a library like `RichText` or a custom `TextFieldValue` wrapper).
    - [ ] Implement toggles for Bold, Italic, Underline.
    - [ ] Implement Header toggle (H1, H2).
    - [ ] Map Paperclip button to the existing media selection flow.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Rich Text State Management' (Protocol in workflow.md)

## Phase 3: AddEntryScreen Integration
- [ ] Task: Replace standard `OutlinedTextField` with the new Rich Text editor.
    - [ ] Position the toolbar fixed at the top of the entry area.
    - [ ] Ensure HTML strings are correctly saved to the Room database.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: AddEntryScreen Integration' (Protocol in workflow.md)

## Phase 4: UI Refinements & Truncation
- [ ] Task: Implement "Show More" interaction for truncated cards.
    - [ ] Add smooth ellipsis transition.
    - [ ] Verify 3-line truncation logic in History and Archive screens.
- [ ] Task: Conductor - User Manual Verification 'Phase 4: UI Refinements & Truncation' (Protocol in workflow.md)
