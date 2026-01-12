# /bin/bash

# create namespace
kubectl create namespace payment-simulator

# deploy 
kubectl apply -f payment-simulator.yaml
