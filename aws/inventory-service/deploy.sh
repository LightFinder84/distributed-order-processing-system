# /bin/bash

# create namespace
kubectl create namespace inventory

# create secret
kubectl create secret generic inventory-db-secret --from-literal=password=postgres -n inventory

# deploy postgres
kubectl apply -f postgres.yaml

# deploy
kubectl apply -f inventory-service.yaml
