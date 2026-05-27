# Specification: Enhanced Authentication, Sorting, and Search

## Goal
Implement a more flexible authentication mechanism for Google Drive, and enhance the Journal History screen with sorting and search capabilities.

## Objectives
- **Legacy Authentication:** Enable users to log in to Google Drive using traditional username and password credentials (using App Passwords or relevant OAuth flows if necessary for Drive API).
- **Entry Sorting:** Provide users with the ability to sort the list of journal entries by "Date Added" (Ascending/Descending).
- **Keyword Search:** Implement a search bar to filter journal entries based on keyword matches in their descriptions.

## Technical Considerations
- **Auth:** While the modern Credential Manager is preferred, we will investigate and implement a secure path for credential-based entry if required, ensuring it integrates with the existing `DriveServiceHelper`.
- **Database:** Leverage Room's `@Query` capabilities for efficient sorting and keyword filtering (e.g., using `LIKE` operator).
- **UI:** Integrate a standard Material 3 search bar and a sort menu (e.g., in the TopAppBar) on the History screen.

## Success Criteria
- User can successfully authenticate with Drive using credentials.
- Journal list can be toggled between Ascending and Descending date orders.
- Users can type in a search bar and see the list filter in real-time.
