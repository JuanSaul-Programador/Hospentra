# Hospentra — Hotel Reservation Backend

> Robust, secure, and production-ready Hotel Reservation REST API built with **Java 25**, **Spring Boot 4**, **PostgreSQL**, and **Flyway**.

---

## 🏨 About Hospentra

**Hospentra** is an enterprise-grade backend system designed to manage core hotel operations:
- Room category management (types, capacities, pricing).
- Physical room inventory and operational statuses.
- Guest identity management.
- Anti-overbooking reservation engine with concurrency safety.
- Role-Based Access Control (RBAC) powered by Spring Security & stateless JWT.
- Version-controlled database migrations with Flyway.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Java 25
- **Framework:** Spring Boot 4.1.1
- **Security:** Spring Security 7.1.1 + JJWT 0.12.6 (Stateless JWT, BCrypt)
- **Persistence:** Spring Data JPA + Hibernate 7.4.5.Final
- **Database:** PostgreSQL 18.6
- **Database Migrations:** Flyway 12.4.0
- **Build Tool:** Maven

### Architectural Principles
- **Clean Architecture & Layered Separation:**
  - `domain`: Pure business entities and repository contracts.
  - `application`: Use cases, service interfaces, implementations, and DTO mappers.
  - `infrastructure`: Technical security filters, JWT service, and configuration beans.
  - `web`: REST Controllers, Request/Response Records, and global exception handlers.
- **Concurrency & Anti-Overbooking:**
  - Strict date overlap check: $\text{newCheckIn} < \text{existing.checkOut} \land \text{newCheckOut} > \text{existing.checkIn}$.
  - Pessimistic write lock (`PESSIMISTIC_WRITE`) on `Room` during booking to serialize simultaneous requests and eliminate race conditions.
- **Error Standard:** RFC 7807 `ProblemDetail` for uniform API error responses.

---

## 🔒 Security & Role-Based Access Control (RBAC)

| Role | Responsibilities |
|---|---|
| `ROLE_ADMIN` | Full administrative control: manage staff, room types, rooms, and hotel policies. |
| `ROLE_RECEPTIONIST` | Operational management: check-ins, check-outs, guest registration, reservation confirmation. |
| `ROLE_CUSTOMER` | Public catalog exploration, user self-registration, and making personal reservations. |

---

## 🚀 Getting Started

### 1. Prerequisites
- JDK 25 installed
- PostgreSQL 16+ running locally
- Git

### 2. Database Setup
Create the database in PostgreSQL:
```sql
CREATE DATABASE hotel_reservation_db;
```
*(Flyway will automatically execute and apply all schema migrations on application startup).*

### 3. Environment Variables
Configure the following environment variables (or copy `.env.example`):

| Variable | Description | Default |
|---|---|---|
| `DB_URL` | JDBC Database Connection URL | `jdbc:postgresql://localhost:5432/hotel_reservation_db` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | *(Your password)* |
| `JWT_SECRET_KEY` | 256-bit Base64 secret key | Pre-configured in yaml |
| `JWT_EXPIRATION` | Token validity in milliseconds | `86400000` (24h) |

### 4. Running the Application
Using the Maven wrapper:
```bash
./mvnw spring-boot:run
```

---

## 📡 API Overview

### Authentication (`/api/auth`)
- `POST /api/auth/register` — Public customer registration (automatically links Guest profile).
- `POST /api/auth/login` — Unified login returning signed JWT.

### Staff Management (`/api/users`) — *Admin Only*
- `POST /api/users` — Admin registers new staff members (`ROLE_RECEPTIONIST` or `ROLE_ADMIN`).
- `GET /api/users` — List all user accounts.

### Room Types (`/api/room-types`)
- `GET /api/room-types` — Public room catalog.
- `POST`, `PUT`, `DELETE /api/room-types` — *Admin Only*.

### Rooms (`/api/rooms`)
- `GET /api/rooms` — Public room inventory (supports filtering by `?status=AVAILABLE`).
- `POST`, `PUT`, `DELETE /api/rooms` — *Admin Only*.

### Reservations (`/api/reservations`)
- `POST /api/reservations` — Authenticated booking (customers do not need to send `guestId`).
- `PATCH /api/reservations/{id}/confirm` — *Staff/Admin Only*.
- `PATCH /api/reservations/{id}/complete` — *Staff/Admin Only* (Check-out).
- `PATCH /api/reservations/{id}/cancel` — Authenticated cancellation.

---

## 👤 Author

Developed with care by **Juan Saúl**.
- GitHub: [@JuanSaul-Programador](https://github.com/JuanSaul-Programador)
