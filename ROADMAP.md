# DropGate "Core Slice" - Build Roadmap

> A fair-queue flash-sale engine with real-time bot detection, built as a senior full-stack portfolio project.
> This file is the single source of truth for the Cursor agent. Work phase by phase, tick checkboxes as you go.

---

## 0. How the agent must work

1. Read this whole file before writing code. Re-read the current phase before starting it.
2. Work on **one phase at a time**. Do not start a phase until the previous phase's "Exit criteria" pass.
3. After each task: build, run tests, and fix failures before moving on.
4. Prefer small, reviewable commits using Conventional Commits (`feat(order): add hold expiry worker`).
5. Never hardcode secrets. Use env vars and `application-*.yml` profiles.
6. When a design choice is ambiguous, pick the simplest option, then record it as a short ADR in `docs/adr/`.
7. Keep each service small and independently runnable. No shared "common" module with business logic (only shared event DTOs in `libs/events`).
8. Update the checkboxes in this file as tasks complete.
9. **Do not stop until the whole application is finished and running in Docker.** See section 0b (Completion mandate), which overrides any instruction elsewhere to pause or wait for approval between phases.
10. Progress lives in this file (checkboxes) and in Git commits. If your context gets long or you lose track, re-read this file, check which boxes are ticked, and resume from the first unticked task.

### Definition of Done (applies to every task)
- Compiles, tests pass, `docker compose up` still works.
- New behavior has at least one test (unit or Testcontainers integration).
- No TODOs left without a linked checkbox in this file.
- README or ADR updated if behavior or design changed.

---

## 0b. Completion mandate (MANDATORY - read twice)

**Your job is not finished until the complete DropGate application is built, tested, and running in Docker containers.** Partial progress is not an acceptable stopping point.

### Rules
1. **Run through all phases (0 to 6) continuously.** When a phase's exit criteria pass, immediately start the next phase. Do **not** stop to ask for approval or say "let me know when to continue". Post a short phase summary in the chat and keep going.
2. **Stop and ask me only for true blockers**: the external Postgres is unreachable and you cannot fix it, a credential or file that only I can provide is missing, or a requirement here is impossible. For everything else, choose the simplest reasonable option, record it in an ADR, and continue.
3. **No unfinished work.** No stubs, placeholder implementations, `TODO`s, "left as an exercise", mocked-out core features, or commented-out code standing in for a real feature. Every checkbox in this file must be genuinely implemented, except items cut or simplified under section 0c.
4. **Fix, don't skip.** When a build, test, container, or health check fails, find the root cause and fix it, then re-run. Repeat until it passes. Never delete, disable (`@Disabled`), or weaken a test to make it pass.
5. **Docker is the finish line.** The final state must be the full stack built from the Dockerfiles and started with Docker Compose, all containers healthy, and the application usable end to end. Leave the stack **running** when you finish.
6. If a long task is interrupted, resume from the first unticked checkbox rather than restarting.

### Definition of Complete (all must be true before you report "done")
- [ ] All checkboxes in Phases 0 to 6 are ticked or explicitly marked `[-]` cut under section 0c, and every Exit criteria block passes (as simplified by 0c).
- [ ] From a clean state, `docker compose --profile core --profile search up --build -d` builds every image from its Dockerfile and starts every container; `docker compose ps` shows all of them healthy.
- [ ] The dashboard is reachable at `http://localhost:8080` and every backend `/actuator/health` returns `UP`.
- [ ] The end-to-end smoke test (`scripts/smoke-test.sh`, Phase 6) passes against the **running containers**.
- [ ] The zero-oversell concurrency test passes (run 3 times in a row).
- [ ] Fraud closed loop works: simulated bot traffic is flagged and throttled while normal users are unaffected.
- [ ] Operator view shows live stock, fraud scores, charts, and working search; analytics and forensic endpoints return correct data.
- [ ] The k6 load test has run and `docs/loadtest-results.md` contains real numbers.
- [ ] README, architecture doc, and ADRs are written; CI workflow is committed.
- [ ] Final report delivered (see Phase 6).
- [ ] Total elapsed time from the first command to the stack running in Docker is **less than 1 day (24 hours)**, proven by timestamps in `docs/build-log.md` (section 0c).

---

## 0c. Time budget: complete and running in Docker in LESS THAN 1 DAY (MANDATORY)

**Hard limit: the whole build, from the first command to the full stack running in Docker, must take less than 24 hours. Target about 16 hours of work and keep the rest as buffer.** A lean, working end-to-end system beats breadth and polish.

**Precedence:** where this section conflicts with a phase's task list, **this section wins**. Apply the cut or simplification below instead of the original task. Mark a cut item `[-]` and append `(cut: 1-day plan)`; cut items count as done. Never cut an entire required skill area (Spring IoC, Java Streams, advanced SQL, containers/microservices, Kafka, Redis, Elasticsearch/MongoDB): cut depth, not coverage.

### Schedule and milestones (must hit)
| Hours | Phase | Must be true at the end |
|---|---|---|
| 0-1 | Phase 0 | All Dockerfiles and compose start; services connect to the external `dropgate` database |
| 1-4 | Phase 1 | Redis Lua holds, checkout, expiry worker work in Docker; zero-oversell test passes |
| 4-6 | Phase 2 | Queue and React shopper flow work in Docker |
| 6-9 | Phase 3 | Outbox, Kafka, Kafka Streams fraud closed loop work in Docker |
| 9-13 | Phase 4 | Analytics SQL, Java Streams report, Elasticsearch, MongoDB, operator view work in Docker |
| 13-14 | Phase 5 | Short load test, 2 ADRs, README diagram, minimal CI file |
| 14-16 | Phase 6 | Clean rebuild, smoke test passes, final report. **Stack left running.** |
| 16-24 | Buffer | Only for fixing failures. Do not add scope. |

If you are more than 2 hours behind the table at any milestone, apply the "Emergency cuts" below immediately.

### Scope cuts (applies to the phase tasks)
| Phase | Do this (simplified) | Cut |
|---|---|---|
| 0 | Dockerfiles for all 5 deployables, one compose file, `.env.example`, build log | Makefile, CI beyond a minimal workflow |
| 1 | Strategy map with **2** strategies (FIFO, lottery), `@ConditionalOnProperty` inventory swap, `@ConfigurationProperties` records, request-scoped fingerprint, Lua create/release hold, checkout with idempotency key, Resilience4j circuit breaker on the simulated payment gateway, `SKIP LOCKED` expiry worker, `@Audited` BeanPostProcessor | `TieredPriorityAllocationStrategy`, multi-instance expiry test (keep single-instance expiry test) |
| 2 | Join, **polling** for position (every 2s), admission token, token-bucket rate limiter, cache-aside with stampede guard and TTL jitter, write-behind view counter, one React shopper page | SSE/WebSocket, sliding-window-log limiter, lottery mode in queue, negative caching |
| 3 | Outbox + relay with `SKIP LOCKED`, DLT, Kafka Streams with: 10s windowed click count, stream-table join with known-bad devices, running-risk KTable, `exactly_once_v2`, `TopologyTestDriver` test, closed loop to Redis flag | Distinct-accounts signal, interactive-query endpoint, Testcontainers Kafka integration test, kill-Kafka replay test |
| 4 | SQL: one window-function query, one recursive-CTE ring query, one `LATERAL` query, one materialized view with concurrent refresh, one JSONB/GIN query, `EXPLAIN ANALYZE` before/after for **one** query, seed ~50k rows. Streams: `teeing`, `partitioningBy`, `flatMap`, custom percentile `Collector`, 10-line parallel-vs-sequential timing note. Elasticsearch: one index template with one custom analyzer, Kafka consumer, one search endpoint with filters + highlighting, one `date_histogram`. MongoDB: insert-only audit trail + **one** aggregation endpoint. React: **one** operator page (stock gauge, fraud feed table, one chart, search box) | Session reconstruction, top-IP-range aggregation, raw-fingerprint documents, forensics page, drops table page |
| 5 | k6 with **500** virtual users + a small bot cohort, results table with real numbers, ADR-001 (Redis Lua vs row locking) and ADR-003 (datastore choices), Mermaid architecture diagram in README, minimal GitHub Actions workflow (build + test) | 5.2 resilience checks, 5.3 Redis Cluster, 5.4 observability, ADR-002/004/005 (cover the outbox and Kafka Streams reasoning in 3-4 lines of the README instead), consumer-lag capture |
| 6 | Clean rebuild, `scripts/smoke-test.sh` passing, oversell test run **3 times**, README, final report | Resource tuning beyond fixing unstable containers |

### Speed rules
1. **Thin end-to-end first.** Get queue -> hold -> checkout running **in Docker** in the first hours, then deepen. Build the Docker images from Phase 0 so there are no integration surprises later.
2. **Time boxes are hard.** If a task takes about double its share, implement the simplest version that satisfies the checkbox, note it under "Shortcuts" in `docs/architecture.md`, and move on.
3. **Focused tests only:** zero-oversell concurrency, idempotency, Lua scripts, single-instance expiry, `TopologyTestDriver`, the native SQL queries, and the smoke test. No exhaustive tests of trivial code.
4. **Don't hand-roll.** Use Spring Boot starters, Spring Data, Spring Kafka, Testcontainers modules. Minimal UI: plain CSS or one lightweight library, no design polish.
5. **Fast Docker builds.** Copy `pom.xml` files first so dependency layers cache, use BuildKit cache mounts, and skip tests inside image builds (`-DskipTests`; tests run separately).
6. **Don't ask questions you can answer yourself.** Choose the simplest option, note it, and continue.
7. **Log the time.** `docs/build-log.md` holds the start timestamp (`date -u`) and one timestamp per finished phase.

### Emergency cuts (when behind schedule, in this order)
1. Drop remaining SHOULD items and polish (README extras, second ADR).
2. Reduce Elasticsearch to one index plus the search endpoint (no aggregation), and MongoDB to audit insert plus one read endpoint.
3. Reduce the operator view to the fraud feed and search box.
4. Reduce the load test to 200 virtual users.
Do **not** cut the zero-oversell test, the fraud closed loop, or the Docker finish line.

---

## 1. Product summary

Thousands of users try to buy a small number of items at the same moment. DropGate:
- puts shoppers in a **fair virtual queue**,
- **never oversells** inventory,
- **scores bot behavior in real time** from click and order streams and throttles suspicious users,
- gives operators a **React dashboard** to watch the drop live and search/analyze afterward.

---

## 2. Tech stack (pin these versions)

| Area | Choice |
|---|---|
| Language / build | Java 25, Maven (multi-module) |
| Framework | Spring Boot 3.3.x, Spring Data JPA, Spring Kafka, Spring Data Redis |
| Relational DB | PostgreSQL (my **existing external container**, host port 5432, database `dropgate`; see section 2b) |
| Cache / queue / locks | Redis 7 |
| Streaming | Apache Kafka (KRaft mode) + Kafka Streams |
| Search | Elasticsearch 8.x |
| Document DB | MongoDB 7 |
| Resilience | Resilience4j |
| Migrations | Flyway |
| Testing | JUnit 5, Testcontainers, AssertJ, k6 (load) |
| Frontend | React 18, TypeScript, Vite, TanStack Query, Recharts, React Router |
| CI | GitHub Actions |
| Packaging | **Everything runs in Docker**: one multi-stage Dockerfile per deployable + one root `docker-compose.yml` |

---

## 2b. Docker-first requirement and external PostgreSQL (MANDATORY)

The entire project must **build and run in Docker**. The only things needed on the host are Docker and my already-running Postgres container.

### Docker rules
- Every deployable has its **own Dockerfile**: `order-service`, `queue-service`, `fraud-stream-service`, `insights-service`, and `dashboard`.
- Java services: multi-stage (Maven build stage -> Temurin JRE 25 slim runtime), layered JAR extraction, **non-root user**, `HEALTHCHECK` hitting `/actuator/health`, and a `.dockerignore`.
- Dashboard: multi-stage (Node build -> nginx runtime), with an `nginx.conf` that serves the SPA and proxies `/api/*` to the backend services. Non-root where possible.
- One root `docker-compose.yml` builds and runs everything: `docker compose up --build` is the single command to start the system.
- Compose must include: a user-defined network, **named volumes** for Redis, Kafka, Elasticsearch and MongoDB, healthchecks on every service, `depends_on` with `condition: service_healthy` for services defined in the same compose file, `restart: unless-stopped`, and all config via environment variables (no secrets baked into images).
- Compose **profiles**: `core` (redis, kafka, order-service, queue-service, fraud-stream-service, dashboard), `search` (elasticsearch, mongodb, insights-service), `cluster` (optional Redis Cluster), `observability` (optional Prometheus/Grafana).
- Fixed host ports: dashboard 8080, order-service 8081, queue-service 8082, fraud-stream-service 8083, insights-service 8084, redis 6379, kafka 9092, elasticsearch 9200, mongodb 27017.
- Provide `.env.example` (committed) and `.env` (gitignored).
- Tests that need infrastructure use **Testcontainers** (their own throwaway containers), never my external Postgres.

### External PostgreSQL (do NOT create a postgres service)
Do **not** define a `postgres` service in `docker-compose.yml`. Use my existing Postgres container, which is already running and publishes port 5432 on the host.

| Setting | Value |
|---|---|
| Host from containers | `host.docker.internal` (add `extra_hosts: ["host.docker.internal:host-gateway"]` to every service that uses Postgres, required on Linux) |
| Host from local runs | `localhost` |
| Port | `5432` |
| Database | `dropgate` (a dedicated database, never touch other databases) |
| User / password | env vars `PG_USER` / `PG_PASSWORD`, defaults `postgres` / `postgres` in `.env.example` (local dev only) |
| JDBC URL | `jdbc:postgresql://host.docker.internal:5432/dropgate` |

Rules for the agent:
- Never drop, alter, or write to any database other than `dropgate`.
- The `dropgate` database is created once by me (or by a documented helper script `scripts/create-db.sh` using `docker exec <container> psql -U postgres -c "CREATE DATABASE dropgate;"`). Document this in the README.
- Schema is created only through **Flyway migrations** run by the services on startup.
- Because Postgres is external, `depends_on` cannot wait for it. Configure startup retries (for example `spring.flyway.connect-retries=20` and a Hikari initialization timeout) so services survive a slow or late database.
- Create a `local` Spring profile (`application-local.yml`) that uses `localhost:5432` for running a service outside Docker.
- Postgres version should be 11 or newer (covering indexes need `INCLUDE`); 15 or 16 preferred. Verify with `SELECT version();` in Phase 0 and report it.

---

## 3. Repository layout

```
dropgate/
├── pom.xml                      # parent POM (dependencyManagement, plugins)
├── docker-compose.yml           # builds and runs everything (no postgres service)
├── .env.example                 # PG_USER, PG_PASSWORD, ports, etc.
├── .github/workflows/ci.yml
├── scripts/
│   └── create-db.sh             # creates the "dropgate" DB in the external Postgres
├── libs/
│   └── events/                  # shared event records only (no logic)
├── services/
│   ├── order-service/           # :8081  (Dockerfile, .dockerignore)
│   ├── queue-service/           # :8082  (Dockerfile, .dockerignore)
│   ├── fraud-stream-service/    # :8083  (Dockerfile, .dockerignore) Kafka Streams
│   └── insights-service/        # :8084  (Dockerfile, .dockerignore)
├── dashboard/                   # React app: Vite dev :5173 / nginx container :8080
│   ├── Dockerfile               # node build -> nginx runtime
│   └── nginx.conf               # SPA + /api proxy
├── loadtest/
│   └── k6/
├── docs/
│   ├── architecture.md
│   ├── adr/
│   └── explain-analyze/
└── ROADMAP.md                   # this file
```

---

## 4. Architecture overview

```
 React Dashboard
      │  REST + SSE
      ▼
 queue-service ──(Redis ZSET, token bucket)──► Redis
      │  admits user, issues checkout token
      ▼
 order-service ──(Lua: atomic hold)──► Redis
      │  └──► PostgreSQL (orders, holds, outbox)
      │            │ outbox relay
      ▼            ▼
   Kafka: click-events, order-events, inventory-changes
      │
      ▼
 fraud-stream-service (Kafka Streams) ──► fraud-scores ──► queue-service / order-service (throttle)
      │
      ▼
 insights-service ──► Elasticsearch (search/aggs) + MongoDB (audit trail, fingerprints)
                 └─► PostgreSQL read queries (native SQL, Java Streams reports)
```

### Kafka topics
| Topic | Key | Producer | Consumers |
|---|---|---|---|
| `click-events` | fingerprintId | queue-service | fraud-stream, insights |
| `order-events` | orderId | order-service (via outbox) | fraud-stream, insights |
| `inventory-changes` | dropId | order-service (via outbox) | insights |
| `fraud-scores` | fingerprintId | fraud-stream | queue-service, order-service, insights |
| `audit-events` | entityId | all (via `@Audited`) | insights (Mongo) |
| `*.DLT` | same | error handlers | manual / insights |

### Redis key conventions
| Key | Type | Purpose |
|---|---|---|
| `drop:{dropId}:stock` | string (int) | available units |
| `drop:{dropId}:holds` | hash | holdId -> userId |
| `drop:{dropId}:queue` | sorted set | score = join time (or lottery rank) |
| `drop:{dropId}:meta` | string (JSON) | cached drop metadata |
| `rl:{fingerprintId}` | string/hash | token bucket state |
| `flagged:{fingerprintId}` | string + TTL | throttled by fraud score |
| `views:{dropId}` | string (counter) | write-behind view counter |

Use hash tags (`drop:{dropId}:...`) so multi-key Lua scripts stay on one cluster slot.

---

## Phase 0 - Scaffold & infrastructure (Hours 0-1)

### Tasks
- [ ] Create parent Maven POM with Java 25, Spring Boot BOM, Spotless/Checkstyle (optional).
- [ ] Create the four service modules and `libs/events` with empty Spring Boot apps and `/actuator/health`.
- [ ] Write the root `docker-compose.yml` with: redis, kafka (KRaft), elasticsearch (single node, security off for dev), mongodb, the four services, and the dashboard. **No postgres service** (see section 2b).
- [ ] Add Compose **profiles**: `core` (redis, kafka, order-service, queue-service, fraud-stream-service, dashboard) and `search` (elasticsearch, mongodb, insights-service).
- [ ] Multi-stage Dockerfile **per Java service**: build stage (Maven), runtime stage (Temurin JRE 25 slim), layered JAR extraction, **non-root user**, `HEALTHCHECK`, plus a `.dockerignore`.
- [ ] Dockerfile for `dashboard/` (Node build -> nginx) and `dashboard/nginx.conf` proxying `/api` to the services.
- [ ] Connect services to my **external Postgres** exactly as in section 2b (`host.docker.internal`, `extra_hosts`, database `dropgate`, `PG_USER`/`PG_PASSWORD`), add Flyway connect retries, and create `scripts/create-db.sh`.
- [ ] Verify the Postgres connection and version (`SELECT version();`) and report the result.
- [ ] Create `.env.example` (with `PG_USER=postgres`, `PG_PASSWORD=postgres`, ports) and wire env vars into Compose; add `.env` to `.gitignore`.
- [ ] Scaffold `dashboard/` with Vite + React + TypeScript, ESLint, and a placeholder page.
- [ ] Add `.github/workflows/ci.yml` skeleton (build + test Java, build dashboard).
- [ ] Add `Makefile` with `up`, `down`, `test`, `logs`, `loadtest`.
- [ ] Create `docs/build-log.md`. Run `date -u` and record the **start timestamp** now; append a timestamp line at the end of every phase (section 0c).

### Exit criteria
- `docker compose --profile core up --build` builds every image from its Dockerfile and starts everything, and all health checks are green.
- Every Java service connects to the external `dropgate` database and Flyway runs successfully from inside its container.
- `docker compose config` shows no `postgres` service.
- CI runs on push and passes.

---

## Phase 1 - order-service: inventory, holds, checkout (Hours 1-4)

### 1.1 Postgres schema (Flyway `V1__init.sql`)
Tables (suggested columns):
- `drops(id, name, total_stock, price_cents, starts_at, ends_at, strategy, config jsonb)`
- `accounts(id, email, created_at)`
- `devices(id, fingerprint, first_seen_at)`
- `account_devices(account_id, device_id, first_seen_at)`  *(for ring detection)*
- `payment_methods(id, account_id, card_hash, billing_address_hash)`
- `holds(id, drop_id, account_id, quantity, status, expires_at, created_at)`  status: `ACTIVE|CONVERTED|EXPIRED|RELEASED`
- `orders(id, drop_id, account_id, hold_id, status, idempotency_key unique, total_cents, created_at, attempt_meta jsonb)`
- `outbox(id, aggregate_type, aggregate_id, event_type, payload jsonb, created_at, published_at null)`

Indexes to include:
- [ ] **Partial index** on `holds(expires_at) WHERE status = 'ACTIVE'`.
- [ ] **Covering index** on `orders(drop_id, created_at) INCLUDE (account_id, status, total_cents)`.
- [ ] **GIN index** on `orders.attempt_meta` and `drops.config`.
- [ ] Unique index on `orders(idempotency_key)`.

### 1.2 Spring IoC: allocation strategies
- [ ] Define `interface AllocationStrategy { String name(); AllocationDecision decide(AllocationRequest r); }`.
- [ ] Implement `FifoAllocationStrategy`, `LotteryAllocationStrategy`, `TieredPriorityAllocationStrategy`.
- [ ] Inject all as `Map<String, AllocationStrategy>` into `AllocationService`; select by `drops.strategy`.
- [ ] `@ConfigurationProperties` **records** for `dropgate.hold-ttl`, `dropgate.max-per-account`, strategy config. Validate with `@Validated`.
- [ ] `InventoryPort` interface with two implementations:
  - `InMemoryInventory` (`@Profile("test")` or `@ConditionalOnProperty(dropgate.inventory=memory)`)
  - `RedisInventory` (default)
- [ ] Constructor injection everywhere (no field `@Autowired`).
- [ ] A **request-scoped** `FingerprintContext` bean populated by a servlet filter from headers.

### 1.3 Redis atomic hold (Lua)
- [ ] `create_hold.lua`: KEYS = stock key, holds hash. ARGV = holdId, userId, qty, ttl. Returns remaining stock or `-1` if insufficient. Atomic check + decrement + record hold.
- [ ] `release_hold.lua`: atomically restore stock if the hold still exists.
- [ ] Load scripts via `RedisScript` beans; test with Testcontainers Redis.
- [ ] Enforce per-account max purchase inside the script or immediately before it.

### 1.4 Checkout flow
- [ ] `POST /api/drops/{id}/holds` -> creates hold (requires valid admission token from queue-service).
- [ ] `POST /api/checkout` with `Idempotency-Key` header -> converts hold to order in one DB transaction. Replays with the same key return the original response.
- [ ] Simulated payment gateway behind Resilience4j **circuit breaker + timeout + retry**.
- [ ] Hold expiry worker: scheduled job using native SQL:
  ```sql
  SELECT id, drop_id, quantity FROM holds
  WHERE status = 'ACTIVE' AND expires_at < now()
  ORDER BY expires_at
  LIMIT 100
  FOR UPDATE SKIP LOCKED;
  ```
  For each row: mark `EXPIRED`, run `release_hold.lua`, write outbox event. Safe with multiple instances.

### 1.5 `@Audited` via BeanPostProcessor
- [ ] Create `@Audited` annotation.
- [ ] Implement `AuditedBeanPostProcessor` that wraps annotated beans in a proxy and publishes an `AuditEvent` (method, args summary, user, outcome, duration) to `audit-events`. (Use a proxy factory; explain in an ADR why not plain AOP.)

### 1.6 Tests
- [ ] **Concurrency test (critical):** 500 stock, 5,000 concurrent hold attempts via virtual threads. Assert final holds + remaining stock = 500 and **zero oversell**.
- [ ] Idempotency test: same key twice yields one order.
- [ ] Expiry test: two worker instances, no double release.

### Exit criteria
- Oversell test passes repeatedly (run 10x).
- Swapping `dropgate.inventory=memory|redis` works with no code change.
- All strategies selectable per drop.

---

## Phase 2 - queue-service + React shopper view (Hours 4-6)

### Tasks
- [x] `POST /api/drops/{id}/queue/join` -> adds fingerprint/account to `drop:{id}:queue` ZSET; returns ticket id and position.
- [x] `GET /api/drops/{id}/queue/position` -> `ZRANK` lookup.
- [-] **SSE** endpoint `GET /api/drops/{id}/queue/stream` pushing position updates (Redis pub/sub or Streams -> `SseEmitter`) (cut: 1-day plan).
- [x] Admission scheduler: every N ms, pop the next K users (`ZPOPMIN`) based on remaining stock and issue a signed, short-lived **admission token** (JWT or HMAC).
- [-] Lottery mode: assign random score at join close time instead of timestamp (reuse the IoC strategy idea) (cut: 1-day plan).
- [x] **Rate limiting:** token bucket in Lua keyed by fingerprint; plus a sliding-window log variant (ZSET) for comparison. Choose via config.
- [x] **Cache-aside** for drop metadata (`drop:{id}:meta`):
  - TTL with random jitter,
  - **single-flight lock** (`SET NX PX`) to prevent stampede,
  - negative caching for unknown drops.
- [x] **Write-behind view counter:** `INCR views:{id}` in Redis, flushed to Postgres every 5s by a scheduled job.
- [x] Publish a `click-event` to Kafka for every join/poll/checkout attempt (fingerprint, ip, user-agent hash, action, timestamp).
- [x] Consume `fraud-scores`; when score > threshold, set `flagged:{fingerprintId}` with TTL and apply stricter rate limit or reject.

### React shopper view (`dashboard/`)
- [ ] Router: `/` (drop list), `/drops/:id` (shopper), `/operator` (Phase 4).
- [ ] Drop page: details, stock indicator, **Join queue** button.
- [ ] Live position via `EventSource`; show estimated wait; reconnect on drop.
- [ ] When admitted: countdown for the hold, **Checkout** button with generated idempotency key.
- [ ] States: queued, admitted, holding, purchased, sold out, throttled.
- [ ] Generate a lightweight client fingerprint (canvas/UA hash) and send as `X-Fingerprint`.

### Exit criteria
- Two browser tabs can queue, get admitted in order, and see position update live.
- Stampede test: 1,000 concurrent requests on a cold cache cause exactly **one** DB load.

---

## Phase 3 - Outbox, Kafka & fraud stream (Hours 6-9)

### 3.1 Outbox pattern
- [ ] Order/hold/inventory changes write a row to `outbox` **in the same transaction**.
- [ ] Outbox relay: poll unpublished rows with `FOR UPDATE SKIP LOCKED`, publish to Kafka (idempotent producer, `acks=all`), mark `published_at`.
- [ ] Define event records in `libs/events` (with a version field).
- [ ] Test: kill Kafka mid-flight, restart, verify no lost events and no duplicates downstream (dedupe by event id).

### 3.2 Kafka reliability
- [ ] Consumers use manual/at-least-once with retry + **DLT** (`DefaultErrorHandler` + `DeadLetterPublishingRecoverer`).
- [ ] Idempotent consumer logic where needed (processed-event table or natural idempotency).

### 3.3 fraud-stream-service (Kafka Streams)
- [ ] Topology:
  1. `click-events` -> `groupByKey` (fingerprint) -> **tumbling window 10s** count -> `clicks_per_10s`.
  2. Windowed count of **distinct accounts per fingerprint** (detect account farming).
  3. **KTable** `known-bad-devices` (compacted topic, seed via CSV/endpoint) -> **stream-table join** with click stream.
  4. Combine signals into a **KTable of running risk score** per fingerprint (weighted sum with decay).
  5. Emit to `fraud-scores` only when score crosses a threshold or changes materially.
- [ ] `processing.guarantee=exactly_once_v2`.
- [ ] Expose an interactive-query endpoint: `GET /api/fraud/scores/{fingerprint}`.
- [ ] Unit test the topology with `TopologyTestDriver`.
- [ ] Integration test with Testcontainers Kafka.

### 3.4 Closed loop
- [ ] queue-service and order-service consume `fraud-scores` and flag fingerprints in Redis.
- [ ] Demo script: simulate a bot hitting 200 req/s; show it throttled within ~5 seconds.

### Exit criteria
- Bot traffic gets flagged and throttled end-to-end while normal users are unaffected.
- Topology test suite passes; DLT receives poison messages.

---

## Phase 4 - insights-service + Elasticsearch + Mongo + React operator view (Hours 9-13)

### 4.1 Advanced native SQL (Postgres) - `AnalyticsRepository`
Implement each as a native query with tests against Testcontainers Postgres:
- [ ] **Window functions:** per account, `LAG(created_at)` to compute seconds between purchase attempts; `RANK()` of accounts by attempt frequency per drop; running sum of sales per minute.
- [ ] **Recursive CTE:** find **linked-account rings** (accounts connected via shared device, card hash, or address hash), return ring id and members, with a depth limit and cycle guard.
- [ ] **LATERAL join:** for each drop, the top 3 most recent suspicious accounts.
- [ ] **Materialized view** `drop_summary_mv` (units sold, revenue, unique buyers, p50/p95 checkout time) with `REFRESH MATERIALIZED VIEW CONCURRENTLY` on a schedule (needs a unique index).
- [ ] **JSONB** query using GIN-backed operators (`@>`, `->>`) on `attempt_meta`.
- [ ] Save `EXPLAIN (ANALYZE, BUFFERS)` output **before and after** indexing into `docs/explain-analyze/` and summarize in README.
- [ ] Seed script generating ~50k order/attempt rows (reduced for the 1-day budget) so plans are still meaningful.

### 4.2 Java Streams - `ReportService`
- [ ] `groupingBy` + downstream `teeing` (e.g., total revenue and count in one pass).
- [ ] `partitioningBy` (suspicious vs. normal accounts).
- [ ] `flatMap` to flatten ring members into account lists.
- [ ] **Custom `Collector`** computing p50/p95/p99 latency per drop.
- [ ] Session reconstruction from raw click events using a gap rule (sliding window via `Stream.iterate` / `takeWhile` or a custom spliterator).
- [ ] **Benchmark** (simple timing harness, no JMH): sequential vs parallel streams on ~1M in-memory events; document in 5-10 lines where parallel hurts (small data, ordered ops, shared state).

### 4.3 Elasticsearch
- [ ] Index templates for `orders-*` and `fraud-*` with **custom analyzer** (e.g., edge-ngram on email, keyword for IPs, `ip` type for address ranges).
- [ ] Kafka consumer indexes `order-events` and `fraud-scores` (bulk, idempotent by event id).
- [ ] `GET /api/search/events?q=&from=&to=&status=&minScore=` with filters, pagination, **highlighting**.
- [ ] Aggregations endpoint: per-minute purchase **date_histogram**, top suspicious IP ranges (`ip_range` or terms), score distribution.

### 4.4 MongoDB
- [ ] Consume `audit-events` and store **immutable** audit documents (insert-only repository; no update methods).
- [ ] Store raw device fingerprint documents (flexible schema).
- [ ] Aggregation-pipeline endpoints: forensic timeline for an account, fingerprints shared by >N accounts, actions per hour.
- [ ] Indexes: compound on `(accountId, ts)`, TTL optional for raw fingerprints.

### 4.5 React operator view (`/operator`)
- [ ] Live stock gauge and queue length.
- [ ] Live fraud-score feed (SSE or polling) with severity coloring.
- [ ] Recharts: per-minute purchase histogram, score distribution, top IP ranges.
- [ ] Search page: query box, filters, results with highlighted matches.
- [ ] Forensics page: enter an account id to see timeline (Mongo) and linked-account ring (SQL).
- [ ] Table of drops from the materialized view.

### Exit criteria
- All analytics endpoints return correct results on seeded data.
- Operator dashboard updates live during a simulated drop.

---

## Phase 5 - Load test, resilience, docs, CI (Hours 13-14, lite)

### 5.1 Load test (k6)
- [ ] Scenario: 500 virtual users join a drop of 100 items, plus a 5% bot cohort sending bursts (reduced for the 1-day budget; see section 0c).
- [ ] Thresholds: p95 join latency, error rate, zero oversell check at the end via API.
- [ ] Output a results table into `docs/loadtest-results.md`.
- [ ] Capture Kafka consumer lag during the test and include it in the results.

### 5.2 Resilience checks
- [ ] Stop Redis mid-test: order-service degrades safely (no oversell, clear errors).
- [ ] Stop Kafka: outbox backlog grows, then drains after restart.
- [ ] Payment gateway failure: circuit breaker opens, holds expire and release stock.

### 5.3 Data grid / Redis topology (CUT in the 1-day plan)
- [ ] Add an optional Compose profile `cluster` running **Redis Cluster** (3 primaries minimum).
- [ ] Verify Lua scripts work using hash tags; document slot behavior and a failover test in an ADR.

### 5.4 Observability (CUT in the 1-day plan)
- [ ] Micrometer metrics -> Prometheus -> Grafana dashboard (holds/sec, queue length, fraud flags, consumer lag).
- [ ] OpenTelemetry tracing across services.

### 5.5 Documentation
- [ ] `README.md`: pitch, GIF/screenshots, quickstart, architecture diagram (Mermaid), skill-to-feature map.
- [ ] ADRs in `docs/adr/`:
  - [ ] ADR-001: Why Redis Lua for inventory vs. DB row locking.
  - [ ] ADR-002: Outbox pattern vs. dual writes / CDC.
  - [ ] ADR-003: Datastore choices (Postgres vs. Elasticsearch vs. MongoDB per data shape).
  - [ ] ADR-004: Kafka Streams vs. plain consumers for fraud scoring.
  - [ ] ADR-005 (optional): Request fairness: FIFO vs. lottery.
- [ ] `docs/architecture.md` with trade-offs and a **"what I'd change at 10x scale"** section.

### 5.6 CI
- [ ] GitHub Actions: build, unit tests, Testcontainers integration tests, dashboard lint/build, Docker image build (no push required).
- [ ] Cache Maven and npm dependencies.

### Exit criteria
- Fresh clone + existing Postgres with an empty `dropgate` database -> `docker compose --profile core --profile search up --build` -> demo works, with every component running in a container.
- CI is green; README clearly maps each required skill to concrete code.

---

## Phase 6 - Final integration & acceptance (Hours 14-16)

This phase is what turns "all phases coded" into "application complete and running in Docker".

### Tasks
- [ ] Tear down everything and rebuild from scratch: `docker compose --profile core --profile search down -v` then `up --build -d`. Do not rely on cached state. (Never remove or reset my external Postgres; only drop objects inside the `dropgate` database if a clean schema is needed, and only via Flyway/clean scripts you wrote.)
- [ ] Write `scripts/smoke-test.sh` that, against the running containers: creates a drop, joins the queue with several fingerprints, gets admitted in order, creates holds and checks out with an idempotency key (replaying it once), verifies stock decreased correctly, fires a burst from one fingerprint and verifies it gets throttled, then calls the search, analytics, and forensic endpoints and checks the dashboard responds on port 8080. Exit non-zero on any failure.
- [ ] Run the smoke test and fix every failure. Repeat until it passes.
- [ ] Run the oversell concurrency test 3 times; all must pass.
- [ ] Run the k6 load test against the running stack and save real results.
- [ ] Review resource usage (`docker stats`) and adjust memory limits or JVM flags if any container is unstable or restarting.
- [ ] Confirm every checkbox is ticked or marked cut under section 0c; implement anything else that is missing now.
- [ ] Finalize README with: what the project is, architecture diagram, prerequisites (Docker + my existing Postgres with `dropgate` database), exact run commands, URLs and ports, how to run tests and the load test, and a skill-to-feature map.

### Exit criteria (final acceptance)
- Everything in "Definition of Complete" in section 0b is true.
- The stack is left **running** in Docker.

### Final report (post in chat)
- Status of each Definition of Complete item with evidence (command output summary).
- The URLs to open (dashboard, operator view) and the commands to start, stop, test, and load test.
- Known limitations and what you would change at 10x scale.

---

## 5. Skill coverage checklist (final review)

| Required skill | Where it is demonstrated |
|---|---|
| Spring IoC | Strategy map injection, `@ConditionalOnProperty`, `@ConfigurationProperties` records, request scope, `BeanPostProcessor` + `@Audited` |
| Java Stream | `ReportService`: `teeing`, `partitioningBy`, `flatMap`, custom `Collector`, parallel benchmark |
| Advanced native SQL | `SKIP LOCKED`, window functions, recursive CTE, `LATERAL`, partial/covering/GIN indexes, materialized view, `EXPLAIN ANALYZE` |
| Containerization & microservices | Multi-stage Dockerfiles, Compose profiles, Testcontainers, outbox, idempotency, Resilience4j |
| Kafka & stream apps | Outbox relay, DLT, Kafka Streams windows + stream-table join + KTable scores, exactly-once |
| Redis, caching, data grid | Lua atomic inventory, ZSET queue, cache-aside + stampede guard, write-behind, rate limiters, Redis Cluster |
| Elastic & non-relational | Elasticsearch search/aggregations, MongoDB audit + aggregation pipeline, datastore ADR |
| Frontend | React shopper + operator dashboards with live updates |

---

## 6. Stretch goals (only if time remains)

- [ ] Kubernetes manifests or Helm chart.
- [ ] Spring Cloud Gateway as a dedicated entry point.
- [ ] Hazelcast/Infinispan comparison for the data grid.
- [ ] Keycloak or simple JWT auth for operator routes.
- [ ] Playwright end-to-end test for the shopper flow.

---

## 7. Ready-to-paste prompts for the Cursor agent

**Run to completion (use this to start or resume the whole build)**
> Read `ROADMAP.md`, especially section 0b (Completion mandate) and section 2b (Docker-first and external Postgres). Find the first unticked checkbox and continue from there. Work through every remaining phase, including Phase 6, without stopping for approval. Fix every failure you hit. Do not finish until the Definition of Complete in section 0b is fully true and the stack is running in Docker. Then post the final report.

**Start of a single phase (only if you want to go step by step)**
> Read `ROADMAP.md`. We are starting Phase N. List the tasks you will do in order, create any missing files, implement them one by one, run the tests after each task, and tick the completed checkboxes in `ROADMAP.md`. Stop at the Exit criteria and summarize what passed.

**Fixing failures**
> The last test run failed. Diagnose the root cause before changing code, fix it with the smallest change, and re-run only the relevant tests, then the full suite.

**Writing an ADR**
> Write ADR-00X in `docs/adr/` using the format: Context, Options considered, Decision, Consequences. Keep it under one page and base it on what is actually implemented.

**Before finishing a phase**
> Verify every Exit criterion for this phase with a command or test, not by assumption. Report each one as pass/fail with evidence.

**Docker verification (run after every phase)**
> Verify the Docker setup: run `docker compose --profile core --profile search up --build -d`, confirm every container is healthy with `docker compose ps`, confirm no `postgres` service exists in the compose file, confirm each service connected to the external `dropgate` database and ran Flyway, and hit each `/actuator/health` endpoint. Report each check as pass/fail with evidence, and fix anything failing.
