---
layout: default
title: Plaintext Root Framework
---

# Plaintext Root Framework

An open-source Jakarta Faces application framework for rapidly building multi-tenant web applications.

## What is Plaintext Root?

Plaintext Root provides a complete foundation for building enterprise web applications with:

- **Multi-Tenancy** — Built-in mandate system for tenant data isolation
- **Security** — Spring Security with role-based access control
- **Admin Panels** — Pre-built modules for settings, sessions, cron jobs, emails
- **Menu System** — Annotation-driven, role-aware navigation
- **Template System** — Swappable UI templates with dark/light mode
- **Service Discovery** — MQTT-based discovery between applications

## Quick Start

```bash
git clone https://github.com/daniel-marthaler/plaintext-root.git
cd plaintext-root
docker compose up -d
mvn clean install -DskipTests
mvn spring-boot:run -pl plaintext-root-webapp
```

Open [http://localhost:8080](http://localhost:8080) in your browser.

## Documentation

- [Architecture Overview](ARCHITECTURE.md)
- [Contributing Guide](../CONTRIBUTING.md)
- [Security Policy](../SECURITY.md)

## Tech Stack

| Technology | Version |
|-----------|---------|
| Java | 25 |
| Spring Boot | 3.5 |
| Jakarta Faces | 4.1 |
| PrimeFaces | 15.0 |
| PostgreSQL | 18+ |

## License

Licensed under the [Mozilla Public License 2.0](https://opensource.org/licenses/MPL-2.0).
