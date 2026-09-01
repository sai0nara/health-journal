# Domain & Media

> The pure domain logic and validation under `domain/`, and the media/image compression service under `media/`.

Last updated: 2026-09-01

## What lives here

Two adjacent but distinct concerns share this page because each is a compact package:

- **`domain`** and **`domain/validation`** — pure, framework-light logic: value
  formatting and unit conversion for measurements, date handling, goal validation,
  and the demographics/measurement validation use cases (`ValidateWeightUseCase`,
  `ValidateHeightUseCase`, `ValidateDateOfBirthUseCase`) that return a
  `ValidationResult`.
- **`media`** — the single `MediaCompressionService`, which compresses images before
  they are attached to journal entries.

The domain layer has no Android/UI dependencies and is exercised by the JVM unit
suite; the media service sits closer to the Android platform.

## Cross-references

- [[data-layer]] — the formatters/unit logic is reused by display and persistence.
- [[unit-tests]] — domain validation and formatting are covered by the JVM stack.
- [[ui-layer]] — measurement screens render values through the domain formatters.

## Sources

- `app/src/main/java/com/example/healthjournal/domain/validation/ValidateWeightUseCase.kt` — a validation use case.
- `app/src/main/java/com/example/healthjournal/media/MediaCompressionService.kt` — image compression.

Back to [[overview]]
