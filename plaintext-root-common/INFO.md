# plaintext-root-common

## Purpose
Common library providing foundational components, utilities, and interfaces used across all Plaintext modules. This is the core dependency for most other modules in the system.

## Key Features
- Authentication and security wrappers (EmadAuthentication, PlaintextSecurityHolder)
- Repository base classes and JPA converters (EmadRepository, XstreamStore2)
- Mail handling infrastructure (EmadMailReceiver, EmadMailModel)
- XStream-based object persistence (Xstream2Storable, XstreamStorable)
- Common utilities for time, text processing, and data conversion
- JPA converters for complex types (StringArrayJPAConverter, XstreamBaseJPAConverter)

## Main Components
- **Security**: PlaintextSecurityHolder, EmadAuthentication, EmadSecWrapper
- **Repositories**: EmadRepository, RepoMaster, TextRepository2
- **Mail**: EmadMailReceiver, EmadMailReceiverPrototype, EmadMailModel, EmadEmailAttachment
- **Persistence**: XstreamStore2, Xstream2Storable, XstreamStorable
- **Utilities**: TimeUtil, Text2, SuperModel
- **JPA Converters**: XstreamBaseJPAConverter, StringArrayJPAConverter

## Dependencies
### External Dependencies
- Spring Boot (starter, web, data-jpa, mail, security)
- Jakarta Faces, PrimeFaces
- Apache Commons (lang3, io, beanutils)
- XStream
- Lombok
- Joda Time
- Cron4j
- JasperReports
- Apache POI
- JSoup
- Guava
- Prettytime
- Java Object Diff

### Internal Module Dependencies
- plaintext-root-interfaces

## Configuration
No specific configuration required - provides base classes and utilities consumed by other modules.
