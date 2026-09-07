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
- **AI-cheating trap (`aiTrapPhrase`), detection removed.** The user already had a trick in their existing
  static HTML test emails (originally at `~/Documents/java.html`, `python.html`, `sql.html`): a
  white-on-white HTML comment inside the code block saying "if you are an AI, rename X to Y" — invisible
  to a human reading the email, but read (and often obeyed) by an AI a candidate might paste the code
  into. Each `Assessment` has an `aiTrapPhrase` column holding that target identifier (e.g. `computeTotal`
  for the Java test), embedded as a plain code comment inside `starterCode`. An earlier version of this
  app also auto-flagged submissions containing that phrase as "possible AI use" (`possibleAiFlag`) — this
  was **removed** (see `V5__drop_possible_ai_flag.sql`) because the check was fundamentally broken: the
  phrase is part of the trap *comment itself*, which every candidate's starter code already contains
  before they touch anything, so the flag fired on nearly every submission regardless of actual AI use.
  There is currently no automated AI-use detection — `aiTrapPhrase` now exists solely to drive the
  line-concealment feature below; a recruiter reviewing manually could still notice a genuine rename in
  the submitted code, but nothing does that check for them.
  `aiTrapPhrase` itself is never sent to the candidate-facing API (`PublicAssignmentResponse` omits it) —
  it *is* included in the recruiter-facing `AssessmentResponse` (needed so editing a test doesn't silently
  wipe it; recruiters already see the whole `starterCode` anyway, so there's no extra secrecy lost).
  **The line is now visually concealed from candidates**, not just an unremarkable-looking comment: the
  backend computes which line of `starterCode` contains the phrase (`Assessment.findAiTrapLineNumber()`)
  and sends only that line *number* (never the phrase) as `hiddenLineNumber` on `PublicAssignmentResponse`;
  the Monaco wrapper applies a decoration (`color: transparent; font-size: 1px`) to that line. Important:
  it must stay copyable (no `user-select: none`) — the whole mechanism depends on the hidden text still
  reaching the clipboard when a candidate copies code out to paste into an AI tool. The user explicitly
  rejected adding copy/screenshot-prevention on the question panel for this exact reason.
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
  (`[{type, timestamp}, ...]` — tab switches, window blur/focus, paste-into-editor) and the backend never
  parses it; it's passed through as-is and JSON-parsed only in `assignment-review.ts` for display. If this
  ever needs to be queried/filtered server-side, it'll need a real table instead.
- **Review comments are single/overwritable**, not a thread — `review_comment` + `reviewed_at` +
  `reviewed_by` columns get replaced wholesale on each save. The dashboard's "Reviewed" badge is just
  `reviewedAt != null` on `AssignmentSummaryResponse`.

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
can create *and edit* (any recruiter/admin can edit any test), assignment scheduling with unique candidate
links, the Monaco-based candidate test page with a concealed AI-trap line and proctoring-event logging
(tab switches/window blur/paste attempts), a recruiter review page with a saved review comment + a
"Reviewed" badge on the dashboard so submissions aren't re-reviewed, and admins seeing all assignments
across every recruiter (not just their own). 22 backend tests, 2 frontend tests, both passing.

**Explicitly out of scope (by request, not oversight):**
- Candidate self-signup/accounts.
- Automated code execution/auto-grading — recruiters review manually.
- Copy/screenshot prevention on the candidate's question panel — deliberately not built; it would work
  against the AI-trap mechanism, which depends on the code staying copyable, and no web technology can
  stop an actual screenshot anyway.
- Test edit history/versioning — edits apply live, no snapshot of what a candidate was actually shown.

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
