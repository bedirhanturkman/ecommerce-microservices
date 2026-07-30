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
