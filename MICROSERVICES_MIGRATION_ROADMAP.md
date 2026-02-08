# CivicPulse - Monolith to Microservices Migration Roadmap

## Executive Summary

This document provides a comprehensive analysis of the CivicPulse Backend monolithic application and outlines a strategic roadmap for migrating to a microservices architecture.

---

## 1. Current Architecture Analysis

### 1.1 Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.3.5 |
| Language | Java 17 |
| Build Tool | Maven |
| Database | MySQL (with H2 for development) |
| ORM | Spring Data JPA |
| Security | Spring Security + JWT |
| Email | Spring Mail (SMTP/Gmail) |
| Migration | Flyway |
| Validation | Jakarta Validation + Hibernate Validator |
| Export | Apache POI (Excel export) |

### 1.2 Package Structure

```
com.example.backend/
├── BackendApplication.java          # Main application entry point
├── config/
│   ├── MailConfig.java              # Email configuration
│   └── WebConfig.java               # Web/Resource handlers
├── controller/
│   ├── AuthController.java          # Authentication endpoints
│   ├── ComplaintsController.java    # Admin/Officer complaint management
│   ├── DebugController.java         # Debug endpoints (dev only)
│   ├── GlobalExceptionHandler.java  # Global error handling
│   ├── OfficerController.java       # Officer management
│   ├── TestController.java          # Health check
│   └── UserController.java          # User info endpoints
├── dto/
│   ├── AssignRequest.java           # Complaint assignment DTO
│   ├── AuthRequest.java             # Login request DTO
│   ├── AuthResponse.java            # Login response DTO
│   ├── OfficerRequest.java          # Officer creation DTO
│   └── RegistrationRequest.java     # Registration DTO
├── grievance/
│   ├── Feedback.java                # Feedback entity
│   ├── FeedbackController.java      # Feedback API
│   ├── FeedbackRepository.java      # Feedback data access
│   ├── FeedbackService.java         # Feedback business logic
│   ├── Grievance.java               # Grievance entity
│   ├── GrievanceController.java     # Grievance API
│   ├── GrievanceRepository.java     # Grievance data access
│   ├── GrievanceService.java        # Grievance business logic
│   └── Status.java                  # Grievance status enum
├── model/
│   ├── Role.java                    # User roles enum
│   └── User.java                    # User entity
├── otp/
│   ├── EmailService.java            # OTP email sender
│   ├── OtpController.java           # OTP registration flow
│   ├── OtpUser.java                 # Temporary OTP user entity
│   └── OtpUserRepository.java       # OTP data access
├── repository/
│   └── UserRepository.java          # User data access
├── security/
│   ├── JwtFilter.java               # JWT authentication filter
│   ├── JwtUtils.java                # JWT generation/validation
│   ├── JwtsParser.java              # JWT parsing utilities
│   └── SecurityConfig.java          # Security configuration
└── service/
    ├── AppUserDetailsService.java   # Spring Security UserDetailsService
    └── UserService.java             # User registration logic
```

---

## 2. Identified Features & Bounded Contexts

### 2.1 Core Business Domains

Based on the analysis, the application has the following distinct bounded contexts:

#### **Domain 1: User Management**
- **Entities**: `User`, `Role`, `OtpUser`
- **Features**:
  - User registration (citizen, officer, admin)
  - OTP-based email verification
  - Role-based access control (RBAC)
  - User profile management
- **API Endpoints**:
  - `POST /api/auth/register` - User registration
  - `POST /api/auth/login` - User authentication
  - `GET /api/user/me` - Current user info
  - `POST /api/simple/register` - OTP registration
  - `POST /api/simple/verify-otp` - OTP verification
  - `POST /api/simple/resend-otp` - Resend OTP

#### **Domain 2: Grievance Management**
- **Entities**: `Grievance`, `Status`
- **Features**:
  - Grievance submission (with images)
  - Grievance status tracking
  - Grievance assignment to officers
  - Resolution workflow
  - Reopen workflow with evidence
  - Excel export for reports
- **Status Workflow**: `PENDING → ASSIGNED → IN_PROGRESS → RESOLVED`
- **API Endpoints**:
  - `POST /api/grievances` - Submit grievance
  - `GET /api/grievances/me` - My grievances
  - `GET /api/grievances/{id}/image` - Get grievance image
  - `PUT /api/grievances/{id}/image` - Update grievance image
  - `PUT /api/grievances/{id}/reopen` - Submit reopen evidence
  - `GET /api/grievances/export` - Export to Excel (Admin)
  - `GET /api/complaints` - List all complaints (Admin)
  - `GET /api/complaints/{id}` - Get complaint details
  - `PUT /api/complaints/{id}/assign` - Assign officer (Admin)
  - `PUT /api/complaints/{id}/update` - Update resolution
  - `GET /api/complaints/user/{userId}` - Complaints by user
  - `GET /api/complaints/officer/me` - Officer's assigned complaints
  - `GET /api/complaints/officer/{id}` - Complaints by officer (Admin)

#### **Domain 3: Officer Management**
- **Entities**: Uses `User` with `ROLE_OFFICER`
- **Features**:
  - Officer creation (Admin only)
  - Officer listing for assignment dropdown
  - Officer task management (via Grievance domain)
- **API Endpoints**:
  - `POST /api/officers` - Add officer (Admin)
  - `GET /api/officers` - List officers (Admin)

#### **Domain 4: Feedback & Rating**
- **Entities**: `Feedback`
- **Features**:
  - User feedback on resolved grievances
  - Rating aggregation (average + count)
  - Feedback listing for analysis
- **API Endpoints**:
  - `POST /api/feedback` - Submit feedback
  - `GET /api/feedback/complaint/{id}` - Feedback for a complaint
  - `GET /api/feedback` - All feedback (Admin/Officer)

#### **Domain 5: Notification (Implicit)**
- **Services**: `EmailService`
- **Features**:
  - OTP email notification
  - (Planned) Assignment notification to officers
- **Current State**: Only OTP emails implemented; officer notification is a TODO

---

## 3. Database Schema Analysis

### 3.1 Current Entities

```
┌─────────────────────┐         ┌─────────────────────┐
│       users         │         │    user_roles       │
├─────────────────────┤         ├─────────────────────┤
│ id (PK)             │────────<│ user_id (FK)        │
│ username (unique)   │         │ roles (enum)        │
│ email (unique)      │         └─────────────────────┘
│ password            │
│ fullname            │
└─────────────────────┘
         │
         │ userId
         ▼
┌─────────────────────────────────────────┐
│              grievances                  │
├─────────────────────────────────────────┤
│ id (PK)                                 │
│ title                                   │
│ description                             │
│ category                                │
│ subcategory                             │
│ location                                │
│ image_data (BLOB)                       │
│ image_type                              │
│ status (enum: PENDING/ASSIGNED/etc.)    │
│ user_id (FK)                            │
│ officer_id (FK to users)                │
│ priority                                │
│ deadline                                │
│ created_at                              │
│ resolution_note                         │
│ resolution_image_data (BLOB)            │
│ resolution_image_type                   │
│ reopen_image_data (BLOB)                │
│ reopen_image_type                       │
│ reopen_note                             │
│ resolved_at                             │
└─────────────────────────────────────────┘
         │
         │ grievanceId
         ▼
┌─────────────────────┐
│      feedback       │
├─────────────────────┤
│ id (PK)             │
│ grievance_id (FK)   │
│ user_id (FK)        │
│ rating (1-5)        │
│ comments (TEXT)     │
│ created_at          │
│ reopened            │
└─────────────────────┘

┌─────────────────────┐
│     otp_users       │
├─────────────────────┤
│ id (PK)             │
│ fullname            │
│ username            │
│ email (unique)      │
│ password            │
│ otp                 │
│ expiry_time         │
└─────────────────────┘
```

### 3.2 Data Relationships

| Relationship | Type | Description |
|--------------|------|-------------|
| User → Grievances | 1:N | A user (citizen) can submit multiple grievances |
| Officer → Grievances | 1:N | An officer can be assigned multiple grievances |
| Grievance → Feedback | 1:N | A grievance can have multiple feedback (one per user) |
| User → Feedback | 1:N | A user can give feedback on multiple grievances |

---

## 4. Proposed Microservices Architecture

### 4.1 Service Decomposition

```
                                ┌───────────────────┐
                                │   API Gateway     │
                                │   (Spring Cloud)  │
                                └─────────┬─────────┘
                                          │
         ┌────────────────────────────────┼────────────────────────────────┐
         │                                │                                │
         ▼                                ▼                                ▼
┌─────────────────┐            ┌─────────────────┐            ┌─────────────────┐
│ User Service    │            │ Grievance       │            │ Feedback        │
│                 │            │ Service         │            │ Service         │
├─────────────────┤            ├─────────────────┤            ├─────────────────┤
│ - Registration  │            │ - Submit        │            │ - Submit        │
│ - Login/Auth    │            │ - Assign        │            │ - Rate          │
│ - OTP Flow      │            │ - Update Status │            │ - Analytics     │
│ - Profile Mgmt  │◀──────────▶│ - Image Upload  │◀──────────▶│                 │
│ - Officer Mgmt  │   (Events) │ - Export        │   (Events) │                 │
└────────┬────────┘            └────────┬────────┘            └────────┬────────┘
         │                              │                              │
         ▼                              ▼                              ▼
    ┌─────────┐                   ┌─────────┐                   ┌─────────┐
    │ User DB │                   │Grievance│                   │Feedback │
    │(MySQL)  │                   │   DB    │                   │   DB    │
    └─────────┘                   └─────────┘                   └─────────┘
                                          │
                                          ▼
                                ┌─────────────────┐
                                │ File Storage    │
                                │ Service         │
                                │ (S3/MinIO)      │
                                └─────────────────┘
                                          
                    ┌─────────────────────────────────────┐
                    │         Message Broker              │
                    │    (RabbitMQ / Apache Kafka)        │
                    └─────────────────────────────────────┘
                                          │
                                          ▼
                                ┌─────────────────┐
                                │ Notification    │
                                │ Service         │
                                │ (Email/SMS)     │
                                └─────────────────┘
```

### 4.2 Proposed Microservices

| Service | Responsibility | Database | Key Technologies |
|---------|---------------|----------|------------------|
| **User Service** | User registration, authentication, OTP verification, JWT issuing, officer management | MySQL | Spring Security, JWT |
| **Grievance Service** | Grievance CRUD, assignment, status workflow, image handling | MySQL + Blob/S3 | Spring Data JPA |
| **Feedback Service** | Feedback collection, rating aggregation, analytics | MySQL | Spring Data JPA |
| **Notification Service** | Email/SMS notifications, OTP delivery | None (stateless) | Spring Mail, (future) SMS provider |
| **API Gateway** | Routing, authentication, rate limiting | Redis (cache) | Spring Cloud Gateway |
| **File Storage Service** | Image upload/download, CDN integration | S3/MinIO | Spring Cloud AWS |

### 4.3 Cross-Cutting Concerns

| Concern | Solution |
|---------|----------|
| Service Discovery | Spring Cloud Eureka or Consul |
| Configuration Management | Spring Cloud Config Server |
| Distributed Tracing | Zipkin / Jaeger with Spring Cloud Sleuth |
| Centralized Logging | ELK Stack (Elasticsearch, Logstash, Kibana) |
| Circuit Breaker | Resilience4j |
| API Documentation | OpenAPI 3.0 (Springdoc) |
| Monitoring | Prometheus + Grafana |

---

## 5. Migration Roadmap

### Phase 1: Foundation (4-6 weeks)

#### 1.1 Infrastructure Setup
- [ ] Set up CI/CD pipeline (GitHub Actions/Jenkins)
- [ ] Create Docker configurations for local development
- [ ] Set up Kubernetes cluster (or Docker Compose for dev)
- [ ] Configure container registry (DockerHub/ECR)

#### 1.2 Shared Libraries
- [ ] Extract common DTOs into a shared library
- [ ] Create common exception handling module
- [ ] Create shared security utilities (JWT validation)
- [ ] Create API response standards

#### 1.3 Database Preparation
- [ ] Audit current database schema
- [ ] Design service-specific schemas
- [ ] Plan data migration strategy
- [ ] Set up database per service (MySQL instances or schemas)

### Phase 2: API Gateway & User Service (4-6 weeks)

#### 2.1 API Gateway Implementation
- [ ] Set up Spring Cloud Gateway
- [ ] Configure routing rules
- [ ] Implement authentication passthrough
- [ ] Add rate limiting
- [ ] Add CORS configuration

#### 2.2 User Service Extraction
- [ ] Create new Spring Boot project for User Service
- [ ] Migrate User, Role, OtpUser entities
- [ ] Migrate AuthController, UserController, OfficerController
- [ ] Migrate UserService, AppUserDetailsService
- [ ] Migrate security configuration (JwtUtils, etc.)
- [ ] Migrate OTP/email functionality
- [ ] Create user-service-specific database
- [ ] Implement service-to-service authentication

### Phase 3: Grievance Service (4-6 weeks)

#### 3.1 Grievance Service Extraction
- [ ] Create new Spring Boot project for Grievance Service
- [ ] Migrate Grievance entity and Status enum
- [ ] Migrate GrievanceController, ComplaintsController
- [ ] Migrate GrievanceService, GrievanceRepository
- [ ] Create grievance-specific database
- [ ] Implement user validation via REST call or JWT

#### 3.2 File Storage Migration
- [ ] Set up object storage (S3/MinIO)
- [ ] Migrate image storage from BLOB to object storage
- [ ] Update image URLs to use storage service
- [ ] Create image upload/download APIs

### Phase 4: Feedback Service (2-3 weeks)

#### 4.1 Feedback Service Extraction
- [ ] Create new Spring Boot project for Feedback Service
- [ ] Migrate Feedback entity
- [ ] Migrate FeedbackController, FeedbackService
- [ ] Create feedback-specific database
- [ ] Implement grievance validation via REST call

### Phase 5: Notification Service (2-3 weeks)

#### 5.1 Notification Service Creation
- [ ] Create new Spring Boot project for Notification Service
- [ ] Migrate EmailService
- [ ] Set up message queue (RabbitMQ/Kafka)
- [ ] Implement async notification handling
- [ ] Add SMS notification support (optional)

#### 5.2 Event-Driven Integration
- [ ] Publish events from User Service (registration, OTP)
- [ ] Publish events from Grievance Service (assignment, status change)
- [ ] Subscribe to events in Notification Service

### Phase 6: Testing & Stabilization (3-4 weeks)

#### 6.1 Integration Testing
- [ ] Create end-to-end test suite
- [ ] Test all service interactions
- [ ] Load testing with realistic traffic
- [ ] Chaos engineering tests

#### 6.2 Documentation
- [ ] Update API documentation (OpenAPI)
- [ ] Create deployment runbooks
- [ ] Update developer onboarding guide

### Phase 7: Production Migration (2-4 weeks)

#### 7.1 Gradual Rollout
- [ ] Deploy services to staging
- [ ] Canary deployment to production
- [ ] Monitor metrics and logs
- [ ] Full production rollout
- [ ] Decommission monolith

---

## 6. Inter-Service Communication Strategy

### 6.1 Synchronous Communication (REST)

Used for:
- User validation (Grievance → User Service)
- Getting user details (Feedback → User Service)

```java
// Example: Grievance Service calling User Service
@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/api/internal/users/{id}")
    UserDTO getUserById(@PathVariable Long id);
    
    @GetMapping("/api/internal/users/validate")
    boolean validateUser(@RequestHeader("Authorization") String token);
}
```

### 6.2 Asynchronous Communication (Events)

Used for:
- Notifications (email, SMS)
- Analytics/reporting
- Cross-service data synchronization

```java
// Example: Publishing event when grievance is assigned
@Service
public class GrievanceService {
    private final ApplicationEventPublisher eventPublisher;
    
    public void assignGrievance(Long id, Long officerId) {
        // ... business logic
        eventPublisher.publishEvent(new GrievanceAssignedEvent(id, officerId));
    }
}
```

### 6.3 Event Types

| Event | Source | Consumers | Payload |
|-------|--------|-----------|---------|
| `UserRegistered` | User Service | Notification | userId, email |
| `OtpGenerated` | User Service | Notification | email, otp |
| `GrievanceCreated` | Grievance Service | Analytics | grievanceId, userId |
| `GrievanceAssigned` | Grievance Service | Notification | grievanceId, officerId |
| `GrievanceResolved` | Grievance Service | Notification, Feedback | grievanceId |
| `FeedbackSubmitted` | Feedback Service | Analytics | feedbackId, rating |

---

## 7. Security Considerations

### 7.1 Authentication Flow

```
┌────────┐     ┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│ Client │────▶│ API Gateway │────▶│ User Service │────▶│ Users DB    │
└────────┘     └─────────────┘     └──────────────┘     └─────────────┘
     │                │                    │
     │ 1. Login       │                    │
     │────────────────▶                    │
     │                │ 2. Validate        │
     │                │───────────────────▶│
     │                │                    │ 3. Check credentials
     │                │ 4. JWT Token       │◀───────────────────
     │◀───────────────│◀───────────────────│
     │                │                    │
     │ 5. Request with JWT                 │
     │────────────────▶                    │
     │                │ 6. Validate JWT    │
     │                │───────────────────▶│
     │                │ 7. Forward to service
     │                │──────────────────────────────▶ (Grievance/Feedback)
```

### 7.2 Service-to-Service Authentication

- **Option 1**: JWT token passthrough (current user's token)
- **Option 2**: Service account tokens with internal scope
- **Option 3**: mTLS (mutual TLS) for internal communication

### 7.3 Security Improvements

- [ ] Externalize JWT secret to configuration server
- [ ] Implement token refresh mechanism
- [ ] Add OAuth2/OIDC support (optional)
- [ ] Implement API key authentication for external integrations

---

## 8. Data Migration Strategy

### 8.1 Database Per Service

| Service | Tables | Migration Priority |
|---------|--------|-------------------|
| User Service | `users`, `user_roles`, `otp_users` | High |
| Grievance Service | `grievances` | High |
| Feedback Service | `feedback` | Medium |

### 8.2 Migration Steps

1. **Schema Extraction**
   - Export schema definitions per service
   - Create service-specific Flyway migrations

2. **Data Migration**
   - Use ETL process for initial data migration
   - Implement dual-write during transition period
   - Validate data consistency

3. **Foreign Key Handling**
   - Replace foreign keys with service calls
   - Store only IDs across service boundaries
   - Implement eventual consistency patterns

---

## 9. Technology Recommendations

### 9.1 Spring Cloud Components

```xml
<dependencies>
    <!-- API Gateway -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    
    <!-- Service Discovery -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    
    <!-- Config Server -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-config</artifactId>
    </dependency>
    
    <!-- Circuit Breaker -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
    </dependency>
    
    <!-- Feign Client -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
</dependencies>
```

### 9.2 Containerization

```dockerfile
# Example Dockerfile for each service
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 9.3 Docker Compose (Development)

```yaml
version: '3.8'
services:
  eureka-server:
    image: civicpulse/eureka-server
    ports:
      - "8761:8761"
  
  config-server:
    image: civicpulse/config-server
    ports:
      - "8888:8888"
  
  api-gateway:
    image: civicpulse/api-gateway
    ports:
      - "8080:8080"
    depends_on:
      - eureka-server
  
  user-service:
    image: civicpulse/user-service
    depends_on:
      - eureka-server
      - user-db
  
  grievance-service:
    image: civicpulse/grievance-service
    depends_on:
      - eureka-server
      - grievance-db
  
  feedback-service:
    image: civicpulse/feedback-service
    depends_on:
      - eureka-server
      - feedback-db
  
  user-db:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: user_db
      MYSQL_ROOT_PASSWORD: secret
  
  grievance-db:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: grievance_db
      MYSQL_ROOT_PASSWORD: secret
  
  feedback-db:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: feedback_db
      MYSQL_ROOT_PASSWORD: secret
  
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
```

---

## 10. Estimated Timeline

| Phase | Duration | Effort |
|-------|----------|--------|
| Phase 1: Foundation | 4-6 weeks | 2-3 developers |
| Phase 2: API Gateway & User Service | 4-6 weeks | 2-3 developers |
| Phase 3: Grievance Service | 4-6 weeks | 2-3 developers |
| Phase 4: Feedback Service | 2-3 weeks | 1-2 developers |
| Phase 5: Notification Service | 2-3 weeks | 1-2 developers |
| Phase 6: Testing & Stabilization | 3-4 weeks | Full team |
| Phase 7: Production Migration | 2-4 weeks | Full team + DevOps |
| **Total** | **21-32 weeks** | |

---

## 11. Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Data inconsistency during migration | High | Implement dual-write, thorough testing |
| Service communication failures | Medium | Circuit breakers, retries, fallbacks |
| Performance degradation | Medium | Caching, async processing, monitoring |
| Team learning curve | Medium | Training, documentation, pair programming |
| Increased operational complexity | High | Proper monitoring, alerting, runbooks |

---

## 12. Success Metrics

- **Deployment frequency**: Ability to deploy services independently
- **Mean time to recovery**: Faster recovery from failures
- **Service availability**: 99.9% uptime per service
- **Response time**: < 200ms for 95th percentile
- **Error rate**: < 0.1% for all APIs

---

## 13. Next Steps

1. **Review and approve this roadmap** with stakeholders
2. **Set up development environment** with Docker/K8s
3. **Create GitHub repository structure** for microservices
4. **Begin Phase 1** - Infrastructure and shared libraries
5. **Establish code review and testing practices**

---

## Appendix A: API Endpoints Summary

### Current Monolith Endpoints

| Method | Endpoint | Service (Proposed) |
|--------|----------|-------------------|
| POST | `/api/auth/register` | User Service |
| POST | `/api/auth/login` | User Service |
| GET | `/api/user/me` | User Service |
| POST | `/api/simple/register` | User Service |
| POST | `/api/simple/verify-otp` | User Service |
| POST | `/api/simple/resend-otp` | User Service |
| POST | `/api/officers` | User Service |
| GET | `/api/officers` | User Service |
| POST | `/api/grievances` | Grievance Service |
| GET | `/api/grievances/me` | Grievance Service |
| GET | `/api/grievances/{id}/image` | Grievance Service / File Service |
| PUT | `/api/grievances/{id}/image` | Grievance Service / File Service |
| PUT | `/api/grievances/{id}/reopen` | Grievance Service |
| GET | `/api/grievances/export` | Grievance Service |
| GET | `/api/complaints` | Grievance Service |
| GET | `/api/complaints/{id}` | Grievance Service |
| PUT | `/api/complaints/{id}/assign` | Grievance Service |
| PUT | `/api/complaints/{id}/update` | Grievance Service |
| GET | `/api/complaints/user/{userId}` | Grievance Service |
| GET | `/api/complaints/officer/me` | Grievance Service |
| GET | `/api/complaints/officer/{id}` | Grievance Service |
| POST | `/api/feedback` | Feedback Service |
| GET | `/api/feedback/complaint/{id}` | Feedback Service |
| GET | `/api/feedback` | Feedback Service |

---

## Appendix B: Team Roles

| Role | Responsibility |
|------|----------------|
| **Technical Lead** | Architecture decisions, code reviews |
| **Backend Developers** | Service implementation |
| **DevOps Engineer** | CI/CD, Kubernetes, monitoring |
| **QA Engineer** | Testing strategy, automation |
| **Database Administrator** | Schema design, migration |

---

*Document Version: 1.0*  
*Last Updated: 2026-02-08*  
*Author: CivicPulse Team*
