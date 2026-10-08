# Enterprise Order Management System

Spring Boot reference application for customers, products, inventory and transactional order processing,
exposed as a REST API and packaged to run on **IBM WebSphere Liberty** in **Docker** and **OpenShift**.

| | |
|---|---|
| Runtime | Java 17, Spring Boot 3.5, WebSphere Liberty 24.0.0.3 (OpenJ9) |
| Persistence | Spring Data JPA / Hibernate, PostgreSQL 14+ (external) |
| Build | Maven 3.9+, multi-stage Docker build |
| API docs | springdoc OpenAPI / Swagger UI |

## Contents

1. [Repository layout](#1-repository-layout)
2. [Prerequisites](#2-prerequisites)
3. [Configuration](#3-configuration)
4. [Build the artifact](#4-build-the-artifact)
5. [Build the container image](#5-build-the-container-image)
6. [Run with Docker Compose](#6-run-with-docker-compose)
7. [Deploy to OpenShift](#7-deploy-to-openshift)
8. [Run locally without containers](#8-run-locally-without-containers)
9. [Application reference](#9-application-reference)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Repository layout

```
.
├── code/                          Maven project
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/ordermanagement/
│       │     controller/ service/ repository/ entity/ dto/{request,response}/
│       │     mapper/ exception/ config/ enums/ util/
│       ├── main/resources/        application.yml, schema.sql, log4j2.xml
│       └── test/                  unit, slice and integration tests
├── docker/
│   ├── Dockerfile                 stage 1: Maven build; stage 2: WebSphere Liberty runtime
│   ├── server.xml                 Liberty features, HTTP endpoint, JNDI datasource, Spring Boot app
│   └── postgres/                  PostgreSQL JDBC driver used by the Liberty datasource
├── deployment/
│   └── docker-compose.yml         runs the application container (database is external)
└── .env.example                   environment variables the application reads
```

**Deployment flow**

```
source (code/) ──mvn package──▶ order-management-*.jar ──docker build──▶ Liberty image ──▶ Docker Compose
                                                                              └──push──▶ registry ──▶ OpenShift
```

## 2. Prerequisites

| Tool | Needed for |
|---|---|
| JDK 17, Maven 3.9+ | local build, tests, running without containers |
| Docker 24+ with Compose v2 | image build and Compose run; also the `*IT` tests (Testcontainers) |
| PostgreSQL 14+ | an **empty database** reachable from the container |
| `oc` CLI, OpenShift 4.x access, an image registry | OpenShift deployment only |

## 3. Configuration

All settings are environment variables; no credentials are stored in the image or in the compose file.
See [.env.example](.env.example).

**Container / Liberty:**

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_HOST` | yes | - | PostgreSQL host |
| `DB_PORT` | no | `5432` | PostgreSQL port |
| `DB_NAME` | yes | - | Database name |
| `DB_USERNAME` | yes | - | Database user |
| `DB_PASSWORD` | yes | - | Database password |
| `LOG_DIR` | no | `/logs` | Liberty log directory |
| `APP_LOG_DIR` | no | `/shared/logs` | Application log directory |

**Datasource.** There is a single datasource definition: `jdbc/orderManagementDS` in `docker/server.xml`, built
from the `DB_*` variables above, with the connection pool settings (`connectionManager`). The image sets
`SPRING_DATASOURCE_JNDI_NAME=jdbc/orderManagementDS`, so Spring looks the datasource up through JNDI. `application.yml` deliberately contains no URL, credentials or pool settings.

**Other application settings** (optional): `APP_LOG_LEVEL` (`INFO`), `SQL_INIT_MODE` (`always`).

**Local runs without Liberty** have no JNDI datasource, so the `dev` profile (`application-dev.yml`) reads
`DB_URL`, `DB_USERNAME` and `DB_PASSWORD` instead. The tests define their own datasource (H2 / Testcontainers).

**Database schema.** On start-up the application runs `schema.sql` (idempotent `CREATE ... IF NOT EXISTS` and
`ALTER ... ADD COLUMN IF NOT EXISTS`, so it also upgrades older databases). Hibernate only *validates* the
result (`ddl-auto: validate`). Set `SQL_INIT_MODE=never` to disable. There is no seed data; create data through
the API.

## 4. Build the artifact

```bash
cd code
mvn clean package            # compile, unit tests, package the JAR
# or: mvn clean verify       # also runs the *IT integration tests (Docker required)
```

Output: `code/target/order-management-<version>.jar`.

Notes:

- Embedded Tomcat is `provided` in `pom.xml` and excluded from the JAR; on Liberty the app runs on Liberty's
  servlet container (`servlet-6.0`). The packaged JAR is meant for Liberty, **not** for `java -jar`.
- Tests: `*Test` (Mockito, H2) run via Surefire; `*IT` (PostgreSQL via Testcontainers, real `schema.sql`) run via
  Failsafe, including `OrderConcurrencyIT` and the HTTP-level API tests.

You can skip this step when building the image: stage 1 of the Dockerfile performs the same Maven build (tests
skipped). Run it beforehand to verify the code, since the image build does not run tests.

## 5. Build the container image

The [Dockerfile](docker/Dockerfile) is a multi-stage build; run it **from the repository root**:

| Stage | Base image | What it does |
|---|---|---|
| `builder` | `maven:3.9-eclipse-temurin-17` | resolves dependencies, then builds the JAR (`-DskipTests`) |
| runtime | `icr.io/appcafe/websphere-liberty:24.0.0.3-full-java17-openj9-ubi` | adds `server.xml`, the PostgreSQL driver and the JAR; runs as non-root user `1001` |

```bash
docker build -f docker/Dockerfile -t order-management:1.0.0 .
```

The runtime image contains:

| Path in image | Source |
|---|---|
| `/config/server.xml` | `docker/server.xml` |
| `/opt/ibm/wlp/usr/shared/resources/postgres/` | `docker/postgres/` (JDBC driver) |
| `/config/apps/order-management.jar` | JAR from the builder stage |
| `/logs`, `/shared/logs` | writable log directories (owner `1001`, group `0`, group-writable) |

Exposed ports: `9080` (HTTP), `9443` (HTTPS).

## 6. Run with Docker Compose

PostgreSQL is **not** part of the stack. Create an empty database first, then start the application from the
repository root.

**Bash / Git Bash**

```bash
export DB_HOST=host.docker.internal DB_PORT=5432 DB_NAME=order_management \
       DB_USERNAME=<user> DB_PASSWORD=<password>
docker compose -f deployment/docker-compose.yml up --build -d
```

**PowerShell**

```powershell
$env:DB_HOST="host.docker.internal"; $env:DB_PORT="5432"; $env:DB_NAME="order_management"
$env:DB_USERNAME="<user>"; $env:DB_PASSWORD="<password>"
docker compose -f deployment/docker-compose.yml up --build -d
```

`host.docker.internal` reaches a database running on the Docker host; otherwise use the database's address.

Verify:

```bash
docker compose -f deployment/docker-compose.yml logs -f
curl http://localhost:9080/actuator/health
```

- Swagger UI: <http://localhost:9080/swagger-ui.html> - OpenAPI JSON: `/api-docs`
- Stop: `docker compose -f deployment/docker-compose.yml down`

## 7. Deploy to OpenShift

The image is the same as for Docker; only configuration is supplied differently. OpenShift runs containers
with an arbitrary UID in group `0`, which the image supports (group-writable log directories, no root).

> No OpenShift manifests are committed yet. The steps below are the reference procedure; adapt names,
> namespace and registry to your cluster.

### 7.1 Push the image

```bash
oc login --server=<api-url>
oc new-project order-management

# Option A - external registry (Quay, ICR, ...)
docker tag order-management:1.0.0 <registry>/<org>/order-management:1.0.0
docker push <registry>/<org>/order-management:1.0.0

# Option B - OpenShift internal registry
docker login -u "$(oc whoami)" -p "$(oc whoami -t)" <internal-registry-route>
docker tag order-management:1.0.0 <internal-registry-route>/order-management/order-management:1.0.0
docker push <internal-registry-route>/order-management/order-management:1.0.0
```

### 7.2 Create configuration and secret

```bash
oc create configmap order-management-config \
  --from-literal=DB_HOST=<db-host> --from-literal=DB_PORT=5432 --from-literal=DB_NAME=order_management

oc create secret generic order-management-db \
  --from-literal=DB_USERNAME=<user> --from-literal=DB_PASSWORD=<password>
```

### 7.3 Deploy, expose and verify

```yaml
# deployment/openshift/order-management.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-management
spec:
  replicas: 2
  selector:
    matchLabels: { app: order-management }
  template:
    metadata:
      labels: { app: order-management }
    spec:
      containers:
        - name: order-management
          image: <registry>/<org>/order-management:1.0.0
          ports:
            - containerPort: 9080
          envFrom:
            - configMapRef: { name: order-management-config }
            - secretRef:    { name: order-management-db }
          env:
            - { name: LOG_DIR,     value: /logs }
            - { name: APP_LOG_DIR, value: /shared/logs }
          readinessProbe:
            httpGet: { path: /actuator/health, port: 9080 }
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet: { path: /actuator/health, port: 9080 }
            initialDelaySeconds: 60
            periodSeconds: 20
          resources:
            requests: { cpu: 250m, memory: 512Mi }
            limits:   { cpu: "1",  memory: 1Gi }
---
apiVersion: v1
kind: Service
metadata:
  name: order-management
spec:
  selector: { app: order-management }
  ports:
    - { name: http, port: 9080, targetPort: 9080 }
---
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: order-management
spec:
  to: { kind: Service, name: order-management }
  port: { targetPort: http }
  tls: { termination: edge, insecureEdgeTerminationPolicy: Redirect }
```

```bash
oc apply -f deployment/openshift/order-management.yaml
oc rollout status deployment/order-management
oc logs deployment/order-management
curl "https://$(oc get route order-management -o jsonpath='{.spec.host}')/actuator/health"
```

Requirements and notes:

- The database must be reachable from the cluster network (allow the egress / firewall rule for `DB_HOST:DB_PORT`).
- If the image is in the internal registry of another project, grant pull access
  (`oc policy add-role-to-user system:image-puller system:serviceaccount:order-management:default -n <image-project>`).
- Several replicas run `schema.sql` at start-up; it is idempotent, so this is safe.
- Update: push a new tag, then `oc set image deployment/order-management order-management=<registry>/<org>/order-management:<tag>`.
  Roll back with `oc rollout undo deployment/order-management`.

## 8. Run locally without containers

```bash
cd code
export DB_URL=jdbc:postgresql://localhost:5432/order_management DB_USERNAME=<user> DB_PASSWORD=<password>
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Swagger UI: <http://localhost:8080/swagger-ui.html> - health: `/actuator/health`.

## 9. Application reference

### Architecture

```
Controller  ->  Service  ->  Repository  ->  PostgreSQL
 (HTTP, DTO)    (business     (Spring Data
                rules, tx)     JPA)
```

- Controllers only translate HTTP to service calls; they never touch repositories or entities.
- Services hold business rules and transaction boundaries (`@Transactional`).
- Entities are never exposed; requests and responses are DTOs.
- Errors use one format from `GlobalExceptionHandler`: `timestamp, status, error, message, path`.

### Transactions and concurrency

- `OrderService.createOrder` is one transaction: order, items and inventory reservations commit or roll back together.
- Inventory rows are locked with `SELECT ... FOR UPDATE` while reserving/releasing, and items are processed in
  product-id order, so concurrent orders cannot deadlock or oversell. `Inventory` and `Order` also have a
  `@Version` column; a lost update returns `409 CONCURRENT_MODIFICATION`.
- Status changes follow `OrderStatus.allowedTransitions()`. Cancelling releases reserved stock; shipping consumes it.
- Order numbers, customer numbers and product codes come from database sequences.

### Database model

`customer` 1-N `orders` 1-N `order_item` N-1 `product` 1-1 `inventory`. Money is `NUMERIC(19,4)` (`BigDecimal`);
`order_item.unit_price` is the price captured at order time.

### API summary

| Resource | Endpoints |
|---|---|
| Customers | `GET/POST /api/customers`, `GET/PUT/DELETE /api/customers/{id}`, `GET /api/customers/number/{n}`, `PATCH /api/customers/{id}/status` |
| Products | `GET/POST /api/products`, `GET/PUT/DELETE /api/products/{id}`, `GET /api/products/code/{c}`, `PATCH /api/products/{id}/status` |
| Inventory | `GET/POST /api/inventory/{productId}`, `POST .../stock`, `.../reduce`, `.../reserve`, `.../release` |
| Orders | `GET/POST /api/orders`, `GET /api/orders/{id}`, `GET /api/orders/number/{n}`, `POST /api/orders/{id}/cancel`, `PUT /api/orders/{id}/status` |

List endpoints accept `page`, `size` (max 100) and `sort=field,asc|desc` (whitelisted fields), plus filters:
customers `status`, `search`; products `status`, `category`, `search`; orders `customerId`, `status`, `from`, `to`.

```bash
curl -X POST http://localhost:9080/api/orders -H "Content-Type: application/json" \
  -d '{"customerId":1,"items":[{"productId":1,"quantity":2}]}'
```

```json
{ "timestamp": "2026-01-01T10:00:00", "status": 422, "error": "INSUFFICIENT_INVENTORY",
  "message": "Insufficient inventory for product 'PROD-1001': requested 5, available 1",
  "path": "/api/orders" }
```

| Error code | HTTP |
|---|---|
| `VALIDATION_ERROR`, `BUSINESS_VALIDATION_ERROR`, `MALFORMED_REQUEST`, `TYPE_MISMATCH` | 400 |
| `NOT_FOUND` | 404 |
| `DUPLICATE_RESOURCE`, `DATA_INTEGRITY_VIOLATION`, `CONCURRENT_MODIFICATION` | 409 |
| `INSUFFICIENT_INVENTORY`, `INVALID_ORDER_STATE` | 422 |

**Order state machine:** `CREATED -> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED`;
`CREATED / CONFIRMED / PROCESSING -> CANCELLED`. Delivered and cancelled orders are final.

## 10. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Schema-validation: missing column [version] in table [orders]` | The database was created by an older schema. Restart with `SQL_INIT_MODE=always` (default) so `schema.sql` adds the column, or run the `ALTER TABLE` statements in `schema.sql` manually. |
| `DB_HOST is required` on `docker compose up` | Required variables are not exported in the current shell. |
| Connection refused / timeout to the database | Host not reachable from the container; use `host.docker.internal` for a host-local DB, and check firewall and `pg_hba.conf`. |
| `factory already defined` on Liberty | Embedded Tomcat is on the classpath; keep it `provided` in `pom.xml`. |
| Permission denied writing `/logs` on OpenShift | Rebuild the image so the log directories are group-writable (Dockerfile). |

## Status and roadmap

The Docker image, Liberty datasource, Compose file and OpenShift procedure have not yet been verified end to
end in a clean environment. JMS, LDAP/AD security, CI/CD and Helm/Argo CD are planned for later phases;
authentication is intentionally absent in this phase.
