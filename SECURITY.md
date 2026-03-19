# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.19.x  | :white_check_mark: |
| < 1.19  | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability, please report it responsibly:

1. **Do NOT** open a public GitHub issue
2. Send an email to the maintainers with:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
3. Allow reasonable time for a fix before public disclosure

## Security Features

Plaintext Root includes several built-in security features:

- **Spring Security** integration with CSRF protection
- **Role-based access control** (ROLE_USER, ROLE_ADMIN, ROLE_ROOT)
- **Multi-tenancy isolation** (mandate-based data separation)
- **Session tracking** and audit logging
- **API token authentication** for REST endpoints
- **Secure cookie handling** for theme preferences
- **Page access guards** for menu-based navigation security
