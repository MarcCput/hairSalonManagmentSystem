#  Hair Salon Management System

A full-stack salon booking and management platform: a **Spring Boot** REST API paired with a **Vue 3** single-page application. Customers book appointments online and pay in store, stylists see their own schedule, and administrators manage every part of the business — services, staff, stock, promotions, payments and feedback.

---

##  Contents

1. [What the System Does](#-what-the-system-does)
2. [Tech Stack](#-tech-stack)
3. [Architecture](#-architecture)
4. [Domain Model](#-domain-model)
5. [REST API](#-rest-api)
6. [The Booking & In-Store Payment Workflow](#-the-booking--in-store-payment-workflow)
8. [Getting Started](#-getting-started)
7. [Repository Layout](#-repository-layout)
8. [Team](#-team)
9. [UML Class Diagram](#-uml-class-diagram)

---

##  What the System Does

The application serves three audiences, each with its own dashboard:

| Role | What they can do |
| --- | --- |
| **Customer** | Browse the public site, book an appointment, track bookings, view loyalty points and rewards, leave feedback, manage their profile |
| **Stylist** | Sign in by name and view their own upcoming appointments |
| **Admin** | Ten CRUD dashboards — Customers, Stylists, Services, Appointments, Payments, Promotions, Inventory, Feedback, Notifications, Users & Roles — plus confirming and cancelling bookings |

Business rules live in the **service layer** and are enforced regardless of which client calls the API:

- Customers and stylists cannot be **double-booked** for overlapping time slots
- Appointments can only be made against **active** stylists and services
- Appointment status transitions are tracked automatically (`PENDING → CONFIRMED → COMPLETED`, or `CANCELLED`)
- Promotions are only redeemable **inside their valid date range**, validated by code
- Loyalty points accrue and redeem against **tier** rules
- Payments must correspond to a real appointment and its services
- Stock adjustments are checked, and low-stock products can be listed on demand

---

##  Tech Stack

### Backend

| | |
| --- | --- |
| Language | Java 22 |
| Framework | Spring Boot 3.5.4 |
| Build | Maven |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL 8 (with an H2 in-memory fallback profile) |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| API docs | springdoc-openapi 2.8.5 → `/swagger-ui.html` |
| Testing | JUnit 5 + Mockito |

### Frontend

| | |
| --- | --- |
| Framework | Vue 3 (Composition API) |
| Language | TypeScript |
| Build | Vite |
| Styling | Tailwind CSS 4 |
| Routing | Vue Router 4 (history mode) |
| Charts / calendar | ApexCharts, FullCalendar |
| Base template | TailAdmin Vue dashboard |

---

##  Architecture

The backend is a **layered, DDD-flavoured** Spring Boot application under the `za.ac.cput` package. Every layer has exactly one job, so a change to "how a promotion discount is calculated" only ever touches the service layer, and a change to a JSON response shape only ever touches the web layer.

```
HTTP request
     │
     ▼
┌─────────────┐   thin: translates HTTP <-> service calls, no business logic
│ controller  │
└─────────────┘
     │
     ▼
┌─────────────┐   all business rules live here; depends on interfaces only
│  service    │──────────────┐
└─────────────┘              │
     │                       ▼
     ▼                 ┌──────────┐   static build*() methods, validation
┌─────────────┐        │ factory  │───────────┐
│ repository  │        └──────────┘           │
└─────────────┘                               ▼
     │                                  ┌──────────┐
     ▼                                  │   util   │  shared Helper
┌─────────────┐                         └──────────┘
│   database  │
└─────────────┘

Anything thrown along the way → exception/GlobalExceptionHandler → clean JSON error
```

### Package responsibilities

| Package | What lives here | Why it's separate |
| --- | --- | --- |
| `domain` | JPA `@Entity` classes built with the **Builder pattern** — no public setters, so an entity is either fully valid or doesn't exist | The data model shouldn't know anything about HTTP or SQL |
| `domain/valueobject` | Small immutable types — `Email`, `PhoneNumber`, `Money`, `TimeSlot`, `DateRange` | Makes invalid data unconstructable: `Email.of("not-an-email")` throws immediately rather than flowing through the system |
| `domain/enums` | Fixed sets of named values | A typo like `"Pendnig"` becomes a compile error instead of a runtime bug |
| `factory` | One static `build*(...)` per entity; validates raw input via `util/Helper` and returns a built entity or `null` | Construction and validation in one place, not scattered across services |
| `repository` | One `interface XRepository extends JpaRepository<X, String>` plus derived queries | The only layer that knows SQL/JPA exists |
| `service` / `service/impl` | One `IXService` interface + `XServiceImpl`, over a shared generic `IService<T, ID>` | Depending on the *interface* is what makes the whole suite mockable |
| `controller` | One `@RestController` per resource under `/api/<resource>` | Thin by design — an `if` in a controller usually belongs in a service |
| `exception` | `ResourceNotFoundException`, `InvalidOperationException`, `ApiError`, and a `@RestControllerAdvice` handler | Every error returns the same JSON shape instead of a raw stack trace |
| `util` | `Helper.java` — shared email/phone validation, ID generation, discount and loyalty maths | One regex for "what's a valid email", not ten near-copies |
| `config` | `CorsConfig` (allows the Vite dev server to call the API), `SpaFallbackController` (single-JAR mode only) | Cross-cutting setup that belongs to no single resource |

### Error format

Every thrown exception is converted to one consistent body:

```json
{
  "status": 404,
  "message": "Customer not found with id: C-1042",
  "timestamp": "2026-09-20T14:31:07.882"
}
```

---

##  Domain Model

**12 entities**, each with a full repository → service → controller stack:

| Entity | Represents |
| --- | --- |
| `Customer` | People who register and book appointments |
| `Stylist` | Salon staff who provide services — name, specialisation, years of experience |
| `SalonService` | A service on the menu (cut, colour, treatment) with price, duration and category |
| `Appointment` | A scheduled session between a customer and a stylist — date, time slot, status |
| `Payment` | A recorded payment against a completed appointment — method and amount |
| `Promotion` | A discount code valid over a date range |
| `LoyaltyReward` | Points balance and tier for a customer, with accrual and redemption |
| `ProductInventory` | Retail/back-bar stock levels, with low-stock reporting |
| `Feedback` | Customer reviews tied to a customer and appointment |
| `Notification` | In-app messages to customers (reminders, promotions, booking confirmations) |
| `User` | Staff/admin account records |
| `Role` | Named role assigned to a user |

**5 value objects** — `Email`, `PhoneNumber`, `Money`, `TimeSlot`, `DateRange`.
Each exposes its raw form via `@JsonValue`, so they serialise as plain strings and numbers (`"email": "jane@example.com"`), not nested objects — which is what keeps the frontend's TypeScript interfaces simple.

**9 enums** — `AppointmentStatus`, `PaymentStatus`, `PaymentMethod`, `ServiceCategory`, `DiscountType`, `LoyaltyTier`, `NotificationType`, `NotificationStatus`, `NotificationChannel`.

---

##  REST API

Base URL: `http://localhost:8080/api` · Interactive docs: `http://localhost:8080/swagger-ui.html`

Every resource supports the standard set (`POST` create, `GET /{id}`, `GET` list all, `DELETE /{id}`). The table below lists the additions beyond that baseline.

| Resource | Base path | Notable endpoints |
| --- | --- | --- |
| Appointments | `/api/appointments` | `POST /book`, `POST /{id}/confirm`, `POST /{id}/cancel`, `POST /{id}/complete`, `GET /customer/{customerId}`, `GET /stylist/{stylistId}` |
| Customers | `/api/customers` | `GET /email/{email}`, `PUT /{id}` |
| Stylists | `/api/stylists` | `GET /active`, `PUT /{id}` |
| Services | `/api/services` | `GET /active`, `GET /category/{category}`, `PUT /{id}` |
| Payments | `/api/payments` | `POST /process`, `GET /appointment/{appointmentId}` |
| Promotions | `/api/promotions` | `GET /validate/{code}` |
| Loyalty rewards | `/api/loyalty-rewards` | `GET /customer/{customerId}`, `POST /customer/{customerId}/add-points`, `POST /customer/{customerId}/redeem` |
| Products | `/api/products` | `GET /low-stock`, `POST /{id}/adjust-stock`, `PUT /{id}` |
| Feedback | `/api/feedback` | `GET /customer/{customerId}` |
| Notifications | `/api/notifications` | `POST /appointment-reminder`, `POST /promotion`, `GET /customer/{customerId}` |
| Users | `/api/users` | `GET /username/{username}` |
| Roles | `/api/roles` | `GET /name/{name}` |

---

##  The Booking & In-Store Payment Workflow

**There is no online payment, by design.** Money changes hands at the salon. The flow is:

1. A customer books through the public site → `POST /api/appointments/book` → the appointment is created with status **`PENDING`**.
2. The customer immediately sees the price on the confirmation screen and under "My Appointments", always labelled **payable in store**.
3. An admin reviews the booking in the Appointments dashboard and clicks **Confirm** → `POST /api/appointments/{id}/confirm`.
4. The backend flips the status to **`CONFIRMED`** and automatically creates an in-app **`Notification`** row for that customer — no email or SMS provider is involved; the customer's dashboard simply reads the notification.
5. After the visit, the appointment is marked **`COMPLETED`** and a `Payment` is recorded against it.

---

##  Getting Started

### Prerequisites

- **JDK 22**
- **Maven 3.9+**
- **MySQL 8** running on `localhost:3306` *(optional — see the H2 fallback below)*
- **Node.js 20+** and npm, for the frontend

### 1. Configure the database

Credentials are read from environment variables and are **never** hardcoded. `application.properties` defaults the database name to `hair_salon_db` and creates it on first run if it doesn't exist.

```bash
export DB_NAME=hair_salon_db
export DB_USERNAME=salon_app
export DB_PASSWORD=your_password_here
```

On Windows PowerShell:

```powershell
$env:DB_NAME="hair_salon_db"; $env:DB_USERNAME="salon_app"; $env:DB_PASSWORD="your_password_here"
```

**No MySQL installed?** Start with the H2 in-memory profile instead — nothing to install, data resets on every restart, and the H2 console is at `/h2-console`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### 2. Run the backend

```bash
mvn spring-boot:run
```

The API comes up on **`http://localhost:8080`**. Browse it at `http://localhost:8080/swagger-ui.html`.

### 3. Run the frontend

From the frontend project folder:

```bash
npm install
npm run dev
```

The app comes up on **`http://localhost:5173`** and talks to the backend at `http://localhost:8080/api` (set in `src/api/client.ts`). `CorsConfig` already allowlists ports `5173` and `3000`, so no further setup is needed.

### Two ways to run the whole system

| Mode | How | When to use it |
| --- | --- | --- |
| **Two dev servers** *(default)* | Backend on `:8080`, frontend on `:5173`. Vite hot-reloads on every save. | Day-to-day development |
| **Single JAR** | `npm run build`, copy the resulting `dist/` into the backend's `src/main/resources/static`, then run the backend. `localhost:8080` serves the whole app, frontend and API, from one process. | A production-style deployment test |

> In single-JAR mode, Vue Router runs in history mode, so refreshing on a route like `/admin/customers` sends a real request for that path. `SpaFallbackController` forwards any dot-less path to `index.html` so the router can take over client-side, while real `/api/**` routes still win — Spring always matches the most specific registered pattern first.

---

##  Repository Layout

The system lives in three sibling projects:

```
hairSalonManagementSystem-backend/     <- this repo: the Spring Boot REST API
├── src/main/java/za/ac/cput/
│   ├── config/          CorsConfig (+ SpaFallbackController in single-JAR mode)
│   ├── controller/      12 @RestControllers under /api/*
│   ├── domain/          12 JPA entities
│   │   ├── enums/       9 enums
│   │   └── valueobject/ 5 immutable value objects
│   ├── exception/       ApiError + GlobalExceptionHandler
│   ├── factory/         static build*() methods
│   ├── repository/      12 Spring Data repositories
│   ├── service/         12 interfaces + generic IService<T, ID>
│   │   └── impl/        12 implementations — all business rules
│   ├── util/            Helper.java
│   └── Main.java
├── src/main/resources/  application.properties, application-h2.properties
├── src/test/java/       34 test classes, 233 tests
└── pom.xml

hairSalonManagementSystem-frontend/    <- the Vue 3 SPA
└── src/
    ├── api/             one typed module per resource + shared client.ts, types.ts
    ├── components/      layout/ (AdminLayout, PublicLayout, header) + UI library
    ├── composables/     useSession.ts — the localStorage pseudo-session
    ├── config/          menus.ts — sidebar menus per role
    ├── router/          route definitions (history mode)
    └── views/
        ├── public/      Home, About, Services, Stylists, Gallery, Contact, BookAppointment
        ├── Auth/        Signin, Signup (simplified — see Authentication)
        ├── customer/    customer dashboard pages
        ├── stylist/     stylist dashboard pages
        └── admin/       10 CRUD dashboards
```

---

## 👥 Team

**Leader**
- *Marc Kabala* — System coordination & architecture

**Contributors**

| Member | Student № | Entity ownership |
| --- | --- | --- |
| Marc Kabala | 230701876 | Customer |
| Dayyaan Francis | 222277343 | Appointment |
| Will Koeries | 240160711 | Stylist |
| Witcha Francisco | 222894822 | Service |
| Reece Josephs | 218152701 | Payment |

---

## 📊 UML Class Diagram

<img width="1980" height="1800" alt="assignment1UML" src="https://github.com/user-attachments/assets/860ca28a-36cb-4f49-9b79-9fb5def4e2a6" />
