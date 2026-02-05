# 🎫 TicketBlitz - High-Concurrency Event Ticketing Platform

**TicketBlitz** is a polyglot, production-grade distributed system designed to handle high-traffic event booking. It combines **Java Spring Boot** for transactional integrity with **Python FastAPI** and **LangChain** for high-performance AI operations.

It features a "Survival Mode" recommendation engine, prevents "double-booking" using **Pessimistic Locking**, and validates end-to-end reliability via a custom **Java E2E Test Suite**.

## 📂 Project Structure

```text

TicketBlitz/

├── .idea/                      # IDE Configuration

├── api-gateway/                # Spring Cloud Gateway (Entry Point & Rate Limiting)

├── auth-service/               # OAuth2/JWT Authentication & RBAC

├── booking-service/            # Booking Saga Orchestrator (State Machine)

├── event-service/              # Inventory Management (Pessimistic Locking)

├── payment-service/            # Stripe Integration & Payment Reconciliation

├── notification-service/       # Email Delivery (Mailtrap)

├── recommendation-service/     # Python + LangChain + Vector Engine

├── user-service/               # User Profile Management

├── docker-compose.dev.yml      # Infrastructure Orchestration (Postgres, Redis, Kafka, Zookeeper)

├── TicketBlitzE2ETest.java     # Single-file E2E Safety Net

└── pom.xml                     # Root Maven Aggregator

```

---

## 🏗️ Polyglot Architecture

The system is built on a **Microservices Architecture** with 7 distinct services, all communicating within a distributed Docker network.

| Service | Lang | Port | Responsibility |

| --- | --- | --- | --- |

| **API Gateway** | Java | `8080` | Entry point, Rate Limiting, Route Management. |

| **Auth Service** | Java | `8081` | Stateless JWT issuance & validation. |

| **Event Service** | Java | `8082` | Inventory management, Optimistic/Pessimistic locking. |

| **Booking Service** | Java | `8083` | Core transaction engine, Distributed Sagas. |

| **Payment Service** | Java | `8084` | Stripe integration, Payment processing. |

| **User Service** | Java | `8085` | User profile persistence & management. |

| **Notification Service** | Java | `8086` | Email delivery (Mailtrap) |

| **Recommendation Service** | Python | `8087` | LangChain Orchestrator, RAG, Vector Search, pybreaker, pydantic |

### 🧠 Key Patterns Implemented

- **Database per Service:** Strict data isolation using 4 separate PostgreSQL instances.

- **Saga Pattern (Choreography):** Event-driven consistency flows using Kafka (Booking Payment Notification).

- **Remote Lock Pattern:** Centralized inventory locking in the Event Service to ensure data integrity across distributed transactions.

- **Compensating Transactions:** Automated rollback mechanisms (releasing tickets) if payment fails.

- **Stateless Authentication:** JWT-based security with centralized User persistence.

---

## 🛡️ Production Readiness & Resilience

This system has been hardened for production environments using **Resilience4j** and verified under heavy load.

### 1. Resilience Patterns (Implemented)

- **Circuit Breakers:** Configured on all internal Feign Clients to prevent cascading failures when a downstream service is slow or unreachable.

- **Rate Limiters:** Applied to public-facing API Gateway routes to prevent abuse (Token Bucket algorithm).

- **Retry Mechanisms:** Configured for transient network glitches during Kafka message consumption with exponential backoff.

---

## 🛠️ Tech Stack

- **Core:** Java 21, Spring Boot 3.3+, Python 3.11, FastAPI, Pydantic

- **Messaging:** Apache Kafka, Zookeeper

- **Resilience:** Resilience4j (Circuit Breaker, Rate Limiter, Retry)

- **Database:** PostgreSQL (Primary), Redis (Caching), pgvector (Vector DB)

- **Observability:** Micrometer Tracing, OpenTelemetry, Zipkin/Jaeger

- **AI:** LangChain (Orchestration, Prompt Templates, Model Chaining), Google Gemini 2.5 Flash (GenAI)

- **Payments:** Stripe API

- **Testing:** JUnit 5, Mockito (Unit), Testcontainers (Integration)

---

## 🚀 Getting Started

### Prerequisites

- Docker & Docker Compose

- Java 21 JDK

- Maven

### Run the "World"

We use a unified Docker Compose file to spin up the entire platform (Services + Infrastructure).

```bash

# 1. Build all services (from root)

mvn clean package -DskipTests



# 2. Start Infrastructure & Microservices

docker-compose -f docker-compose.dev.yml up -d



```

### Access Points

- **API Gateway:** `http://localhost:8080`

- **Jaeger UI (Tracing):** `http://localhost:16686`

- **Mailtrap (Emails):** Check your Mailtrap Sandbox inbox.

---

## 🧪 Testing Strategy

We enforce a strict **Quality Gate** of **90% Code Coverage**.

- **Unit Testing:** We use `Mockito` to isolate business logic, covering happy paths, edge cases, and exception handling.

- **Integration Testing:** We use `Testcontainers` (Real Postgres/Kafka) to verify end-to-end flows without mocking database interactions.

To run the full test suite:

```bash

mvn test



```

---

## 🔮 Deployment Roadmap

While the system is fully containerized with Docker, the final phase involves Orchestration.

- **Kubernetes (K8s):**

- Deployment manifests (`deployment.yaml`, `service.yaml`) for high availability.

- **Ingress Controller** setup for routing external traffic.

- **Horizontal Pod Autoscaling (HPA)** based on CPU/Memory metrics.

---

**Developed by [Abhinav Pathak]** | _TicketBlitz 2025_

```



```
