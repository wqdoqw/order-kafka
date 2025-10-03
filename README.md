@ -0,0 +1,168 @@
# 주문 이벤트 (Kafka) — 재시도 & DLQ 예제

이 프로젝트는 Spring Boot와 Kafka를 사용하여 이벤트 발행, 소비, 실패 시 재시도(Retry), DLQ(Dead Letter Queue) 처리 흐름을 구현한 샘플입니다.

Kafka 기반 이벤트 처리에서 자주 발생하는 문제(실패, 재시도, 무한 루프)를 실무적인 방식으로 어떻게 설계하고 해결하는지를 보여주는 것이 목적입니다.

## 기술 스택

- Java 17
- Spring Boot 3.2.x
- Spring for Apache Kafka
- Spring Data JPA + H2 (인메모리 데이터베이스)
- Docker / Docker Compose (Kafka 로컬 실행)

## 아키텍처 개요

애플리케이션은 Kafka를 통해 주문 이벤트를 비동기적으로 처리합니다:

```
POST /orders
    ↓
OrderController
    ↓
OrderEventProducer
    ↓
Kafka (order.created.v1)
    ↓
OrderConsumer
    ├─ 정상 처리 → 종료
    └─ 예외 발생
          ↓
        재시도
          ↓
Kafka (order.created.v1.dlq)
          ↓
DlqConsumer (로그 출력 후 종료)
```

## Kafka 토픽 구성

- **order.created.v1**: 주문 생성 이벤트를 처리하는 메인 토픽
- **order.created.v1.dlq**: 재시도 실패 메시지가 이동하는 DLQ 토픽

## 실행 환경

### 필수 조건

- JDK 17
- Docker Desktop
- Gradle (Wrapper 포함)

### 애플리케이션 실행

1. **Kafka 실행**

   프로젝트 루트 디렉터리에서 다음 명령어를 실행합니다:
   ```
   docker compose up -d
   ```

   Kafka 브로커는 다음 주소에서 실행됩니다:
   - localhost:9092

2. **애플리케이션 실행**

   ```
   ./gradlew bootRun
   ```

   정상 실행 시 다음 로그를 확인할 수 있습니다:
   ```
   Tomcat started on port(s): 8080
   Started OrderApplication
   ```

## 설정 정보

`application.yml` 예시:

```yaml
server:
  port: 8080

spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: order-consumer-group
      auto-offset-reset: latest

  datasource:
    url: jdbc:h2:mem:kafkadb;MODE=MYSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        format_sql: true

  h2:
    console:
      enabled: true
      path: /h2-console

app:
  kafka:
    topics:
      order-created: order.created.v1
      order-created-dlq: order.created.v1.dlq
```

## API 명세

### 주문 생성 이벤트 발행

- **Method**: POST
- **URL**: /orders
- **Content-Type**: application/json

**요청 바디 예시**:
```json
{
  "orderId": "ORD-1",
  "amount": 250
}
```

**PowerShell 호출 예시 (Windows)**:
```powershell
Invoke-RestMethod -Method Post "http://localhost:8080/orders" `
  -ContentType "application/json" `
  -Body '{"orderId":"ORD-1","amount":250}'
```

## 정상 처리 / 실패 처리 흐름

### 정상 처리

아래와 같은 요청은 Consumer에서 정상 처리됩니다:
```json
{
  "orderId": "ORD-1",
  "amount": 100
}
```

- Consumer 정상 처리
- Retry 및 DLQ 미사용

### 실패 → 재시도 → DLQ 처리

아래 요청은 의도적으로 실패를 발생시킵니다:
```json
{
  "orderId": "ORD-1", 
  "amount": 999
}
```

처리 흐름:
1. OrderConsumer에서 비즈니스 예외 발생
2. 설정된 재시도 정책에 따라 재시도
3. 재시도 실패 시 DLQ 토픽으로 메시지 이동
4. DlqConsumer에서 메시지를 수신하고 로그 출력 후 종료
