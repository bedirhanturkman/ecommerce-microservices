# Legacy PostgreSQL final cleanup

## Preconditions and target validation

Final cleanup began only after fresh bootstrap, application cutover, regression and Saga testing, Patroni switchover/failover validation, legacy PostgreSQL scale-to-zero, compute/network retirement, and more than fifteen minutes of operation without the old instance. The operator explicitly confirmed that the old data was no longer required.

The node was Ready with `MemoryPressure=False` and memory below 80%. Patroni was 3/3 Ready on timeline 5 with one leader, two zero-lag streaming replicas, and one ready primary Service endpoint. All applications were Ready, all outbox rows were published, Kafka lag was zero, no active Saga existed, and API readiness returned HTTP 200.

The sole cleanup target was `postgres-data-postgres-0`. Kubernetes reported `Used By: <none>`; no Pod, StatefulSet, Deployment, or Job referenced it, and no active repository manifest could recreate its claim. Its claim UID was `1c2243dc-540f-45ae-8d4a-74041fd16e2e`, capacity 5Gi, StorageClass `hostpath`, status Bound, and bound PV `pvc-1c2243dc-540f-45ae-8d4a-74041fd16e2e`. The PV claimRef pointed only to `ecommerce/postgres-data-postgres-0` and its reclaim policy was `Delete`.

The three protected Patroni claims had distinct names and remained Bound:

- `postgres-data-postgres-patroni-0`
- `postgres-data-postgres-patroni-1`
- `postgres-data-postgres-patroni-2`

## Permanent PVC and PV deletion

At `2026-08-03T14:56:10.0896772Z` (`2026-08-03T17:56:10.0918546+03:00`), only `postgres-data-postgres-0` was deleted. The PVC disappeared after approximately 451 ms. Its Delete-policy PV was removed by the storage controller after approximately 1.315 seconds; no finalizer, force deletion, manual PV deletion, or hostpath filesystem command was used.

This operation is irreversible. The old hostpath database data is no longer retained and no replacement PVC or recovery attempt was made. Patroni, MongoDB, Kafka, Prometheus, and Grafana claims and PVs remained Bound.

## Legacy Secret key cleanup

Live Deployment specifications were statically compared with the Secret key-name set before mutation. Customer, Order, Inventory, and Payment use only Patroni-specific application keys. The first inline JSON Patch attempt was rejected by the API server because of PowerShell argument quoting and changed nothing. A reviewed value-free patch file then removed exactly these obsolete Kubernetes keys:

- `CUSTOMER_DB_USERNAME`
- `CUSTOMER_DB_PASSWORD`
- `ORDER_DB_USERNAME`
- `ORDER_DB_PASSWORD`
- `INVENTORY_DB_USERNAME`
- `INVENTORY_DB_PASSWORD`
- `PAYMENT_DB_USERNAME`
- `PAYMENT_DB_PASSWORD`

The four Patroni admin/replication keys, all eight Patroni-specific application keys, JWT key, internal API key, and every other project key remained present. Post-patch static comparison found no missing active `secretKeyRef`. No application pod was restarted. The same eight obsolete placeholders were removed from `k8s/base/secret.example.yaml`; Docker Compose environment-variable names and local PostgreSQL configuration were not changed.

## Final application and data validation

Registration/login, customer profile read, and product list passed after cleanup. A controlled successful order reached Order `PAID`, Inventory `CONFIRMED`, and Payment `SUCCEEDED`. All resulting rows were visible on the primary and both replicas. Every outbox row was published, Kafka lag was zero, processed-event order IDs remained unique, and no duplicate business effect appeared.

Patroni finished 3/3 Ready on timeline 5 with `postgres-patroni-1` as leader and two zero-lag streaming replicas. The primary Service exposed one endpoint and no split-brain indicator appeared. Applications remained Ready with no restart caused by cleanup.

Prometheus reported 10/10 targets UP. Customer, Order, Inventory, Payment, and API Gateway readiness returned HTTP 200; `ecommerce_orders_total` and `ecommerce_inventory_reservations_total` remained available; and Grafana could reach its Ready Prometheus datasource. Final node memory was approximately 76% with `MemoryPressure=False`. Disk/PV inventory decreased by exactly the one authorized legacy claim and volume.

## Availability limitation

Patroni is now the only active and persistent Kubernetes PostgreSQL infrastructure, but this Docker Desktop environment still has one node, `hostpath` storage, and one physical failure domain. The cleanup completes the local migration; it does not turn the environment into production-grade physical high availability. Production requires independent nodes and storage failure domains, backups/PITR, and tested operational recovery.
