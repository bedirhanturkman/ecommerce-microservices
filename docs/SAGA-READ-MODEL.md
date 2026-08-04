# Order Saga read model

`GET /api/v1/orders/{orderId}/saga` exposes the choreography Saga result
without exposing Payment or Inventory reservation HTTP APIs and without
cross-service database access.

## Model and flow

Inventory and Payment outbox events feed relational read tables in `order_db`:

- `order_saga_projections`: one row per order.
- `order_saga_reservations`: one row per `(order_id, product_id)`.
- `order_saga_processed_events`: one row per real Kafka `outbox-event-id`.

The projection starts in the Order creation transaction. The endpoint never
blocks; clients poll while `sagaStatus=PROCESSING`. `COMPLETED` means one of:

- `PAID + CONFIRMED + SUCCEEDED`
- `INVENTORY_FAILED + FAILED + NOT_CREATED`
- `PAYMENT_FAILED + RELEASED + FAILED`

Business failures are completed Saga outcomes, not technical failures.

## Idempotency and ordering

The real outbox UUID Kafka header has a unique primary key, so redelivery is a
no-op. Reservation rows also have a unique order/product constraint. Terminal
Inventory and Payment states do not regress on late reserved events. Completion
is recalculated centrally from all observed states rather than arrival order.

## Security

JWT is mandatory. USER can read only an Order matching the JWT `customerId`;
ADMIN can read all Orders; SELLER is denied. Missing and non-owned Orders both
return 404. The existing Gateway `/api/v1/orders/**` route covers the endpoint.

## Eventual consistency and examples

Immediately after creation the endpoint can show `CREATED`, Inventory
`PENDING`, Payment `exists=false/status=NOT_CREATED`, and `PROCESSING`.
Terminal responses expose reservation metadata, Inventory failure code/message,
and Payment failure code/reason when those values exist in real events.

Existing Orders without a projection return `PROCESSING` with
`projectionAvailable=false`; historical data is not invented. Reservation
ID/version originate from Inventory entities. Payment events do not carry an
entity version, so the API does not expose a fictitious Payment version.

## Postman

Import `postman/ecommerce-saga-demo.postman_collection.json`. Set `baseUrl` and
role token variables. Prepare Product/Inventory fixtures with ADMIN or SELLER,
create Orders with USER, then poll the Saga request until `COMPLETED`. Never
store real passwords, JWTs, or secrets in the collection.
