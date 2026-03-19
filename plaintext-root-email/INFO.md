# plaintext-root-email

## Purpose
Provides email sending and receiving functionality, including IMAP/SMTP configuration, email templates, and email management UI.

## Key Features
- Email sending via SMTP
- Email receiving via IMAP
- Email configuration management
- Email template system
- Email history and tracking
- Email attachments support

## Main Components
- **EmailModuleConfiguration**: Spring configuration for email module
- **EmailsMenu**: Menu for email management
- **EmailConfigMenu**: Configuration UI menu
- Email repositories and services

## Dependencies
### External Dependencies
- Spring Boot Mail
- Jakarta Mail
- PrimeFaces (for UI)

### Internal Module Dependencies
- plaintext-root-common
- plaintext-root-interfaces
- plaintext-root-menu

## Configuration
- SMTP server settings
- IMAP server settings
- Email templates
- Attachment storage configuration
