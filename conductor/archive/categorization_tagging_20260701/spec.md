# Specification: Categorization and Tagging (Illness, Checkup, Doctor, Exercises)

## 1. Overview
The goal of this track is to introduce structured categorization and tagging to the health journal. This allows users to quickly filter historical medical data without relying solely on text-based search.

## 2. User Experience (UX)
### 2.1 Tagging an Entry
- **Location:** `JournalDetailScreen` (Creation/Editing).
- **UI Component:** A horizontal row of Material 3 `FilterChip` components.
- **Interaction:** Tapping a chip toggles the tag. The UI must update instantly.
- **Accessibility:** Minimum 48dp x 48dp hit boxes for chips.

### 2.2 Filtering Entries
- **Location:** `HistoryScreen` and `ArchiveScreen`.
- **UI Component:** A scrollable horizontal row of `FilterChip` components appearing below the search input.
- **Interaction:** Selecting a chip instantly filters the entries.
- **Combined Filtering:** Users can combine a text query with one or more selected tags.
- **Visuals:** Use `Modifier.animateItemFast()` (or equivalent) for smooth entry filtering.

## 3. Technical Architecture
### 3.1 Data Model (Room)
To support many-to-many relationships (one entry can have multiple tags), we will use a cross-reference table.

- **JournalEntryEntity:** Existing entity.
- **EntryTagCrossRef:** New entity with primary keys `(entryId, tag)`.
  - `entryId`: String (foreign key to `journal_entries.id`).
  - `tag`: String (the tag value, e.g., "ILLNESS").

### 3.2 Business Logic
- **ViewModel:** Maintain `searchQuery: StateFlow<String>` and `selectedTags: StateFlow<Set<JournalTag>>`.
- **Repository:** Combine these states to execute a dynamic SQL query in the DAO.

### 3.3 DAO Query
The query must handle both text search and optional tag filtering in a single trip.
```sql
SELECT * FROM journal_entries 
WHERE isArchived = :isArchived 
AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
AND (:hasFilters = 0 OR id IN (SELECT entryId FROM entry_tag_cross_ref WHERE tag IN (:tags)))
ORDER BY lastModified DESC
```

## 4. Synchronization
- **Payload:** Tags should be represented as a string array in the cloud payload (e.g., `tags: ["ILLNESS", "DOCTOR"]`).
- **Strategy:** Last-Write-Wins on the entire tag array.
- **Trigger:** Updating tags must set `syncStatus = PENDING_SYNC` and update `lastModified`.
