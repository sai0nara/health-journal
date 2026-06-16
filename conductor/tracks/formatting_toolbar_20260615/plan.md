# Implementation Plan: Rich Text Formatting Toolbar

## Phase 1: Toolbar UI Component
- [x] Task: Create `RichTextToolbar` composable.
    - [x] Implement dark mode styling (charcoal background).
    - [x] Create circular icon buttons with active state highlights.
    - [x] Add vertical dividers between logical groups.
- [x] Task: Conductor - User Manual Verification 'Phase 1: Toolbar UI Component' (Protocol in workflow.md)

## Phase 2: Rich Text State Management
- [x] Task: Integrate a rich text state controller (e.g., using a library like `RichText` or a custom `TextFieldValue` wrapper).
    - [x] Implement toggles for Bold, Italic, Underline.
    - [x] Implement Header toggle (H1, H2).
    - [x] Map Paperclip button to the existing media selection flow.
- [x] Task: Conductor - User Manual Verification 'Phase 2: Rich Text State Management' (Protocol in workflow.md)

## Phase 3: AddEntryScreen Integration
- [x] Task: Replace standard `OutlinedTextField` with the new Rich Text editor.
    - [x] Position the toolbar fixed at the top of the entry area.
    - [x] Ensure HTML strings are correctly saved to the Room database.
- [x] Task: Conductor - User Manual Verification 'Phase 3: AddEntryScreen Integration' (Protocol in workflow.md)

## Phase 5: Bug Fixes
- [~] Task: Fix formatting toolbar theme (use MaterialTheme colors).
- [~] Task: Implement 'H' (Header) and Link button functionality.
- [~] Task: Debug and fix numbered list serialization/restoration issue.
- [ ] Task: Conductor - User Manual Verification 'Phase 5: Bug Fixes' (Protocol in workflow.md)
