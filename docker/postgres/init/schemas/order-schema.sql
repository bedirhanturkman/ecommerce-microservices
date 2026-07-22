CREATE TABLE IF NOT EXISTS public.orders
(
    id BIGSERIAL NOT NULL,
    customer_id BIGINT NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT orders_pkey
    PRIMARY KEY (id),

    CONSTRAINT chk_orders_status
    CHECK (
              status IN (
              'CREATED',
              'PAID',
              'PAYMENT_FAILED',
              'INVENTORY_FAILED'
                        )
    )
    );

CREATE TABLE IF NOT EXISTS public.order_items
(
    id BIGSERIAL NOT NULL,
    order_id BIGINT NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,

    CONSTRAINT order_items_pkey
    PRIMARY KEY (id),

    CONSTRAINT fk_order_items_order
    FOREIGN KEY (order_id)
    REFERENCES public.orders (id)
    ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS public.order_outbox_events
(
    id UUID NOT NULL,

    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,

    event_type VARCHAR(150) NOT NULL,
    topic VARCHAR(150) NOT NULL,
    message_key VARCHAR(150) NOT NULL,

    payload TEXT NOT NULL,

    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,

    next_attempt_at TIMESTAMP WITH TIME ZONE
    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_at TIMESTAMP WITH TIME ZONE
    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    published_at TIMESTAMP WITH TIME ZONE,

                               last_error VARCHAR(1000),

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT order_outbox_events_pkey
    PRIMARY KEY (id),

    CONSTRAINT uk_order_outbox_aggregate_event
    UNIQUE (
               aggregate_type,
               aggregate_id,
               event_type
           ),

    CONSTRAINT chk_order_outbox_retry_count
    CHECK (retry_count >= 0),

    CONSTRAINT chk_order_outbox_status
    CHECK (status IN ('PENDING', 'PUBLISHED'))
    );

CREATE INDEX IF NOT EXISTS idx_order_outbox_pending
    ON public.order_outbox_events (
    status,
    next_attempt_at,
    created_at
    );