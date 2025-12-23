# Kafka Local Setup Guide (Windows)

If this project is a plain Spring Boot application without Docker, you need to run Kafka locally on your machine.

## 1. Download and Extract Kafka
1. Download the latest Kafka binary from [kafka.apache.org](https://kafka.apache.org/downloads).
2. Extract the `.tgz` file to a folder (e.g., `C:\kafka`).

## 2. Start Zookeeper
Open a terminal and run:
```powershell
cd C:\kafka
.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties
```

## 3. Start Kafka Broker
Open a **new** terminal and run:
```powershell
cd C:\kafka
.\bin\windows\kafka-server-start.bat .\config\server.properties
```

## 4. Create the Required Topic
Open a **third** terminal and run:
```powershell
cd C:\kafka
.\bin\windows\kafka-topics.bat --create --topic bank-transfer-payment-update --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

## 5. Verify the Topic
```powershell
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
```

## 6. (Optional) Test the Topic with Producer
If you want to manually send a payment event to test the application:
```powershell
.\bin\windows\kafka-console-producer.bat --topic bank-transfer-payment-update --bootstrap-server localhost:9092
```
Then paste a JSON event like this (replace `RESERVATION_ID` with an actual ID from your logs):
```json
{"paymentId":"PAY123","debtorAccountNumber":"ACC789","amountReceived":500.0,"transactionDescription":"Payment RESERVATION_ID"}
```

---

**Note:** The application is configured in `application.properties` to connect to `localhost:9092`. ensure Kafka is running on this port before starting the Spring Boot application.
