# Local Kubernetes

## Patroni fresh schema bootstrap

Existing business data was not transferred. Fresh bootstrap completed successfully through `infra/postgres-patroni/bootstrap-job.yaml` and `infra/postgres-patroni/schema-configmap.yaml`, creating four empty business databases and their schemas with separate restricted Patroni application roles; all initial business row counts were zero. Existing application credentials were unchanged. The first Job attempt stopped safely before mutation because of an invalid ClusterIP/backend-IP comparison, then completed after the check and credential separation were corrected. See `infra/postgres-patroni/FRESH-BOOTSTRAP.md` for the runbook and verification details.

Applications remain connected to the old PostgreSQL, which is retained with its Services and PVC for rollback. The Job no-ops on an exact expected schema, fails on partial state, never performs automatic DROP, and cutover belongs to a separate branch.

## Patroni application cutover

The controlled, reversible application cutover completed successfully and is documented in `infra/postgres-patroni/CUTOVER.md`. Four PostgreSQL-backed Deployments connect directly to the Patroni primary Service with separate restricted credentials. API and success/inventory-failure/payment-failure Saga checks passed, outbox and Kafka lag returned to zero, and replica visibility was verified. The old PostgreSQL Service and storage remain unchanged for rollback. Failover testing and old PostgreSQL removal are separate follow-up work.

## Patroni switchover and failover validation

Controlled switchover and active-primary pod failure tests completed successfully; timings, sequence acceptance criteria, application recovery, Saga results, and monitoring evidence are documented in `infra/postgres-patroni/FAILOVER-TEST.md`. Both transitions preserved business data, post-promotion inserts generated IDs greater than the previous table maxima without constraint conflicts, successful Sagas drained their outboxes and Kafka lag to zero, and no application restart was required. Sequence gaps were accepted and no `setval` or sequence reset was used. The old PostgreSQL and every PVC remain retained. This single-node Docker Desktop/hostpath result validates local leader-election behavior, not production physical high availability.

This directory contains the complete local Kubernetes architecture for the
Docker Desktop cluster.

## Current architecture

All application services run in the `ecommerce` namespace. Eureka Server,
Config Server, API Gateway, Auth, Customer, Product, Order, Inventory, and
Payment are stateless Deployments. PostgreSQL, MongoDB, and Kafka run as
single-replica StatefulSets with persistent storage. Prometheus and Grafana
also run in Kubernetes with dedicated PVCs.

The active in-cluster endpoints are:

- PostgreSQL: `postgres:5432`
- MongoDB: `mongodb:27017`
- Kafka: `kafka:9092`
- Config Server: `config-server:8888`
- Eureka Server: `eureka-server:8761`

Local external access is:

- API through Traefik Ingress: `http://ecommerce.local`
- API Gateway NodePort fallback: `http://localhost:32323`
- Grafana NodePort: `http://localhost:30305`

Test the Ingress without changing the Windows hosts file:

```powershell
curl.exe --resolve ecommerce.local:80:127.0.0.1 `
  http://ecommerce.local/actuator/health
```

The optional Windows hosts entry is:

```text
127.0.0.1 ecommerce.local
```

This is a local HTTP environment; TLS is not configured. The stopped Compose
PostgreSQL, MongoDB, and Kafka containers and their volumes are retained only
for rollback. Kubernetes applications do not use them during normal operation.

## Patroni preparation

The local capacity preflight passed with approximately `9845.6Mi` allocatable
memory. Request-based memory headroom is approximately `21.4%` while the
existing PostgreSQL and three future Patroni pods coexist during migration,
and approximately `17.5%` if the Product Service HPA also starts its second
pod. This approval is only for controlled local testing.

This preparation adds only the namespace-scoped ServiceAccount, RBAC, and
isolated `postgres-patroni-headless` and `postgres-patroni-primary` Services.
It does not create Patroni pods or PVCs, and it does not change the existing
PostgreSQL StatefulSet or `postgres` Service. Applications continue to use
`postgres:5432`; that Service must remain unchanged until the cutover branch.

Patroni will use the Kubernetes API with the recommended Endpoints DCS mode.
The image/version branch must confirm Endpoints support and the configured
role-label contract before creating the cluster. The primary Service follows
the Patroni 4.x defaults `cluster-name=postgres-patroni` and `role=primary`;
an older image must not be used unless it is explicitly configured to emit
the same labels. ConfigMap and Lease permissions are intentionally omitted.

Apply only these preparation resources, in order:

1. `k8s/infra/postgres-patroni/serviceaccount.yaml`
2. `k8s/infra/postgres-patroni/rbac.yaml`
3. `k8s/infra/postgres-patroni/service.yaml`

### Patroni image and configuration

The local Patroni image is based on the immutable PostgreSQL
`17.10-bookworm` image digest and installs Patroni `4.1.4` with Kubernetes DCS
support and Psycopg `3.3.4` in an isolated Python virtual environment. Build
it in the Docker Desktop image store without pushing it to a registry:

```powershell
docker build -t ecommerce-postgres-patroni:17-patroni-4.1.4 `
  k8s/infra/postgres-patroni
```

The configuration uses Kubernetes Endpoints DCS and asynchronous streaming
replication. Pod-specific identity, IP addresses, and the superuser and
replication credentials must be supplied by the future StatefulSet through
Patroni environment variables sourced from pod fields and Secrets. The
ConfigMap contains no credentials and has not been applied to the cluster.

No Patroni StatefulSet, pod, or PVC exists yet. The current PostgreSQL
StatefulSet and `postgres:5432` application endpoint remain unchanged. Do not
reuse the same local image tag for a materially different rebuild; either
remove and deliberately rebuild the local tag from the reviewed sources or
use a new immutable revision tag. Production requires a trusted registry,
immutable image digest, vulnerability scanning, REST API authentication/TLS,
network isolation, and durable multi-node storage.

Synchronous replication, WAL archiving, and a custom archive command remain
disabled for this local asynchronous baseline.

### Isolated Patroni cluster

The isolated `postgres-patroni` StatefulSet defines three members: one primary
and two asynchronous replicas. Each member receives its own `5Gi` `hostpath`
PVC. The existing PostgreSQL StatefulSet, its PVC, and the `postgres:5432`
Service remain unchanged; applications are not connected to Patroni yet.

Before bootstrap, verify that `ecommerce-secrets` contains these key names
without printing their values: `patroni-superuser-username`,
`patroni-superuser-password`, `patroni-replication-username`, and
`patroni-replication-password`. Apply resources in this order:

1. Verify the Secret key names.
2. `k8s/infra/postgres-patroni/serviceaccount.yaml`
3. `k8s/infra/postgres-patroni/rbac.yaml`
4. `k8s/infra/postgres-patroni/service.yaml`
5. `k8s/infra/postgres-patroni/configmap.yaml`
6. `k8s/infra/postgres-patroni/statefulset.yaml`

Rollback must not automatically delete the StatefulSet PVCs. This Docker
Desktop environment has one node and local `hostpath` storage, so three
members test Patroni behavior but do not provide physical node or storage HA.

During bootstrap, the Docker Desktop `hostpath` mount root did not allow
`initdb` to change its permissions. The PVC mount remains
`/var/lib/postgresql/data`, while Patroni's PostgreSQL `data_dir` is
`/var/lib/postgresql/data/pgdata`. PostgreSQL UID 999 can create and manage
that child directory, allowing non-root bootstrap without a root or privileged
init container.

## Prerequisites

- Docker Desktop Kubernetes is enabled.
- The active context is `docker-desktop`.
- The cluster node is ready.
- `kubectl`, Docker CLI, and Helm are available.

Verify the environment:

```powershell
kubectl config current-context
kubectl get nodes
kubectl get pods -n kube-system
kubectl get storageclass
```

## Build the image

Build Eureka Server with the fixed tag used by its Deployment:

```powershell
docker build -t ecommerce/eureka-server:k8s-cf4aa68 ./eureka-server
```

Build Config Server with the fixed tag used by its Deployment:

```powershell
docker build -t ecommerce/config-server:k8s-3da1dd6 ./config-server
```

Build the core service images with the fixed tags used by their Deployments:

```powershell
docker build -t ecommerce/api-gateway:k8s-26faf35 ./api-gateway
docker build -t ecommerce/auth-service:k8s-26faf35 -f auth-service/Dockerfile .
docker build -t ecommerce/customer-service:k8s-26faf35 -f customer-service/Dockerfile .
```

Build Product Service with the fixed tag used by its Deployment:

```powershell
docker build -t ecommerce/product-service:k8s-a6454ba `
  -f product-service/Dockerfile .
```

Build the Saga service images with the fixed tags used by their Deployments:

```powershell
docker build -t ecommerce/order-service:k8s-a91231c `
  -f order-service/Dockerfile .
docker build -t ecommerce/inventory-service:k8s-a91231c `
  -f inventory-service/Dockerfile .
docker build -t ecommerce/payment-service:k8s-a91231c `
  -f payment-service/Dockerfile .
```

Docker Desktop Kubernetes with the kubeadm provisioner uses the local Docker
image store. The Deployment therefore uses `imagePullPolicy: IfNotPresent`.
If the cluster is recreated with a different provisioner or runtime, load the
image into that runtime or publish it to a registry before applying the
Deployment.

## Apply

Use this order for a clean cluster. Stop if a dependency, restore validation,
or rollout fails.

1. Verify the Kubernetes context.
2. Create the `ecommerce` namespace.
3. Create required Secrets from local environment variables.
4. Apply Eureka Server.
5. Apply Config Server.
6. Apply PostgreSQL and MongoDB.
7. Restore verified backups and compare exact data/schema metadata.
8. Apply Kafka.
9. Create the six business topics.
10. Apply API Gateway, Auth, Customer, Product, Inventory, Payment, and Order.
11. Apply Prometheus and Grafana.
12. Install the pinned Traefik Helm release.
13. Apply the API Ingress.
14. Run the complete acceptance suite.

First verify the context and create the namespace:

```powershell
kubectl config current-context
kubectl get nodes
kubectl apply --dry-run=client -f k8s/base/namespace.yaml
kubectl apply --dry-run=server -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/namespace.yaml
```

Create the shared Secret only after the namespace exists. Do not commit or
print real values. This example reads them from the current PowerShell process:

```powershell
kubectl create secret generic ecommerce-secrets `
  --namespace ecommerce `
  --from-literal=JWT_SECRET="$env:JWT_SECRET" `
  --from-literal=INTERNAL_API_KEY="$env:INTERNAL_API_KEY" `
  --from-literal=CUSTOMER_DB_USERNAME="$env:POSTGRES_USER" `
  --from-literal=CUSTOMER_DB_PASSWORD="$env:POSTGRES_PASSWORD" `
  --from-literal=ORDER_DB_USERNAME="$env:POSTGRES_USER" `
  --from-literal=ORDER_DB_PASSWORD="$env:POSTGRES_PASSWORD" `
  --from-literal=INVENTORY_DB_USERNAME="$env:POSTGRES_USER" `
  --from-literal=INVENTORY_DB_PASSWORD="$env:POSTGRES_PASSWORD" `
  --from-literal=PAYMENT_DB_USERNAME="$env:POSTGRES_USER" `
  --from-literal=PAYMENT_DB_PASSWORD="$env:POSTGRES_PASSWORD"
```

`k8s/base/secret.example.yaml` documents the required keys only. It must not be
applied without replacing its placeholders, and real values must never be
committed.

Apply Eureka and Config Server first:

```powershell
kubectl apply --dry-run=client -f k8s/apps/eureka-server/
kubectl apply --dry-run=server -f k8s/apps/eureka-server/
kubectl apply -f k8s/apps/eureka-server/
kubectl rollout status deployment/eureka-server -n ecommerce
kubectl apply --dry-run=client -f k8s/apps/config-server/
kubectl apply --dry-run=server -f k8s/apps/config-server/
kubectl apply -f k8s/apps/config-server/
kubectl rollout status deployment/config-server -n ecommerce
```

Apply PostgreSQL and MongoDB, then restore only verified external backups as
described in the database migration section. Do not start database clients
until exact counts, constraints, and indexes match:

```powershell
kubectl apply -f k8s/infra/postgres/service.yaml
kubectl apply -f k8s/infra/postgres/statefulset.yaml
kubectl rollout status statefulset/postgres -n ecommerce
kubectl apply -f k8s/infra/mongodb/service.yaml
kubectl apply -f k8s/infra/mongodb/statefulset.yaml
kubectl rollout status statefulset/mongodb -n ecommerce
```

Apply Kafka after database restore validation. Create the six business topics
with three partitions and replication factor one; do not create internal
topics manually:

```powershell
kubectl apply -f k8s/infra/kafka/service.yaml
kubectl apply -f k8s/infra/kafka/statefulset.yaml
kubectl rollout status statefulset/kafka -n ecommerce

$topics = @(
  "order-created",
  "order-created-dlt",
  "inventory-reserved",
  "inventory-reservation-failed",
  "payment-succeeded",
  "payment-failed"
)
foreach ($topic in $topics) {
  kubectl exec -n ecommerce kafka-0 -- `
    /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:9092 `
    --create --if-not-exists `
    --topic $topic --partitions 3 --replication-factor 1
}
```

Apply application Services and Deployments. Start Inventory and Payment before
Order so consumers are ready before a new order event can be produced:

```powershell
kubectl apply -f k8s/apps/api-gateway/
kubectl apply -f k8s/apps/auth-service/
kubectl apply -f k8s/apps/customer-service/
kubectl apply -f k8s/apps/product-service/
kubectl apply -f k8s/apps/inventory-service/
kubectl rollout status deployment/inventory-service -n ecommerce
kubectl apply -f k8s/apps/payment-service/
kubectl rollout status deployment/payment-service -n ecommerce
kubectl apply -f k8s/apps/order-service/
kubectl rollout status deployment/order-service -n ecommerce
kubectl rollout status deployment/api-gateway -n ecommerce
kubectl rollout status deployment/auth-service -n ecommerce
kubectl rollout status deployment/customer-service -n ecommerce
kubectl rollout status deployment/product-service -n ecommerce
```

Next install Prometheus and Grafana using the monitoring section, install the
pinned Traefik Helm release, and apply `k8s/infra/ingress/ingress.yaml`.
Complete the process with health, register/login/profile, Product, three Saga,
outbox, idempotency, Kafka lag, monitoring, Ingress, and NodePort fallback
acceptance checks.

## Test

Forward the ClusterIP Service to the local machine:

```powershell
kubectl port-forward -n ecommerce service/eureka-server 8761:8761
```

While port-forward is running, open `http://localhost:8761` or check:

```powershell
curl.exe http://localhost:8761/actuator/health
curl.exe http://localhost:8761/actuator/health/liveness
curl.exe http://localhost:8761/actuator/health/readiness
```

Forward Config Server in a separate terminal:

```powershell
kubectl port-forward -n ecommerce service/config-server 8888:8888
```

Check Config Server health and native repository endpoints:

```powershell
curl.exe http://localhost:8888/actuator/health
curl.exe http://localhost:8888/actuator/health/liveness
curl.exe http://localhost:8888/actuator/health/readiness
curl.exe http://localhost:8888/customer-service/default
curl.exe http://localhost:8888/api-gateway/default
```

## Core service smoke test

Use `http://ecommerce.local` after adding the optional hosts entry described in
Current architecture. Without that entry, run equivalent requests with
`curl.exe --resolve`. NodePort `32323` remains the fallback. Use a unique test
email:

```powershell
$gatewayBaseUrl = "http://ecommerce.local"
$testEmail = "k8s-smoke-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())@example.test"
$testPassword = "ReplaceForLocalSmokeTest123!"

$registerBody = @{
  firstName = "Kubernetes"
  lastName = "Smoke"
  email = $testEmail
  password = $testPassword
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "$gatewayBaseUrl/api/v1/auth/register" `
  -ContentType "application/json" `
  -Body $registerBody | Out-Null

$loginBody = @{
  email = $testEmail
  password = $testPassword
} | ConvertTo-Json

$login = Invoke-RestMethod -Method Post `
  -Uri "$gatewayBaseUrl/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody

$headers = @{ Authorization = "Bearer $($login.token)" }
Invoke-RestMethod -Method Get `
  -Uri "$gatewayBaseUrl/api/v1/customers/me" `
  -Headers $headers
```

The customer profile endpoint must return `401` without a token. Requests to
`/internal/**` through the Gateway must not expose Customer Service internal
endpoints.

Product Service uses the same Ingress endpoint. With a valid USER token, verify
the product list and get endpoints. With a SELLER or ADMIN token, create and
patch a uniquely named product. Verify that a USER receives `403` for writes,
an unauthenticated write receives `401`, an unknown product returns `404`, and
a duplicate product name returns `409`. Do not print JWT values in smoke-test
output.

For the Saga smoke test, use the Ingress endpoint and a valid USER token to
create orders against products with prepared inventory. Verify successful,
insufficient-stock, and simulated-payment-failure flows. Check final order
status, inventory reservation or compensation, payment state, processed-event
idempotency rows, and each service's outbox transition from `PENDING` to
`PUBLISHED`. Do not print JWTs, credentials, or complete Kafka payloads.

## Self-healing

Delete one pod at a time and wait for its Deployment before testing the
register/login flow again:

```powershell
kubectl delete pod -n ecommerce `
  -l app.kubernetes.io/name=customer-service
kubectl rollout status deployment/customer-service -n ecommerce

kubectl delete pod -n ecommerce `
  -l app.kubernetes.io/name=auth-service
kubectl rollout status deployment/auth-service -n ecommerce

kubectl delete pod -n ecommerce `
  -l app.kubernetes.io/name=api-gateway
kubectl rollout status deployment/api-gateway -n ecommerce

kubectl delete pod -n ecommerce `
  -l app.kubernetes.io/name=product-service
kubectl rollout status deployment/product-service -n ecommerce

kubectl delete pod -n ecommerce `
  -l app.kubernetes.io/name=order-service
kubectl rollout status deployment/order-service -n ecommerce

kubectl delete pod -n ecommerce `
  -l app.kubernetes.io/name=inventory-service
kubectl rollout status deployment/inventory-service -n ecommerce

kubectl delete pod -n ecommerce `
  -l app.kubernetes.io/name=payment-service
kubectl rollout status deployment/payment-service -n ecommerce
```

## Safe component removal

Routine cleanup must not delete the `ecommerce` namespace, StatefulSet PVCs,
monitoring PVCs, Compose volumes, or external backups. Namespace deletion
removes every namespaced resource. The default Docker Desktop `hostpath`
StorageClass uses reclaim policy `Delete`, so PVC deletion can also permanently
delete its PV data.

Remove only the application Ingress when routing must be disabled:

```powershell
kubectl delete -f k8s/infra/ingress/ingress.yaml
```

The Gateway remains available through NodePort `32323`. If no other Ingress
uses Traefik, remove its Helm release separately:

```powershell
kubectl get ingress -A
helm uninstall traefik -n traefik
kubectl get all -n traefik
```

Scale stateless applications to zero when they must be stopped without
removing configuration or data:

```powershell
kubectl scale deployment/api-gateway deployment/auth-service `
  deployment/customer-service deployment/product-service `
  deployment/order-service deployment/inventory-service `
  deployment/payment-service -n ecommerce --replicas=0
```

StatefulSet, PVC, PV, Secret, or namespace removal requires separate explicit
data-loss approval and verified off-cluster backups. Docker Desktop Kubernetes
reset/delete can destroy all local `hostpath` data and must not be used as
routine cleanup. Keep stopped Compose database/Kafka containers and their
volumes until the rollback window has been explicitly closed.

## Local environment notes

- Eureka and Config Server are internal-only `ClusterIP` Services.
- PostgreSQL, MongoDB, and Kafka are internal-only Kubernetes Services backed
  by StatefulSets and PVCs.
- API traffic enters through Traefik; Gateway NodePort `32323` remains a local
  fallback.
- Probe and resource values are local-development starting points and should
  be tuned from observed runtime data.
- Real JWT secrets, internal API keys, and database credentials must never be
  written to repository files, command output, logs, or test reports.

## Kubernetes monitoring

### Metrics Server

Metrics Server exposes recent pod and node CPU/memory resource usage through
the Kubernetes Resource Metrics API. Kubernetes HPA uses this API for standard
CPU- and memory-based scaling decisions. Prometheus is the long-term
monitoring, querying, and dashboard data source; having Prometheus running does
not make the Resource Metrics API available to HPA.

This repository pins the official Metrics Server `v0.8.0` release manifest at
`k8s/platform/metrics-server/components.yaml`. Its upstream source is:

```text
https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.8.0/components.yaml
```

Install it after the cluster and node are healthy and before installing any
resource-metric HPA. Validate both client and server schemas first:

```powershell
kubectl config current-context
kubectl get nodes
kubectl apply --dry-run=client `
  -f k8s/platform/metrics-server/components.yaml
kubectl apply --dry-run=server `
  -f k8s/platform/metrics-server/components.yaml
kubectl apply -f k8s/platform/metrics-server/components.yaml
```

Verify the deployment, aggregated API, and resource metrics:

```powershell
kubectl get deployment metrics-server -n kube-system
kubectl get pods -n kube-system -l k8s-app=metrics-server
kubectl get apiservice v1beta1.metrics.k8s.io
kubectl describe apiservice v1beta1.metrics.k8s.io
kubectl top nodes
kubectl top pods -n ecommerce
```

The normal kubelet certificate validation attempt fails on this Docker Desktop
cluster because the kubelet serving certificate does not contain the node IP
in its Subject Alternative Names. The checked-in local manifest therefore uses
`--kubelet-insecure-tls`. This option disables verification of kubelet serving
certificates. It is limited to this local Docker Desktop environment and is not
appropriate for production clusters. Production clusters must use kubelet
serving certificates signed by a trusted CA with the required node addresses.

Rollback removes only the resources defined by the pinned Metrics Server
manifest after their exact names and active context have been verified. Do not
remove the `kube-system` namespace or any application workload, PVC, Secret, or
database resource. After removal, confirm that the Metrics APIService is absent
and expect `kubectl top` and resource-metric HPA operation to be unavailable.

### Product Service horizontal pod autoscaling

Product Service is the first horizontal autoscaling target because its safe
read path does not create business data or involve Kafka, Saga, or outbox
processing. Its HPA requires a healthy Metrics Server and an available
`v1beta1.metrics.k8s.io` APIService.

The `autoscaling/v2` manifest is stored at
`k8s/apps/product-service/hpa.yaml`. It keeps at least one replica, permits at
most two replicas in the local environment, and targets average CPU
utilization of 65% relative to the Product Service container's CPU request.
The capacity limit is based on the controlled JWT read-only load test: HPA
scaled from one to two successfully, the second pod became Ready, and the
EndpointSlice grew from one to two endpoints. When HPA requested a third
replica, that pod remained Pending with `Insufficient memory`, while node
memory reached approximately 89–90%. After load stopped, the 300-second
scale-down stabilization window was observed and HPA naturally returned to one
replica. Therefore `maxReplicas` is limited to two for this local single-node
environment. Higher replica counts require increasing Docker Desktop node
memory capacity and repeating the load test.

Scale-up has no stabilization delay and can add at most one pod per 60
seconds. Scale-down considers the previous 300 seconds of recommendations and
can remove at most one pod per 120 seconds. Readiness continues to control when
a new pod enters the Product Service endpoints.

Validate and apply the HPA only after Metrics Server checks pass:

```powershell
kubectl get apiservice v1beta1.metrics.k8s.io
kubectl top nodes
kubectl top pods -n ecommerce
kubectl apply --dry-run=client -f k8s/apps/product-service/hpa.yaml
kubectl apply --dry-run=server -f k8s/apps/product-service/hpa.yaml
kubectl apply -f k8s/apps/product-service/hpa.yaml
```

Monitor the HPA and its target:

```powershell
kubectl get hpa product-service -n ecommerce
kubectl describe hpa product-service -n ecommerce
kubectl get deployment product-service -n ecommerce
kubectl get pods -n ecommerce
kubectl top pods -n ecommerce
```

The HPA `TARGETS` column must contain a numeric current CPU value, such as
`3%/65%`. An `<unknown>` value means the controller cannot obtain a usable
resource metric. Stop before load testing and inspect HPA events for
`FailedGetResourceMetric`, then verify Metrics Server, pod metrics, and the
container CPU request.

Do not run manual `kubectl scale` while the HPA is active. The HPA owns the
target Deployment's desired replica count and can overwrite manual changes on
its next reconciliation. The Deployment manifest intentionally retains
`replicas: 1` as its non-HPA baseline; reapplying that field while the HPA is
active can cause a temporary replica change before HPA reconciles it again.

The first controlled load test will use `GET /api/v1/products`. Do not use a
write endpoint or a health endpoint for that test. Do not increase
`maxReplicas` beyond two in this local environment without first increasing
Docker Desktop node memory capacity and re-evaluating node memory headroom.
Before starting the load test, verify that `ecommerce.local` is included in
`NO_PROXY`/`no_proxy`; otherwise the client may send requests to an unrelated
proxy endpoint. The JWT and other sensitive test values must be supplied via
environment variables and must never be printed to the terminal or logs.

For rollback, stop load generation and wait for active requests to drain,
verify the context and exact HPA target, then remove only the Product Service
HPA resource. Reconcile the Product Service Deployment from its manifest to
restore the one-replica baseline. Confirm Product Service readiness and GET
behavior afterward. Metrics Server and all stateful, monitoring, Secret, and
storage resources remain untouched.

Prometheus and Grafana use the same pinned image versions as the Compose
environment:

- `prom/prometheus:v3.13.1`
- `grafana/grafana:13.1.1`

Create the Grafana admin Secret manually. Replace the shell variable locally;
never put its real value in a manifest, README, log, test report, or Git diff:

```powershell
$grafanaAdminPassword = Read-Host -AsSecureString
$grafanaAdminPasswordText = [System.Net.NetworkCredential]::new(
  "", $grafanaAdminPassword
).Password
kubectl create secret generic grafana-admin -n ecommerce `
  --from-literal=admin-password="$grafanaAdminPasswordText"
Remove-Variable grafanaAdminPasswordText
```

Validate the resources before applying them:

```powershell
kubectl apply --dry-run=client -f k8s/monitoring/prometheus/
kubectl apply --dry-run=client -f k8s/monitoring/grafana/
kubectl apply --dry-run=server -f k8s/monitoring/prometheus/
kubectl apply --dry-run=server -f k8s/monitoring/grafana/
```

Apply persistent storage and configuration first, then Prometheus:

```powershell
kubectl apply -f k8s/monitoring/prometheus/pvc.yaml
kubectl apply -f k8s/monitoring/prometheus/configmap.yaml
kubectl apply -f k8s/monitoring/prometheus/service.yaml
kubectl apply -f k8s/monitoring/prometheus/deployment.yaml
kubectl rollout status deployment/prometheus -n ecommerce
```

Check Prometheus locally and inspect target health:

```powershell
kubectl port-forward -n ecommerce service/prometheus 9090:9090
curl.exe http://localhost:9090/-/healthy
curl.exe http://localhost:9090/-/ready
curl.exe "http://localhost:9090/api/v1/targets"
```

After all Prometheus targets are `UP`, apply Grafana:

```powershell
kubectl apply -f k8s/monitoring/grafana/pvc.yaml
kubectl apply -f k8s/monitoring/grafana/configmap.yaml
kubectl apply -f k8s/monitoring/grafana/service.yaml
kubectl apply -f k8s/monitoring/grafana/deployment.yaml
kubectl rollout status deployment/grafana -n ecommerce
kubectl get service grafana -n ecommerce `
  -o jsonpath='{.spec.ports[0].nodePort}'
```

Docker Desktop publishes the assigned Grafana NodePort on localhost. A
port-forward can be used instead:

```powershell
curl.exe http://localhost:30305/api/health
kubectl port-forward -n ecommerce service/grafana 3000:3000
curl.exe http://localhost:3000/api/health
```

In Grafana, confirm that the provisioned `Prometheus` datasource uses UID
`prometheus`, reports a successful connection, and that the E-Commerce
dashboard is loaded. Its existing queries, including
`ecommerce_orders_total` and
`ecommerce_inventory_reservations_total`, must remain unchanged.
The final local acceptance baseline is 10/10 Prometheus targets `UP`.

The Prometheus configuration uses Kubernetes Service DNS names and
`/actuator/prometheus` for Eureka, Config Server, Gateway, Auth, Customer,
Product, Order, Inventory, and Payment. This ClusterIP scrape approach is
appropriate while every application has one replica. If a Service is scaled
to multiple pods, scraping its ClusterIP does not guarantee that every pod
instance is collected separately; migrate to Kubernetes service discovery or
pod annotations before scaling.

Prometheus and Grafana have separate PVCs named `prometheus-data` and
`grafana-data`. The default StorageClass is used. Delete each monitoring pod
and wait for its replacement to test self-healing and persistence:

```powershell
kubectl delete pod -n ecommerce -l app.kubernetes.io/name=prometheus
kubectl rollout status deployment/prometheus -n ecommerce
kubectl delete pod -n ecommerce -l app.kubernetes.io/name=grafana
kubectl rollout status deployment/grafana -n ecommerce
kubectl get pvc -n ecommerce prometheus-data grafana-data
```

The CPU and memory requests/limits in these manifests are local-development
starting values and must be tuned from observed usage.

Stop Kubernetes monitoring without deleting its configuration or data:

```powershell
kubectl scale deployment/grafana -n ecommerce --replicas=0
kubectl scale deployment/prometheus -n ecommerce --replicas=0
kubectl get pvc -n ecommerce prometheus-data grafana-data
```

PVC cleanup is intentionally omitted because it permanently removes local
monitoring history and Grafana state. Delete `grafana-data` or
`prometheus-data` only with explicit data-loss approval.

## Storage and database backup preparation

Keep PostgreSQL and MongoDB backups outside this repository in a
user-restricted local directory. Do not commit dumps: they contain real
database content. Create one PostgreSQL custom-format dump per database with
`pg_dump -Fc`, validate it with `pg_restore --list`, and record its SHA-256
checksum. Back up `product_db` with `mongodump`, verify its BSON and metadata
files, and checksum every backup file. Test restores only in temporary
databases, compare schemas, indexes, constraints, and exact record counts, then
remove the temporary databases. Never restore over the active databases.

Persistent capacities in the final local architecture are:

| Component | PVC capacity |
| --- | ---: |
| PostgreSQL | 5Gi |
| MongoDB | 2Gi |
| Kafka | 5Gi |
| Prometheus | 2Gi |
| Grafana | 1Gi |

No manifest fixes `storageClassName` or uses a direct `hostPath`; dynamic
claims use the cluster's default Docker Desktop `hostpath` StorageClass.

The disposable storage manifests use the default StorageClass without fixing
`storageClassName`:

```powershell
kubectl apply --dry-run=client -f k8s/infra/storage/
kubectl apply --dry-run=server -f k8s/infra/storage/
kubectl apply -f k8s/infra/storage/pvc-test.yaml
kubectl wait --for=jsonpath='{.status.phase}'=Bound `
  pvc/storage-persistence-test -n ecommerce --timeout=60s
kubectl apply -f k8s/infra/storage/pod-test.yaml
kubectl wait --for=condition=Ready pod/storage-persistence-test `
  -n ecommerce --timeout=60s
kubectl exec -n ecommerce storage-persistence-test -- `
  test -f /data/persistence-marker
```

Delete and recreate only the test pod to prove that its marker remains on the
PVC:

```powershell
kubectl delete -f k8s/infra/storage/pod-test.yaml
kubectl apply -f k8s/infra/storage/pod-test.yaml
kubectl wait --for=condition=Ready pod/storage-persistence-test `
  -n ecommerce --timeout=60s
kubectl exec -n ecommerce storage-persistence-test -- `
  grep -Fx ecommerce-storage-persistence-test /data/persistence-marker
kubectl delete -f k8s/infra/storage/pod-test.yaml
```

Deleting a pod does not delete its PVC. PVC deletion is intentionally a
separate, destructive step and can delete the stored data. Docker Desktop's
default `hostpath` StorageClass has reclaim policy `Delete`, so deleting the
claim can also delete its dynamically provisioned volume. Docker Desktop or
computer stop/start normally preserves the local cluster, but Kubernetes
reset/delete can remove hostpath data. Never reset the cluster without verified
external backups.

Before a database migration, use this rollback sequence:

1. Scale Order, Inventory, Payment, Customer, and Product to zero.
2. Stop all new writes to the Kubernetes databases and Kafka.
3. Start the retained Compose PostgreSQL, MongoDB, and Kafka containers.
4. Restore the five Deployment database endpoints to
   `host.docker.internal` and the three Kafka bootstrap overrides to
   `host.docker.internal:29093`.
5. Apply the rollback Deployment manifests.
6. Start Customer and Product, then Inventory and Payment consumers, and start
   Order last.
7. Run health, register/login/profile, Product, and all three Saga tests.
8. Verify outbox publication, compensation, idempotency, and Kafka lag.
9. Preserve Kubernetes PVCs, Compose named volumes, and every backup set until
   data consistency and the final operating location are confirmed.

## PostgreSQL and MongoDB migration

PostgreSQL and MongoDB run as single-replica StatefulSets for local
development; this provides stable identity and persistent storage, not high
availability. Both use the default StorageClass. Never delete their PVCs,
Compose named volumes, or external backups as part of a rollout or rollback.
Docker Desktop Kubernetes reset/delete can destroy local `hostpath` data.

PostgreSQL reuses the equal database username/password values already stored
under `CUSTOMER_DB_USERNAME` and `CUSTOMER_DB_PASSWORD` in
`ecommerce-secrets`. MongoDB retains the existing local no-auth model and is
exposed only through ClusterIP Services. No real secret belongs in Git.

Before cutover, record exact counts, confirm no active Saga or pending outbox
work, then scale down in this order:

```powershell
kubectl scale deployment/order-service deployment/inventory-service `
  deployment/payment-service -n ecommerce --replicas=0
kubectl scale deployment/customer-service deployment/product-service `
  -n ecommerce --replicas=0
```

Take final external `pg_dump -Fc` and `mongodump` backups, record SHA-256
checksums, and validate them before applying database resources:

```powershell
kubectl apply -f k8s/infra/postgres/service.yaml
kubectl apply -f k8s/infra/postgres/statefulset.yaml
kubectl rollout status statefulset/postgres -n ecommerce
kubectl apply -f k8s/infra/mongodb/service.yaml
kubectl apply -f k8s/infra/mongodb/statefulset.yaml
kubectl rollout status statefulset/mongodb -n ecommerce
```

Create the four empty PostgreSQL databases, copy only the verified final dumps
temporarily to `postgres-0`, and restore each with
`pg_restore --exit-on-error`. Copy the verified MongoDB dump temporarily to
`mongodb-0` and restore `product_db` with `mongorestore`. Stop immediately on
any restore error. Remove only the temporary in-pod dump copies afterward.
Before starting applications, compare exact counts, table/collection lists,
PostgreSQL constraints and indexes, and MongoDB indexes with the final source
snapshot.

Apply and start applications in dependency order:

```powershell
kubectl apply -f k8s/apps/customer-service/deployment.yaml
kubectl apply -f k8s/apps/product-service/deployment.yaml
kubectl scale deployment/customer-service deployment/product-service `
  -n ecommerce --replicas=1
kubectl apply -f k8s/apps/order-service/deployment.yaml
kubectl apply -f k8s/apps/inventory-service/deployment.yaml
kubectl apply -f k8s/apps/payment-service/deployment.yaml
kubectl scale deployment/order-service deployment/inventory-service `
  deployment/payment-service -n ecommerce --replicas=1
```

After smoke and Saga tests, delete `postgres-0` and `mongodb-0` separately and
verify that each StatefulSet recreates the same pod identity, reattaches its
PVC, and preserves exact counts. Do not run this during an active Saga.
Compose PostgreSQL and MongoDB may be stopped only after all regression tests
pass; never remove their containers or named volumes.

For rollback, scale the five writing applications to zero, stop new writes to
the Kubernetes databases, start Compose PostgreSQL and MongoDB, restore the
five manifest endpoints to `host.docker.internal`, apply them, scale customer
and product up before the Saga services, and rerun smoke and Saga tests. Keep
all Kubernetes PVCs, external backups, and Compose named volumes until data
consistency has been independently confirmed.

## Kafka migration

Kafka runs as a single-broker KRaft StatefulSet for local development. Apply
the headless and client Services before the StatefulSet:

```powershell
kubectl apply -f k8s/infra/kafka/service.yaml
kubectl apply -f k8s/infra/kafka/statefulset.yaml
kubectl rollout status statefulset/kafka -n ecommerce
```

The non-secret KRaft `CLUSTER_ID` is generated once with
`kafka-storage.sh random-uuid` and remains fixed in the StatefulSet. Kafka data
is stored at `/var/lib/kafka/data` on the 5Gi `kafka-data-kafka-0` PVC. Do not
delete this PVC. Docker Desktop Kubernetes reset/delete can also destroy its
local `hostpath` data.

After the broker is Ready, explicitly create these topics with three
partitions and replication factor one: `order-created`, `order-created-dlt`,
`inventory-reserved`, `inventory-reservation-failed`, `payment-succeeded`,
and `payment-failed`. Do not manually create internal Kafka topics.

Before cutover, verify final Saga states, all outbox `PENDING` counts, consumer
lag, and topic end offsets. Then stop producers and consumers:

```powershell
kubectl scale deployment/order-service deployment/inventory-service `
  deployment/payment-service -n ecommerce --replicas=0
```

Apply the three Deployment manifests with `KAFKA_BOOTSTRAP_SERVERS=kafka:9092`.
Start `inventory-service`, then `payment-service`, and finally
`order-service`, returning each to one replica. Verify consumer membership and
zero lag before creating a new order.

Run successful, insufficient-stock, and payment-failure Saga tests; verify
compensation, ownership, outbox publication, and idempotency. With no active
Saga, delete only `kafka-0` and confirm that the StatefulSet recreates it with
the same cluster ID, PVC, topics, partition offsets, leaders, and ISR.

Compose Kafka may be stopped only after all tests pass. Never remove its
container, anonymous volumes, or `/tmp/kafka-logs` data. For rollback, scale
the three Saga services to zero, start Compose Kafka, restore their bootstrap
override to `host.docker.internal:29093`, apply the manifests, then start
Inventory and Payment before Order and repeat health, lag, and Saga tests.
Never delete either Kafka data set during rollback. A single broker and RF=1
provide no high availability and are not production topology.

## Local API ingress

The local cluster uses Traefik instead of ingress-nginx, which is retired.
Traefik is installed from the official `https://traefik.github.io/charts`
Helm repository with chart version `41.1.0` explicitly pinned. This
configuration enables only the Kubernetes Ingress provider, exposes local
HTTP, and does not expose the Traefik dashboard or configure TLS.

Prepare and inspect the release before installation:

```powershell
helm repo add traefik https://traefik.github.io/charts
helm repo update traefik
$chartDir = Join-Path $env:TEMP "traefik-chart-41.1.0"
New-Item -ItemType Directory -Force $chartDir | Out-Null
helm pull traefik/traefik --version 41.1.0 --untar --untardir $chartDir
helm lint "$chartDir/traefik" `
  -f k8s/infra/ingress/values.yaml
helm template traefik traefik/traefik --version 41.1.0 `
  --namespace traefik --skip-crds -f k8s/infra/ingress/values.yaml
```

Install or upgrade the `traefik` release in its own namespace, wait for the
controller, and then apply the application Ingress:

```powershell
helm upgrade --install traefik traefik/traefik --version 41.1.0 `
  --namespace traefik --create-namespace --skip-crds `
  -f k8s/infra/ingress/values.yaml --wait
kubectl apply -f k8s/infra/ingress/ingress.yaml
kubectl get deployment,service -n traefik
kubectl get ingressclass
kubectl get ingress ecommerce-api -n ecommerce
```

The Ingress routes `http://ecommerce.local/` to `api-gateway:8080`. Without
changing the Windows hosts file, test it using the LoadBalancer address:

```powershell
curl.exe --resolve ecommerce.local:80:127.0.0.1 `
  http://ecommerce.local/actuator/health
```

If Docker Desktop returns a different reachable LoadBalancer address, replace
`127.0.0.1` in the test. An optional manual hosts entry is:

```text
127.0.0.1 ecommerce.local
```

The `/` prefix also preserves the Gateway's existing local
`/actuator/health` behavior; no new Actuator route is created. Run register,
login, authenticated profile, product, order, and all three Saga tests through
the hostname. Verify compensation, zero pending outbox events, and zero Kafka
consumer lag. The existing Gateway NodePort remains the fallback:

```powershell
curl.exe http://localhost:32323/actuator/health
```

With no active Saga, delete only the Traefik pod and confirm that its
Deployment recreates it, reloads `ecommerce-api`, and serves an authenticated
request. The Gateway NodePort must remain available throughout this test.

To roll back application routing, remove only the Ingress and continue through
NodePort `32323`:

```powershell
kubectl delete -f k8s/infra/ingress/ingress.yaml
```

If no other Ingress depends on Traefik, uninstall the controller separately:

```powershell
kubectl get ingress -A
helm uninstall traefik -n traefik
kubectl get all -n traefik
```

Inspect the namespace before deleting it, and remove any manually added hosts
entry separately. Docker Desktop Kubernetes reset/delete removes the
controller and may destroy local PVC data; reinstall from the pinned chart
after a reset and do not reset without verified database and Kafka backups.
This setup is unencrypted local HTTP only and is not a production ingress
configuration.

## Production limitations

The current architecture is complete for local development, but production
requires additional engineering:

- The Kubernetes cluster has one node.
- PostgreSQL and MongoDB each have one instance.
- MongoDB authentication is not enabled.
- Kafka has one broker and replication factor one.
- Ingress uses local HTTP without TLS.
- Persistent data uses Docker Desktop local `hostpath` storage.
- There is no automated off-cluster backup schedule.
- There are no NetworkPolicies.
- Secret rotation and an external secret-management system are not configured.
- Not every application image has been validated to run as non-root.
- Prometheus uses Service ClusterIP targets and is not prepared to discover
  every pod independently after scaling to multiple replicas.
