#!/bin/sh
# Roda apenas na primeira inicializacao do volume do Postgres (deploys do zero).
# No volume ja existente, o database "umami" e criado manualmente uma vez.
set -e
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "CREATE DATABASE umami OWNER \"$POSTGRES_USER\";"
