CREATE SEQUENCE IF NOT EXISTS public.customers_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS public.customer
(
    id BIGINT NOT NULL
    DEFAULT nextval('public.customers_id_seq'::regclass),

    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,

    role VARCHAR(50) NOT NULL DEFAULT 'USER',

    CONSTRAINT customers_pkey
    PRIMARY KEY (id),

    CONSTRAINT customers_email_key
    UNIQUE (email)
    );

ALTER SEQUENCE public.customers_id_seq
    OWNED BY public.customer.id;