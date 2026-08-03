# Patroni Fresh Schema Bootstrap

This runbook prepares empty application databases and schemas on the running Patroni cluster. It transfers no existing business data and performs no application cutover.

## Design and sources

The authoritative SQL files are `docker/postgres/init/schemas/*.sql`, because `docker-compose.yml` mounts `docker/postgres/init` into PostgreSQL's initialization directory. SQL keys in `schema-configmap.yaml` are reviewed generated copies; update them whenever an authoritative file changes. `docker/postgres/local-init` is a legacy duplicate whose current differences are whitespace-only. The package is well below the 1 MiB ConfigMap limit and contains no credential or binary.

The cluster is already initialized, so `/docker-entrypoint-initdb.d` and Patroni `initdb`/`post_bootstrap` hooks will not run. A Job is preferable to ad-hoc `kubectl exec`: it is auditable, uses SecretKeyRef, targets only `postgres-patroni-primary:5432`, and preserves status/logs.

## Safety and reruns

The Job connects only to the fixed `postgres-patroni-primary` Service and verifies PostgreSQL 17, `cluster_name=postgres-patroni`, and writable-primary state before mutation. It creates restricted login roles and UTF8 databases owned by the matching application roles, then runs each schema transactionally as its application role.

An exact complete schema is a no-op; an empty database with the expected owner can be initialized. Partial/unexpected schema, wrong owner, elevated application role, or conflicting duplicated credentials fails without any DROP. Never automatically repair or delete a partial database.

Before apply, verify node memory below 80%, MemoryPressure=False, all workloads Ready, Patroni 3/3, one primary, two streaming replicas with low lag, one primary endpoint, three headless endpoints, Bound PVCs, required Secret key names, ConfigMap size, and client/server dry-runs.

```powershell
kubectl apply -f k8s/infra/postgres-patroni/schema-configmap.yaml
kubectl apply -f k8s/infra/postgres-patroni/bootstrap-job.yaml
kubectl get job postgres-patroni-fresh-bootstrap -n ecommerce
kubectl logs -n ecommerce -l app.kubernetes.io/component=schema-bootstrap
```

Applications remain on `postgres:5432`. Old PostgreSQL and its PVC remain for rollback. Cutover, restarts, Saga/regression and failover tests, and old PostgreSQL removal belong to a separate branch.

## Completed bootstrap

The fresh bootstrap completed successfully. The existing application credentials were not changed; separate Patroni-specific Secret keys supplied four restricted application roles. `customer_db`, `inventory_db`, `payment_db`, and `order_db` were created with their expected schemas and matching application-role ownership. Every business table had an initial row count of zero.

The first Job attempt stopped before mutation because it incorrectly compared the Service ClusterIP with the backend pod IP. A later preflight also rejected the legacy elevated application credential before mutation. After correcting the endpoint check and switching to the separate restricted Patroni credentials, the Job completed. Primary catalog checks and read-only catalog checks on both streaming replicas confirmed the expected schema metadata.

Applications are still connected to the old `postgres:5432` Service. Cutover remains a separate-branch operation.
