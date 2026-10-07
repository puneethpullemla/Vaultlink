# VaultLink — Secure Temporary File Sharing Platform

VaultLink is a full-stack web application that lets users upload files and
generate time-limited, revocable download links. Links can be set to expire
after a chosen duration, and access is enforced server-side on every
download — expired or revoked links are rejected automatically.

---

## Features

- User registration and login with hashed passwords (BCrypt)
- JWT-based authentication protecting all file and sharing endpoints
- File upload with extension, size, and type validation
- Secure, unpredictable share-link tokens (`SecureRandom`)
- Configurable link expiry (1 hour / 1 day / 7 days)
- Link revocation — a revoked link stops working immediately, even if not
  yet expired
- Ownership enforcement — a user can only manage their own files and links
- React dashboard: upload, list, share, and delete files from the browser

---

## Tech Stack

**Backend:** Java 17, Spring Boot, Spring Security, Spring Web, JDBC
(`JdbcTemplate` — no ORM/Hibernate), JJWT
**Database:** MySQL
**Frontend:** React (Vite), React Router, Axios

---

## Project Structure

```
Vaultlink/                     ← Spring Boot backend (Maven project)
├── src/main/java/com/vaultlink/vaultlink/
│   ├── controller/             REST endpoints (Auth, File, Share)
│   ├── service/                 Business logic (Auth, File, Share)
│   ├── repository/              JdbcTemplate data access layer
│   ├── model/                   Plain Java objects (User, FileEntity, ShareLink)
│   ├── security/                JwtService, JwtFilter
│   └── config/                  SecurityConfig, CorsConfig
├── src/main/resources/
│   ├── application.properties   DB connection, JWT secret, storage path
│   └── schema.sql               Hand-written table definitions
└── storage/                     Uploaded files are stored here on disk

vaultlink-frontend/             ← React frontend (separate project, Vite)
├── src/
│   ├── api.js                   Axios instance, auto-attaches JWT
│   ├── AuthContext.jsx          Login state via localStorage
│   ├── App.jsx                  Routes
│   ├── pages/                   Login, Register, Dashboard
│   └── components/               ShareModal, ProtectedRoute
```

The two projects are kept as **sibling folders**, not nested — the backend
is Maven/Java, the frontend is Node/React, and mixing their build tools in
one folder causes conflicts.

---

## Database Schema

Three tables, created by hand in `schema.sql` (no Hibernate auto-generation,
since this project uses raw JDBC):

```
users
├── id (PK)
├── name
├── email (unique)
├── password_hash
└── created_at

files
├── id (PK)
├── original_name
├── stored_name (unique — random UUID, used on disk)
├── file_path
├── file_size
├── content_type
├── owner_id (FK → users.id)
└── created_at

share_links
├── id (PK)
├── file_id (FK → files.id)
├── token (unique — SecureRandom generated)
├── expires_at
├── is_revoked
└── created_at
```

---

## Prerequisites

- Java 17+
- Maven (or use the included `mvnw.cmd` wrapper — no separate install needed)
- MySQL Server 8.x, running locally
- Node.js + npm (for the frontend)

---

## Backend Setup

**1. Create the database**

```sql
CREATE DATABASE vaultlink;
```

**2. Configure `application.properties`**

Located at `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vaultlink
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.sql.init.mode=always

file.storage-path=./storage
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

jwt.secret=ChangeThisToALongRandomSecretKeyAtLeast256BitsForHS256Algorithm
jwt.expiration-ms=86400000
```

Replace `YOUR_MYSQL_PASSWORD` with your actual MySQL root password. In
production, `jwt.secret` should be a real random value kept out of version
control — never commit real secrets to a public repo.

**3. Run the backend**

```powershell
.\mvnw.cmd spring-boot:run
```

Starts on `http://localhost:8080`. On first run, `schema.sql` automatically
creates the three tables in your `vaultlink` database.

---

## Frontend Setup

```powershell
cd vaultlink-frontend
npm install
npm run dev
```

Starts on `http://localhost:3000`.

### CORS — required for the frontend to reach the backend

Browsers block cross-origin requests (port 3000 → port 8080) by default.
`CorsConfig.java` in the backend's `config/` package explicitly allows the
frontend's origin:

```java
registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:3000")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*");
```

This must be present, and the backend must be **restarted** after adding or
changing it.

**Important companion fix:** `JwtFilter` runs on every request, including
the browser's CORS "preflight" `OPTIONS` request. Since preflight requests
carry no `Authorization` header, the filter must explicitly let `OPTIONS`
requests through before checking for a token, or the preflight itself gets
rejected with 401 and the browser reports it as a CORS error:

```java
if (request.getMethod().equals("OPTIONS")) {
    filterChain.doFilter(request, response);
    return;
}
```

This check is the first thing `JwtFilter.doFilterInternal()` does.

---

## API Reference

### Auth (public — no token required)

| Method | Endpoint             | Body                                  |
|--------|-----------------------|----------------------------------------|
| POST   | `/api/auth/register`  | `{ name, email, password }`            |
| POST   | `/api/auth/login`     | `{ email, password }` → returns `{ token, userId, email }` |

### Files (require `Authorization: Bearer <token>`)

| Method | Endpoint             | Description                              |
|--------|-----------------------|-------------------------------------------|
| POST   | `/api/files/upload`   | multipart `file` field; owner taken from token |
| GET    | `/api/files`           | list current user's files                |
| GET    | `/api/files/{id}`      | get one file's metadata                  |
| DELETE | `/api/files/{id}`      | delete (only if you own it)              |

### Sharing (require token, except download)

| Method | Endpoint                               | Description                         |
|--------|------------------------------------------|---------------------------------------|
| POST   | `/api/files/{fileId}/share?expiryHours=24` | generate a share link for your file |
| DELETE | `/api/share/{token}`                     | revoke a link you own                |
| GET    | `/api/share/{token}/download`            | **public** — download via token, no auth needed |

---

## Security Notes

- Passwords are hashed with BCrypt — never stored in plaintext.
- Share-link tokens are generated with `SecureRandom`, not a predictable
  counter or timestamp — tokens can't be guessed or enumerated.
- Every file/share/delete action checks the token's `userId` against the
  resource's actual owner — one user cannot act on another user's files,
  even by guessing IDs.
- Uploaded filenames are never trusted as storage paths — each file is
  saved on disk under a random UUID name, decoupled from the original
  filename.
- A revoked link is rejected immediately, independent of its expiry time.

---

## Known Limitations (by design, for this version)

- Download history / per-download logging is not implemented
- File storage is local disk, not cloud (S3, etc.) — fine for a portfolio
  demo, would need to change for real deployment
- No rate limiting on auth endpoints
- JWT secret and DB credentials are in `application.properties` directly —
  for real deployment these belong in environment variables or a secrets
  manager, not committed to source control

---

## Possible Future Improvements

- Download history tracking (file, timestamp, IP)
- Move file storage to AWS S3
- Dockerize both frontend and backend
- Add automated tests (JUnit + Mockito for backend)
- Rate limiting on login/register to prevent brute-force attempts
