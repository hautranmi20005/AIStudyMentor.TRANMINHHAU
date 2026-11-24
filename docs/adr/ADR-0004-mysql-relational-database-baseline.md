# ADR-0004: MySQL as the Primary Relational Database Baseline

- **Status**: Approved
- **Deciders**: Product Owner and Backend Architecture Team
- **Date**: 2026-11-10

## 1. Context & Problem Statement

Development, tests, and runtime configuration need a single relational database baseline so migrations and behavior are reproducible. Multiple database engines increase compatibility work and create uncertainty about which schema is authoritative.

## 2. Decision Drivers & Alternatives Considered

- Use MySQL 8 as the primary relational database for local development and the application baseline.
- Keep Flyway as the schema migration mechanism.
- Align environments to the same database engine.
- Alternatives considered: PostgreSQL for production with MySQL for development, or supporting both databases as equal targets.

## 3. Decision Outcome & Justification

MySQL is the approved relational database baseline. PostgreSQL-specific runtime configuration, Compose services, drivers, and migration artifacts are deprecated for this application baseline and should be archived rather than treated as a supported parallel target.

The current codebase is only partially aligned: the development stack and core migrations use MySQL, while `application-prod.yml`, `docker-compose.prod.yml`, `pom.xml`, and PostgreSQL migration artifacts still configure or include PostgreSQL. This ADR records the approved target; it does not claim that those artifacts have already been removed or archived.

## 4. Consequences

- **Positive**: One database engine simplifies local reproduction, integration testing, and migration validation.
- **Negative**: PostgreSQL-specific deployment configuration cannot remain an active supported path. MySQL compatibility must be verified for every migration and query.
- Archived PostgreSQL artifacts may be retained for historical reference but must not be presented as the current production baseline.

## 5. References & Code Artifacts

- [Development Compose stack](../../docker-compose.dev.yml)
- [Core MySQL migrations](../../backend/src/main/resources/db/migration)
- [Production application configuration](../../backend/src/main/resources/application-prod.yml)
- [Production Compose stack](../../backend/docker-compose.prod.yml)
- [Backend Maven dependencies](../../backend/pom.xml)
- [Database design](../../Architecture_and_Engineering/DATABASE_DESIGN.md)
