# -----------------------------------------------------------------------------------------------------------
FROM postgres:12.7 as transient-db

# -----------------------------------------------------------------------------------------------------------
FROM flyway/flyway:8.5.9-alpine as migrate-db

COPY tools/docker/migrations_entrypoint.sh /bin/migrations_entrypoint.sh
COPY tools/sql /flyway/sql

ENTRYPOINT ["/bin/migrations_entrypoint.sh"]
CMD ["migrate"]
