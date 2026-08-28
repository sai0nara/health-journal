# Personal Card - Specification

## Overview
A Personal Card feature that consolidates user medical information into a single standardized profile. Users can view and edit their medical profile, demographics, medical history, and emergency contacts. Accessible via a new icon in the top app bar.

## Data Model

### PersonalCard Entity
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | String (PK) | Yes | UUID, singleton per user |
| `lastModified` | Long | Yes | For LWW sync conflict resolution |
| `isSynced` | Boolean | Yes | Cloud sync state |
| `syncStatus` | String | Yes | PENDING_SYNC / SYNCED |

### Demographics (embedded in PersonalCard)
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `fullName` | String | No | User's full name |
| `dateOfBirth` | String | No | ISO 8601 date format |
| `sex` | String | No | Male / Female / Other / Prefer not to say |
| `heightCm` | Double? | No | Height in centimeters |
| `weightKg` | Double? | No | Weight in kilograms |
| `raceEthnicity` | String | No | Free text |

### Medical Profile (embedded in PersonalCard)
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `bloodType` | String | No | O+, O-, A+, A-, B+, B-, AB+, AB- |
| `allergies` | List<String> | No | Medication, food, latex, insect sting allergies |
| `medications` | List<MedicationEntry> | No | Active medications |
| `adverseReactions` | List<String> | No | Non-allergic side effects |

### MedicationEntry (embedded data class)
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `name` | String | Yes | Drug name |
| `dosage` | String | Yes | e.g., "500mg" |
| `schedule` | String | Yes | e.g., "Twice daily" |
| `purpose` | String | No | Why prescribed |

### Medical History (embedded in PersonalCard)
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `hereditaryDiseases` | List<String> | No | Family disease history |
| `chronicConditions` | List<String> | No | Ongoing diagnoses |
| `surgicalHistory` | List<String> | No | Past procedures with dates |

### Emergency Contacts (embedded in PersonalCard)
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `contacts` | List<EmergencyContact> | No | Emergency contact list |

### EmergencyContact (embedded data class)
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `name` | String | Yes | Contact name |
| `relationship` | String | Yes | e.g., "Spouse", "Parent" |
| `phoneNumber` | String | Yes | Primary phone number |

## Functional Requirements

### FR-1: Personal Card Screen
- Navigate to Personal Card via top app bar icon
- Display data in card-based grid layout with sections:
  - Demographics Card
  - Medical Profile Card
  - Medical History Card
  - Emergency Contacts Card
- Each card shows summary when collapsed, full details when expanded

### FR-2: View/Edit Toggle
- Default state: View mode (read-only fields)
- Edit button toggles to Edit mode
- Edit mode shows input fields, dropdowns, and list management
- Save button persists changes and returns to View mode
- Cancel button discards changes and returns to View mode

### FR-3: List Management
- Allergies, Medications, Adverse Reactions, Hereditary Diseases, Chronic Conditions, Surgical History, and Emergency Contacts support add/remove items
- Add: Tap "+" to add new entry via dialog or inline form
- Remove: Swipe-to-delete or tap "x" on each item
- Empty states show helpful prompts ("No medications added yet")

### FR-4: Data Persistence
- Store PersonalCard in Room database as singleton entity
- Single row per user (id = "personal_card")
- All embedded data serialized via Gson TypeConverter

### FR-5: Cloud Sync
- PersonalCard syncs via Google Drive like JournalEntry
- LWW conflict resolution using `lastModified` timestamp
- Tombstone handling for sync consistency

### FR-6: Navigation Integration
- New icon in top app bar (person/user icon)
- Back arrow returns to previous screen
- Route: `"personal_card"`

## Non-Functional Requirements

### NFR-1: Privacy
- Medical data stored locally first
- Sync only when user is authenticated
- No analytics or tracking of medical data

### NFR-2: Performance
- Screen loads in <500ms
- Smooth scrolling with lazy loading for long lists

### NFR-3: Accessibility
- All fields labeled for screen readers
- Touch targets minimum 44x44px
- High contrast support for medical data

## Acceptance Criteria

1. User can navigate to Personal Card from top app bar
2. All four data sections display correctly in card grid
3. User can toggle between view and edit modes
4. All list fields support add/remove operations
5. Data persists locally in Room database
6. Data syncs to Google Drive when authenticated
7. Screen works correctly in light and dark themes
8. Unit tests cover ViewModel, Repository, and DAO
9. UI tests cover navigation, view/edit toggle, and list operations

## Out of Scope
- Preventive Health section (immunizations, baseline vitals, lifestyle)
- Insurance & Identification details
- Advance Directives
- Quick-access overview card
- PDF/QR code export of personal card
- Sharing personal card with others
