# Kubernetes Service DNS migration

## Decision

Kubernetes no longer runs Eureka. Kubernetes already provides stable Service
DNS names, ready-endpoint selection, and Service-level load balancing, so a
second application registry added no value in this local cluster. Spring Cloud
Kubernetes Discovery was deliberately not added: the applications do not need
to read the Kubernetes API, and no new ServiceAccount or RBAC is required.

The same source tree retains two explicit runtime contracts:

```text
Docker Compose
lb://CUSTOMER-SERVICE -> Eureka -> service instance

Kubernetes
http://customer-service:8081 -> Kubernetes DNS -> Kubernetes Service -> ready pod
```

Docker Compose keeps Eureka Server, Eureka clients, Config Server, Compose
service names, and the existing Gateway `lb://` routes. Kubernetes uses the
separate Config Server repository ConfigMap as the environment boundary.

## Kubernetes configuration

The Kubernetes ConfigMap disables Eureka registration and registry fetching,
disables Spring Cloud discovery, and sets `service-discovery.mode=dns`.
Gateway discovery locator remains disabled and its existing routes are:

| Route | Path | Kubernetes URI |
| --- | --- | --- |
| auth-service | `/api/v1/auth/**` | `http://auth-service:8084` |
| customer-service | `/api/v1/customers/**` | `http://customer-service:8081` |
| product-service | `/api/v1/products/**` | `http://product-service:8083` |
| inventory-service | `/api/v1/inventory/**` | `http://inventory-service:8085` |
| order-service | `/api/v1/orders/**` | `http://order-service:8082` |

No Payment route was added. Auth calls Customer at
`http://customer-service:8081/internal/api/v1/customers...`; Order calls Product
at `http://product-service:8083/api/v1/products/{id}`. Both select the normal
`RestClient` in DNS mode and retain the load-balanced builder by default for
Compose. The `X-Internal-Api-Key` header and internal paths are unchanged.

Config Server does not consume its own remote repository, so its Deployment
uses explicit Eureka and discovery disable environment properties. Its
ConfigMap is mounted as a directory rather than with `subPath`; a controlled
Config Server rollout is still required so the native repository is reloaded.

## Controlled rollout and removal

Use this order and wait for readiness after every step:

1. Apply the Config Server repository ConfigMap.
2. Apply and roll out Config Server; wait for readiness.
3. Roll out Customer, Product, Inventory, Payment, Order, Auth, then Gateway.
4. Verify DNS routes, internal calls, absence of Eureka registration attempts,
   readiness, outbox, Kafka lag, and Patroni replication.
5. Run register/login/profile/Product checks and the successful,
   insufficient-stock, and payment-failure Saga cases.
6. Remove the Eureka-specific Prometheus target.
7. Delete only `deployment/eureka-server` and `service/eureka-server`.
8. Observe for at least ten minutes without restarting application pods, then
   repeat the final API and successful Saga checks.

## Validation record

Before Eureka removal, all application readiness probes were UP. Register,
login, profile read/update, Product list, and the Auth-to-Customer registration
call succeeded. External access to `/internal/**` returned 401. The three Saga
outcomes were PAID/CONFIRMED/SUCCEEDED, INVENTORY_FAILED with no payment, and
PAYMENT_FAILED/RELEASED/FAILED. Order, Inventory, and Payment outbox PENDING
counts returned to zero, Kafka consumer lag was zero, processed-event duplicate
counts were zero, and Patroni had one leader plus two streaming replicas at
zero receive/replay lag.

Product had one replica during migration, so it was not force-scaled. Its
ClusterIP Service and ready EndpointSlice were verified. If HPA later creates a
second ready replica, the same Service distributes requests across both ready
endpoints; no client-side discovery is involved.

Prometheus used static Service targets. The Eureka-only target was removed to
avoid a permanently down target. Application and Prometheus health, required
business metrics, Grafana datasource health, node pressure, and resource usage
must be checked again after the observation window.

Eureka Deployment and Service were removed at
`2026-08-04T10:39:37.1585471Z`. The observation exceeded ten minutes without
restarting application pods. All application readiness checks remained UP and
their post-rollout restart counts remained zero. A Prometheus rolling restart
exposed the existing single-PVC/rolling-pod lock conflict, so its Deployment
strategy was corrected to `Recreate`; the PVC was preserved. The final nine
Prometheus targets were UP with no Eureka target, both required business
metrics returned data, and Grafana reported database health `ok`. Final node
memory was 77% with `MemoryPressure=False`. Eureka's pre-removal working set
was approximately 377 MiB; this is an instantaneous observation, not a
benchmark.

The post-removal register/login/profile/Product checks and a successful Saga
also passed. The final Saga reached PAID/CONFIRMED/SUCCEEDED, all three outbox
PENDING counts were zero, Kafka lag was zero, both Patroni replicas contained
the final order, and receive/replay lag was zero.

## Rollback

Do not roll back databases, Patroni, Kafka, or business data. Before Eureka is
deleted, leave it running, restore the previous Kubernetes ConfigMap and
Deployment configuration, restart Config Server, and roll clients out in the
dependency order.

After deletion, first stop producing new external traffic. Restore the last
active Eureka Deployment and Service from Git history, restore the Eureka-enabled
Kubernetes ConfigMap with `lb://` routes, restart Config Server, then roll out
clients in dependency order. Verify registrations, Gateway routes, internal
calls, outbox, Kafka lag, and Saga state. Rollback is manual and requires an
operator decision.

## Local-cluster limitation

Docker Desktop has one node and local `hostpath` storage. Kubernetes Service
DNS and load balancing remain valid, but this validation does not demonstrate
multi-node availability, failure-domain isolation, or production-grade
persistence.
