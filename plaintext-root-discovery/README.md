# Discovery Multi-Instance Navigation

🌐 **PKI-encrypted MQTT-based cross-app navigation for Plaintext applications**

## Features

- **🔄 Automatic App Discovery** - Apps announce themselves via MQTT when users log in
- **🔐 PKI Encryption** - Secure communication using RSA public/private key pairs  
- **🎫 Cross-App Login** - Seamless authentication between app instances with temporary tokens
- **📊 Environment Awareness** - Support for prod, dev, int, test environments
- **📈 Statistics Dashboard** - Monitoring and analytics for ROOT users
- **💓 Heartbeat Monitoring** - Periodic health checks and cleanup

## Architecture

```
┌─────────────────┐    MQTT Topics    ┌─────────────────┐
│   App Instance  │◄─────────────────►│   App Instance  │
│   (Trimstein)   │                   │   (BIT Prod)    │
└─────────────────┘                   └─────────────────┘
         │                                     │
         ▼                                     ▼
    ┌──────────────────────────────────────────────────┐
    │           MQTT Broker (192.168.1.224)           │
    │                                                  │
    │  Topics:                                         │
    │  • plaintext/discovery    - User login announce │
    │  • plaintext/response/*   - App recognition     │
    │  • plaintext/login/*      - Token requests      │
    │  • plaintext/heartbeat    - Health monitoring   │
    └──────────────────────────────────────────────────┘
```

## Protocol Flow

### 1. User Login Announcement
```json
{
  "type": "USER_LOGIN",
  "fromAppId": "trimstein-prod",
  "fromAppName": "Trimstein Production",
  "userEmail": "user@example.com",
  "userId": 123,
  "userName": "John Doe",
  "appUrl": "https://trimstein.plaintext.ch",
  "environment": "prod",
  "publicKey": "-----BEGIN PUBLIC KEY-----..."
}
```

### 2. App Recognition Response
```json
{
  "type": "APP_RESPONSE", 
  "targetAppId": "trimstein-prod",
  "inResponseToMessageId": "uuid-123",
  "userEmail": "user@example.com",
  "userKnown": true,
  "appUrl": "https://bit.plaintext.ch",
  "appDisplayName": "BIT Production",
  "environment": "prod"
}
```

### 3. Cross-App Navigation
```json
{
  "type": "LOGIN_TOKEN_REQUEST",
  "targetAppId": "bit-prod", 
  "userEmail": "user@example.com"
}
```

```json
{
  "type": "LOGIN_TOKEN_RESPONSE",
  "targetAppId": "trimstein-prod",
  "userEmail": "user@example.com", 
  "encryptedToken": "encrypted-with-rsa...",
  "loginUrl": "https://bit.plaintext.ch/discovery/login?token=xyz",
  "tokenValidForSeconds": 300
}
```

## Configuration

### Environment Variables
```bash
PLAINTEXT_ENV=prod                    # Environment: prod, dev, int, test
DISCOVERY_MQTT_BROKER=tcp://192.168.1.224:1883
DISCOVERY_APP_NAME="BIT Production"
```

### application.properties
```properties
# Core Discovery Settings
discovery.enabled=true
discovery.app.id=${spring.application.name}
discovery.app.name=Plaintext App
discovery.app.environment=${PLAINTEXT_ENV:dev}

# MQTT Configuration  
discovery.mqtt.broker=tcp://192.168.1.224:1883
discovery.mqtt.client-id=plaintext-discovery

# Heartbeat & Cleanup
discovery.heartbeat.enabled=true
discovery.heartbeat.interval-ms=120000
discovery.heartbeat.session-timeout-hours=6

# Token Security
discovery.token.validity-seconds=300
discovery.token.encryption-enabled=true
```

## UI Integration

### User Menu Integration
The discovery feature adds remote app links to the user dropdown menu:

```xhtml
<!-- Auto-generated in topbar.xhtml -->
<ui:fragment rendered="#{discoveryTopbarBackingBean.hasRemoteApps()}">
    <ui:repeat value="#{discoveryTopbarBackingBean.remoteApps}" var="remoteApp">
        <li>
            <a href="#" onclick="#{discoveryTopbarBackingBean.navigateToRemoteApp(remoteApp.appUrl)}">
                <i class="#{remoteApp.icon}"></i>
                <span>#{remoteApp.appName}</span>
                <span class="env-badge">#{remoteApp.environment}</span>
            </a>
        </li>
    </ui:repeat>
</ui:fragment>
```

### Environment-Specific Icons
- 🏢 **PROD** - `pi pi-server` (Production systems)
- 🔧 **DEV** - `pi pi-wrench` (Development)  
- ⚙️ **INT** - `pi pi-cog` (Integration)
- ✅ **TEST** - `pi pi-verified` (Testing)

## Database Schema

### discovery_app
Tracks discovered remote application instances:
- `app_id` - Unique app identifier
- `app_name` - Human-readable name
- `app_url` - Base URL for navigation
- `environment` - PROD/DEV/INT/TEST
- `public_key` - RSA public key for encryption
- `last_seen_at` - Last heartbeat timestamp
- `active` - Health status

### discovery_user_session  
Tracks user sessions across app instances:
- `user_email` - Email for cross-app matching
- `user_id` - Local user ID
- `logged_in_at` - Session start time
- `session_active` - Active status
- `login_token` - Temporary cross-app token
- `token_expires_at` - Token expiration

## REST API

### Endpoints
```
GET  /api/discovery/health          - Service health check
GET  /api/discovery/apps            - List active apps
GET  /api/discovery/user/{email}/apps - Remote apps for user
POST /api/discovery/announce-login  - Announce user login
POST /api/discovery/request-token   - Request cross-app token
```

### Example Usage
```bash
# Health check
curl http://localhost:8080/api/discovery/health

# Get remote apps for user  
curl http://localhost:8080/api/discovery/user/user@example.com/apps

# Request login token
curl -X POST http://localhost:8080/api/discovery/request-token \
  -H "Content-Type: application/json" \
  -d '{"targetAppId":"bit-prod","userEmail":"user@example.com"}'
```

## Statistics Dashboard (ROOT Only)

Access: `/discoveryStats.xhtml`

**Features:**
- 📊 Active apps and user counts
- 🌍 Environment distribution  
- 📈 Session activity timeline
- 🔍 Cross-app navigation patterns
- 💼 App health monitoring

**Metrics:**
- Total discovered apps
- Active user sessions (24h)  
- Cross-app navigation events
- Token generation/usage stats
- MQTT message volume

## Security Considerations

### PKI Encryption
- Each app generates RSA 2048-bit key pairs on startup
- Public keys shared via MQTT for encrypted communication
- Login tokens encrypted with recipient's public key
- Keys regenerated on app restart (ephemeral)

### Token Security
- Tokens valid for 5 minutes by default
- Single-use only (marked as consumed)
- Encrypted in transit
- Automatic cleanup of expired tokens

### Network Security
- MQTT traffic on internal network only (192.168.1.224)
- No internet-facing discovery endpoints
- App URLs must be whitelisted/trusted

## Troubleshooting

### Common Issues

**MQTT Connection Failures**
```bash
# Check MQTT broker connectivity
telnet 192.168.1.224 1883
```

**Missing Remote Apps in Menu**
1. Check user has sessions in multiple apps
2. Verify MQTT message flow in logs
3. Confirm environment matching (prod apps only discover other prod)

**Cross-App Login Failures**  
1. Check token generation logs
2. Verify RSA key exchange
3. Confirm token expiration (5min default)

### Logging
```properties
# Enable discovery debug logging
logging.level.ch.plaintext.discovery=DEBUG
```

## Development Setup

1. **Start MQTT Broker**
   ```bash
   docker run -p 1883:1883 eclipse-mosquitto
   ```

2. **Configure Environment**
   ```bash
   export PLAINTEXT_ENV=dev
   export DISCOVERY_APP_NAME="Local Development"
   ```

3. **Test Multi-Instance**
   ```bash
   # Instance 1 (port 8080)
   mvn spring-boot:run -Dserver.port=8080 -Ddiscovery.app.id=dev-1

   # Instance 2 (port 8081)  
   mvn spring-boot:run -Dserver.port=8081 -Ddiscovery.app.id=dev-2
   ```

4. **Verify Discovery**
   - Login to both instances with same user
   - Check user menu for remote app links
   - Test cross-app navigation

---

**🚀 Ready for multi-instance navigation across your Plaintext ecosystem!**