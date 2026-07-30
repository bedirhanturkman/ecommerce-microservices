# Local Kubernetes

This directory contains the initial Kubernetes resources for the local
Docker Desktop cluster. The first pilot workload is Eureka Server.

## Prerequisites

- Docker Desktop Kubernetes is enabled.
- The active context is `docker-desktop`.
- The cluster node is ready.
- `kubectl` and Docker CLI are available.

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

Create the shared Secret before deploying the core services. Do not commit or
paste real values into a manifest. The following example reads the values from
the current PowerShell process environment:

```powershell
kubectl create secret generic ecommerce-secrets `
  --namespace ecommerce `
  --from-literal=JWT_SECRET="$env:JWT_SECRET" `
  --from-literal=INTERNAL_API_KEY="$env:INTERNAL_API_KEY" `
  --from-literal=CUSTOMER_DB_USERNAME="$env:POSTGRES_USER" `
  --from-literal=CUSTOMER_DB_PASSWORD="$env:POSTGRES_PASSWORD"
```

`k8s/base/secret.example.yaml` documents the required keys only. It must not be
applied without replacing its placeholders, and real values must never be
committed.

Before deploying the Saga services, add their database credentials to the
existing Secret without printing the values:

```powershell
$dbUser = [Convert]::ToBase64String(
  [Text.Encoding]::UTF8.GetBytes($env:POSTGRES_USER)
)
$dbPassword = [Convert]::ToBase64String(
  [Text.Encoding]::UTF8.GetBytes($env:POSTGRES_PASSWORD)
)
$secretPatch = @{
  data = @{
    ORDER_DB_USERNAME = $dbUser
    ORDER_DB_PASSWORD = $dbPassword
    INVENTORY_DB_USERNAME = $dbUser
    INVENTORY_DB_PASSWORD = $dbPassword
    PAYMENT_DB_USERNAME = $dbUser
    PAYMENT_DB_PASSWORD = $dbPassword
  }
} | ConvertTo-Json -Compress
kubectl patch secret ecommerce-secrets -n ecommerce `
  --type merge --patch $secretPatch
```

Validate and apply the resources:

```powershell
kubectl apply --dry-run=client -f k8s/base/namespace.yaml
kubectl apply --dry-run=client -f k8s/apps/eureka-server/
kubectl apply --dry-run=server -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/namespace.yaml
kubectl apply --dry-run=server -f k8s/apps/eureka-server/
kubectl apply -f k8s/apps/eureka-server/
kubectl apply --dry-run=client -f k8s/apps/config-server/
kubectl apply --dry-run=server -f k8s/apps/config-server/
kubectl apply -f k8s/apps/config-server/configmap.yaml
kubectl apply -f k8s/apps/config-server/service.yaml
kubectl apply -f k8s/apps/config-server/deployment.yaml
kubectl apply --dry-run=client -f k8s/apps/customer-service/
kubectl apply --dry-run=client -f k8s/apps/auth-service/
kubectl apply --dry-run=client -f k8s/apps/api-gateway/
kubectl apply --dry-run=server -f k8s/apps/customer-service/
kubectl apply --dry-run=server -f k8s/apps/auth-service/
kubectl apply --dry-run=server -f k8s/apps/api-gateway/
kubectl apply -f k8s/apps/customer-service/
kubectl rollout status deployment/customer-service -n ecommerce
kubectl apply -f k8s/apps/auth-service/
kubectl rollout status deployment/auth-service -n ecommerce
kubectl apply -f k8s/apps/api-gateway/
kubectl rollout status deployment/api-gateway -n ecommerce
kubectl apply --dry-run=client -f k8s/apps/product-service/
kubectl apply --dry-run=server -f k8s/apps/product-service/
kubectl apply -f k8s/apps/product-service/service.yaml
kubectl apply -f k8s/apps/product-service/deployment.yaml
kubectl rollout status deployment/product-service -n ecommerce
docker compose stop order-service inventory-service payment-service
kubectl apply --dry-run=client -f k8s/apps/order-service/
kubectl apply --dry-run=client -f k8s/apps/inventory-service/
kubectl apply --dry-run=client -f k8s/apps/payment-service/
kubectl apply --dry-run=server -f k8s/apps/order-service/
kubectl apply --dry-run=server -f k8s/apps/inventory-service/
kubectl apply --dry-run=server -f k8s/apps/payment-service/
kubectl apply -f k8s/apps/order-service/
kubectl rollout status deployment/order-service -n ecommerce
kubectl apply -f k8s/apps/inventory-service/
kubectl rollout status deployment/inventory-service -n ecommerce
kubectl apply -f k8s/apps/payment-service/
kubectl rollout status deployment/payment-service -n ecommerce
```

Check the rollout and workload:

```powershell
kubectl rollout status deployment/eureka-server -n ecommerce
kubectl rollout status deployment/config-server -n ecommerce
kubectl rollout status deployment/customer-service -n ecommerce
kubectl rollout status deployment/auth-service -n ecommerce
kubectl rollout status deployment/api-gateway -n ecommerce
kubectl rollout status deployment/product-service -n ecommerce
kubectl rollout status deployment/order-service -n ecommerce
kubectl rollout status deployment/inventory-service -n ecommerce
kubectl rollout status deployment/payment-service -n ecommerce
kubectl get pods,services,endpointslices -n ecommerce
```

Find the automatically assigned Gateway NodePort:

```powershell
kubectl get service api-gateway -n ecommerce `
  -o jsonpath='{.spec.ports[0].nodePort}'
```

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

Docker Desktop publishes the Gateway NodePort on localhost. Set the port and use
a unique test email:

```powershell
$gatewayNodePort = kubectl get service api-gateway -n ecommerce `
  -o jsonpath='{.spec.ports[0].nodePort}'
$gatewayBaseUrl = "http://localhost:$gatewayNodePort"
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

Product Service uses the same Gateway NodePort. With a valid USER token, verify
the product list and get endpoints. With a SELLER or ADMIN token, create and
patch a uniquely named product. Verify that a USER receives `403` for writes,
an unauthenticated write receives `401`, an unknown product returns `404`, and
a duplicate product name returns `409`. Do not print JWT values in smoke-test
output.

For the Saga smoke test, use the Gateway NodePort and a valid USER token to
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

## Cleanup

Remove the pilot workloads:

```powershell
kubectl delete -f k8s/apps/eureka-server/
kubectl delete -f k8s/apps/config-server/
kubectl delete -f k8s/apps/api-gateway/
kubectl delete -f k8s/apps/auth-service/
kubectl delete -f k8s/apps/customer-service/
kubectl delete -f k8s/apps/product-service/
kubectl delete -f k8s/apps/payment-service/
kubectl delete -f k8s/apps/inventory-service/
kubectl delete -f k8s/apps/order-service/
kubectl delete secret ecommerce-secrets -n ecommerce
```

To roll back only the Saga services, delete them in reverse order and then
restart their Compose instances:

```powershell
kubectl delete -f k8s/apps/payment-service/
kubectl delete -f k8s/apps/inventory-service/
kubectl delete -f k8s/apps/order-service/
docker compose up -d order-service inventory-service payment-service
```

Remove the complete local namespace and everything in it:

```powershell
kubectl delete -f k8s/base/namespace.yaml
```

## Local environment notes

- Eureka is internal-only and uses a `ClusterIP` Service.
- No persistent volume, ConfigMap, Secret, NodePort, or Ingress is required for
  this pilot.
- Probe timings allow for Java startup and should be tuned using observed
  startup behavior.
- CPU and memory requests/limits are initial local-development values. They
  must be reviewed using runtime metrics before broader service migration.
- No fixed JVM heap setting is applied.
- PostgreSQL temporarily remains in Docker Compose and is not deployed to
  Kubernetes.
- Customer Service reaches the Compose PostgreSQL port through
  `host.docker.internal`; this is a Docker Desktop-specific local dependency.
- MongoDB temporarily remains in Docker Compose and is not deployed to
  Kubernetes.
- Product Service reaches MongoDB through
  `mongodb://host.docker.internal:27017/product_db`; this is a Docker
  Desktop-specific local dependency.
- PostgreSQL and Kafka remain in Docker Compose for the Saga services.
- Kafka keeps `localhost:9092` for host clients and advertises the separate
  `host.docker.internal:29093` listener to Kubernetes pods.
- Never run Compose and Kubernetes instances of Order, Inventory, or Payment
  together without an intentional consumer-group migration; doing so causes
  rebalances and makes test-event ownership nondeterministic.
- Real JWT secrets, internal API keys, and database credentials must never be
  written to repository files, command output, logs, or test reports.

## Kubernetes monitoring

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
kubectl port-forward -n ecommerce service/grafana 3000:3000
curl.exe http://localhost:3000/api/health
```

In Grafana, confirm that the provisioned `Prometheus` datasource uses UID
`prometheus`, reports a successful connection, and that the E-Commerce
dashboard is loaded. Its existing queries, including
`ecommerce_orders_total` and
`ecommerce_inventory_reservations_total`, must remain unchanged.

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

Roll back Kubernetes monitoring without deleting its data:

```powershell
kubectl delete -f k8s/monitoring/grafana/deployment.yaml
kubectl delete -f k8s/monitoring/grafana/service.yaml
kubectl delete -f k8s/monitoring/grafana/configmap.yaml
kubectl delete secret grafana-admin -n ecommerce
kubectl delete -f k8s/monitoring/prometheus/deployment.yaml
kubectl delete -f k8s/monitoring/prometheus/service.yaml
kubectl delete -f k8s/monitoring/prometheus/configmap.yaml
docker compose up -d prometheus grafana
```

PVC cleanup is intentionally separate because it permanently removes local
monitoring history and Grafana state:

```powershell
kubectl delete -f k8s/monitoring/grafana/pvc.yaml
kubectl delete -f k8s/monitoring/prometheus/pvc.yaml
```

## Storage and database backup preparation

Keep PostgreSQL and MongoDB backups outside this repository in a
user-restricted local directory. Do not commit dumps: they contain real
database content. Create one PostgreSQL custom-format dump per database with
`pg_dump -Fc`, validate it with `pg_restore --list`, and record its SHA-256
checksum. Back up `product_db` with `mongodump`, verify its BSON and metadata
files, and checksum every backup file. Test restores only in temporary
databases, compare schemas, indexes, constraints, and exact record counts, then
remove the temporary databases. Never restore over the active databases.

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

1. Stop application write traffic.
2. Stop Kafka producers and consumers in a controlled order.
3. Take final PostgreSQL and MongoDB backups.
4. Record source schema and record counts.
5. Start the Kubernetes database resources.
6. Restore the verified backups.
7. Compare schemas, constraints, indexes, and record counts.
8. Change application database endpoints.
9. Run smoke and Saga tests.
10. On failure, stop the Kubernetes application pods.
11. Restore the Compose database endpoints.
12. Verify data consistency again before resuming traffic.

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
