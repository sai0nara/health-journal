## Your task

Build an **LLMwiki** in this repository: an in-repo, agent-maintained knowledge base
of the durable answers about this codebase, kept honest by a deterministic lint and
repaired by the agent in the same commit as the code that invalidated it.

It is a knowledge base **for agents and the engineers who run them** — not general
service documentation, not a wiki for stakeholders, not a replacement for the
README.

Work in phases, in order. Do not skip Phase 0. Do not start writing pages before
Phase 2 is agreed.

---

## The contract — three rules that govern everything

1. **The agent owns the vault.** When a change touches code a page cites, that page
   is updated in the **same commit** as the code. There is no separate documentation
   change-set and no human doc-owner to hand off to. A human reviews the change like
   any other code review.
2. **Cite, don't copy.** Durable structure gets explained. Mutable values get *cited
   to the file that owns them*, never transcribed.
3. **The lint exits 0 before the turn ends.**

Rule 2 is load-bearing. The reasoning: **drift and hallucination come from copied
values going stale, not from explaining architecture.** Internalise this before you
write a single page.

| Treat as durable — explain it | Treat as mutable — cite the owning file |
|---|---|
| What a component is for, and its boundaries | URLs, hostnames, ports |
| How data flows through a request | Dependency and image versions |
| Precedence/ordering rules between components | Cloud account ids, registry paths |
| Where a kind of thing lives, and how to trace it | Environment names and per-env values |
| How to run something, and which switch selects what | Anything stored in a database or config service |
| Known gotchas, and why a workaround exists | Credentials — **never**, under any circumstances |
| Which neighbour systems exist and which direction calls flow | Replica counts, quotas, weights, thresholds |

When you catch yourself about to paste a value, stop and name the file that owns it
instead.

---

## Phase 0 — Read before you write

1. Read the repository's own documentation first: root README, any per-module
   READMEs, any existing agent-instruction file (`CLAUDE.md`, `AGENTS.md`, or
   similar), any architecture notes, any contributing guide.
2. Read the build configuration to learn the real module/package layout rather than
   guessing it.
3. Read the CI configuration to learn the real pipeline structure and which
   variables gate which jobs.
4. Skim the test tree(s). If there is more than one test stack, establish what
   distinguishes them — conflating two test stacks is one of the most common and
   most damaging documentation errors.

**Rule for this phase: the wiki complements the repo's existing docs and never
duplicates them.** Anything already well-maintained in a README or agent-instruction
file gets **linked**, not copied.

Produce a short written inventory of what you found before moving on.

---

## Phase 1 — Establish ground truth for the hard parts

Identify the two or three things about this repo that a newcomer reliably gets
wrong, and verify them **by reading code**, not by inference. Typical candidates:

- Which direction a dependency between two systems actually runs. Check for the
  absence of a client, not just the presence of a name — a name can appear in tests
  while the production code has no dependency at all.
- Where a piece of configuration is really owned, when several places mention it.
- Which of two similarly named test stacks a given command actually runs.

Record how you verified each one. These verifications become the most valuable
sentences in the vault, and they are the ones you must never write from memory.

---

## Phase 2 — Design the taxonomy

**Derive categories from this repository.** Do not impose a fixed list. Use the
following as a starting template and adapt it — rename, merge, drop, or add
categories so that each one answers a question someone actually asks:

```
<vault>/
├─ index.md          the catalog — every page listed exactly once
├─ service/          what this system is and how its core decision/flow works
├─ modules/          one page per build module or major component
├─ tests/            the test framework(s), how suites are selected, test data
│   └─ domains/      per-domain test notes, when there are enough to warrant it
├─ ci/              pipelines, reporting, CI-side integrations
├─ deploy/          charts, environments, deployment path
├─ integrations/    one page per neighbour system
└─ meta/            governance: the contract and the spec. Not content.
```

Structural invariants — these are not style preferences, the lint enforces them:

- **One page per subject.** Filenames lower-kebab-case.
- **Basenames unique vault-wide.** Links resolve by basename, never by path, so two
  pages sharing a name makes one unreachable.
- **Every page appears in `index.md` exactly once.**
- **One page has no inbound-link requirement** — designate a single root page (an
  overview) that every other page's footer points back toward.
- **`meta/` is exempt from the structural checks** because its documents are *about*
  the vault and contain example link syntax. Therefore `meta/` pages must be
  referenced by backticked path, never by wikilink — a wikilink into `meta/` could
  never resolve.

Aim for pages of roughly 30–80 lines. Prefer several focused pages over one large
one: a page you can hold in context is a page you can keep true.

**Present the proposed taxonomy and page list, and get agreement before writing.**

---

## Phase 3 — Write the governance pages first

Before any content page, write `meta/` — the contract the rest of the vault obeys.
Two documents, and the split matters:

- **The spec.** The rules themselves: what may go in, who publishes, how freshness
  is maintained, the accuracy gate. This is the document you cite.
- **The operational schema.** How to author and file a page *in this vault*: layout,
  page conventions, citation format, the lint workflow.

The schema **cites** the spec rather than restating it. Rules ratified in two places
drift apart; rules ratified once and cited cannot. If you deviate from the spec
anywhere, **record the deviation and its reasoning in the spec itself** as a table —
a documented departure is a decision, an undocumented one is drift.

---

## Phase 4 — Write the content pages

### Page template

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

Back to [[root-page]]
```

No YAML frontmatter. `Last updated:` must be on its own line in exactly that form,
because the lint parses it.

### Citation format — read this twice

```
- `path/from/repo/root.ext` — what this file is the source of.
```

Only the text **before the em-dash** is parsed as a citation path. Everything after
it is prose and is not path-checked — which is exactly where sibling filenames,
class names, XML tags and globs belong.

Consequences you must respect:

- **One path per bullet.** A bullet listing two backticked paths before the dash
  makes the second one parse as a repo-root path, and the lint reports it stale.
- **Cross-repository references use a full `https://` URL.** A backticked
  path-with-slashes is read as a local path and false-flags.
- **Cite only what a fresh clone has.** A path that exists on your machine but is
  not committed resolves for you and misleads everyone else — they cannot tell
  whether the page is stale or the file was never shared. The single exception is
  environment setup, where naming a file the reader must create is the point: say
  what to create, and cite the tracked template it comes from.

### Neighbour systems

A page describing an interaction with another system must link **outward** to that
system's own source of truth, not only inward. This vault owns *how this repo uses*
the neighbour; the neighbour's own repository owns the detail. State the direction
of the dependency explicitly, and say so when this repo holds no client at all.

### Accuracy

**A resolving path does not make the sentence around it true.** Read the code before
making a claim. Prefer silence to the present tense about something that may have
been removed. If you cannot verify a claim, either verify it or leave it out — do
not hedge it into the page.

Before finishing each page, check it against this gate:

1. No copied mutable values; they are cited instead.
2. Every cited path exists, and is committed.
3. No invented entities — no channel, URL, or environment name you did not verify.
4. Operational claims match the real setup, not a plausible-sounding alternative.
5. No one-off or task-specific debris.
6. `Last updated:` and `## Sources` both present.
7. Outward link present on any page describing another system.

---

## Phase 5 — Build the lint

Write a script that checks the vault deterministically. **Standard library and `git`
only. No AI, no network.** It **detects and never edits content** — that division is
what makes it trustworthy without review.

Eight findings. Four are failures because each is a pointer that resolves to nothing
for the reader; four are advisory.

| Level | Meaning | Exit 1 |
|---|---|---|
| `STALE` | a cited path no longer exists | yes |
| `UNTRACKED` | a cited path exists locally but is not committed | yes |
| `BROKEN` | a wikilink points at a page that does not exist | yes |
| `DUPLICATE` | two pages share a basename, so one is unreachable | yes |
| `REVIEW` | a cited source's last commit is newer than the page's `Last updated:` | no |
| `ORPHAN` | nothing links to the page | no |
| `INDEX` | page missing from the catalog, or catalogued but gone | no |
| `FOOTER` | page missing its back-navigation footer | no |

Implementation requirements:

- Expose the citation parser as a **named, importable function**. The hook in
  Phase 6 must call *this* function rather than reimplementing it, so the two can
  never disagree about what counts as a citation.
- Parse only the text before the em-dash in a `## Sources` bullet.
- Treat a token as a path only if it is plausibly one. Skip anything starting with a
  URL scheme, and skip tokens containing `@` (cross-repo refs never resolve
  locally). A slash-bearing token whose first segment looks like a dotted hostname
  is a namespaced key, not a path.
- Support glob citations: a glob resolves if at least one file matches; zero matches
  keeps the stale signal.
- `UNTRACKED` must test **tracked-ness**, not existence on disk. A directory counts
  as tracked when it contains at least one tracked file.
- Apply the structural exemptions from Phase 2: the root page needs no inbound link
  and no footer; the catalog needs no inbound link, no footer, and no self-entry,
  and must be excluded as a *source* of inbound links — otherwise it references
  everything and `ORPHAN` can never fire; `meta/` is exempt from structure but not
  from freshness.
- `DUPLICATE` must be reported rather than silently resolved. A basename collision
  would otherwise drop one page out of the structural checks entirely, losing
  coverage precisely where it is needed.

**Ship the lint with its own tests**, and document how to run them without assuming
a test framework is installed. The structural exemptions are easy to break by
accident, which is exactly why they need tests.

---

## Phase 6 — Wire the automation

Two end-of-turn hooks, plus one line in the agent-instruction file.

**Hook A — name the invalidated pages.**

- Build a page → cited-paths map by calling the **lint's own** citation parser.
- Intersect it with the paths this turn changed: tracked modifications *and* new
  untracked files.
- Emit the affected page names, each with the citation that matched, plus the scoped
  instruction below.
- Stay **silent** when only vault files changed — the agent is already editing the
  wiki.
- **Always exit successfully.** A failing end-of-turn hook interferes with the turn.
- Provide environment-variable overrides for the repo root and the changed-path list
  so the hook can be tested against a fixture.

Two traps worth avoiding explicitly:

- Do not derive changed paths by stripping a fixed number of characters from
  porcelain status output. Renames emit two records, and the second has no status
  prefix, so a blind strip corrupts the filename. Use a plain name-only diff plus a
  separate listing of untracked files.
- Do not reimplement citation parsing in the hook. Import it.

**The instruction the hook emits** — the wording matters, because a vague
instruction produces vague edits:

> This turn changed code that N page(s) cite. You own the vault. For each page:
> re-read the changed source **and** the page, then edit **only** claims that are now
> false or incomplete. Bump `Last updated:` **only** on pages you actually edited —
> bumping without re-reading launders a stale claim past the freshness check. If a
> change does not affect any claim (a rename, formatting, a test tweak), leave the
> page alone and say so. Stage wiki edits alongside the code, then confirm the lint
> still exits 0.

**Hook B — report vault health.** Run the lint and surface its findings.

**Agent-instruction file.** Add a section telling the agent that when the wiki is in
scope it must read the operational schema **first**, because that is the contract.
Also state the three rules, the lint command, and the one-line content boundary.
That single pointer is what turns a directory of markdown into a protocol —
without it, nothing here is discoverable.

Register both hooks in the **committed** configuration, so every engineer on the
repository gets them with no personal setup.

---

## Phase 7 — Verify, then report honestly

1. Run the lint. It must exit 0. Fix what it names.
2. Run the lint's own tests. They must pass.
3. Exercise Hook A against a fixture path that you know is cited, and confirm it
   names the expected pages.
4. Confirm every page appears in the catalog exactly once, and that the counts you
   state anywhere match reality — generate them, don't count by hand.
5. Re-read the two or three verified claims from Phase 1 and confirm the pages say
   what you verified.

Then report what you built, including **what it does not do**:

- **The lint checks pointers, not prose.** Every level is a structural or path fact.
  A page can report zero failures and still assert something false in a sentence.
  Say this out loud; it is the system's real ceiling.
- **Any advisory findings you are leaving behind**, and why they are not fixable
  mechanically. `REVIEW` means *re-read the source and confirm* — a page whose date
  is bumped without re-reading is worse than one that reports `REVIEW`.
- **Set expectations honestly if asked whether this helps.** A compiled knowledge
  base mainly reduces *cost* — fewer tool calls, fewer tokens, less wall-clock, and
  the ability to start a fresh session per task instead of carrying a long
  conversation forward to preserve context. It does not reliably make a capable
  agent *more correct* on well-scoped questions it could reconstruct from source
  given enough budget. Claim the cost benefit; don't oversell accuracy.

---

## Do not

- Transcribe a mutable value instead of citing the file that owns it.
- Bump `Last updated:` on a page you did not actually edit and re-verify.
- Cite a path that is not committed.
- Put an aliased wikilink inside a table cell — the escaped pipe breaks link parsing.
  Put it in prose.
- Add screenshots, diagrams-as-images, or any binary asset. This is an agent
  knowledge base; heavy files bloat the repository and serve no reader here. Link
  out to wherever such assets already live.
- Create a page about a single task, ticket, or investigation. Pages are durable
  subjects.
- Duplicate content that the README or the agent-instruction file already maintains.
- Assert an entity — a channel, a URL, an environment name — that you did not verify
  against its source.
- Paste a credential, even one you found in a committed file.
- Use a wikilink to reach a `meta/` page.
- Conflate two similarly named things (two test stacks, two config layers) because
  their names look alike.

## Definition of done

- [ ] Inventory of existing docs written; nothing duplicated from them.
- [ ] Taxonomy agreed before pages were written.
- [ ] `meta/` spec and operational schema written; schema cites the spec; any
      deviation recorded with its reasoning.
- [ ] Every page: title, one-line description, `Last updated:`, body, `## Sources`,
      back-navigation footer.
- [ ] Every page in the catalog exactly once; basenames unique vault-wide.
- [ ] Every outward-facing page links to the neighbour's own source of truth.
- [ ] Lint implemented, all eight levels, stdlib + `git` only, never edits content,
      exposes an importable citation parser.
- [ ] Lint has its own tests, and they pass; how to run them is documented.
- [ ] Both end-of-turn hooks implemented and registered in committed configuration.
- [ ] Hook A imports the lint's parser and exits successfully in all cases.
- [ ] Agent-instruction file points at the operational schema first.
- [ ] Lint exits 0.
- [ ] Limitations reported, including that the lint cannot check prose.
