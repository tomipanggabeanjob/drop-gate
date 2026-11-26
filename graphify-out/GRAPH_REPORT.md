# Graph Report - drop-gate  (2026-11-26)

## Corpus Check
- 118 files · ~17,169 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 30 file(s) not represented in the graph (top: (none) 14, .exit 4, .cmd 4)

## Summary
- 1144 nodes · 1797 edges · 77 communities (54 shown, 23 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 74 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `76e7a9d0`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.junit.jupiter.api.Test
- QueueAdmissionService
- .checkout
- OutboxEvent
- org.springframework.stereotype.Component
- package.json
- spring
- RedisInventory
- spring
- spring
- spring
- environment
- with
- compilerOptions
- AuditedBeanPostProcessor.java
- interval
- depends_on
- RateLimitService
- org.springframework.boot.autoconfigure.SpringBootApplication
- spring
- V1__init.sql
- spring
- FraudScoringTopologyTests.java
- datasource
- datasource
- datasource
- datasource
- ResilientPaymentGateway.java
- environment
- services
- dashboard
- elasticsearch
- postgres
- queue-service/src/main/resources/application.yml
- datasource
- datasource
- compilerOptions
- hazelcast
- management
- consumer
- HazelcastGridConfiguration.java
- redis
- fraud-stream-service
- org.springframework.context.annotation.Bean
- org.springframework.stereotype.Service
- order-service/src/main/resources/application.yml
- spring
- producer
- spring
- producer
- queue-service
- Audited.java
- FraudScoringTopology.java
- flyway
- jpa
- flyway
- jpa
- ApiExceptionHandler.java
- com.dropgate:events
- health
- tsconfig.json
- events/AuditEvent.java
- InventoryChangedEvent.java
- create-db.sh
- queue/FraudScoreEvent.java
- V1__drop_view_counts.sql
- com.dropgate:dropgate-parent
- fraud-stream-service
- order-service

## God Nodes (most connected - your core abstractions)
1. `QueueAdmissionService` - 26 edges
2. `OrderServiceMvpTests` - 17 edges
3. `compilerOptions` - 16 edges
4. `OutboxEvent` - 16 edges
5. `environment` - 15 edges
6. `RedisInventory` - 15 edges
7. `RateLimitService` - 15 edges
8. `DropgateProperties` - 13 edges
9. `services` - 12 edges
10. `InMemoryInventory` - 12 edges

## Surprising Connections (you probably didn't know these)
- `com.dropgate:fraud-stream-service` --depends_on--> `com.dropgate:events`  [EXTRACTED]
  services/fraud-stream-service/pom.xml → libs/events/pom.xml
- `com.dropgate:insights-service` --depends_on--> `com.dropgate:events`  [EXTRACTED]
  services/insights-service/pom.xml → libs/events/pom.xml
- `com.dropgate:order-service` --depends_on--> `com.dropgate:events`  [EXTRACTED]
  services/order-service/pom.xml → libs/events/pom.xml
- `com.dropgate:queue-service` --depends_on--> `com.dropgate:events`  [EXTRACTED]
  services/queue-service/pom.xml → libs/events/pom.xml
- `OrderServiceMvpTests` --references--> `AllocationService`  [EXTRACTED]
  services/order-service/src/test/java/com/dropgate/order/OrderServiceMvpTests.java → services/order-service/src/main/java/com/dropgate/order/AllocationService.java

## Import Cycles
- None detected.

## Communities (77 total, 23 thin omitted)

### Community 0 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (15): FraudStreamServiceApplicationTests, InsightsServiceApplicationTests, OrderServiceApplicationTests, OrderServiceMvpTests, ExampleAuditedService, ResilientPaymentGatewayTests, DropMetadata, DropMetadataCacheService (+7 more)

### Community 1 - "QueueAdmissionService"
Cohesion: 0.08
Nodes (12): CheckoutController, InventoryController, InventorySnapshot, AdmissionToken, AdmissionTokenService, QueueAdmissionRequest, QueueAdmissionResponse, QueueAdmissionScheduler (+4 more)

### Community 3 - "OutboxEvent"
Cohesion: 0.06
Nodes (11): OrderEvent, HoldExpiryWorker, ExpiredHold, InventoryPort, OutboxEvent, OutboxRelay, OutboxRepository, OutboxRelayTests (+3 more)

### Community 4 - "org.springframework.stereotype.Component"
Cohesion: 0.06
Nodes (10): AllocationDecision, AllocationRequest, AllocationService, AllocationStrategy, DefaultPaymentGateway, FifoAllocationStrategy, FingerprintContext, FingerprintContextFilter (+2 more)

### Community 5 - "package.json"
Cohesion: 0.06
Nodes (49): dependencies, react@^18.3.1, react-dom@^18.3.1, react-router-dom@^6.28.0, recharts@^2.12.0, @tanstack/react-query@^5.0.0, devDependencies, eslint@^9.16.0 (+41 more)

### Community 6 - "spring"
Cohesion: 0.04
Nodes (47): name, auto-offset-reset, group-id, redis, driver-class-name, hikari, password, url (+39 more)

### Community 7 - "RedisInventory"
Cohesion: 0.09
Nodes (7): DropgateProperties, InMemoryInventory, HoldRecord, InventoryHoldRequest, InventoryHoldResponse, RedisInventory, ViewCounterService

### Community 8 - "spring"
Cohesion: 0.05
Nodes (40): name, redis, driver-class-name, hikari, password, url, username, health (+32 more)

### Community 9 - "spring"
Cohesion: 0.08
Nodes (24): driver-class-name, password, url, username, dropgate, grid, enabled, address (+16 more)

### Community 10 - "spring"
Cohesion: 0.09
Nodes (22): auto-create, driver-class-name, password, url, username, enabled, ddl-auto, dialect (+14 more)

### Community 11 - "environment"
Cohesion: 0.09
Nodes (22): CLUSTER_ID, KAFKA_ADVERTISED_LISTENERS, KAFKA_AUTO_CREATE_TOPICS_ENABLE, KAFKA_CONTROLLER_LISTENER_NAMES, KAFKA_CONTROLLER_QUORUM_VOTERS, KAFKA_INTER_BROKER_LISTENER_NAME, KAFKA_LISTENER_SECURITY_PROTOCOL_MAP, KAFKA_LISTENERS (+14 more)

### Community 12 - "with"
Cohesion: 0.11
Nodes (21): runs-on, steps, runs-on, steps, jobs, dashboard, java, name (+13 more)

### Community 13 - "compilerOptions"
Cohesion: 0.11
Nodes (18): compilerOptions, allowJs, allowSyntheticDefaultImports, esModuleInterop, forceConsistentCasingInFileNames, isolatedModules, jsx, lib (+10 more)

### Community 14 - "AuditedBeanPostProcessor.java"
Cohesion: 0.14
Nodes (3): AuditedBeanPostProcessor, AuditEvent, AuditEventSink

### Community 15 - "interval"
Cohesion: 0.24
Nodes (18): healthcheck, healthcheck, healthcheck, interval, retries, start_period, test, timeout (+10 more)

### Community 16 - "depends_on"
Cohesion: 0.17
Nodes (16): elasticsearch, hazelcast, kafka, mongodb, postgres, redis, condition, depends_on (+8 more)

### Community 17 - "RateLimitService"
Cohesion: 0.16
Nodes (5): FraudScoreConsumer, RateLimitProperties, RateLimitService, BucketState, RateLimitAndCacheTests

### Community 18 - "org.springframework.boot.autoconfigure.SpringBootApplication"
Cohesion: 0.23
Nodes (4): FraudStreamServiceApplication, InsightsServiceApplication, OrderServiceApplication, QueueServiceApplication

### Community 19 - "spring"
Cohesion: 0.12
Nodes (15): redis, driver-class-name, password, url, username, baseline-on-migrate, connect-retries, enabled (+7 more)

### Community 20 - "V1__init.sql"
Cohesion: 0.25
Nodes (15): account_devices, accounts, devices, drops, holds, idx_accounts_email_unique, idx_devices_fingerprint_unique, idx_drops_config_gin (+7 more)

### Community 21 - "spring"
Cohesion: 0.12
Nodes (15): redis, driver-class-name, password, url, username, baseline-on-migrate, connect-retries, enabled (+7 more)

### Community 22 - "FraudScoringTopologyTests.java"
Cohesion: 0.15
Nodes (4): ClickEvent, FraudScoreEvent, FraudScoringService, FraudScoringTopologyTests

### Community 23 - "datasource"
Cohesion: 0.13
Nodes (14): driver-class-name, password, url, username, enabled, ddl-auto, dialect, hibernate (+6 more)

### Community 24 - "datasource"
Cohesion: 0.13
Nodes (14): driver-class-name, password, url, username, enabled, ddl-auto, dialect, hibernate (+6 more)

### Community 25 - "datasource"
Cohesion: 0.14
Nodes (13): redis, driver-class-name, password, url, username, baseline-on-migrate, connect-retries, enabled (+5 more)

### Community 26 - "datasource"
Cohesion: 0.14
Nodes (13): redis, driver-class-name, password, url, username, baseline-on-migrate, connect-retries, enabled (+5 more)

### Community 28 - "environment"
Cohesion: 0.31
Nodes (14): DB_HOST, DB_PORT, DROPGATE_INVENTORY, HAZELCAST_ADDRESS, PG_PASSWORD, PG_USER, REDIS_HOST, SERVER_PORT (+6 more)

### Community 29 - "services"
Cohesion: 0.18
Nodes (10): driver, networks, dropgate-net, services, volumes, elasticsearch-data, kafka-data, mongodb-data (+2 more)

### Community 30 - "dashboard"
Cohesion: 0.20
Nodes (10): depends_on, networks, ports, profiles, restart, order-service, queue-service, condition (+2 more)

### Community 31 - "elasticsearch"
Cohesion: 0.18
Nodes (11): environment, image, networks, ports, profiles, restart, volumes, discovery.type (+3 more)

### Community 32 - "postgres"
Cohesion: 0.17
Nodes (12): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, healthcheck, image, networks, ports (+4 more)

### Community 33 - "queue-service/src/main/resources/application.yml"
Cohesion: 0.18
Nodes (10): dropgate, grid, web, include, address, management, endpoints, server (+2 more)

### Community 34 - "datasource"
Cohesion: 0.20
Nodes (10): driver-class-name, hikari, password, url, username, connection-timeout, initialization-fail-timeout, maximum-pool-size (+2 more)

### Community 35 - "datasource"
Cohesion: 0.20
Nodes (10): driver-class-name, hikari, password, url, username, connection-timeout, initialization-fail-timeout, maximum-pool-size (+2 more)

### Community 36 - "compilerOptions"
Cohesion: 0.22
Nodes (8): compilerOptions, allowSyntheticDefaultImports, composite, module, moduleResolution, skipLibCheck, strict, include

### Community 37 - "hazelcast"
Cohesion: 0.25
Nodes (8): HZ_CLUSTERNAME, environment, image, networks, ports, profiles, restart, hazelcast

### Community 38 - "management"
Cohesion: 0.22
Nodes (9): health, web, include, probes, management, endpoint, endpoints, enabled (+1 more)

### Community 39 - "consumer"
Cohesion: 0.22
Nodes (9): auto-offset-reset, group-id, key-deserializer, properties, value-deserializer, consumer, spring.deserializer.value.delegate.class, spring.json.use.type.headers (+1 more)

### Community 41 - "redis"
Cohesion: 0.22
Nodes (9): command, healthcheck, image, networks, ports, profiles, restart, volumes (+1 more)

### Community 42 - "fraud-stream-service"
Cohesion: 0.21
Nodes (13): context, dockerfile, build, build, build, extra_hosts, networks, ports (+5 more)

### Community 46 - "order-service/src/main/resources/application.yml"
Cohesion: 0.29
Nodes (6): dropgate, hold-ttl-seconds, inventory, max-per-account, server, port

### Community 47 - "spring"
Cohesion: 0.29
Nodes (7): name, redis, host, port, spring, application, data

### Community 48 - "producer"
Cohesion: 0.29
Nodes (7): bootstrap-servers, producer, key-serializer, properties, value-serializer, spring.json.add.type.headers, kafka

### Community 49 - "spring"
Cohesion: 0.29
Nodes (7): name, redis, host, port, spring, application, data

### Community 50 - "producer"
Cohesion: 0.29
Nodes (7): bootstrap-servers, producer, key-serializer, properties, value-serializer, spring.json.add.type.headers, kafka

### Community 52 - "queue-service"
Cohesion: 0.29
Nodes (7): extra_hosts, healthcheck, networks, ports, profiles, restart, queue-service

### Community 55 - "flyway"
Cohesion: 0.33
Nodes (6): baseline-on-migrate, baseline-version, connect-retries, enabled, table, flyway

### Community 56 - "jpa"
Cohesion: 0.33
Nodes (6): ddl-auto, dialect, hibernate, properties, hibernate, jpa

### Community 57 - "flyway"
Cohesion: 0.33
Nodes (6): baseline-on-migrate, baseline-version, connect-retries, enabled, table, flyway

### Community 58 - "jpa"
Cohesion: 0.33
Nodes (6): ddl-auto, dialect, hibernate, properties, hibernate, jpa

### Community 60 - "com.dropgate:events"
Cohesion: 0.40
Nodes (5): com.dropgate:events, com.dropgate:fraud-stream-service, com.dropgate:insights-service, com.dropgate:order-service, com.dropgate:queue-service

### Community 61 - "health"
Cohesion: 0.50
Nodes (4): health, probes, endpoint, enabled

### Community 72 - "fraud-stream-service"
Cohesion: 0.29
Nodes (7): extra_hosts, healthcheck, networks, ports, profiles, restart, fraud-stream-service

### Community 73 - "order-service"
Cohesion: 0.29
Nodes (7): extra_hosts, healthcheck, networks, ports, profiles, restart, order-service

## Knowledge Gaps
- **5 isolated node(s):** `com.dropgate:dropgate-parent`, `com.dropgate:fraud-stream-service`, `com.dropgate:insights-service`, `com.dropgate:order-service`, `com.dropgate:queue-service`
  These have ≤1 connection - possible missing edges. (Counts symbols only; 452 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **23 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `services` connect `services` to `postgres`, `hazelcast`, `fraud-stream-service`, `order-service`, `fraud-stream-service`, `environment`, `redis`, `interval`, `queue-service`, `dashboard`, `elasticsearch`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **What connects `com.dropgate:dropgate-parent`, `com.dropgate:fraud-stream-service`, `com.dropgate:insights-service` to the rest of the system?**
  _5 weakly-connected nodes found - possible missing edges._
- **Should `org.junit.jupiter.api.Test` be split into smaller, more focused modules?**
  _Cohesion score 0.05540499849442939 - nodes in this community are weakly interconnected._
- **Why does `QueueAdmissionService` connect `QueueAdmissionService` to `OutboxEvent`, `HazelcastGridConfiguration.java`, `QueueAdmissionService.java`, `org.springframework.stereotype.Service`, `RateLimitService`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **Should `QueueAdmissionService` be split into smaller, more focused modules?**
  _Cohesion score 0.07744107744107744 - nodes in this community are weakly interconnected._
- **Should `OutboxEvent` be split into smaller, more focused modules?**
  _Cohesion score 0.05737704918032787 - nodes in this community are weakly interconnected._
- **Should `org.springframework.stereotype.Component` be split into smaller, more focused modules?**
  _Cohesion score 0.06363636363636363 - nodes in this community are weakly interconnected._