# Legacy PostgreSQL retirement

## Preconditions

The legacy single-instance PostgreSQL was retired only after fresh Patroni bootstrap, application cutover, API and Saga regression, controlled switchover, and primary-pod failure recovery had completed. The retirement gate required a Ready node with `MemoryPressure=False`, node memory below 80%, Patroni 3/3 Ready with one primary and two zero-lag streaming replicas, every application Ready, all outbox rows published, Kafka consumer lag zero, no active Saga, and no recent database connection error.

Live Customer, Order, Inventory, and Payment pod specifications used `postgres-patroni-primary:5432` and their Patroni-specific Secret key references. The old PostgreSQL reported no external client connection, and application logs contained no legacy `postgres:5432` connection attempt or fallback. Secret values were neither read nor printed.

## Baseline

Before retirement, `StatefulSet/postgres` had one replica, generation 1, image `postgres:17`, and pod `postgres-0`. The ClusterIP Service was `postgres`, the headless Service was `postgres-headless`, and both selected only the old pod. The retained claim was `postgres-data-postgres-0`, 5Gi, Bound, using the `hostpath` StorageClass.

Patroni started the operation on timeline 5 with `postgres-patroni-1` as leader and two streaming replicas at receive/replay lag zero. All applications were Ready, API Gateway readiness returned HTTP 200, and node memory was approximately 75%.

## Scale-to-zero stage

At `2026-08-03T14:11:29.1847846Z` (`2026-08-03T17:11:29.1859147+03:00`), only the legacy StatefulSet was scaled from one replica to zero. `postgres-0` terminated in approximately 2.537 seconds. Both legacy EndpointSlices then contained zero endpoints while the legacy PVC remained Bound.

The zero-replica state was observed for 925 seconds. All 31 samples showed Patroni 3/3 Ready, one primary endpoint, seven relevant application Deployments Ready, API readiness HTTP 200, no legacy endpoint, and the retained PVC Bound. Node memory remained between approximately 74% and 77%, below the 85% stop threshold. No rollback was required.

During this stage, registration/login, customer profile read, product list, and a successful order Saga completed through Patroni. The order reached `PAID`, Inventory reached `CONFIRMED`, Payment reached `SUCCEEDED`, all outboxes returned to zero pending rows, Kafka lag returned to zero, and no duplicate business effect appeared.

If the observation had failed before deletion, the limited rollback was to scale `StatefulSet/postgres` back to one replica and verify its pod, PVC mount, and Service endpoint. Application datasource manifests would not have been changed automatically.

## Resource removal and retained recovery material

After the successful observation, only these live resources were deleted:

- `StatefulSet/postgres`
- `Service/postgres`
- `Service/postgres-headless`

Their active Kubernetes manifests were removed from the repository so a normal installation cannot recreate the legacy database accidentally. Docker Compose PostgreSQL remains unchanged for local Compose development.

The `postgres-data-postgres-0` PVC and its bound PV were deliberately retained during retirement, and all legacy application Secret keys remained unchanged at that stage. The later, explicitly authorized irreversible cleanup removed that PVC/PV and the obsolete keys; see `LEGACY-POSTGRES-CLEANUP.md` for the final record.

## Final validation

After resource removal, registration/login, customer profile read, product list, and a second successful Saga passed. Order reached `PAID`, Inventory `CONFIRMED`, and Payment `SUCCEEDED`. All outbox records were published, Kafka lag was zero, processed-event order IDs remained unique, and all three Patroni members exposed matching final data.

Patroni remained 3/3 Ready on timeline 5 with `postgres-patroni-1` leader, two streaming replicas, and receive/replay lag zero. No application restart was needed. Prometheus reported 10/10 targets UP, application Actuator readiness returned HTTP 200, the existing business metric series remained present, and Grafana could reach its Prometheus datasource. Final node memory was approximately 76%; instantaneous node measurements are not a benchmark, although removal eliminated the old pod's approximately 86Mi application working set and its reserved compute resources.

## Availability limitation

The active Patroni topology runs on one Docker Desktop Kubernetes node with `hostpath` storage. It validates local process and pod recovery but does not provide physical node, disk, host, or failure-domain high availability. Production requires multiple nodes, independent failure domains, durable storage, backup/PITR, and operational recovery testing.
