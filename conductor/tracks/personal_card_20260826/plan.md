# Personal Card - Implementation Plan

## Phase 1: Data Layer Setup
- [x] Task: Create PersonalCard Room Entity with all embedded data classes
- [x] Task: Create PersonalCardDao with CRUD operations
- [x] Task: Add Gson TypeConverters for list fields
- [x] Task: Update JournalDatabase to version 12 with migration
- [x] Task: Create PersonalCardRepository
- [x] Task: Write unit tests for PersonalCardDao
- [ ] Task: Conductor - User Manual Verification 'Data Layer' (Protocol in workflow.md)

## Phase 2: ViewModel & Business Logic
- [ ] Task: Create PersonalCardViewModel with view/edit state management
- [ ] Task: Implement add/remove operations for list fields
- [ ] Task: Implement save/cancel with validation
- [ ] Task: Integrate with Google Drive sync via SyncWorker
- [ ] Task: Write unit tests for PersonalCardViewModel
- [ ] Task: Conductor - User Manual Verification 'ViewModel' (Protocol in workflow.md)

## Phase 3: UI - View Mode
- [ ] Task: Create PersonalCardScreen with card-based grid layout
- [ ] Task: Implement DemographicsCard (view mode)
- [ ] Task: Implement MedicalProfileCard (view mode)
- [ ] Task: Implement MedicalHistoryCard (view mode)
- [ ] Task: Implement EmergencyContactsCard (view mode)
- [ ] Task: Add top app bar icon in HistoryScreen
- [ ] Task: Write UI tests for PersonalCardScreen
- [ ] Task: Conductor - User Manual Verification 'UI View Mode' (Protocol in workflow.md)

## Phase 4: UI - Edit Mode
- [ ] Task: Implement edit mode toggle with save/cancel buttons
- [ ] Task: Create list management components (add/remove items)
- [ ] Task: Implement DemographicsCard (edit mode)
- [ ] Task: Implement MedicalProfileCard (edit mode)
- [ ] Task: Implement MedicalHistoryCard (edit mode)
- [ ] Task: Implement EmergencyContactsCard (edit mode)
- [ ] Task: Write UI tests for edit mode interactions
- [ ] Task: Conductor - User Manual Verification 'UI Edit Mode' (Protocol in workflow.md)

## Phase 5: Integration & Polish
- [ ] Task: Wire navigation route in MainActivity
- [ ] Task: Add haptic feedback for destructive actions
- [ ] Task: Test light/dark theme rendering
- [ ] Task: Test cloud sync flow end-to-end
- [ ] Task: Write integration tests
- [ ] Task: Conductor - User Manual Verification 'Integration' (Protocol in workflow.md)