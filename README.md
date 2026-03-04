# TaskFlow — Full-Stack Task Manager

> **Portfolio project** demonstrating production-ready Java/Spring Boot + React/TypeScript skills.

[![CI](https://github.com/AaronCx/task-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/AaronCx/task-manager/actions/workflows/ci.yml)

---

## Screenshots

> *(Add screenshots here after first run — see the [Screenshots](#-screenshots) section)*

| Login | Dashboard | Task Editor |
|-------|-----------|-------------|
| ![Login](docs/login.png) | ![Dashboard](docs/dashboard.png) | ![Editor](docs/editor.png) |

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Task API** | Java 17, Spring Boot 3.2, Spring Security, Hibernate/JPA |
| **Notification API** | Spring Boot 3.2, Spring Kafka (`@KafkaListener`), JPA |
| **Auth** | JWT (jjwt 0.11.5) — stateless Bearer tokens, shared across services |
| **Message Bus** | Apache Kafka 3.5 (via Confluent images) + Zookeeper |
| **Database** | PostgreSQL 15 (shared by both services) |
| **API Docs** | Springdoc OpenAPI 3 / Swagger UI (both services) |
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS |
| **HTTP Client** | Axios with in-memory JWT + notification polling |
| **Routing** | React Router v6 with protected routes |
| **DevOps** | Docker, Docker Compose, GitHub Actions CI |

---

## Architecture Overview

### Full System (with Kafka)

```
┌────────────────────────────────────────────────────────────────────────────┐
│                           Client (Browser)                                 │
│   React 18 + TypeScript + Tailwind CSS                                     │
│   JWT in-memory (AuthContext) — never localStorage                         │
│   Notification bell polls /api/notifications every 30 s                    │
└───────────┬───────────────────────────────────┬────────────────────────────┘
            │ /api/auth, /api/tasks              │ /api/notifications
            │ Bearer token (Axios)               │ Bearer token (same JWT)
            ▼                                   ▼
┌───────────────────────┐           ┌───────────────────────────────────────┐
│  Task Manager API     │           │  Notification Service                 │
│  Spring Boot  :8080   │           │  Spring Boot  :8081                   │
│                       │           │                                        │
│  AuthController       │           │  NotificationController               │
│  TaskController       │           │    GET  /api/notifications             │
│  ─────────────────    │           │    GET  /api/notifications/unread-count│
│  JwtAuthFilter        │           │    PUT  /api/notifications/read-all    │
│  Spring Security      │           │  ──────────────────────────────────── │
│  ─────────────────    │           │  JwtAuthFilter  (same secret)         │
│  AuthService          │           │  NotificationService                  │
│  TaskService ─────────┼──────┐    │    handleTaskCreated()                │
│  ─────────────────    │      │    │    handleTaskUpdated()                 │
│  UserRepository       │      │    │    handleTaskDeleted()                 │
│  TaskRepository       │      │    │  ──────────────────────────────────── │
│  DataSeeder           │      │    │  NotificationRepository               │
└──────────┬────────────┘      │    │  UserRepository  (read-only)          │
           │ JPA               │    └─────────────┬─────────────────────────┘
           │                   │                  │ JPA
           ▼                   │                  ▼
┌──────────────────────────────┼──────────────────────────────────────────┐
│                        PostgreSQL 15                                      │
│    tables:  users  │  tasks  │  notifications                            │
│             ↑ seeded by DataSeeder on first boot                         │
└──────────────────────────────┬───────────────────────────────────────────┘
                               │
                               ▼
                  ┌────────────────────────┐
                  │   Apache Kafka         │
                  │   (+ Zookeeper)        │
                  │                        │
                  │  topics:               │
                  │    task.created  ──────┼──▶ @KafkaListener
                  │    task.updated  ──────┼──▶ @KafkaListener
                  │    task.deleted  ──────┼──▶ @KafkaListener
                  │                        │
                  │  Producer: KafkaTemplate (TaskService)
                  │  Consumer: ConcurrentKafkaListenerContainerFactory
                  │  Serialiser: JsonSerializer / JsonDeserializer
                  │  Error handling: DefaultErrorHandler (3 retries, 2 s)
                  └────────────────────────┘
```

### Kafka Event Flow

```
User Action              Task API                 Kafka              Notifications
─────────────────────────────────────────────────────────────────────────────────
POST /api/tasks    ──▶  TaskService.createTask()
                         └─ taskRepository.save()
                         └─ eventProducer.publish() ──▶ task.created ──▶ @KafkaListener
                                                                          handleTaskCreated()
                                                                          └─ persist Notification
                                                                          └─ log meaningful msg

PUT /api/tasks/:id ──▶  TaskService.updateTask()
                         └─ capture oldStatus
                         └─ taskRepository.save()
                         └─ eventProducer.publish() ──▶ task.updated ──▶ @KafkaListener
                                                                          handleTaskUpdated()
                                                                          └─ detect status change
                                                                          └─ persist Notification

DELETE /api/tasks  ──▶  TaskService.deleteTask()
                         └─ capture task data
                         └─ taskRepository.delete()
                         └─ eventProducer.publish() ──▶ task.deleted ──▶ @KafkaListener
                                                                          handleTaskDeleted()
                                                                          └─ persist Notification

GET /notifications ──▶  NotificationController ──▶ NotificationService
                         └─ returns last 20 notifications for current user
```

### Request Flow (authenticated, task API)

1. React sends `Authorization: Bearer <jwt>` header via Axios.
2. `JwtAuthenticationFilter` validates the token and populates `SecurityContext`.
3. Controller receives `@AuthenticationPrincipal User`; service executes business logic.
4. JPA queries PostgreSQL; response is mapped to a DTO.
5. After successful DB write, `TaskEventProducer.publish()` fires a Kafka event (non-blocking).
6. Notification service consumer receives the event and persists a `Notification` row.
7. Frontend bell icon picks up the new count on the next 30-second poll.

---

## API Endpoints

### Task Manager API — port 8080

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/register` | Public | Create a new account |
| `POST` | `/api/auth/login`    | Public | Authenticate → receive JWT |
| `GET`  | `/api/tasks`         | Bearer | List tasks (optional `?status=` filter) |
| `GET`  | `/api/tasks/{id}`    | Bearer | Get a single task |
| `POST` | `/api/tasks`         | Bearer | Create a task → publishes `task.created` |
| `PUT`  | `/api/tasks/{id}`    | Bearer | Update a task → publishes `task.updated` |
| `DELETE` | `/api/tasks/{id}` | Bearer | Delete a task → publishes `task.deleted` |

Swagger UI: **http://localhost:8080/swagger-ui.html**

### Notification Service API — port 8081

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET`  | `/api/notifications`               | Bearer | Recent 20 notifications |
| `GET`  | `/api/notifications/unread-count`   | Bearer | Count for bell badge |
| `PUT`  | `/api/notifications/read-all`       | Bearer | Mark all as read |

Swagger UI: **http://localhost:8081/swagger-ui.html**

---

## Local Development

### Prerequisites

- Java 17+
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- Node.js 20+
- Docker & Docker Compose

### Option A — Docker Compose (recommended, full stack)

```bash
# Clone the repo
git clone https://github.com/AaronCx/task-manager.git
cd task-manager

# Start everything: Zookeeper + Kafka + PostgreSQL + API + Notifications
docker compose up --build

# Services available at:
#   Task Manager API:    http://localhost:8080/swagger-ui.html
#   Notification API:    http://localhost:8081/swagger-ui.html
#   Kafka (host):        localhost:29092
```

Then start the frontend separately:

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

### Option B — Manual setup

You need a running PostgreSQL and Kafka. The easiest way is to start just the
infrastructure services via Docker:

```bash
# Start only infrastructure (Kafka + Zookeeper + PostgreSQL)
docker compose up zookeeper kafka db -d
```

Then in separate terminals:

```bash
# Task Manager API (port 8080)
cd backend && ./mvnw spring-boot:run

# Notification Service (port 8081)
cd backend-notifications && ./mvnw spring-boot:run

# React frontend (port 5173)
cd frontend && npm install && npm run dev
```

---

## Running Tests

```bash
# Backend unit tests (uses H2 in-memory — no PostgreSQL needed)
cd backend
./mvnw test

# Frontend type-check
cd frontend
npx tsc --noEmit
```

---

## Demo Credentials

The database is seeded with sample data on first run:

| Email | Password | Tasks |
|-------|----------|-------|
| `alice@demo.com` | `password123` | 8 tasks across all statuses |
| `bob@demo.com`   | `password123` | 2 tasks |

---

## Project Structure

```
task-manager/
├── backend/                          Task Manager API — Spring Boot 3 (port 8080)
│   ├── src/main/java/com/portfolio/taskmanager/
│   │   ├── config/                   SecurityConfig, OpenApiConfig, KafkaProducerConfig
│   │   ├── controller/               AuthController, TaskController
│   │   ├── dto/                      Request/Response records
│   │   ├── entity/                   User, Task (JPA entities)
│   │   ├── enums/                    TaskStatus, TaskPriority
│   │   ├── exception/                GlobalExceptionHandler + custom exceptions
│   │   ├── kafka/                    TaskEvent (record), TaskEventProducer
│   │   ├── repository/               UserRepository, TaskRepository
│   │   ├── security/                 JwtTokenProvider, JwtAuthenticationFilter
│   │   ├── seeder/                   DataSeeder (2 users + 10 tasks on first boot)
│   │   └── service/                  AuthService, TaskService (publishes Kafka events)
│   ├── Dockerfile                    Multi-stage build (JDK builder → JRE runtime)
│   └── pom.xml
│
├── backend-notifications/            Notification Service — Spring Boot 3 (port 8081)
│   ├── src/main/java/com/portfolio/notifications/
│   │   ├── config/                   SecurityConfig, KafkaConsumerConfig
│   │   ├── controller/               NotificationController
│   │   ├── dto/                      NotificationResponse
│   │   ├── entity/                   Notification, User (read-only view)
│   │   ├── exception/                GlobalExceptionHandler
│   │   ├── kafka/                    TaskEvent (mirror), TaskEventConsumer (@KafkaListener)
│   │   ├── repository/               NotificationRepository, UserRepository
│   │   ├── security/                 JwtTokenProvider, JwtAuthFilter (same secret)
│   │   └── service/                  NotificationService (event → message → persist)
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                         React 18 + TypeScript + Vite (port 5173)
│   └── src/
│       ├── api/                      axiosClient, auth.ts, tasks.ts, notifications.ts
│       ├── components/               Layout (with bell), ProtectedRoute, Badges,
│       │                             NotificationsDropdown (polls every 30 s)
│       ├── context/                  AuthContext (in-memory JWT)
│       ├── pages/                    Login, Register, Dashboard, TaskDetail
│       └── types/                    Shared TypeScript interfaces
│
├── .github/workflows/ci.yml          CI: backend + notifications + frontend + docker
├── docker-compose.yml                Zookeeper + Kafka + PostgreSQL + API + Notifications
└── README.md
```

---

## Key Design Decisions

- **Stateless JWT** — no server-side sessions; same secret shared across both services so one login works everywhere.
- **In-memory token storage** — JWT lives in React state (never `localStorage`) to reduce XSS surface.
- **Kafka fire-and-forget** — `TaskEventProducer` wraps `KafkaTemplate.send()` in a try-catch; task operations succeed even when Kafka is unavailable.
- **Consumer error handling** — `DefaultErrorHandler` retries failed messages 3× with 2 s back-off, then skips to prevent partition stall.
- **`ErrorHandlingDeserializer`** — wraps `JsonDeserializer` so a single malformed Kafka message is skipped, not re-queued forever.
- **Shared PostgreSQL** — both services connect to the same DB. The notification service reads from `users` (read-only) and owns the `notifications` table.
- **Ownership scoping** — `TaskRepository.findByIdAndOwner` ensures users can only CRUD their own tasks.
- **Global exception handler** — every service returns a consistent `ErrorResponse` JSON shape.
- **Multi-stage Docker builds** — JDK builder stage discarded; runtime images use JRE-only alpine (~250 MB).
- **DataSeeder guard** — checks `userRepository.count()` before seeding; safe to restart containers.

---

## License

MIT
