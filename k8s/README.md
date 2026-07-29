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

Docker Desktop Kubernetes with the kubeadm provisioner uses the local Docker
image store. The Deployment therefore uses `imagePullPolicy: IfNotPresent`.
If the cluster is recreated with a different provisioner or runtime, load the
image into that runtime or publish it to a registry before applying the
Deployment.

## Apply

Validate and apply the resources:

```powershell
kubectl apply --dry-run=client -f k8s/base/namespace.yaml
kubectl apply --dry-run=client -f k8s/apps/eureka-server/
kubectl apply --dry-run=server -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/namespace.yaml
kubectl apply --dry-run=server -f k8s/apps/eureka-server/
kubectl apply -f k8s/apps/eureka-server/
```

Check the rollout and workload:

```powershell
kubectl rollout status deployment/eureka-server -n ecommerce
kubectl get pods,services,endpointslices -n ecommerce
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

## Cleanup

Remove only the Eureka pilot:

```powershell
kubectl delete -f k8s/apps/eureka-server/
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
