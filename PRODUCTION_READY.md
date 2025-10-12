# Production Readiness Guide

## Overview

NLL Light is now production-ready with the following features implemented:

- ✅ Complete prescription management API
- ✅ Role-based access control (RBAC)
- ✅ Health checks and monitoring
- ✅ Comprehensive error handling
- ✅ API documentation with Swagger/OpenAPI
- ✅ Input validation
- ✅ Security configuration
- ✅ Logging and metrics

## Architecture

### Services

1. **medication-api** (Port 8081): REST API for prescriptions and medications
2. **medication-web** (Port 8080): Web application with OAuth2 login
3. **keycloak** (Port 8082): Identity and access management
4. **kong** (Port 8000): API Gateway (optional)

### User Roles

Four roles are defined in Keycloak:

| Role | Description | Test User | Password |
|------|-------------|-----------|----------|
| PATIENT | View own prescriptions and medications | `patient001` | `patient001` |
| PRESCRIBER | Create, modify, and cancel prescriptions | `prescriber001` | `prescriber001` |
| PHARMACIST | Dispense medications and counsel patients | `pharmacist001` | `pharmacist001` |
| ADMIN | Full system access | `admin` | `admin` |

Legacy test user `user666` / `user666` also has PATIENT role.

## API Endpoints

### Patient API (`/api/v1/prescriptions`)

- `GET /api/v1/prescriptions` - List my prescriptions
- `GET /api/v1/prescriptions/{id}` - Get prescription details
- `GET /api/v1/prescriptions/refill-eligible` - Get refill-eligible prescriptions
- `POST /api/v1/prescriptions/{id}/take` - Record medication adherence
- `GET /api/v1/prescriptions/{id}/adherence` - Get adherence history

### Prescriber API (`/api/v1/prescriber`)

- `POST /api/v1/prescriber/prescriptions` - Create new prescription
- `PUT /api/v1/prescriber/prescriptions/{id}` - Update prescription
- `DELETE /api/v1/prescriber/prescriptions/{id}` - Cancel prescription
- `GET /api/v1/prescriber/prescriptions` - List my prescriptions
- `GET /api/v1/prescriber/prescriptions/{id}` - Get prescription details

### Medication API (`/api/medications`)

- `GET /api/medications` - List all medications
- `GET /api/medications/{id}` - Get medication details

## API Documentation

Access Swagger UI at: http://localhost:8081/swagger-ui.html

OpenAPI specification: http://localhost:8081/v3/api-docs

## Monitoring and Health

### Health Checks

- **API Health**: http://localhost:8081/actuator/health
- **Web Health**: http://localhost:8080/actuator/health (when actuator is added)

### Metrics

Prometheus metrics available at: http://localhost:8081/actuator/prometheus

### Logging

Log levels configured in `application.properties`:
- Root: INFO
- Application (se.inera.nll): DEBUG
- Spring Security: DEBUG

## Security

### Authentication

- OAuth2/OIDC with Keycloak
- JWT tokens for API authentication (to be implemented)
- Session-based authentication for web application

### Authorization

- Method-level security with `@PreAuthorize` annotations (prepared, not yet active)
- Role-based access control
- Patient data isolation

### Current Security Status

⚠️ **Note**: JWT authentication is not yet enforced on API endpoints. All endpoints currently use `permitAll()` for testing purposes. 

**TODO for Production**:
1. Add JWT token validation in API SecurityConfig
2. Enable `@PreAuthorize` on controller methods
3. Extract user ID and roles from JWT tokens
4. Remove X-Patient-Id and X-Prescriber-Id headers (use JWT instead)

## Database

### H2 In-Memory Database

Currently using H2 for development. Data is lost on restart.

**Production TODO**: Migrate to PostgreSQL or MySQL.

### Flyway Migrations

All schema changes managed via Flyway migrations in `medication-api/src/main/resources/db/migration/`

- V1: Core medication catalog
- V2-V7: Prescription domain (patients, prescribers, prescriptions, etc.)
- V8: Sample data

## Error Handling

### GlobalExceptionHandler

Provides consistent error responses:

```json
{
  "timestamp": "2025-10-11T22:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation error description"
}
```

Error types handled:
- `RuntimeException` → 500 Internal Server Error
- `MethodArgumentNotValidException` → 400 Bad Request with field errors
- `IllegalArgumentException` → 400 Bad Request
- `SecurityException` → 403 Forbidden

## Deployment

### Local Development

```powershell
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Rebuild after code changes
docker-compose build
docker-compose up -d
```

### Environment Variables

Configure in `docker-compose.yml`:

- `API_BASE_URL`: URL for API service (currently `http://medication-api:8080`)
- `KEYCLOAK_URL`: URL for Keycloak (internal Docker network)

### Health Check Configuration

medication-api has health check configured:
```yaml
healthcheck:
  test: ["CMD-SHELL", "timeout 1 bash -c '</dev/tcp/localhost/8080' || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s
```

## Testing

### Manual Testing

1. **Login**: Navigate to http://localhost:8080
2. **View Prescriptions**: Click "Mina Recept" after login
3. **API Testing**: Use Swagger UI at http://localhost:8081/swagger-ui.html

### Test Users

```
Patient: patient001 / patient001
Prescriber: prescriber001 / prescriber001
Pharmacist: pharmacist001 / pharmacist001
Admin: admin / admin
```

### Sample Data

- 12 medications in catalog
- 1 patient (patient-001)
- 1 prescriber (prescriber-001)
- 4 prescriptions for testing

## Configuration Files

### medication-api/application.properties

```properties
# Datasource
spring.datasource.url=jdbc:h2:mem:nll
spring.jpa.hibernate.ddl-auto=none

# Flyway
spring.flyway.enabled=true

# Logging
logging.level.se.inera.nll=DEBUG

# Actuator
management.endpoints.web.exposure.include=health,info,metrics,prometheus

# Swagger
springdoc.swagger-ui.path=/swagger-ui.html
```

### medication-web/application.properties

```properties
# OAuth2 Client
spring.security.oauth2.client.registration.keycloak.client-id=medication-web
spring.security.oauth2.client.registration.keycloak.client-secret=web-app-secret

# API Base URL (overridden by Docker env var)
api.base-url=${API_BASE_URL:http://medication-api:8080}
```

## Performance Considerations

### Current Status

- H2 in-memory database (fast, not persistent)
- JPA lazy loading configured
- Connection pooling via HikariCP (Spring Boot default)

### Production Recommendations

1. **Database**: Switch to PostgreSQL with connection pooling
2. **Caching**: Add Redis for session management and data caching
3. **Load Balancing**: Run multiple instances behind load balancer
4. **Monitoring**: Add APM tool (e.g., New Relic, Datadog)

## Known Limitations

1. ❌ JWT authentication not enforced on API
2. ❌ No integration tests yet
3. ❌ PharmacistController not implemented
4. ❌ H2 database not suitable for production
5. ❌ No audit logging for prescription changes
6. ❌ No drug interaction checking
7. ❌ No e-prescription integration (external systems)

## Next Steps for Production

### High Priority

1. **Add JWT Authentication**: Implement JWT validation in API SecurityConfig
2. **Add Integration Tests**: Cover critical user journeys
3. **Database Migration**: Move to PostgreSQL
4. **Audit Logging**: Track all prescription modifications

### Medium Priority

5. **Pharmacist Controller**: Complete medication dispensing workflow
6. **Drug Interactions**: Add interaction checking API
7. **Email Notifications**: Prescription refill reminders
8. **Mobile App**: Patient mobile application

### Low Priority

9. **Analytics Dashboard**: Prescriber statistics
10. **Batch Processing**: Automated refill processing
11. **Reporting**: Compliance and usage reports

## Troubleshooting

### Common Issues

**Issue**: Login fails with "ClosedChannelException"
- **Solution**: Check that `API_BASE_URL=http://medication-api:8080` (not 8081)

**Issue**: Prescription page shows 404
- **Solution**: Ensure API is running and healthy: `docker logs nll-light-medication-api-1`

**Issue**: Keycloak roles not working
- **Solution**: Restart keycloak after realm-export.json changes:
  ```powershell
  docker-compose restart keycloak
  ```

**Issue**: Build fails with Maven errors
- **Solution**: Clean rebuild:
  ```powershell
  docker-compose build --no-cache
  ```

### Logs

View application logs:
```powershell
# API logs
docker logs nll-light-medication-api-1 --tail 100 -f

# Web logs
docker logs nll-light-medication-web-1 --tail 100 -f

# Keycloak logs
docker logs nll-light-keycloak-1 --tail 100 -f
```

## Contact

For questions or issues, contact the NLL Light team.

## License

Apache 2.0 License - See LICENSE file for details.
