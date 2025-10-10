# nll-light

Java multi-module project with:
- **medication-api**: Spring Boot REST API for medications (H2 in-memory DB)
- **medication-web**: Spring Boot web app consuming the API
- **kong**: API Gateway routing requests between web and API

## Architecture

```
┌─────────────────┐    ┌─────────────┐    ┌─────────────────┐
│   Web Browser   │────│     Kong    │────│  Medication API │
│                 │    │  Gateway    │    │                 │
│ http://localhost│    │ :8000       │    │ :8080           │
└─────────────────┘    └─────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────┐
                       │   H2 Database│
                       │ (In-memory)  │
                       └─────────────┘
```

## Features

### API Endpoints
- `GET /api/medications` - List all medications
- `GET /api/medications/{id}` - Get medication by ID
- `GET /api/medications/search?name={query}` - Search medications by name

### Pre-seeded Data
- **Alimemazin**: Antihistamin. Exempelindikation: allergiska besvär.
- **Elvanse**: CNS-stimulerande läkemedel. Exempelindikation: ADHD.
- **Melatonin**: Hormonpreparat. Exempelindikation: sömnstörningar.

## Quick Start

### Docker (Recommended)
```bash
# Build and start all services
docker compose up --build

# Access points:
# - Web App: http://localhost:8080
# - API Gateway: http://localhost:8000
# - API Direct: http://localhost:8081
# - Kong Admin: http://localhost:8001
# - Swagger UI: http://localhost:8000/swagger-ui.html
# - API Docs: http://localhost:8000/v3/api-docs
```

### Manual (Maven)
```bash
# Start API
mvn -pl medication-api spring-boot:run

# Start Web App (in another terminal)
mvn -pl medication-web spring-boot:run
```

## API Documentation

### Swagger UI
- **Direct API**: http://localhost:8081/swagger-ui/index.html
- **Via Kong**: http://localhost:8000/swagger-ui/index.html

### Kong Gateway Routes
- `/api/medications*` → `medication-api:8080`

## Testing

The application includes comprehensive tests with sequence diagrams for all test scenarios. Tests are automatically run during the Docker build process to ensure code quality.

### Test Coverage

- **MedicationControllerTest**: Tests for medication API endpoints
  - `list_shouldReturnSeededMedications`: Verifies listing all medications
  - `search_shouldFindMelatoninByPartial`: Tests partial name search functionality
  - `get_shouldReturnSingleMedication`: Tests retrieving individual medication by ID

### Test Documentation

Detailed test documentation with sequence diagrams is available in `docs/TEST-DOCUMENTATION.md`. The documentation includes:
- Test case descriptions
- Expected behavior
- PlantUML sequence diagrams for each test
- API endpoint specifications

### Running Tests

Tests are executed automatically during the Docker build process. To run tests manually:

```bash
# Run tests during build
docker-compose build medication-api

# Or run the full application stack
docker-compose up
```

All tests must pass for the application to build successfully.

## Manual Test Execution
When the application is running, you can manually execute the test scenarios using the complete URLs:

1. **List all medications**: http://localhost:8000/api/medications
2. **Search medications**: http://localhost:8000/api/medications/search?name=mel
3. **Get specific medication**: http://localhost:8000/api/medications/1

Use tools like curl, Postman, or your browser to test these endpoints.

### Swagger UI Testing
The API can also be tested interactively using Swagger UI:
- **URL**: http://localhost:8000/swagger-ui.html
- **Features**: Try out endpoints, view request/response examples, explore API schema

## Configuration

### Environment Variables
- `API_BASE_URL`: Web app API endpoint (default: `http://kong:8000`)
- `SPRING_PROFILES_ACTIVE`: Spring profile (default: `docker`)

### Kong Configuration
- **Declarative Config**: `kong.yml`
- **Routes**: 
  - `/api/medications` → medication API service
  - `/v3/api-docs` → OpenAPI specification
  - `/swagger-ui.html` → Swagger UI
  - `/swagger-ui/**` → Swagger UI resources
- **Database**: DB-less mode (configuration only)

## Development

### Project Structure
```
nll-light/
├── medication-api/          # REST API module
│   ├── src/main/java/       # Application code
│   ├── src/main/resources/  # Config & data
│   └── src/test/java/       # Integration tests
├── medication-web/          # Web application module
│   └── src/main/java/       # Thymeleaf templates & controllers
├── docs/                    # Documentation & diagrams
│   ├── TEST-DOCUMENTATION.md
│   └── *.puml               # Sequence diagrams
├── kong.yml                 # Kong gateway configuration
├── docker-compose.yml       # Multi-service setup
└── pom.xml                  # Multi-module Maven config
```

### Database
- **Type**: H2 In-memory
- **Schema**: Auto-created by Hibernate
- **Data**: Seeded from `data.sql`
- **Reset**: Data resets on application restart

## Monitoring

### Kong Admin API
```bash
# View services
curl http://localhost:8001/services

# View routes
curl http://localhost:8001/routes

# Health check
curl http://localhost:8001/status
```

### Application Logs
```bash
# View all logs
docker compose logs -f

# View specific service
docker compose logs -f medication-api
```

## Troubleshooting

### Common Issues
1. **Kong not routing**: Check `kong.yml` configuration
2. **Tests failing**: Ensure test `data.sql` is loaded
3. **Port conflicts**: Verify ports 8000, 8001, 8080, 8081 are free

### Reset Everything
```bash
docker compose down -v  # Remove containers and volumes
docker compose up --build
```