# plaintext-root-flyway

## Purpose
Manages database schema migrations using Flyway for version-controlled database evolution across all Plaintext modules.

## Key Features
- Flyway migration configuration
- Database versioning
- Migration script validation
- Cross-module migration coordination
- HSQLDB-specific syntax support

## Main Components
- Flyway configuration
- Migration script repository
- Version control integration

## Dependencies
### External Dependencies
- Flyway Core
- Spring Boot
- HSQLDB

### Internal Module Dependencies
- plaintext-root-jpa

## Configuration
- Migration scripts in `db/migration`
- Flyway properties in application.properties
- Use `./getflywaynr` script to generate patch numbers (calculates seconds since 2000, checks for conflicts)
- Use HSQLDB syntax for migration scripts
