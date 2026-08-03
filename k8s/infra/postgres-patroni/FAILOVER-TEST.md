# Patroni switchover and failover test

## Scope and preflight

This run validates PostgreSQL process and pod-level leader election in the local `ecommerce` namespace. Before either test, the node was Ready with `MemoryPressure=False`, memory usage was below 80%, the retained PostgreSQL StatefulSet was 1/1 Ready, Patroni was 3/3 Ready with one leader and two zero-lag streaming replicas, all application Deployments were Ready, every outbox had zero pending rows, Kafka consumer lag was zero, and no active Saga or recurring database connection error was present.

The applications use the fixed `postgres-patroni-primary` Service name. No explicit Hikari `connectionTimeout`, `validationTimeout`, `keepaliveTime`, or `maxLifetime` overrides exist, so recovery relied on Hikari/JDBC discarding failed connections and opening new connections through the Service without restarting application pods.

## Baseline and sequence evaluation

Primary and replica business row counts, `MAX(id)` values, order/reservation/payment states, outbox states, and processed-event counts matched before mutation. The initial leader was `postgres-patroni-2`, both replicas were streaming on timeline 3, and receive/replay lag was zero.

Sequence `last_value` was deliberately not used as a primary/replica equality gate. The primary exposed values close to current table maxima while both standbys exposed WAL-preallocated value 33. PostgreSQL sequences are non-transactional, are not gapless, and standby-visible values may be ahead because of sequence WAL preallocation. No sequence reset, manual synchronization, or `setval` was performed. Safety was instead verified by matching business rows and `MAX(id)`, zero replication lag, successful post-promotion inserts, generated IDs greater than the corresponding pre-promotion `MAX(id)`, and absence of primary-key or unique-constraint conflicts. ID gaps were accepted.

## Test A: controlled switchover

At `2026-08-03T13:15:38.5202728Z` (`2026-08-03T16:15:38.5225000+03:00`), `patronictl switchover` performed an immediate, explicit transition from `postgres-patroni-2` to the zero-lag candidate `postgres-patroni-0`. No labels or Service selectors were patched.

- Leader and primary EndpointSlice transition observed: 4.245 seconds
- Endpoint transition: `10.1.0.188` to `10.1.0.187`
- Timeline: 3 to 4
- Application restarts: none
- Database connection errors after transition: none observed
- Exact API interruption during the transition: unavailable because the first polling helper rejected its cross-process timestamp type and retained no samples; subsequent readiness polling was continuously HTTP 200 during the stability window

Business counts and maxima were unchanged after switchover. A controlled customer insert generated ID 34 from a pre-switchover maximum of 2. The insert succeeded without a key conflict and appeared on the new primary and both replicas. A successful Saga then reached Order `PAID`, Inventory `CONFIRMED`, and Payment `SUCCEEDED`; all outbox rows were published, Kafka lag returned to zero, and processed-event effects remained unique.

The cluster was observed for more than ten minutes. Patroni remained 3/3 Ready with one leader, two timeline-4 streaming replicas and zero lag; API readiness stayed HTTP 200, all applications remained Ready, and node memory stayed between 72% and 74%.

## Test B: active primary pod failure

Before pod deletion, a marker customer generated ID 35 and was confirmed on the primary and both replicas. This established a bounded marker with expected RPO zero before the asynchronous failover test.

At `2026-08-03T13:34:58.0664345Z` (`2026-08-03T16:34:58.0674941+03:00`), only the active `postgres-patroni-0` pod was deleted. Its StatefulSet, PVC, Services, configuration, and the other members were not changed.

- New leader: `postgres-patroni-1`
- Leader election observed: 3.007 seconds
- Primary EndpointSlice transition observed: 3.007 seconds
- Endpoint transition: `10.1.0.187` to `10.1.0.184`
- Timeline: 4 to 5
- API readiness polling: 240/240 HTTP 200; no observed outage
- Application restarts: none
- Kafka consumer reconnect errors: none observed
- Replacement `postgres-patroni-0` object created: 5.178 seconds
- Replacement member streaming: 8.820 seconds
- Replacement pod Ready: 16.448 seconds

The replacement reused its original PVC, returned with the same ordinal, joined as a streaming replica, remained in recovery, and did not attempt to reclaim leadership. No split-brain indicator appeared.

The replicated marker remained visible on the new primary, so measured marker RPO was zero. A post-failover customer insert generated ID 68 from a pre-failover maximum of 35. The forward jump was accepted; the ID remained greater than the old maximum and no key conflict occurred. The record appeared on all three members. A second successful Saga reached Order `PAID`, Inventory `CONFIRMED`, and Payment `SUCCEEDED`. Final outbox pending and Kafka lag were zero, processed-event order IDs remained unique, and no duplicate business effect was detected.

## Final stability and monitoring

The final observation lasted 918 seconds. All 31 samples showed Patroni 3/3 Ready, one primary endpoint, six relevant application Deployments Ready, and API readiness HTTP 200. Both replicas remained streaming on timeline 5 with receive/replay lag zero. Node memory stayed between 73% and 76%, below the 85% stop threshold, and `MemoryPressure` remained false.

Prometheus reported 10/10 active targets UP. Customer, Order, Inventory, and Payment readiness endpoints returned HTTP 200. The `ecommerce_orders_total` and `ecommerce_inventory_reservations_total` series remained available, and the Grafana pod could reach the Ready Prometheus datasource endpoint.

No stop criterion occurred. The retained PostgreSQL StatefulSet remained 1/1 Ready, and all old and Patroni PVCs remained Bound. No manifest, Secret, schema, database, HPA, Kafka, monitoring, or storage resource was changed.

## Availability limitation

This test demonstrates Patroni leader election, Kubernetes Service endpoint movement, application connection recovery, and StatefulSet pod recovery. It is not proof of production high availability. Docker Desktop supplies one Kubernetes node, hostpath storage, and one physical machine; all three Patroni pods therefore share the same node, disk, host, and physical failure domain. A node, disk, or host failure can still remove the entire cluster.
