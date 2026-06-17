# Specification: Rich Text and Truncation

## Overview
Enable rich text formatting (bold, italics, headers) for journal entries using Markdown. Implement 3-line truncation with ellipsis on list views (Main/Archive) and full expansion on detail view.

## Functional Requirements
- Store entries as raw Markdown strings.
- Parse Markdown into `AnnotatedString` for rendering in Compose.
- Main/Archive feed: Truncate to 3 lines, add ellipsis.
- Detail view: Expand all content.
- Support bold, italics, headers.

## Out of Scope
- Direct WYSIWYG editing toolbar (might be added later).
- OCR or advanced document processing.
