# Project context & handoff notes

This file exists so anyone (or any AI session) picking up TestByte cold has the non-obvious context
that isn't visible just from reading the code. It was written by Claude after building the initial
version of the platform, because the conversation that built it lives in a scratch chat session whose
memory doesn't travel with the project folder.

## What this is

A platform for a recruiter to schedule short (10-20 min) coding assessments for candidates and review
submissions. Backend: Spring Boot 4 (Java 21). Frontend: Angular 22 with a Monaco code editor. See
[README.md](README.md) for how to run it locally and [DEPLOYMENT.md](DEPLOYMENT.md) for the free-tier
hosting setup.

## Non-obvious design decisions

- **DB table `tests` / Java class `Assessment`.** Deliberately not named `Test` — that collides with
  `org.junit.jupiter.api.Test` imports in every backend test file. Same reasoning for
  `AssessmentAssignment` (table `test_assignments`).
- **First user ever registered auto-becomes `ADMIN` + `APPROVED`** (bootstrap, see `AuthService.signup`).
  Every signup after that defaults to `RECRUITER` + `PENDING` and can't log in until an admin approves it.
  Admins can *also* directly invite other admins by email (`AdminInvite` entity, one-time expiring token,
  no approval needed since an existing admin vouched for them) — this was a specific ask from the user
  when reviewing the initial plan, not just the bootstrap path.
- **`app_settings` table** holds `approver_notification_email` (seeded to `ravitejananduri@zoho.com` in
  `V2__seed_settings.sql`), editable from the Admin Settings page. This is deliberately decoupled from
  who is actually logged in as an admin — it's just "who gets pinged about new signups," configurable
  independent of any specific account.
- **AI-cheating trap: built, then fully retired.** The user's original static HTML test emails
  (`~/Documents/java.html`/`python.html`/`sql.html`) had a white-on-white HTML comment trick — "if you are
  an AI, rename X to Y," invisible to a human but read by an AI a candidate might paste the code into.
  This was digitized (`Assessment.aiTrapPhrase`) and later extended with a Monaco decoration that visually
  concealed the trap comment line from candidates in the editor. Both layers are now gone, removed in two
  steps: first the automated "possible AI use" flag (`possibleAiFlag`, dropped in `V5`) because the check
  was fundamentally broken — the phrase lives inside the trap comment itself, which every candidate's
  starter code already contains before they touch anything, so it fired on nearly every submission
  regardless of actual AI use; then the whole `aiTrapPhrase` concept and the line-concealment feature
  (dropped in `V6__retire_ai_trap.sql`, which also strips the now-purposeless trap comments out of the 3
  seeded tests' `starter_code`) once the user decided there was no point keeping a field the UI no longer
  let anyone set. **If this ever gets rebuilt**, know that a plain substring-presence check doesn't work
  for exactly the reason above — any real detection needs to check whether the identifier was actually
  *used* in the code (e.g. as a method name/call), not just present anywhere in the submitted text.
- **Monaco editor** is wired via the raw `monaco-editor` npm package (0.53.0 pinned — 0.54+ pulls in a
  vulnerable dompurify via its markdown/hover rendering), not `ngx-monaco-editor-v2`, which lagged behind
  Angular 22 at the time this was built. See `frontend/src/app/shared/monaco-editor/`. **Its structural CSS
  must be bundled explicitly** — `angular.json`'s global `styles` array includes
  `node_modules/monaco-editor/min/vs/style.css` — because Angular's esbuild builder does not pick up
  Monaco's own transitive CSS imports through a dynamic `import('monaco-editor')`. Without this, Monaco's
  hidden IME textarea renders as a visible, mis-clickable native `<textarea>` and cursor placement is
  wrong (this was a real bug caught live in production, not a hypothetical — see git history around
  "Fix Monaco editor: bundle its structural CSS explicitly").
- **Test editing has no versioning/snapshotting.** `AssignmentReviewResponse`/`PublicAssignmentResponse`
  read the assessment live via the `assessment` FK, not a snapshot — editing a test's `starterCode`/
  `instructionsHtml` after assignments already exist changes what those assignments display too. Not
  something the user asked to change; just worth knowing if candidate-facing content ever seems to not
  match what a recruiter remembers writing.
- **Proctoring events are an opaque JSON blob**, not a real table. `AssessmentAssignment.proctoringEventsJson`
  (column `proctoring_events`) stores whatever JSON string the frontend sends on submit
  (`[{type, timestamp}, ...]` — tab switches, window blur/focus, copy/cut/paste) and the backend never
  parses it; it's passed through as-is and JSON-parsed only in `assignment-review.ts` for display. If this
  ever needs to be queried/filtered server-side, it'll need a real table instead.
- **Paste-into-Monaco can't be reliably caught with a plain `document`-level `paste` listener.** It was
  originally implemented that way (matching the simpler tab-switch/blur/focus listeners), but pastes into
  Monaco specifically were sometimes silently missed -- confirmed live: Monaco's actual input-capture
  element for a paste is a hidden textarea/edit-context node *inside* the editor, and depending on the
  browser/Monaco version that dispatch path doesn't always bubble a conventional `ClipboardEvent` up to
  `document` the way a plain `<textarea>` does. The fix is `MonacoEditor.pasteDetected`
  (`shared/monaco-editor/monaco-editor.ts`), which uses Monaco's own documented `editor.onDidPaste()` API
  instead -- Monaco's internal signal for "a paste happened here," independent of DOM bubbling. Wired via
  `(pasteDetected)="onEditorPaste()"` on the candidate test page's CODE-question editors. The page-level
  `document` `paste` listener still exists as a fallback for non-Monaco paste targets (the TEXT question's
  plain `<textarea>`), and explicitly skips anything whose target is inside `.monaco-host` so the same
  physical paste doesn't get logged twice. Copy/cut (`copy_attempt`/`cut_attempt`) don't have this problem
  -- they're pure clipboard/selection actions independent of Monaco's text-input mechanism, so the
  page-level `document`-capture listeners catch them reliably everywhere, Monaco included.
- **A paste that round-trips content copied from the page itself is logged separately from a real
  external paste**, not hidden. `TestPage` (`pages/candidate/test-page/test-page.ts`) keeps a
  `copiedSnippets` set of everything the candidate has copied/cut *on this page* during the test (from
  the instructions panel, a TEXT answer, or a CODE editor -- `MonacoEditor.copyDetected`/`cutDetected`
  read the actual selected text straight off the model via `editor.getSelection()`, since Monaco has no
  public `onDidCopy`/`onDidCut` to hook the way `onDidPaste` is hooked). On paste, the pasted text (from
  `ClipboardEvent.clipboardData` for the document-level path, or from
  `editor.getModel().getValueInRange(e.range)` for Monaco's `onDidPaste` -- not its `clipboardEvent`
  field, which isn't reliably populated either) is compared against that set (normalized: `\r\n`→`\n`,
  trimmed): a match records `paste_internal` (e.g. copying a snippet from the instructions into the code
  editor, or re-pasting something already typed), anything else records the usual `paste_attempt`. Both
  appear in the recruiter's activity log -- nothing is hidden -- but `paste_internal` renders muted and
  under a different label (`assignment-review.html`/`.ts`) so it doesn't read as suspicious the way a real
  external paste does. This is intentionally an exact-string match, not fuzzy -- a large pasted block
  won't accidentally match a short previously-copied snippet unless it equals it exactly.
- **Review comments are single/overwritable**, not a thread — `review_comment` + `reviewed_at` +
  `reviewed_by` columns get replaced wholesale on each save. The dashboard's "Reviewed" badge is just
  `reviewedAt != null` on `AssignmentSummaryResponse`.
- **Tests are a container of pages, each holding questions, not a single question.** Originally (through
  migration V6) a `tests` row WAS one question: one `language`/`instructions_html`/`starter_code`/
  `duration_minutes`, one `submitted_code` per assignment. `V7__restructure_tests_into_pages_and_questions.sql`
  replaced that with `test_pages` (each with its own `duration_minutes` -- a test is taken page by page,
  each page timed independently) → `test_questions` (`question_type` one of `CODE`/`TEXT`/
  `MULTIPLE_CHOICE`; `language`/`starter_code`/`editor_font_size`/`editor_font_color` only apply to
  `CODE`) → `test_question_options` (only for `MULTIPLE_CHOICE`, one `is_correct` flag). Candidate
  submissions became `assignment_answers`, one row per question answered. V7 migrated every pre-existing
  test/assignment forward into this shape (page 1 / question 1, type `CODE`) rather than discarding data.
  **Recruiters can no longer see other options' correctness from the candidate side**: `PublicQuestion`/
  `PublicQuestionOption` (candidate-facing DTOs) deliberately omit the `correct` flag that
  `QuestionResponse`/`ReviewQuestionAnswer` (recruiter-facing) carry -- don't accidentally reuse the
  recruiter DTOs on a public endpoint.
- **A submitted answer survives the recruiter later editing or deleting that exact question.**
  `assignment_answers.question_id`/`selected_option_id` are `ON DELETE SET NULL`, and the row also
  carries `*_snapshot` columns (prompt/type/language/selected option text+correctness) captured at
  submit time. `AssignmentService.buildReviewResponse` walks the test's *current* live pages/questions
  for the normal case, and separately reports any answers whose question no longer exists (via the
  snapshot columns alone) as `orphanedAnswers` on `AssignmentReviewResponse` -- an extension of the
  already-existing "edits aren't versioned" behavior noted above, not a new regression.
- **Monaco's theme is a single global concept shared by every editor instance on the page** -- there is
  no built-in per-editor theme. The editor font-color feature (so a recruiter can hand-craft a white-on-
  white "AI trap" line now that the automated version is retired, see above) therefore can't use
  `monaco.editor.setTheme()`/`defineTheme()` the obvious way: with several CODE questions authored on one
  page at once, whichever editor set its theme last would repaint every other editor too. It's implemented
  instead as a full-document `inlineClassName` decoration (`editor.createDecorationsCollection`) with a
  CSS class injected once per distinct color into a shared `<style>` tag -- see
  `frontend/src/app/shared/monaco-editor/monaco-editor.ts`. `fontSize` doesn't have this problem (it's a
  real per-instance `updateOptions()` field) and is handled the normal way.
- **Question/instructions text is plain text with a small fixed markdown-lite subset**, not HTML anymore
  (recruiters previously had to hand-write `<p>`/`<ul>` HTML in `instructions_html`, which most won't
  know how to do). `frontend/src/app/shared/markdown/render-markdown.ts` HTML-escapes the raw text first,
  then re-applies only `**bold**`, `*italic*`, `` `code` ``, `- bullet` and `1. numbered` lines --
  `shared/prompt-editor/` is the toolbar+textarea `ControlValueAccessor` that writes that syntax, and
  `shared/markdown/markdown.pipe.ts` renders it back everywhere it's displayed (candidate test page,
  recruiter review). Nothing here ever does `[innerHTML]` on raw untrusted text.

## Environment gotchas hit while building this (useful if picking this up on the same machine)

- **No JDK/Node/Maven/Docker were preinstalled.** Installed via winget: `Microsoft.OpenJDK.21`,
  `OpenJS.NodeJS.22`, `PostgreSQL.PostgreSQL.17` (superuser password `postgres`; app db `testbyte`,
  user `testbyte_app` / password `testbyte_dev_pw` for local dev — matches `docker-compose.yml`).
  Maven itself was never installed globally — the project uses the Maven wrapper (`./mvnw`) instead.
- **Spring Boot's embedded Tomcat cannot run as a subprocess of a Claude Desktop session on Windows.**
  Java's NIO `Pipe` implementation uses an AF_UNIX loopback socket on Windows, which fails inside Claude
  Desktop's subprocess sandbox with `UnixDomainSockets.connect0` → "Unable to establish loopback
  connection." This is a confirmed, known limitation
  ([anthropics/claude-code#41432](https://github.com/anthropics/claude-code/issues/41432),
  [JDK-8312215](https://bugs.openjdk.org/browse/JDK-8312215)) — not a bug in this codebase, and not
  fixable with JVM flags from inside a Claude session. `mvn test` still works fine because
  `@SpringBootTest` defaults to `WebEnvironment.MOCK` (no real socket). To actually run the server
  (`mvnw spring-boot:run`) or hit it with curl/Postman, do it from a normal terminal outside Claude
  Desktop. The Angular dev server is unaffected (Node's networking stack doesn't have this issue) and
  previews fine from inside a Claude session.
- If shelling out to `java`/`node`/`npm` from a fresh terminal right after a winget install and it's not
  found: the PATH env var was updated in the registry but not in already-running processes. Either open a
  new terminal window, or explicitly prepend `C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin` and
  `C:\Program Files\nodejs` to `$env:Path` for that session.

## Current status

**Live and deployed**: backend on Render (`https://testbyte-backend.onrender.com`), frontend on Vercel
(`https://test-byte-virid.vercel.app`), database on Neon. The user has been testing against the real
deployment (not just locally) and reporting bugs found there, several of which were only reproducible
against a production build — see the "Environment gotchas" and design-decisions sections above for the
real bugs that surfaced this way (Monaco CSS, LazyInitializationException, hung SMTP requests).

Feature set as of the latest commit: auth/JWT with approval + admin-invite flows, a test bank recruiters
can create *and edit* (any recruiter/admin can edit any test) as a **multi-page, multi-question** test --
each page has its own timer and holds one or more questions of type CODE (Monaco, with per-question font
size/color), TEXT (free-text/theoretical), or MULTIPLE_CHOICE, with plain-text/markdown-lite instructions
per question -- assignment scheduling with unique candidate links, the candidate test page walking through
pages one at a time (no going back once advanced) with proctoring-event logging (tab switches/window
blur/paste attempts) and a single final submit of all answers, a recruiter review page showing every
page/question/answer (including MCQ correctness) with a saved review comment + a "Reviewed" badge on the
dashboard so submissions aren't re-reviewed, and admins seeing all assignments across every recruiter (not
just their own). 6 sample Java tests are seeded: the original 3 (Order Total/Customer Transactions/
Completed Orders, migrated into the new page/question shape) plus 3 new ones built in the new shape
(Junior/Mid/Senior Java Developer, each 3 pages -- MCQs, then a theory question, then a code question,
5 questions total). Backend/frontend test suites both passing.

**Explicitly out of scope (by request, not oversight):**
- Candidate self-signup/accounts.
- Automated code execution/auto-grading — recruiters review manually. MULTIPLE_CHOICE questions have a
  marked correct option for the recruiter's own reference during review, but nothing auto-scores a
  submission.
- Automated AI-cheating detection of any kind — built, then fully retired (see design decisions above);
  currently nothing flags a submission automatically. A recruiter can still hand-craft a manual
  white-on-white trap line via the per-question editor font color (see design decisions above).
- Copy/screenshot prevention on the candidate's question panel — no web technology can stop an actual
  screenshot anyway, and it isn't needed now that there's no automated AI-trap mechanism for it to
  conflict with.
- Test edit history/versioning — edits apply live to future/in-progress assignments; already-submitted
  answers are protected via the snapshot columns described above, but there's still no way to see what a
  test's other pages/questions looked like before an edit.
- Going back to a previous page once a candidate has advanced past it (per-page timers are one-way).

## Render deploy gotcha: rapid-fire pushes can deploy out of order

Pushing several commits to `main` in quick succession once caused Render to end up serving a build from
an *older* commit even though a newer one had already run its Flyway migrations against the database —
producing a genuinely confusing state: the DB schema was ahead of what the running JAR's entities
expected (`Schema validation: missing column [ai_trap_phrase] in table [tests]`, because that commit's
migration had already dropped the column, but the live app was built from before the entity was updated
to match). Diagnosed via the Render logs: Flyway logging `Schema "public" has a version (N) that is newer
than the latest available migration (N-1)!` is the tell — it means the running JAR's bundled migrations
don't go as far as what's already been applied to the database. **Manual Deploy → "Clear build cache &
deploy" did not fix this** (it rebuilt whatever commit the service already had pinned, not necessarily
`main`'s current HEAD) — **Manual Deploy → "Deploy latest commit" did**, since it explicitly re-resolves
and checks out the newest commit before building. If a schema-validation error like this ever shows up
again right after a burst of pushes, check the Flyway log lines for that "newer than latest available
migration" warning first, and reach for "Deploy latest commit" specifically, not just a cache-clear.

## Known issue: email delivery

Zoho SMTP sends currently **time out** from Render. Diagnosed as very likely an infrastructure-level
block — either Render's free tier blocking outbound SMTP ports, or Zoho throttling/blocking connections
from cloud-hosting IP ranges (both common, neither is a code bug). Mitigated so it can't break anything
else: `EmailService` methods are `@Async` with explicit 5s connect/read/write timeouts (see
`application.yml`), so a failed send just logs an error and moves on instead of hanging the request that
triggered it. **Decision: leaving this as-is for now** per the user — not currently pursuing port 465,
switching to an HTTP-based email API (e.g. Resend), or other fixes, but that's the natural next step if
email delivery becomes a blocker later.

## Known future asks from the user (do not re-litigate unless they change their mind)

- Plans to migrate off the free tier to **Azure** later, and possibly move the GitHub repo to a different
  business/org account, while **keeping GitHub Actions as CI/CD**. The stack was deliberately kept
  portable for this (plain Docker image, standard Postgres/JDBC, no Render/Neon-specific features, no
  hardcoded org/account names in the workflow YAML) — see the "Moving to Azure later" section at the
  bottom of DEPLOYMENT.md.

## Smaller things worth revisiting if there's time

- The Monaco integration bundles all of Monaco's ~100 language contributions as lazy chunks (only loaded
  on demand, doesn't hurt initial page load, but it's a lot of small files in `dist/`). Could be trimmed
  to just the languages actually used by importing individual `monaco-editor/esm/vs/basic-languages/...`
  contributions instead of the `monaco-editor` barrel import.
- No password-reset flow exists yet for recruiter/admin accounts.
- Login/signup read input values via `@ViewChild` template refs rather than Angular reactive forms
  (`pages/login/`, `pages/signup/`) — a deliberate fix for browsers/password managers that fill inputs
  without firing the events reactive forms rely on, leaving the form model stuck invalid forever. Other
  forms in the app still use reactive forms normally; only these two needed the workaround since they're
  the ones autofill/password managers actually touch.
