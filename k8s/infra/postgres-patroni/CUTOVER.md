# PostgreSQL to Patroni Cutover

This runbook moves Customer, Inventory, Payment, and Order services from the retained `postgres:5432` instance to `postgres-patroni-primary:5432`. Product uses MongoDB and Auth has no direct datasource. The old PostgreSQL StatefulSet, Services, credentials, and PVC remain unchanged for rollback.

## Preflight

Require a clean cutover branch, Ready node, MemoryPressure=False, node memory below 80%, old PostgreSQL 1/1, Patroni 3/3 with one primary and two streaming replicas, low lag, one ready primary endpoint, completed fresh-bootstrap Job, expected schemas, all required Patroni application Secret key names, and all application pods Ready. Never print or decode Secret values.

Record each affected Deployment's JDBC URL, Secret key names, replicas, image, generation, and rollout revision. Verify client/server dry-runs and confirm that only the four application Deployments and this documentation change.

## Active-work drain

Before stopping workloads, check the old PostgreSQL instance for zero pending Order, Inventory, and Payment outbox rows and zero active Saga markers. Check every Kafka consumer-group partition for lag zero and confirm API Gateway/application readiness.

Prevent new work producer-first. Stop API Gateway and Auth, then Order. Recheck outbox and Kafka lag while Inventory and Payment consumers remain available to drain. Once both are zero, stop Inventory, Payment, and Customer. Preserve the recorded replica counts.

## Apply and startup order

With the four workloads at zero replicas, apply only:

- `k8s/apps/customer-service/deployment.yaml`
- `k8s/apps/inventory-service/deployment.yaml`
- `k8s/apps/payment-service/deployment.yaml`
- `k8s/apps/order-service/deployment.yaml`

Restore and validate one service at a time: Customer, Inventory, Payment, Order, Auth, then API Gateway. For each service require successful rollout, Ready probes, Actuator health, Eureka registration, Patroni connection, Hibernate schema validation, and no authentication or scheduler/consumer error before proceeding.

## Validation

Confirm live pod specs use the Patroni primary host and Patroni-specific Secret key names. Confirm service-level connections on the Patroni primary without exposing role names, and observe that the old PostgreSQL receives no new application connections.

Run repository-defined authentication/customer/product API checks, followed by separate successful-order, inventory-failure, and payment-failure Saga scenarios. After every scenario verify database state, processed-event idempotency, outbox PENDING=0, Kafka lag=0, no duplicate effects, and healthy Patroni replication. Do not invent endpoints or test controls.

Verify Prometheus targets, Grafana datasource availability, Actuator endpoints, JVM/HTTP metrics, `ecommerce_orders_total`, and `ecommerce_inventory_reservations_total`.

## Stop criteria

Stop new test traffic on any authentication/schema error, unhealthy application or database, outbox backlog, Kafka lag that does not drain, retry loop, duplicate business effect, node memory at or above 85%, MemoryPressure=True, Patroni readiness loss, multiple primaries, broken streaming, or increasing replica lag. Do not delete databases, tables, StatefulSets, or PVCs.

## Rollback

Stop API traffic and Order production, assess consumer/outbox drain, then restore the four previous `postgres:5432` JDBC URLs and legacy application Secret key references. Start Customer, Inventory, Payment, Order, Auth, and API Gateway in controlled order and validate readiness/schema checks against the old PostgreSQL.

Do not delete Patroni or merge data automatically. Writes made after cutover exist only in Patroni and will be absent after rollback; report any split writes explicitly.

The old PostgreSQL remains available until a later removal branch. Patroni failover/switchover testing also belongs to a separate branch.

## Completed cutover

The controlled cutover completed successfully. Customer, Inventory, Payment, and Order now use `postgres-patroni-primary:5432` with their Patroni-specific restricted credentials. Auth and Product datasource behavior was unchanged. The retained `postgres` Service still selects the old PostgreSQL pod and received no new business-service connections after cutover.

Traffic was stopped producer-first, old outbox/active-Saga counts and every Kafka partition lag were confirmed zero, and the four services were stopped before their manifests were applied. Services were restored in the documented order; every rollout, Hibernate schema validation, Eureka registration, and Actuator readiness check succeeded.

Controlled API tests covered registration, login, customer read/update, product list/create/update, and three separate Saga paths. The successful order reached `PAID`; insufficient inventory reached `INVENTORY_FAILED` without a payment; simulated payment rejection reached `PAYMENT_FAILED` with inventory release. All resulting outbox rows were published, pending counts and Kafka lag returned to zero, processed-event rows remained unique, and both replicas exposed the final test data read-only with zero lag.

The old PostgreSQL and all old/new PVCs remain intact. A rollback would lose Patroni-only test writes from the old system; no automatic merge is provided.
