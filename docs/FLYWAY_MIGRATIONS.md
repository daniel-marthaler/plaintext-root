# Creating Flyway Database Migrations

## Overview

Plaintext Root uses [Flyway](https://flywaydb.org/) for database schema management. Migrations are SQL files that are automatically executed on application startup.

## Migration File Location

Each module can have its own migrations in:

```
{module}/src/main/resources/db/migration/
```

All migrations from all modules on the classpath are discovered and executed in order.

## Naming Convention

```
V{timestamp}__{description}.sql
```

- **V**: Prefix (required by Flyway)
- **{timestamp}**: Seconds since January 1, 2000 (ensures unique, ordered names)
- **__**: Double underscore separator (required by Flyway)
- **{description}**: Snake_case description of the change

### Examples

```
V820503544__create_webapp_tables.sql
V1770481749__add_expires_at_and_enlarge_token.sql
V1772661684__create_discovery_tables.sql
```

## Generating a Timestamp

### Option 1: Shell Command

```bash
# Seconds since January 1, 2000 (Unix epoch + offset)
echo $(( $(date +%s) - 946684800 ))
```

### Option 2: Python

```bash
python3 -c "import time; print(int(time.time()) - 946684800)"
```

### Option 3: Use the included script (if available)

```bash
./getflywaynr
```

## Writing Migrations

### SQL Syntax

Write migrations in **standard SQL** compatible with both **H2 (PostgreSQL mode)** and **PostgreSQL**:

```sql
-- Use IF NOT EXISTS for idempotent table creation
CREATE TABLE IF NOT EXISTS my_table (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_date TIMESTAMP,
    last_modified_date TIMESTAMP,
    mandat VARCHAR(255)
);

-- Add columns safely
ALTER TABLE my_table ADD COLUMN IF NOT EXISTS new_column VARCHAR(100);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_my_table_name ON my_table(name);
```

### Supported Data Types

| Type | H2 (PostgreSQL mode) | PostgreSQL |
|------|---------------------|------------|
| `BIGSERIAL` | Auto-increment BIGINT | Native BIGSERIAL |
| `SERIAL` | Auto-increment INT | Native SERIAL |
| `VARCHAR(n)` | Supported | Supported |
| `TEXT` | Supported | Supported |
| `BOOLEAN` | Supported | Supported |
| `TIMESTAMP` | Supported | Supported |
| `BYTEA` | Supported | Supported |
| `INTEGER` | Supported | Supported |
| `BIGINT` | Supported | Supported |

### Base Entity Columns

If your entity extends `SuperModel`, include these standard columns:

```sql
CREATE TABLE IF NOT EXISTS my_entity (
    id BIGSERIAL PRIMARY KEY,

    -- Your fields
    name VARCHAR(255),

    -- SuperModel fields (multi-tenancy + audit)
    mandat VARCHAR(255),
    created_by VARCHAR(255),
    created_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    tags VARCHAR(5000)
);
```

## Best Practices

1. **Never modify existing migrations** — Create new ones instead
2. **Use `IF NOT EXISTS`** — Makes migrations idempotent
3. **One change per migration** — Easier to debug
4. **Test with H2 first** — Faster iteration than PostgreSQL
5. **Include rollback comments** — Document how to undo if needed
6. **Add indexes for foreign keys** — PostgreSQL doesn't auto-index FKs

## Configuration

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true     # Creates baseline for existing DBs
    out-of-order: true            # Allows migrations from different modules
```

## Troubleshooting

### Migration fails on startup

```bash
# Repair Flyway metadata (marks failed migrations as resolved)
# The app does this automatically via FlywayRepairCallback
```

### H2 vs PostgreSQL differences

- H2's PostgreSQL mode covers most syntax, but some edge cases differ
- Test with both databases before releasing
- Use `CREATE ... IF NOT EXISTS` to handle both cleanly
