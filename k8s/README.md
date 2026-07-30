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
```

Check the rollout and workload:

```powershell
kubectl rollout status deployment/eureka-server -n ecommerce
kubectl rollout status deployment/config-server -n ecommerce
kubectl rollout status deployment/customer-service -n ecommerce
kubectl rollout status deployment/auth-service -n ecommerce
kubectl rollout status deployment/api-gateway -n ecommerce
kubectl rollout status deployment/product-service -n ecommerce
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
kubectl delete secret ecommerce-secrets -n ecommerce
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
- Real JWT secrets, internal API keys, and database credentials must never be
  written to repository files, command output, logs, or test reports.
