# TestByte — Skills You Can See

A platform for scheduling short (10-20 min) coding assessments and reviewing candidate submissions.

## Structure

```
backend/    Spring Boot API (Java 21, Maven, PostgreSQL, Flyway, JWT auth, email via Zoho SMTP)
frontend/   Angular app (recruiter/admin dashboard + public candidate test page with Monaco editor)
```

## Local development

**Prerequisites:** JDK 21, Node.js 22, and a local PostgreSQL instance (either the `docker-compose.yml` in this repo, or a native install).

Database (either option):
```bash
docker compose up -d
```
or point the backend at a native PostgreSQL 17 instance with a `testbyte` database and a `testbyte_app` user.

Backend:
```bash
cd backend
./mvnw spring-boot:run
```

Frontend:
```bash
cd frontend
npm install
npm start
```

See [DEPLOYMENT.md](DEPLOYMENT.md) for the free-tier hosting setup (Vercel + Render + Neon) and how it migrates to Azure later.
