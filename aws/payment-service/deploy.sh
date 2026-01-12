# /bin/bash

# create namespace
kubectl create namespace payment

# create secret
kubectl create secret generic payment-db-secret --from-literal=password=postgres -n payment

# deploy postgres
kubectl apply -f postgres.yaml

# deploy
kubectl apply -f payment-service.yaml
