#!/usr/bin/env bash

set -Eeuo pipefail

echo "Creating ecommerce databases..."

psql \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set ON_ERROR_STOP=1 <<'EOSQL'

CREATE DATABASE customer_db;
CREATE DATABASE order_db;
CREATE DATABASE inventory_db;
CREATE DATABASE payment_db;

EOSQL

echo "Creating customer-service schema..."

psql \
  --username "$POSTGRES_USER" \
  --dbname customer_db \
  --set ON_ERROR_STOP=1 \
  --file /docker-entrypoint-initdb.d/schemas/customer-schema.sql

echo "Creating order-service schema..."

psql \
  --username "$POSTGRES_USER" \
  --dbname order_db \
  --set ON_ERROR_STOP=1 \
  --file /docker-entrypoint-initdb.d/schemas/order-schema.sql

echo "Creating inventory-service schema..."

psql \
  --username "$POSTGRES_USER" \
  --dbname inventory_db \
  --set ON_ERROR_STOP=1 \
  --file /docker-entrypoint-initdb.d/schemas/inventory-schema.sql

echo "Creating payment-service schema..."

psql \
  --username "$POSTGRES_USER" \
  --dbname payment_db \
  --set ON_ERROR_STOP=1 \
  --file /docker-entrypoint-initdb.d/schemas/payment-schema.sql

echo "All ecommerce databases and schemas were created."