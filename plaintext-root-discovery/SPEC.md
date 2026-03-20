# Modul: plaintext-z-discovery

> Multi-Instanz-Discovery und Cross-App-Navigation via MQTT mit PKI-Verschlüsselung.

## Business-Kontext

### Zweck
- Ermöglicht mehreren Plaintext-Boot-Instanzen sich gegenseitig zu erkennen und Benutzer nahtlos zwischen Instanzen navigieren zu lassen.

### Akteure
- **Benutzer**: Navigiert zwischen Instanzen (z.B. Dev → Prod)
- **Plaintext-Instanzen**: Kommunizieren untereinander via MQTT
- **ROOT-User**: Sieht Discovery-Statistiken im Admin-Bereich

### Fachliche Regeln
- **BR-01**: Bei User-Login wird eine MQTT-Nachricht an alle Instanzen gesendet (Topic: `plaintext/discovery`)
- **BR-02**: Empfangende Instanzen prüfen ob der User lokal bekannt ist und antworten via `plaintext/response/{appId}`
- **BR-03**: Cross-App-Navigation nutzt temporäre Login-Tokens (UUID, 5 Min gültig, einmalig verwendbar)
- **BR-04**: Tokens werden mit dem RSA-Public-Key der Ziel-Instanz verschlüsselt
- **BR-05**: Heartbeats werden alle 2 Minuten gesendet; Instanzen ohne Heartbeat werden als inaktiv markiert
- **BR-06**: Stale Sessions (6+ Stunden inaktiv) und abgelaufene Tokens werden automatisch bereinigt
- **BR-07**: Das Modul ist per Property `discovery.enabled` aktivierbar (default: true)
- **BR-08**: RSA-Schlüsselpaare sind ephemer — werden bei jedem Start neu generiert

## Architektur

### Abhängigkeiten
```
plaintext-z-discovery --> plaintext-root-common     (Utilities, SuperModel, PlaintextSecurityHolder)
plaintext-z-discovery --> plaintext-root-menu        (MenuAnnotation für Admin-Menü)
plaintext-z-discovery --> plaintext-root-interfaces   (Service-Contracts)
Extern: Eclipse Paho MQTT Client 1.2.5
```

### Schichten
| Schicht      | Klasse/Datei                    | Verantwortung                          |
|--------------|---------------------------------|----------------------------------------|
| View         | `discoveryStats.xhtml`          | Statistik-Dashboard (ROOT only)        |
| Backing Bean | `DiscoveryTopbarBackingBean`    | Remote-Apps in Topbar, Navigation      |
| Backing Bean | `DiscoveryStatsBackingBean`     | Statistik-Daten für Dashboard          |
| REST         | `DiscoveryRestController`       | REST-API (`/api/discovery/*`)          |
| Controller   | `DiscoveryLoginController`      | Cross-App-Login (`/discovery/login`)   |
| Service      | `DiscoveryService`              | Kern-Businesslogik                     |
| Service      | `DiscoveryMqttService`          | MQTT-Client, Publish/Subscribe         |
| Service      | `DiscoveryMessageHandler`       | Eingehende MQTT-Nachrichten verarbeiten|
| Service      | `DiscoveryHeartbeatService`     | Heartbeat + Cleanup (Scheduled)        |
| Service      | `DiscoveryEncryptionService`    | RSA 2048 Schlüsselverwaltung           |
| Repository   | `DiscoveryAppRepository`        | Datenzugriff: Apps                     |
| Repository   | `DiscoveryUserSessionRepository`| Datenzugriff: User-Sessions            |
| Entity       | `DiscoveryApp`                  | Entdeckte Instanzen                    |
| Entity       | `DiscoveryUserSession`          | User-Sessions über Instanzen           |
| Config       | `DiscoveryAutoConfiguration`    | Spring Auto-Config                     |
| Config       | `DiscoveryProperties`           | Properties-Binding                     |
| Menu         | `DiscoveryStatsMenu`            | Admin-Menüeintrag                      |

### Datenmodell

**discovery_app**
| Spalte           | Typ            | Constraint       | Bemerkung                    |
|------------------|----------------|------------------|------------------------------|
| `id`             | `BIGSERIAL`    | PK               |                              |
| `app_id`         | `VARCHAR(100)` | NOT NULL, UNIQUE | z.B. "trimstein-prod"        |
| `app_name`       | `VARCHAR(200)` | NOT NULL         | Anzeigename                  |
| `app_url`        | `VARCHAR(500)` | NOT NULL         | Basis-URL                    |
| `environment`    | `VARCHAR(20)`  | NOT NULL         | Enum: PROD, DEV, INT, TEST   |
| `public_key`     | `TEXT`         |                  | RSA Public Key (Base64)      |
| `last_seen_at`   | `TIMESTAMP`    | NOT NULL         | Letzter Heartbeat            |
| `version`        | `VARCHAR(50)`  |                  | App-Version                  |
| `active`         | `BOOLEAN`      | NOT NULL, def TRUE|                             |
| + SuperModel-Felder (audit, mandat, deleted, tags)                              |

**discovery_user_session**
| Spalte            | Typ            | Constraint       | Bemerkung                   |
|-------------------|----------------|------------------|-----------------------------|
| `id`              | `BIGSERIAL`    | PK               |                             |
| `app_id`          | `BIGINT`       | FK → discovery_app| ON DELETE CASCADE           |
| `user_email`      | `VARCHAR(255)` | NOT NULL         | Cross-App Matching          |
| `user_id`         | `BIGINT`       | NOT NULL         | Lokale User-ID              |
| `user_name`       | `VARCHAR(200)` |                  | Anzeigename                 |
| `logged_in_at`    | `TIMESTAMP`    | NOT NULL         | Session-Start               |
| `last_activity_at`| `TIMESTAMP`    | NOT NULL         | Letzte Aktivität            |
| `session_active`  | `BOOLEAN`      | NOT NULL, def TRUE|                            |
| `login_token`     | `VARCHAR(500)` |                  | Temporärer Token            |
| `token_expires_at`| `TIMESTAMP`    |                  | Token-Ablauf                |
| `token_used`      | `BOOLEAN`      | NOT NULL, def FALSE| Einmal-Verwendung          |
| + SuperModel-Felder (audit, mandat, deleted, tags)                              |

## MQTT-Protokoll

### Topics
| Topic                          | Nachricht              | Richtung   |
|--------------------------------|------------------------|------------|
| `plaintext/discovery`          | UserLoginMessage       | Broadcast  |
| `plaintext/response/{appId}`   | AppResponseMessage     | Unicast    |
| `plaintext/login/{appId}`      | LoginToken-Req/Resp    | Unicast    |
| `plaintext/heartbeat`          | HeartbeatMessage       | Broadcast  |

### Nachrichtentypen
- **UserLoginMessage**: userEmail, userId, userName, appUrl, publicKey
- **AppResponseMessage**: targetAppId, userKnown, appUrl
- **LoginTokenRequestMessage**: targetAppId, userEmail, returnUrl
- **LoginTokenResponseMessage**: encryptedToken, loginUrl, tokenValidForSeconds
- **HeartbeatMessage**: appUrl, appVersion, activeUserCount, publicKey

## API / Schnittstellen

### REST-Endpunkte
| Methode | Pfad                            | Auth | Request                  | Response                    |
|---------|---------------------------------|------|--------------------------|-----------------------------|
| POST    | `/api/discovery/announce-login` | -    | UserLoginMessage         | `{status, message}`         |
| POST    | `/api/discovery/request-token`  | -    | LoginTokenRequestMessage | `{status, token, loginUrl}` |
| GET     | `/api/discovery/apps`           | -    | -                        | `List<DiscoveryApp>`        |
| GET     | `/api/discovery/user/{email}/apps`| -  | -                        | `List<DiscoveryUserSession>`|
| GET     | `/api/discovery/health`         | -    | -                        | `{status, activeApps, ...}` |
| GET     | `/discovery/login?token=X`      | -    | Query-Param              | Redirect                    |

### UI-Aktionen
| Aktion              | Methode                                        | Ergebnis                    |
|---------------------|-------------------------------------------------|-----------------------------|
| Remote-Apps laden   | `DiscoveryTopbarBackingBean.loadRemoteApps()`   | Liste in Topbar             |
| Zu App navigieren   | `DiscoveryTopbarBackingBean.navigateToRemoteApp()`| Token-Request + Redirect  |
| Statistiken laden   | `DiscoveryStatsBackingBean.loadStatistics()`    | Dashboard-Daten             |

## Konfiguration

| Property                              | Default                    | Bemerkung              |
|---------------------------------------|----------------------------|------------------------|
| `discovery.enabled`                   | `true`                     | Modul an/aus           |
| `discovery.mqtt.broker`               | `tcp://localhost:1883` | MQTT-Broker            |
| `discovery.mqtt.clientId`             | `plaintext-discovery`      | Client-ID Prefix       |
| `discovery.app.id`                    | `${spring.application.name}`| Instanz-ID            |
| `discovery.app.name`                  | `Plaintext App`            | Anzeigename            |
| `discovery.app.environment`           | `dev`                      | prod/dev/int/test      |
| `discovery.heartbeat.enabled`         | `true`                     | Heartbeat an/aus       |
| `discovery.heartbeat.intervalMs`      | `120000`                   | 2 Minuten              |
| `discovery.heartbeat.cleanupIntervalMs`| `600000`                  | 10 Minuten             |
| `discovery.heartbeat.sessionTimeoutHours`| `6`                     | Session-Timeout        |
| `discovery.token.validitySeconds`     | `300`                      | Token-Gültigkeit       |
| `discovery.token.encryptionEnabled`   | `true`                     | PKI-Verschlüsselung   |

## Sicherheit

- **Zugriff Dashboard**: ROOT-Rolle (via `@MenuAnnotation(roles = {"ROOT"})`)
- **CSRF**: `<input type="hidden" name="_csrf" value="#{_csrf.token}"/>` in discoveryStats.xhtml
- **Verschlüsselung**: RSA 2048-bit für Token-Austausch
- **Tokens**: UUID, 5 Min TTL, Einmal-Verwendung, DB-gespeichert
- **MQTT**: Internes Netzwerk (localhost), kein Internet-Exposure
- **Schlüssel**: Ephemer, bei jedem Start neu generiert

## Offene Punkte / TODOs

- [ ] REST-Endpunkte haben keine Auth-Prüfung
- [ ] TopbarBackingBean: Erster Klick auf Remote-App redirected ohne Token (MQTT async), erst zweiter Klick nutzt cached URL

## Änderungslog

| Datum | Änderung |
|-------|----------|
| 2026-03-06 | SPEC erstellt basierend auf bestehendem Code |
| 2026-03-06 | Alle 8 TODOs implementiert: isUserKnownLocally (via UserDetailsService), generateLoginToken (persistiert in DB), storeRemoteLoginUrl (ConcurrentHashMap), Token-PKI-Verschlüsselung, DiscoveryLoginListener re-enabled, PlaintextLoginEvent publishing in AuthSuccessHandler, Auto-Login in DiscoveryLoginController, TopbarBean cached URL navigation. Flyway-Migration: user_id nullable. Security: /discovery/login permitAll + CSRF ignore. Modul im Parent-POM und webapp aktiviert. |
