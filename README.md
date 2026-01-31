# Distributed Order Processing System

A microservices-based order processing system built with Spring Boot, Kafka, PostgreSQL, and Kubernetes. This system demonstrates event-driven architecture with independent services for order management, inventory tracking, and payment processing.

## Architecture Overview

This project implements a distributed order processing system with the following microservices:

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│                 │      │                  │      │                 │
│  Order Service  │◄────►│  Payment Service │◄────►│ Payment         │
│                 │      │                  │      │ Simulator       │
└────────┬────────┘      └────────┬─────────┘      └─────────────────┘
         │                        │
         │                        │
         └────────┬───────────────┘
                  │
              ┌───▼──────┐
              │   Kafka  │
              │  Cluster │
              └───┬──────┘
                  │
         ┌────────▼────────┐
         │                 │
         │ Inventory       │
         │ Service         │
         │                 │
         └─────────────────┘
```

### Microservices

#### 1. **Order Service** (`order-service/`)
- Manages order creation, processing, and lifecycle
- Consumes and produces Kafka events
- Publishes `order-events` for other services
- Listens to `inventory-events` and `payment-events`
- Handles order persistence in PostgreSQL
- Implements event-driven order status updates

#### 2. **Inventory Service** (`inventory-service/`)
- Manages product inventory and stock levels
- Listens to `order-events` to update inventory
- Publishes `inventory-events` for order fulfillment
- Maintains inventory data in PostgreSQL

#### 3. **Payment Service** (`payment-service/`)
- Processes payment transactions
- Listens to `order-events` to trigger payments
- Publishes `payment-events` with payment status
- Integrates with payment processing logic
- Persists payment records in PostgreSQL

#### 4. **Payment Simulator** (`payment-simulator/`)
- Web-based UI for simulating payment events
- Uses Spring Thymeleaf for templating
- REST client for interacting with payment service
- Useful for testing and demonstration

### Infrastructure

#### Message Broker
- **Apache Kafka**: Event streaming platform for asynchronous communication
- **Topics**:
  - `order-events`: Order creation and updates
  - `inventory-events`: Inventory updates
  - `payment-events`: Payment status updates

#### Databases
- **PostgreSQL**: Persistent storage for each microservice
- Each service has its own isolated database instance
- Automatic schema creation using Hibernate DDL

#### Kubernetes Deployment
- **Deployment files**: `aws/` directory
- Containerized services with Docker
- Kubernetes manifests for orchestration
- AWS-based infrastructure

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 4.0.1 |
| Java Version | OpenJDK | 17 |
| Message Broker | Apache Kafka | - |
| Database | PostgreSQL | - |
| Container | Docker | - |
| Orchestration | Kubernetes | - |
| Build Tool | Maven | 3.9.12 |
| Libraries | Lombok, Jackson | 1.18.42 |

## Project Structure

```
distributed_order_processing_system/
├── order-service/           # Order management microservice
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── HELP.md
├── inventory-service/       # Inventory management microservice
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── HELP.md
├── payment-service/         # Payment processing microservice
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── HELP.md
├── payment-simulator/       # Payment simulation UI
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── HELP.md
├── aws/                     # Kubernetes and deployment configurations
│   ├── k8s/                # Kubernetes setup scripts and configuration
│   │   ├── master_setup.sh
│   │   ├── worker_setup.sh
│   │   ├── local-storage-class.sh
│   │   └── kernel_modules.conf
│   ├── kafka/              # Kafka deployment manifest
│   ├── order-service/      # Order service deployment
│   ├── inventory-service/  # Inventory service deployment
│   ├── payment-service/    # Payment service deployment
│   └── payment-simulator/  # Payment simulator deployment
├── sql-scripts/            # Database initialization and cleanup scripts
│   ├── clear-data.sh
│   ├── drop-database.sql
│   └── clear-*-db.sql
└── README.md
```

## Prerequisites

- **Java 17** or later
- **Maven 3.9+**
- **Docker** & **Docker Compose**
- **Kubernetes** (kubectl configured)
- **PostgreSQL** (or Docker image)
- **Apache Kafka** (or Docker image)

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd distributed_order_processing_system
```

### 2. Build All Services

```bash
# Build Order Service
cd order-service
mvn clean package
cd ..

# Build Inventory Service
cd inventory-service
mvn clean package
cd ..

# Build Payment Service
cd payment-service
mvn clean package
cd ..

# Build Payment Simulator
cd payment-simulator
mvn clean package
cd ..
```

### 3. Build Docker Images

```bash
# Order Service
cd order-service
docker build -t order-service:latest .
cd ..

# Inventory Service
cd inventory-service
docker build -t inventory-service:latest .
cd ..

# Payment Service
cd payment-service
docker build -t payment-service:latest .
cd ..

# Payment Simulator
cd payment-simulator
docker build -t payment-simulator:latest .
cd ..
```

### 4. Deploy to Kubernetes

#### Initialize Kubernetes Cluster

```bash
cd aws/k8s

# Setup master node
bash master_setup.sh

# Setup worker nodes
bash worker_setup.sh

# Create local storage class
bash local-storage-class.sh
```

#### Deploy Services

```bash
cd aws

# Deploy Kafka
kubectl apply -f kafka/kafka.yaml

# Deploy Order Service
cd order-service
bash deploy.sh
cd ..

# Deploy Inventory Service
cd inventory-service
bash deploy.sh
cd ..

# Deploy Payment Service
cd payment-service
bash deploy.sh
cd ..

# Deploy Payment Simulator
cd payment-simulator
bash deploy.sh
cd ..
```

### 5. Database Setup


## Configuration

Each microservice is configured via Kubernetes ConfigMaps with `application.properties`:

### Common Properties

```properties
# Kafka Configuration
spring.kafka.bootstrap-servers=kafka-0.kafka-service.kafka.svc.cluster.local:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer

# Database Configuration
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Service-specific topics
application.topic.order=order-events
application.topic.inventory=inventory-events
application.topic.payment=payment-events
application.batch-size=10
```

Refer to individual service deployment manifests in `aws/*/` for service-specific configurations.

## Event Flow

### Order Processing Flow

1. **Order Creation**
   - Order Service receives order request
   - Publishes `order-events` to Kafka

2. **Inventory Update**
   - Inventory Service consumes `order-events`
   - Updates stock levels
   - Publishes `inventory-events`

3. **Payment Processing**
   - Payment Service consumes `order-events`
   - Processes payment
   - Publishes `payment-events` with status

4. **Order Completion**
   - Order Service consumes `inventory-events` and `payment-events`
   - Updates order status
   - Completes order lifecycle

## Development

### Running Services Locally

For local development without Kubernetes, services can be run with:

```bash
cd order-service
mvn spring-boot:run
```

Ensure PostgreSQL and Kafka are running locally on default ports.

### Testing

```bash
# Run all tests in a service
cd order-service
mvn test

# Run specific test class
mvn test -Dtest=YourTestClass
```

Integration tests are included in each service under `src/test/java`.

## Database Schema

Each service manages its own PostgreSQL database with independent schemas:

- **order-service**: Orders, OrderItems
- **inventory-service**: Products, Inventory, Stock
- **payment-service**: Payments, Transactions

Initial schemas are created automatically via Hibernate DDL at startup.

## Monitoring & Logging

Services output logs to stdout, which can be captured by Kubernetes:

```bash
# View service logs
kubectl logs -f deployment/order-service -n order
kubectl logs -f deployment/inventory-service -n inventory
kubectl logs -f deployment/payment-service -n payment
```

## Troubleshooting

### Kafka Connection Issues
- Verify Kafka pod is running: `kubectl get pods -n kafka`
- Check service DNS: `nslookup kafka-0.kafka-service.kafka.svc.cluster.local`

### Database Connection Issues
- Verify PostgreSQL is accessible from service pods
- Check database username/password in ConfigMaps
- Review deployment manifests for correct database URLs

### Service Communication
- Verify services are in same Kubernetes cluster
- Check network policies and ingress configuration
- Review Kafka topic configurations

## Contributing

1. Create a feature branch
2. Make changes to the relevant service
3. Test locally and in Kubernetes
4. Submit pull request

## License

This project is provided as-is for educational and demonstration purposes.

## Support

For issues, questions, or suggestions, please refer to individual service README files or create an issue in the repository.
