CREATE TABLE IF NOT EXISTS public.order_saga_projections
(
    order_id BIGINT NOT NULL,
    inventory_status VARCHAR(30) NOT NULL,
    inventory_failure_code VARCHAR(100),
    inventory_failure_message VARCHAR(1000),
    payment_exists BOOLEAN NOT NULL DEFAULT FALSE,
    payment_id BIGINT,
    payment_amount NUMERIC(19, 2),
    payment_status VARCHAR(30) NOT NULL,
    payment_failure_code VARCHAR(100),
    payment_failure_reason VARCHAR(500),
    saga_status VARCHAR(30) NOT NULL,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT order_saga_projections_pkey PRIMARY KEY (order_id),
    CONSTRAINT fk_order_saga_projection_order FOREIGN KEY (order_id)
        REFERENCES public.orders (id) ON DELETE CASCADE,
    CONSTRAINT chk_order_saga_inventory_status CHECK (
        inventory_status IN ('PENDING','RESERVED','CONFIRMED','RELEASED','FAILED')),
    CONSTRAINT chk_order_saga_payment_status CHECK (
        payment_status IN ('NOT_CREATED','SUCCEEDED','FAILED')),
    CONSTRAINT chk_order_saga_status CHECK (
        saga_status IN ('PROCESSING','COMPLETED'))
);

CREATE TABLE IF NOT EXISTS public.order_saga_reservations
(
    id BIGSERIAL NOT NULL,
    order_id BIGINT NOT NULL,
    reservation_id BIGINT,
    product_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    reservation_version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT order_saga_reservations_pkey PRIMARY KEY (id),
    CONSTRAINT fk_order_saga_reservation_order FOREIGN KEY (order_id)
        REFERENCES public.orders (id) ON DELETE CASCADE,
    CONSTRAINT uk_order_saga_reservation_order_product
        UNIQUE (order_id, product_id),
    CONSTRAINT chk_order_saga_reservation_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_saga_reservation_status CHECK (
        status IN ('RESERVED','CONFIRMED','RELEASED'))
);

CREATE INDEX IF NOT EXISTS idx_order_saga_reservations_order
    ON public.order_saga_reservations (order_id);

CREATE TABLE IF NOT EXISTS public.order_saga_processed_events
(
    event_id UUID NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    order_id BIGINT NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT order_saga_processed_events_pkey PRIMARY KEY (event_id),
    CONSTRAINT fk_order_saga_processed_event_order FOREIGN KEY (order_id)
        REFERENCES public.orders (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_saga_processed_events_order
    ON public.order_saga_processed_events (order_id);


DO $migration$
DECLARE
    application_owner NAME;
BEGIN
    SELECT tableowner
    INTO application_owner
    FROM pg_tables
    WHERE schemaname = 'public'
      AND tablename = 'orders';

    EXECUTE format(
        'ALTER TABLE public.order_saga_projections OWNER TO %I',
        application_owner);
    EXECUTE format(
        'ALTER TABLE public.order_saga_reservations OWNER TO %I',
        application_owner);
    EXECUTE format(
        'ALTER TABLE public.order_saga_processed_events OWNER TO %I',
        application_owner);
    EXECUTE format(
        'ALTER SEQUENCE public.order_saga_reservations_id_seq OWNER TO %I',
        application_owner);
END
$migration$;
