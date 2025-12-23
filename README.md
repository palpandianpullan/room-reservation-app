# Room Reservation Service

A Spring Boot microservice for managing room reservations for Marvel Hospitality Management Corporation.

## Features

### 1. REST API for Room Reservations
- **Endpoint**: `POST /reservations`
- **Payment Modes**:
  - **Cash**: Reservation confirmed immediately
  - **Credit Card**: Verifies payment with external credit-card-payment-service
  - **Bank Transfer**: Reservation created with PENDING_PAYMENT status

### 2. Event-Driven Architecture
- Consumes `bank-transfer-payment-update` Kafka topic
- Automatically confirms reservations when full payment is received
- Supports partial payments with cumulative tracking

[Credit Card Payment Service](https://github.com/palpandianpullan/credit-card-payment-service.git)

### 3. Automatic Cancellation
- Scheduled task runs daily at 2 AM
- Cancels bank transfer reservations that haven't received full payment 2 days before start date

## API Specification
room-reservation-app/src/main/resources/openapi/openapi.yaml

### Request Example

```json
POST /reservations
{
  "customerName": "John Doe",
  "roomNumber": "101A",
  "startDate": "2025-12-25",
  "endDate": "2025-12-28",
  "roomSegment": "LARGE",
  "modeOfPayment": "BANK_TRANSFER",
  "paymentReference": "TXN123456"
}
```

### Response Example

```json
{
  "reservationId": "P4145478",
  "status": "PENDING_PAYMENT"
}
```

### Validation Rules

- Reservation duration cannot exceed 30 days
- Start date must be before end date
- Payment reference required for credit card payments

## Kafka Event Format

### Bank Transfer Payment Update Event

```json
{
  "paymentId": "PAY123456",
  "debtorAccountNumber": "ACC789012",
  "amountReceived": 450.00,
  "transactionDescription": "1401541457 P4145478"
}
```

**Transaction Description Format**: `<E2E unique id (10 chars)> <reservationId (8 chars)>`

## Configuration

### Application Properties

Key configurations in `application.properties`:

```properties
# Server
server.port=8090

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
kafka.topic.bank-transfer-payment=bank-transfer-payment-update

# External Services
credit.card.payment.service.url=http://localhost:9090/credit-card-payment-api

# Scheduling
reservation.cancellation.cron=0 0 2 * * *
```

## Building and Running

### Prerequisites:
- Java 17+
- Maven 3.6+
- Kafka (for event processing)
- Credit Card Payment API running on port 9090



### Build:

```bash
mvn clean install
```

### Run without docker:

```bash
mvn spring-boot:run
```

### Run with docker:

Dockercompose will take care of building the image and running the container.
run zookeeper and kafka first.
run credit card payment app from another terminal.
run room reservation app.

--docker-compose down

```bash
docker-compose up --build
```

## Access H2 Console (Development Only)

- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:reservationdb`
- Username: `sa`
- Password: (empty)

## Testing

### Test Cash Payment

```bash
curl -X POST http://localhost:8090/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "roomNumber": "101A",
    "startDate": "2025-12-25",
    "endDate": "2025-12-28",
    "roomSegment": "LARGE",
    "modeOfPayment": "CASH"
  }'
```

### Test Bank Transfer Payment

```bash
curl -X POST http://localhost:8090/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Jane Smith",
    "roomNumber": "202B",
    "startDate": "2025-12-30",
    "endDate": "2026-01-05",
    "roomSegment": "EXTRA_LARGE",
    "modeOfPayment": "BANK_TRANSFER"
  }'
```

## Room Pricing

- **SMALL**: $100/day
- **MEDIUM**: $150/day
- **LARGE**: $200/day
- **EXTRA_LARGE**: $300/day


## Notes

- The service uses H2 in-memory database for development. For production, configure a persistent database.
- Kafka must be running for bank transfer payment processing.
- The credit card payment service must be available at the configured URL.
- Reservation IDs are generated in format: P + 7 random characters (e.g., P4145478)


**Features:**
- Mock payment verification endpoint
- Configurable payment references (CONFIRMED/REJECTED)
- Runs on port 9090




