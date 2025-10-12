# nll-light Evolution Roadmap
**From Demo to Production-Grade Medication Information Site**

**Current Status**: Working OAuth2/OIDC demo with basic medication CRUD  
**Vision**: Comprehensive, authoritative medication information platform for Swedish healthcare

---

## Table of Contents

1. [Current Architecture](#current-architecture)
2. [Product Capabilities](#product-capabilities)
3. [Technical Evolution](#technical-evolution)
4. [Phased Implementation](#phased-implementation)
5. [Infrastructure & Operations](#infrastructure--operations)
6. [Security & Compliance](#security--compliance)
7. [Success Metrics](#success-metrics)

---

## Current Architecture

### What We Have ✅
- **medication-api**: Spring Boot 3.3.3 REST API with H2 in-memory database
- **medication-web**: Thymeleaf web app with Keycloak OAuth2 authentication
- **Kong Gateway**: API routing and gateway capabilities (basic routing only)
- **Keycloak**: OIDC provider with pre-configured realm and test users
- **Docker Compose**: Local development orchestration
- **Playwright E2E**: Basic authentication flow tests
- **Swagger/OpenAPI**: API documentation at `/swagger-ui.html`

### Current Limitations ⚠️
- **Data**: 3 hardcoded medications in H2, no persistence
- **Search**: No search capabilities, simple list/get by ID only
- **Security**: Development-mode only (disabled issuer validation, no resource server)
- **Content**: Minimal medication info (name, description only)
- **Scale**: Single instance, no caching, no observability

---

## Product Capabilities

### 1. Rich Data Model

#### Core Medication Model
```
Product (Läkemedelsprodukt)
├── NPL ID (Varunummer)
├── Name (trade name)
├── Manufacturer
├── ATC Classification
├── Active Substances []
│   ├── Name (INN/Swedish)
│   ├── Strength
│   └── Unit
├── Form (tablets, solution, etc.)
├── Route (oral, IV, topical, etc.)
├── Package []
│   ├── Package size
│   ├── NPL package ID
│   └── Availability status
└── Market Info
    ├── Rx/OTC status
    ├── TLV reimbursement status
    ├── Price (AUP/AIP)
    └── Generic/parallel mappings
```

#### Clinical Monograph Model
```
Monograph
├── Product reference
├── Clinical Information
│   ├── Indications []
│   ├── Contraindications []
│   ├── Warnings/Precautions []
│   ├── Adverse Effects []
│   │   ├── Effect
│   │   ├── Frequency (very common, common, etc.)
│   │   └── Severity
│   ├── Interactions []
│   │   ├── Interacting drug/substance
│   │   ├── Mechanism
│   │   ├── Severity (major, moderate, minor)
│   │   └── Management advice
│   └── Dosing []
│       ├── Population (adult, pediatric, geriatric)
│       ├── Indication-specific
│       ├── Standard dose
│       ├── Renal adjustment
│       ├── Hepatic adjustment
│       └── Administration instructions
├── Pharmacology
│   ├── Mechanism of action
│   ├── Pharmacokinetics (absorption, distribution, metabolism, excretion)
│   └── Pharmacodynamics
├── Special Populations
│   ├── Pregnancy (category, trimester-specific advice)
│   ├── Lactation (safety, alternatives)
│   └── Pediatric/geriatric considerations
└── Provenance
    ├── Source (FASS, EMA, Janusmed)
    ├── Last updated
    ├── Version
    └── References/links
```

#### Database Schema (PostgreSQL)

**Core tables**:
```sql
-- Substances (active ingredients)
substances (id, inn_name, swedish_name, cas_number, atc_code)

-- Products (marketed medications)
products (id, npl_id, vnr, trade_name, manufacturer_id, rx_status, market_status)

-- Product-Substance junction (handles combinations)
product_substances (product_id, substance_id, strength, unit, ordinal)

-- Packages
packages (id, product_id, npl_package_id, size, unit, price_aup, price_aip, reimbursement_status)

-- ATC classification
atc_codes (code, level, name_en, name_sv, parent_code, ddd, ddd_unit)

-- Manufacturers
manufacturers (id, name, country, org_number)
```

**Clinical tables**:
```sql
-- Monographs (one per product)
monographs (id, product_id, version, source, last_updated, content_hash)

-- Indications
indications (id, monograph_id, text, icd10_code, priority)

-- Contraindications
contraindications (id, monograph_id, text, absolute, severity)

-- Adverse effects
adverse_effects (id, monograph_id, effect, frequency, severity, soc_code)

-- Interactions (pairwise)
interactions (id, product_a_id, product_b_id, mechanism, severity, evidence_level, management)

-- Dosing regimens
dosing (id, monograph_id, population, indication_id, standard_dose, max_dose, frequency, route, adjustments)
```

**Audit/versioning**:
```sql
-- Content history
monograph_history (id, monograph_id, version, changed_by, changed_at, diff_json)

-- User activity
audit_log (id, user_id, action, resource_type, resource_id, timestamp, ip_address)
```

### 2. Search & Discovery

#### Full-Text Search with OpenSearch/Elasticsearch
```json
{
  "mappings": {
    "properties": {
      "product_name": { 
        "type": "text",
        "analyzer": "swedish",
        "fields": {
          "keyword": { "type": "keyword" },
          "suggest": { "type": "completion" }
        }
      },
      "substance_names": { "type": "text", "analyzer": "swedish" },
      "atc_code": { "type": "keyword" },
      "atc_name": { "type": "text", "analyzer": "swedish" },
      "indications": { "type": "text", "analyzer": "swedish" },
      "form": { "type": "keyword" },
      "route": { "type": "keyword" },
      "rx_status": { "type": "keyword" },
      "manufacturer": { "type": "keyword" },
      "market_status": { "type": "keyword" }
    }
  }
}
```

#### Search API
```
GET /api/v1/medications/search?q={query}
  &atc={atc_prefix}
  &form={form}
  &route={route}
  &rx_status={rx|otc}
  &page={page}
  &size={size}
  &sort={relevance|name|atc}

Response:
{
  "results": [...],
  "total": 1250,
  "facets": {
    "atc_class": [{"code": "N02", "name": "Analgesics", "count": 45}, ...],
    "form": [{"value": "Tablet", "count": 320}, ...],
    "manufacturer": [{"value": "Pfizer", "count": 12}, ...]
  },
  "suggestions": ["Alvedon", "Alvedon Novum"],
  "query_time_ms": 23
}
```

#### Features
- **Fuzzy matching**: Handle typos (e.g., "ibuprofn" → "ibuprofen")
- **Synonyms**: Brand ↔ substance (e.g., "Ipren" → "ibuprofen")
- **Stemming**: Swedish language analyzer
- **Highlighting**: Show matched terms in results
- **Autocomplete**: Real-time suggestions as user types
- **Recent/popular**: Track search analytics

### 3. Clinical Tools

#### Interaction Checker
```
POST /api/v1/interactions/check
{
  "products": [123, 456, 789]  // NPL IDs or internal IDs
}

Response:
{
  "interactions": [
    {
      "product_a": {"id": 123, "name": "Waran"},
      "product_b": {"id": 456, "name": "Trombyl"},
      "severity": "major",
      "mechanism": "Increased bleeding risk via synergistic anticoagulation",
      "evidence": "well-established",
      "management": "Avoid combination. If unavoidable, monitor INR closely.",
      "references": ["FASS-12345", "Janusmed-INT-0023"]
    }
  ],
  "severity_summary": {
    "major": 1,
    "moderate": 0,
    "minor": 2
  }
}
```

#### Dose Calculator
```
POST /api/v1/dosing/calculate
{
  "product_id": 123,
  "indication": "pain",
  "patient": {
    "age": 67,
    "weight_kg": 72,
    "creatinine_umol_l": 145,  // Calculate eGFR
    "hepatic_impairment": "mild"
  }
}

Response:
{
  "standard_dose": "500-1000 mg q4-6h",
  "max_daily": "4000 mg",
  "renal_adjustment": {
    "egfr": 45,
    "recommendation": "Reduce to 500 mg q6h (max 2000 mg/day)",
    "rationale": "eGFR 30-50: dose reduction required"
  },
  "hepatic_adjustment": {
    "recommendation": "Use with caution, consider reduced dose",
    "rationale": "Mild impairment: no specific adjustment, monitor"
  }
}
```

#### Therapeutic Alternatives
```
GET /api/v1/medications/{id}/alternatives?type={same_substance|same_atc|same_indication}

Response:
{
  "alternatives": [
    {
      "product": {...},
      "match_type": "same_substance_different_form",
      "rationale": "Same active ingredient (paracetamol), different formulation",
      "cost_comparison": "15% lower AUP"
    }
  ]
}
```

### 4. Patient & HCP Content

#### Dual-View Architecture
```
GET /api/v1/medications/{id}/monograph?view={patient|hcp}

Patient View:
- Plain language
- Key points first
- Visual aids (icons for warnings)
- "When to call doctor" sections
- Simplified dosing

HCP View:
- Technical terminology
- Complete clinical data
- Pharmacology details
- Literature references
- Dosing algorithms
```

#### Features
- **Print/PDF export**: Formatted patient leaflets
- **Localization**: Swedish primary, English optional
- **Reading level**: Patient content at grade 8-10 level
- **Accessibility**: WCAG 2.1 AA compliant

---

## Technical Evolution

### Phase 1: Foundations (Months 1-3)

#### 1.1 Database Migration

**Replace H2 with PostgreSQL**

`docker-compose.yml` addition:
```yaml
postgres:
  image: postgres:16-alpine
  environment:
    POSTGRES_DB: nll_light
    POSTGRES_USER: nll_user
    POSTGRES_PASSWORD: ${DB_PASSWORD}
  volumes:
    - postgres_data:/var/lib/postgresql/data
  ports:
    - "5432:5432"
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U nll_user"]
    interval: 10s
```

**Add Flyway migrations**

`pom.xml` (medication-api):
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

`application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://postgres:5432/nll_light
spring.datasource.username=nll_user
spring.datasource.password=${DB_PASSWORD}
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

**First migration** - `medication-api/src/main/resources/db/migration/V1__initial_schema.sql`:
```sql
CREATE TABLE substances (
    id BIGSERIAL PRIMARY KEY,
    inn_name VARCHAR(255) NOT NULL,
    swedish_name VARCHAR(255),
    cas_number VARCHAR(50),
    atc_code VARCHAR(10),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    npl_id VARCHAR(20) UNIQUE,
    vnr VARCHAR(20),
    trade_name VARCHAR(255) NOT NULL,
    manufacturer_id BIGINT,
    rx_status VARCHAR(10) DEFAULT 'Rx',
    market_status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE product_substances (
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
    substance_id BIGINT REFERENCES substances(id),
    strength DECIMAL(10,2),
    unit VARCHAR(20),
    ordinal INT DEFAULT 1,
    PRIMARY KEY (product_id, substance_id)
);

CREATE INDEX idx_products_npl ON products(npl_id);
CREATE INDEX idx_products_name ON products(trade_name);
CREATE INDEX idx_substances_inn ON substances(inn_name);
CREATE INDEX idx_substances_atc ON substances(atc_code);
```

#### 1.2 Data Ingestion (ETL)

**Spring Batch Job for NPL Import**

`BatchConfig.java`:
```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Bean
    public Job importNplJob(JobRepository jobRepository, Step importNplStep) {
        return new JobBuilder("importNplJob", jobRepository)
                .start(importNplStep)
                .build();
    }

    @Bean
    public Step importNplStep(JobRepository jobRepository, 
                               PlatformTransactionManager transactionManager,
                               ItemReader<NplRecord> nplReader,
                               ItemWriter<Product> productWriter) {
        return new StepBuilder("importNplStep", jobRepository)
                .<NplRecord, Product>chunk(100, transactionManager)
                .reader(nplReader)
                .processor(nplProcessor())
                .writer(productWriter)
                .build();
    }

    @Bean
    public FlatFileItemReader<NplRecord> nplReader() {
        return new FlatFileItemReaderBuilder<NplRecord>()
                .name("nplReader")
                .resource(new ClassPathResource("data/npl_export.csv"))
                .delimited()
                .names("npl_id", "vnr", "trade_name", "atc", "substance", "strength", "form")
                .targetType(NplRecord.class)
                .build();
    }

    @Bean
    public ItemProcessor<NplRecord, Product> nplProcessor() {
        return record -> {
            // Transform NPL record to Product entity
            // Look up or create substances, manufacturers
            // Handle ATC classification
            return product;
        };
    }
}
```

**Data Sources** (Swedish context):
- **NPL (Nationellt produktregister för Läkemedel)**: E-hälsomyndigheten
- **ATC/DDD**: WHO Collaborating Centre
- **FASS**: Läkemedelsindustriföreningen (requires licensing)
- **Janusmed**: Region Stockholm (public interaction data)

#### 1.3 Search Infrastructure

**Add OpenSearch**

`docker-compose.yml`:
```yaml
opensearch:
  image: opensearchproject/opensearch:2.11.0
  environment:
    - discovery.type=single-node
    - OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m
    - DISABLE_SECURITY_PLUGIN=true  # Dev only
  volumes:
    - opensearch_data:/usr/share/opensearch/data
  ports:
    - "9200:9200"
```

**Spring Data OpenSearch**

`pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

`MedicationSearchDocument.java`:
```java
@Document(indexName = "medications")
public class MedicationSearchDocument {
    @Id
    private Long id;
    
    @Field(type = FieldType.Text, analyzer = "swedish")
    @MultiField(mainField = @Field(type = FieldType.Text),
                otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword),
                    @InnerField(suffix = "suggest", type = FieldType.Completion)
                })
    private String productName;
    
    @Field(type = FieldType.Text, analyzer = "swedish")
    private List<String> substanceNames;
    
    @Field(type = FieldType.Keyword)
    private String atcCode;
    
    @Field(type = FieldType.Text, analyzer = "swedish")
    private String atcName;
    
    @Field(type = FieldType.Keyword)
    private String form;
    
    @Field(type = FieldType.Keyword)
    private String rxStatus;
    
    // Getters, setters, constructors
}
```

`MedicationSearchService.java`:
```java
@Service
public class MedicationSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public SearchResponse<MedicationSearchDocument> search(MedicationSearchQuery query) {
        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(q -> q
                    .multiMatch(m -> m
                        .query(query.getSearchText())
                        .fields("productName^3", "substanceNames^2", "atcName")
                        .fuzziness("AUTO")
                        .analyzer("swedish")
                    )
                )
                .withFilter(buildFilters(query))
                .withPageable(PageRequest.of(query.getPage(), query.getSize()))
                .withAggregations(buildAggregations())
                .withHighlightQuery(new HighlightQuery(
                    new Highlight(List.of(
                        new HighlightField("productName"),
                        new HighlightField("substanceNames")
                    )), null
                ))
                .build();

        return elasticsearchOperations.search(searchQuery, MedicationSearchDocument.class);
    }
}
```

#### 1.4 API Evolution to Resource Server

**Current**: medication-api has no authentication  
**Target**: Validate JWTs from Keycloak, enforce scopes

`pom.xml` (medication-api):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

`application.properties` (medication-api):
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8082/auth/realms/nll-light
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/certs
```

`ResourceServerConfig.java`:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/medications/**").permitAll()  // Public read
                .requestMatchers(HttpMethod.POST, "/api/v1/medications/**").hasAuthority("SCOPE_medication.write")
                .requestMatchers(HttpMethod.PUT, "/api/v1/medications/**").hasAuthority("SCOPE_medication.write")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/medications/**").hasAuthority("SCOPE_medication.admin")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")  // API endpoints use token auth
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("scope");
        grantedAuthoritiesConverter.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtConverter;
    }
}
```

**Kong Configuration for Token Relay**

`kong.yml`:
```yaml
_format_version: "3.0"

services:
  - name: medication-api
    url: http://medication-api:8080
    routes:
      - name: api-route
        paths:
          - /api
        strip_path: false
    plugins:
      - name: cors
        config:
          origins:
            - http://localhost:8080
          credentials: true
          exposed_headers:
            - Authorization
      - name: request-transformer
        config:
          add:
            headers:
              - "Authorization:$(headers.authorization)"  # Relay token
      - name: rate-limiting
        config:
          minute: 100
          policy: local
```

**medication-web: Add token relay**

`WebClientConfig.java`:
```java
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2.setDefaultOAuth2AuthorizedClient(true);

        return WebClient.builder()
                .baseUrl("http://kong:8000")
                .filter(oauth2)
                .build();
    }
}
```

#### 1.5 API Versioning

**Version 1 Endpoints**
```
GET    /api/v1/medications                    # List/search
GET    /api/v1/medications/{id}               # Get by ID
GET    /api/v1/medications/{id}/monograph     # Clinical monograph
GET    /api/v1/medications/search             # Full-text search with filters
POST   /api/v1/medications                    # Create (admin)
PUT    /api/v1/medications/{id}               # Update (admin)
DELETE /api/v1/medications/{id}               # Delete (admin)

GET    /api/v1/interactions/check             # Multi-drug interaction check
POST   /api/v1/dosing/calculate               # Dose calculator
GET    /api/v1/medications/{id}/alternatives  # Therapeutic alternatives
```

**Versioning Strategy**
```java
@RestController
@RequestMapping("/api/v1/medications")
public class MedicationControllerV1 {
    // V1 implementation
}

// Future: V2 with breaking changes
@RestController
@RequestMapping("/api/v2/medications")
public class MedicationControllerV2 {
    // V2 implementation
}
```

---

### Phase 2: Clinical Depth & UX (Months 4-6)

#### 2.1 Expanded Monograph System

**MonographService**
```java
@Service
public class MonographService {

    public MonographDTO getMonograph(Long productId, MonographView view) {
        Monograph monograph = monographRepository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException("Monograph not found"));
        
        return switch (view) {
            case PATIENT -> toPatientView(monograph);
            case HCP -> toHcpView(monograph);
        };
    }

    private MonographDTO toPatientView(Monograph m) {
        return MonographDTO.builder()
                .productName(m.getProduct().getTradeName())
                .whatItIsFor(simplifyIndications(m.getIndications()))
                .howToUse(simplifyDosing(m.getDosing()))
                .warnings(simplifyWarnings(m.getContraindications(), m.getWarnings()))
                .sideEffects(simplifyAdverseEffects(m.getAdverseEffects()))
                .whenToCallDoctor(extractUrgentWarnings(m))
                .storage(m.getStorageInstructions())
                .build();
    }

    private MonographDTO toHcpView(Monograph m) {
        return MonographDTO.builder()
                .productInfo(m.getProduct())
                .indications(m.getIndications())
                .contraindications(m.getContraindications())
                .warnings(m.getWarnings())
                .adverseEffects(groupByFrequency(m.getAdverseEffects()))
                .interactions(m.getInteractions())
                .dosing(m.getDosing())
                .pharmacology(m.getPharmacology())
                .specialPopulations(m.getSpecialPopulations())
                .references(m.getReferences())
                .build();
    }
}
```

#### 2.2 Interaction Checker Implementation

**InteractionService**
```java
@Service
public class InteractionService {

    public InteractionCheckResult checkInteractions(List<Long> productIds) {
        List<Interaction> interactions = interactionRepository
                .findInteractionsBetweenProducts(productIds);
        
        // Also check substance-level interactions
        List<Long> substanceIds = productRepository
                .findSubstanceIdsByProductIds(productIds);
        List<Interaction> substanceInteractions = interactionRepository
                .findInteractionsBetweenSubstances(substanceIds);
        
        return InteractionCheckResult.builder()
                .interactions(mergeAndDeduplicate(interactions, substanceInteractions))
                .severitySummary(calculateSummary(interactions))
                .riskScore(calculateRiskScore(interactions))
                .build();
    }

    private int calculateRiskScore(List<Interaction> interactions) {
        return interactions.stream()
                .mapToInt(i -> switch (i.getSeverity()) {
                    case MAJOR -> 10;
                    case MODERATE -> 5;
                    case MINOR -> 1;
                })
                .sum();
    }
}
```

#### 2.3 Frontend Enhancement Options

**Option A: Enhanced Thymeleaf (Incremental)**
```html
<!-- search.html -->
<div class="search-container" th:fragment="search">
    <form id="search-form" th:action="@{/search}" method="get">
        <input type="text" 
               name="q" 
               id="search-input"
               autocomplete="off"
               placeholder="Sök läkemedel, substans, eller indikation..."
               th:value="${query}">
        
        <!-- Autocomplete suggestions (HTMX) -->
        <div id="suggestions" 
             hx-get="/api/v1/medications/suggest"
             hx-trigger="keyup changed delay:300ms from:#search-input"
             hx-target="#suggestions"></div>
        
        <!-- Filters -->
        <div class="filters">
            <select name="atc" th:field="*{atc}">
                <option value="">ATC-klass</option>
                <option th:each="atc : ${atcClasses}" 
                        th:value="${atc.code}" 
                        th:text="${atc.name}"></option>
            </select>
            <!-- More filters... -->
        </div>
    </form>
    
    <!-- Results with facets -->
    <div class="results-container">
        <aside class="facets">
            <!-- Faceted navigation -->
        </aside>
        <main class="results">
            <div th:each="med : ${results}" class="result-card">
                <!-- Result item -->
            </div>
        </main>
    </div>
</div>
```

Add **HTMX** for dynamic interactions without full SPA:
```xml
<!-- Add to templates -->
<script src="https://unpkg.com/htmx.org@1.9.10"></script>
```

**Option B: React SPA with BFF**

```
Browser ──► medication-web (BFF) ──► Kong ──► medication-api
         OAuth2/Session            Token Relay    JWT validation
```

**Frontend Structure**:
```
medication-web-frontend/
├── src/
│   ├── components/
│   │   ├── Search/
│   │   │   ├── SearchBar.tsx
│   │   │   ├── Autocomplete.tsx
│   │   │   ├── Filters.tsx
│   │   │   └── Results.tsx
│   │   ├── Monograph/
│   │   │   ├── MonographView.tsx
│   │   │   ├── PatientView.tsx
│   │   │   └── HcpView.tsx
│   │   ├── Tools/
│   │   │   ├── InteractionChecker.tsx
│   │   │   └── DoseCalculator.tsx
│   │   └── Layout/
│   ├── services/
│   │   └── api.ts  # Axios with BFF endpoints
│   ├── hooks/
│   │   └── useAuth.ts
│   └── App.tsx
```

**BFF Pattern** (keep medication-web as backend-for-frontend):
```java
@RestController
@RequestMapping("/bff/api")
public class BffController {

    private final WebClient apiClient;

    @GetMapping("/medications/search")
    public Mono<SearchResponse> search(@RequestParam String q, Authentication auth) {
        // Aggregate multiple backend calls, add user context, etc.
        return apiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/medications/search")
                        .queryParam("q", q)
                        .build())
                .retrieve()
                .bodyToMono(SearchResponse.class);
    }
}
```

#### 2.4 Internationalization

**messages_sv.properties**:
```properties
search.placeholder=Sök läkemedel, substans, eller indikation...
monograph.indications=Indikationer
monograph.contraindications=Kontraindikationer
monograph.warnings=Varningar
interaction.severity.major=Allvarlig interaktion
```

**messages_en.properties**:
```properties
search.placeholder=Search medication, substance, or indication...
monograph.indications=Indications
monograph.contraindications=Contraindications
monograph.warnings=Warnings
interaction.severity.major=Major interaction
```

#### 2.5 Accessibility Implementation

**WCAG 2.1 AA Checklist**:
- ✅ Semantic HTML (`<main>`, `<nav>`, `<article>`, `<section>`)
- ✅ ARIA labels on interactive elements
- ✅ Keyboard navigation (tab order, skip links, focus management)
- ✅ Color contrast ratio ≥ 4.5:1 (text), ≥ 3:1 (UI components)
- ✅ Text resize up to 200% without loss of functionality
- ✅ Focus indicators visible
- ✅ Form labels and error messages associated
- ✅ Alt text for images
- ✅ Screen reader testing (NVDA, JAWS)

**Example**:
```html
<button aria-label="Sök läkemedel" 
        aria-describedby="search-help"
        class="btn-search">
    <svg aria-hidden="true"><!-- search icon --></svg>
</button>
<span id="search-help" class="sr-only">
    Ange läkemedelsnamn eller substans och tryck enter
</span>
```

---

### Phase 3: Scale & Governance (Months 7-12)

#### 3.1 Observability Stack

**Metrics with Micrometer/Prometheus**

`pom.xml`:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

`application.properties`:
```properties
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.metrics.tags.application=medication-api
management.metrics.distribution.percentiles-histogram.http.server.requests=true
```

**Custom Metrics**:
```java
@Component
public class SearchMetrics {
    private final Counter searchCounter;
    private final Timer searchTimer;

    public SearchMetrics(MeterRegistry registry) {
        this.searchCounter = Counter.builder("medication.search.total")
                .tag("type", "fulltext")
                .register(registry);
        this.searchTimer = Timer.builder("medication.search.duration")
                .tag("type", "fulltext")
                .register(registry);
    }

    public void recordSearch(Runnable search) {
        searchCounter.increment();
        searchTimer.record(search);
    }
}
```

**Prometheus**

`docker-compose.yml`:
```yaml
prometheus:
  image: prom/prometheus:latest
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
    - prometheus_data:/prometheus
  ports:
    - "9090:9090"
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'

grafana:
  image: grafana/grafana:latest
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
  volumes:
    - grafana_data:/var/lib/grafana
    - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
  ports:
    - "3000:3000"
```

`prometheus.yml`:
```yaml
scrape_configs:
  - job_name: 'medication-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['medication-api:8080']
  
  - job_name: 'medication-web'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['medication-web:8080']
  
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
```

**Distributed Tracing with OpenTelemetry**

`pom.xml`:
```xml
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
```

Jaeger for trace visualization:
```yaml
jaeger:
  image: jaegertracing/all-in-one:latest
  environment:
    - COLLECTOR_OTLP_ENABLED=true
  ports:
    - "16686:16686"  # UI
    - "4318:4318"    # OTLP HTTP
```

**Structured Logging**

`logback-spring.xml`:
```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>trace_id</includeMdcKeyName>
            <includeMdcKeyName>span_id</includeMdcKeyName>
            <includeMdcKeyName>user_id</includeMdcKeyName>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

Correlation IDs:
```java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                     HttpServletResponse response, 
                                     FilterChain filterChain) {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlation_id", correlationId);
        response.setHeader("X-Correlation-ID", correlationId);
        filterChain.doFilter(request, response);
        MDC.clear();
    }
}
```

#### 3.2 Caching Strategy

**Layered Caching**

1. **Kong Proxy Cache** (HTTP responses)
```yaml
# kong.yml
plugins:
  - name: proxy-cache
    config:
      strategy: memory
      cache_ttl: 300
      content_type:
        - application/json
      request_method:
        - GET
      response_code:
        - 200
```

2. **Spring Cache with Redis** (service layer)

`pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

`docker-compose.yml`:
```yaml
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
  volumes:
    - redis_data:/data
```

`CacheConfig.java`:
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
```

Usage:
```java
@Service
public class MonographService {

    @Cacheable(value = "monographs", key = "#productId")
    public MonographDTO getMonograph(Long productId, MonographView view) {
        // Expensive database query
    }

    @CacheEvict(value = "monographs", key = "#productId")
    public void updateMonograph(Long productId, MonographDTO dto) {
        // Update logic
    }
}
```

3. **Database Indices**
```sql
-- Search performance
CREATE INDEX idx_products_search ON products USING gin(to_tsvector('swedish', trade_name));
CREATE INDEX idx_monographs_indications ON indications USING gin(to_tsvector('swedish', text));

-- Join performance
CREATE INDEX idx_product_substances_product ON product_substances(product_id);
CREATE INDEX idx_packages_product ON packages(product_id);

-- Filter performance
CREATE INDEX idx_products_atc ON products(atc_code);
CREATE INDEX idx_products_rx_status ON products(rx_status);
CREATE INDEX idx_products_market_status ON products(market_status);
```

#### 3.3 Editorial CMS

**Admin UI for Content Management**

```java
@RestController
@RequestMapping("/api/v1/admin/monographs")
@PreAuthorize("hasRole('EDITOR')")
public class MonographAdminController {

    @GetMapping("/{id}/versions")
    public List<MonographVersion> getVersionHistory(@PathVariable Long id) {
        return monographHistoryService.getVersions(id);
    }

    @PostMapping("/{id}/draft")
    public MonographDraft createDraft(@PathVariable Long id) {
        return monographService.createDraft(id);
    }

    @PutMapping("/drafts/{draftId}")
    public MonographDraft updateDraft(@PathVariable Long draftId, 
                                      @RequestBody MonographDTO update) {
        return monographService.updateDraft(draftId, update);
    }

    @PostMapping("/drafts/{draftId}/submit-review")
    @PreAuthorize("hasRole('EDITOR')")
    public void submitForReview(@PathVariable Long draftId) {
        workflowService.submitForReview(draftId);
    }

    @PostMapping("/drafts/{draftId}/approve")
    @PreAuthorize("hasRole('REVIEWER')")
    public void approve(@PathVariable Long draftId) {
        workflowService.approve(draftId);
    }

    @PostMapping("/drafts/{draftId}/publish")
    @PreAuthorize("hasRole('PUBLISHER')")
    public Monograph publish(@PathVariable Long draftId) {
        return monographService.publish(draftId);
    }

    @GetMapping("/{id}/diff")
    public MonographDiff compareDraft(@PathVariable Long id,
                                       @RequestParam Long draftId) {
        return monographService.diff(id, draftId);
    }
}
```

**Workflow States**:
```
DRAFT → REVIEW → APPROVED → PUBLISHED
  ↑        ↓         ↓
  └────────┴─────────┴───── REJECTED → DRAFT
```

#### 3.4 CI/CD Pipeline

**GitHub Actions Workflow**

`.github/workflows/ci.yml`:
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: nll_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
      
      - name: Run unit tests
        run: mvn test
      
      - name: Run integration tests
        run: mvn verify -P integration-tests
        env:
          DB_URL: jdbc:postgresql://postgres:5432/nll_test
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3

  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run OWASP Dependency Check
        run: mvn dependency-check:check
      
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'

  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Start services
        run: docker-compose up -d
      
      - name: Wait for services
        run: ./scripts/wait-for-services.sh
      
      - name: Run Playwright tests
        run: |
          cd medication-web/e2e
          npm ci
          npx playwright install --with-deps
          npm test
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: medication-web/e2e/playwright-report/

  build-and-push:
    needs: [test, security-scan, e2e-tests]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3
      
      - name: Build Docker images
        run: docker-compose build
      
      - name: Push to registry
        run: |
          echo ${{ secrets.REGISTRY_PASSWORD }} | docker login -u ${{ secrets.REGISTRY_USERNAME }} --password-stdin
          docker-compose push

  deploy-staging:
    needs: build-and-push
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - name: Deploy to staging
        run: |
          # SSH to staging server and docker-compose pull/up
          # Or use k8s kubectl apply
```

**Database Migration Gating**

```yaml
- name: Check pending migrations
  run: |
    PENDING=$(mvn flyway:info | grep "Pending" | wc -l)
    if [ $PENDING -gt 0 ]; then
      echo "⚠️  Pending migrations detected. Manual approval required."
      # Require manual approval for production
    fi
```

---

## Infrastructure & Operations

### Production Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Load Balancer                        │
│                    (AWS ALB / CloudFlare)                    │
└────────────────────┬─────────────────┬──────────────────────┘
                     │                 │
        ┌────────────┴────────┐   ┌───┴──────────────┐
        │  medication-web     │   │  medication-web  │
        │  (Container x2)     │   │  (Container x2)  │
        └─────────┬───────────┘   └──────┬───────────┘
                  │                      │
                  └──────────┬───────────┘
                             │
                    ┌────────┴─────────┐
                    │   Kong Gateway   │
                    │  (HA cluster)    │
                    └────────┬─────────┘
                             │
                  ┌──────────┼──────────┐
                  │          │          │
        ┌─────────┴───┐  ┌───┴────┐  ┌─┴──────────┐
        │ medication  │  │ Search │  │  Keycloak  │
        │    -api     │  │(OpenSe-│  │  (HA)      │
        │ (Container) │  │ arch)  │  │            │
        └──────┬──────┘  └────────┘  └────────────┘
               │
        ┌──────┴──────┐
        │  PostgreSQL │
        │   (RDS or   │
        │  managed)   │
        └─────────────┘
```

### Container Orchestration

**Kubernetes Manifests** (example)

`k8s/medication-api-deployment.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: medication-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: medication-api
  template:
    metadata:
      labels:
        app: medication-api
    spec:
      containers:
      - name: medication-api
        image: registry.example.com/nll-light/medication-api:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DB_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

### Backup & Disaster Recovery

```bash
# PostgreSQL automated backups
# Daily full backup + continuous WAL archiving

# Backup script
#!/bin/bash
pg_dump -h $DB_HOST -U $DB_USER nll_light | gzip > /backups/nll_light_$(date +%Y%m%d).sql.gz

# Retention: 30 days daily, 12 months monthly

# Test restore monthly
pg_restore -h $TEST_DB -U test -d nll_test /backups/latest.sql.gz
```

### Monitoring & Alerts

**Grafana Dashboards**:
1. **Application Health**: Request rate, error rate, latency (p50, p95, p99)
2. **Database**: Connection pool, query performance, slow queries
3. **Search**: Query latency, index size, cache hit rate
4. **Business Metrics**: Searches per day, popular medications, interaction checks

**Alertmanager Rules**:
```yaml
groups:
  - name: application
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate detected"
      
      - alert: SlowDatabase
        expr: histogram_quantile(0.95, rate(hikaricp_connections_acquire_seconds_bucket[5m])) > 1
        for: 5m
        annotations:
          summary: "Database connection acquisition slow"
```

---

## Security & Compliance

### Production Security Hardening

#### Keycloak Production Configuration
```yaml
keycloak:
  environment:
    KC_HOSTNAME_URL: https://auth.nll-light.se
    KC_HOSTNAME_STRICT: true
    KC_HOSTNAME_STRICT_BACKCHANNEL: true
    KC_PROXY: edge
    KC_HTTP_ENABLED: false
    KC_HTTPS_CERTIFICATE_FILE: /certs/cert.pem
    KC_HTTPS_CERTIFICATE_KEY_FILE: /certs/key.pem
    KC_DB: postgres
    KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
    KC_HEALTH_ENABLED: true
    KC_METRICS_ENABLED: true
  command: start --optimized
```

**Realm Configuration**:
- Short access token lifetime (5 minutes)
- Refresh token rotation enabled
- PKCE required for public clients
- Strong password policy (min 12 chars, complexity)
- MFA/2FA for admin users
- Client secret rotation policy

#### Application Security

**HTTPS/TLS**:
```yaml
# Force HTTPS redirects in Kong
plugins:
  - name: redirect-https
    config:
      https_redirect_status_code: 301
```

**Security Headers**:
```java
@Configuration
public class SecurityHeadersConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.headers(headers -> headers
            .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000))
            .frameOptions().deny()
            .xssProtection().block(true)
            .contentTypeOptions().disable()
        );
        return http.build();
    }
}
```

**Secrets Management**:
```yaml
# Use HashiCorp Vault or AWS Secrets Manager
spring:
  cloud:
    vault:
      uri: https://vault.example.com
      token: ${VAULT_TOKEN}
      kv:
        enabled: true
        backend: secret
        application-name: nll-light
```

### GDPR Compliance

**User Data Handling**:
- Audit logs for all access to personal health information
- Data retention policies (auto-delete after X months)
- Right to access: API for user data export
- Right to erasure: Anonymize user data on request

```java
@RestController
@RequestMapping("/api/v1/gdpr")
public class GdprController {

    @GetMapping("/my-data")
    public UserDataExport exportMyData(Authentication auth) {
        // Export all user data in machine-readable format
    }

    @DeleteMapping("/my-data")
    public void deleteMyData(Authentication auth) {
        // Anonymize user data, keep audit trail
    }
}
```

### Medical Device Regulation (MDR)

**If considered a medical device in EU**:
- CE marking requirements
- Clinical evaluation documentation
- Risk management (ISO 14971)
- Technical documentation
- Post-market surveillance

**Disclaimer Implementation**:
```html
<div class="medical-disclaimer">
    <h3>Medicinsk ansvarsfriskrivning</h3>
    <p>
        Denna tjänst tillhandahåller läkemedelsinformation endast i 
        informationssyfte. Den utgör inte medicinsk rådgivning och ska 
        inte användas som ersättning för konsultation med legitimerad 
        vårdpersonal. Kontakta alltid läkare eller apotekspersonal vid 
        frågor om din medicinering.
    </p>
</div>
```

---

## Success Metrics

### Key Performance Indicators (KPIs)

**Product**:
- Monthly active users (MAU)
- Searches per user per session
- Medication monograph views
- Interaction checks performed
- Time to find medication (search → view)

**Technical**:
- API latency p95 < 200ms
- Search latency p95 < 100ms
- Uptime > 99.9%
- Error rate < 0.1%
- Cache hit rate > 80%

**Content**:
- Medication coverage (% of Swedish market)
- Monograph completeness score
- Data freshness (avg age of updates)
- User-reported data quality issues

**Business**:
- Cost per search
- Infrastructure cost per MAU
- Partner integrations (e.g., EHR systems)

---

## Implementation Priorities

### Must-Have (Phase 1)
1. ✅ PostgreSQL migration with Flyway
2. ✅ Resource server security on API
3. ✅ OpenSearch integration
4. ✅ NPL/ATC data import
5. ✅ Basic search with filters
6. ✅ Monograph schema expansion

### Should-Have (Phase 2)
1. ✅ Interaction checker
2. ✅ Patient/HCP dual views
3. ✅ Internationalization (sv/en)
4. ✅ Accessibility (WCAG 2.1 AA)
5. ✅ Enhanced UI (Thymeleaf + HTMX or SPA)

### Nice-to-Have (Phase 3)
1. ✅ Editorial CMS
2. ✅ Advanced analytics
3. ✅ Mobile app
4. ✅ Integration APIs for partners
5. ✅ ML-powered drug recommendations

---

## Next Steps

### Immediate Actions (Week 1)
1. Set up PostgreSQL container and test connection
2. Create initial Flyway migration scripts
3. Design core domain schema (ERD)
4. Research NPL data access (E-hälsomyndigheten API/exports)
5. Spike OpenSearch integration

### Month 1 Goals
- ✅ Working PostgreSQL setup with migrations
- ✅ 100 medications imported from NPL
- ✅ Basic search working in OpenSearch
- ✅ Resource server protecting API endpoints
- ✅ Updated documentation

### Month 3 Goals
- ✅ 1000+ medications with basic monographs
- ✅ Search with facets and filters
- ✅ Interaction checker (basic substance-level)
- ✅ Patient-friendly monograph views
- ✅ CI/CD pipeline operational

---

## Resources & References

### Swedish Healthcare Data Sources
- **E-hälsomyndigheten NPL**: https://www.ehalsomyndigheten.se/tjanster/npl/
- **FASS**: https://www.fass.se (requires licensing for data)
- **Läkemedelsverket**: https://www.lakemedelsverket.se
- **Janusmed interactions**: https://janusmed.se
- **TLV reimbursement**: https://www.tlv.se

### Technical References
- Spring Boot 3 docs: https://spring.io/projects/spring-boot
- Keycloak docs: https://www.keycloak.org/documentation
- Kong Gateway docs: https://docs.konghq.com
- OpenSearch docs: https://opensearch.org/docs/latest/
- WCAG 2.1: https://www.w3.org/WAI/WCAG21/quickref/

### Standards
- ATC/DDD: https://www.whocc.no/atc_ddd_index/
- ICD-10: https://www.who.int/standards/classifications/classification-of-diseases
- SNOMED CT: https://www.snomed.org
- HL7 FHIR: https://www.hl7.org/fhir/

---

**Document Version**: 1.0  
**Last Updated**: October 11, 2025  
**Status**: Living document - update as implementation progresses
