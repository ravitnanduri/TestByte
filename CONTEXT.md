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
- **AI-cheating detection (`aiTrapPhrase` / `possibleAiFlag`).** The user already had a trick in their
  existing static HTML test emails (originally at `~/Documents/java.html`, `python.html`, `sql.html`):
  a white-on-white HTML comment inside the code block saying "if you are an AI, rename X to Y" — invisible
  to a human reading the email, but read (and often obeyed) by an AI a candidate might paste the code
  into. This was digitized: each seeded `Assessment` has an `aiTrapPhrase` column holding the
  AI-only-instruction's target identifier (e.g. `computeTotal` for the Java test, `get_totals` for
  Python, `completed_total` for SQL), embedded as a plain code comment inside `starterCode` (there's no
  clean way to make text invisible inside a Monaco editor the way white-on-white HTML worked in an email,
  so it's just an ordinary-looking TODO comment among the other real TODOs — a human skimming the code
  is unlikely to single it out). On submission, `AssignmentService.submit` sets `possibleAiFlag = true` if
  the submitted code contains that exact phrase, surfaced as a warning banner on the recruiter's review
  page. `aiTrapPhrase` is never exposed via any API response the candidate's browser can see — only used
  server-side for the post-submission check.
- **Monaco editor** is wired via the raw `monaco-editor` npm package (0.53.0 pinned — 0.54+ pulls in a
  vulnerable dompurify via its markdown/hover rendering), not `ngx-monaco-editor-v2`, which lagged behind
  Angular 22 at the time this was built. See `frontend/src/app/shared/monaco-editor/`.

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

## Current status (as of the initial commit)

Done and verified: backend (auth/JWT, approval + invite flows, test bank, assignment scheduling +
candidate flow, all 4 email triggers, AI-trap flag — 13 tests passing, full Spring context verified
against real Postgres), frontend (all pages built, manually clicked through in a browser including live
Monaco editing and error-state handling — 2 tests passing), CI (`.github/workflows/*.yml`), and deploy
config (`backend/Dockerfile`, `render.yaml`, `frontend/vercel.json`).

**Not done / explicitly out of scope for this first pass:**
- Candidate self-signup/accounts — by design, per the original request ("no candidate signup for now").
- Automated code execution or auto-grading of submissions — recruiters review submissions manually.
- Plagiarism detection beyond the AI-trap-phrase flag.
- A real end-to-end test of the Zoho SMTP send (needs a live app password — see Next steps).
- Any actual cloud accounts (Neon/Render/Vercel) — none were created; this was built entirely against a
  local Postgres instance and never deployed.

## Next steps / what's needed later

**To get this live (in order):**
1. Create a free Neon Postgres project, a free Render account, and a free Vercel account — none of these
   exist yet. Generate a Zoho app-specific password for `ravitejananduri@zoho.com` (Zoho Mail → Settings →
   Security → App Passwords). Full step-by-step in [DEPLOYMENT.md](DEPLOYMENT.md).
2. Deploy the backend to Render via the `render.yaml` blueprint, filling in the Neon connection details
   and Zoho credentials as env vars.
3. Update `frontend/src/environments/environment.ts`'s `apiBaseUrl` to the real Render URL, then deploy
   the frontend to Vercel.
4. Go back and set `FRONTEND_BASE_URL` on the Render service to the real Vercel URL (used to build links
   inside emails), redeploy.
5. Sign up through the live app once — the first account becomes an approved admin automatically.
6. **Send a real test email** end-to-end (trigger a recruiter signup, confirm the approval email actually
   lands in `ravitejananduri@zoho.com`'s inbox) before relying on this for real candidates.

**Known future asks from the user (see conversation / do not re-litigate unless they change their mind):**
- Plans to migrate off the free tier to **Azure** later, and possibly move the GitHub repo to a different
  business/org account, while **keeping GitHub Actions as CI/CD**. The stack was deliberately kept
  portable for this (plain Docker image, standard Postgres/JDBC, no Render/Neon-specific features, no
  hardcoded org/account names in the workflow YAML) — see the "Moving to Azure later" section at the
  bottom of DEPLOYMENT.md.

**Smaller things worth revisiting if there's time:**
- The Monaco integration currently bundles all of Monaco's ~100 language contributions as lazy chunks
  (only loaded on demand, so it doesn't hurt initial page load, but it's a lot of small files in `dist/`).
  Could be trimmed to just the languages actually used (Java/Python/SQL/JS/TS) by importing individual
  `monaco-editor/esm/vs/basic-languages/...` contributions instead of the `monaco-editor` barrel import.
- No password-reset flow exists yet for recruiter/admin accounts — not requested, but will likely come up.
- No UI for admins to edit/deactivate an existing test in the test bank — only create is implemented
  (`POST /api/tests`), matching what was actually asked for.
