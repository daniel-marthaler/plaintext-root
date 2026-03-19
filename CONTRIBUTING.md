# Contributing to Plaintext Root

Thank you for your interest in contributing to Plaintext Root! This document provides guidelines for contributing to this project.

## How to Contribute

### Reporting Bugs

- Use [GitHub Issues](https://github.com/daniel-marthaler/plaintext-root/issues) to report bugs
- Include steps to reproduce, expected behavior, and actual behavior
- Add screenshots if applicable

### Suggesting Features

- Open a [Discussion](https://github.com/daniel-marthaler/plaintext-root/discussions) to propose new features
- Describe the use case and why it would be valuable

### Pull Requests

1. Fork the repository
2. Create a feature branch from `master`: `git checkout -b feature/my-feature`
3. Make your changes
4. Ensure the project builds: `mvn clean install`
5. Commit with a descriptive message
6. Push to your fork and open a Pull Request

### Branch Protection

- Direct pushes to `master` are restricted to maintainers
- All contributions must go through Pull Requests
- Contributors can create branches and open PRs freely

## Development Setup

### Prerequisites

- Java 25+
- Maven 3.9+
- PostgreSQL 18+ (via Docker/Podman)

### Quick Start

```bash
# Clone the repository
git clone https://github.com/daniel-marthaler/plaintext-root.git
cd plaintext-root

# Start the database
docker compose up -d

# Build the project
mvn clean install -DskipTests

# Run the application
mvn spring-boot:run -pl plaintext-root-webapp
```

## Code Style

- Follow existing code conventions
- Use meaningful variable and method names
- Add MPL 2.0 license header to new Java files:

```java
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
```

## License

By contributing, you agree that your contributions will be licensed under the MPL 2.0 license.
