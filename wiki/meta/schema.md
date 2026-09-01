# Operational Schema

> How to author, name, file, and maintain a page in this vault. Cites the contract; it does not restate it.

Last updated: 2026-09-01

The rules that govern the vault live in `wiki/meta/spec.md`. This page is the *how*;
the contract is the *why*. Where this page and the spec disagree, the
spec wins — fix this page.

## Layout

```
wiki/
├─ index.md                 catalog — every page listed exactly once (also the root page)
├─ service/                 what the app is and how its core flow works
├─ modules/                 one page per major component or package area
├─ tests/                   the two test stacks and how each is selected
├─ integrations/            one page per neighbour system (Google Drive, Health Connect)
└─ meta/                    governance: spec + schema. Not content.
```

## File one page per subject

- One page per subject. Filenames are **lower-kebab-case** (`google-drive.md`, not
  `google_drive.md`).
- Basenames are unique vault-wide. Links resolve by basename, so two pages sharing a
  name makes one unreachable.
- In your working copy, decide where a subject belongs *before* writing, then keep
  it there.

## Page template

```markdown
# Title

> One-line description of what this page answers.

Last updated: YYYY-MM-DD

## <Section>

Body. Explain structure and intent. Name owning files instead of pasting values.

## Cross-references

- [[other-page]] — why a reader would go there next.

## Sources

- `path/from/repo/root.ext` — what this file is the source of.

Back to [[overview]]
```

- No YAML frontmatter.
- `Last updated:` must be on its own line in exactly that form — the lint parses it.
- The footer `Back to [[overview]]` is required on every content page. The root page
  (`index.md`) and `meta/` pages carry no footer.

## Citation format

```
- `path/from/repo/root.ext` — what this file is the source of.
```

Only the text **before the em-dash** is parsed as a citation path. Everything after
it is prose and is not path-checked — that is where sibling filenames, class names,
XML tags and globs belong.

Consequences:

- **One path per bullet.** A bullet with two backticked paths before the dash makes
  the second one parse as a repo-root path and the lint reports it stale.
- **Cross-repository references use a full `https://` URL.** A backticked
  slash-path is read as a local path and false-flags.
- **Cite only what a fresh clone has.** A path that exists locally but is not
  committed resolves for you and misleads everyone else. The single exception is
  environment setup: name the file to create, and cite the tracked template it comes
  from.

## Wikilinks

- Use `[[basename-of-page]]` to link to another page. The basename is the filename
  without the `.md` extension.
- Do **not** wikilink into `meta/`. `meta/` pages are exempt from the structural
  checks (their documents contain example link syntax) and are referenced by
  backticked path instead, e.g. `wiki/meta/spec.md`.
- Do not put an aliased wikilink inside a table cell — the escaped pipe breaks link
  parsing. Put it in prose.

## The lint workflow

Run `wiki/lint.sh` from the repo root. It must exit 0 before the turn ends.

- If it names `STALE`, `UNTRACKED`, `BROKEN`, or `DUPLICATE` findings, those are
  failures: each is a pointer that resolves to nothing for the reader. Fix them.
- `REVIEW`, `ORPHAN`, `INDEX`, and `FOOTER` are advisory.
- The lint never edits content. It detects; the agent repairs in the same commit.

The lint's own tests run with `wiki/lint-test.sh` (see the header of `wiki/lint.sh`
for the exact command). It requires only the standard library and `git`; no test
framework is installed.

## When code changes

Follow the contract (rule 1): re-read the changed source **and** the page, edit only
claims that are now false or incomplete, bump `Last updated:` only on pages you
actually edited, and stage the wiki edits alongside the code in the same commit.

## Sources

- `wiki/meta/spec.md` — the contract this schema implements.
- `wiki/lint.sh` — the deterministic checker that enforces these conventions.
