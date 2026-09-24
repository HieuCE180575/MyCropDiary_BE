# MyCropDiary Backend API

Khung Spring Boot theo UC và package diagram của MyCropDiary.

## Công nghệ

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC, Spring Security, Spring Data JPA, Validation
- SQL Server cho môi trường thật; H2 cho chạy khung local

## Cấu trúc package

```text
com.mycropdiary.api

├── config
│
├── security
│
├── controller
│   ├── auth
│   ├── farm
│   ├── farmregistration
│   ├── farmmember
│   ├── productionarea
│   ├── plot
│   ├── environmentalassessment
│   ├── cropseason
│   ├── task
│   ├── farmingactivity
│   ├── material
│   ├── purchase
│   ├── expense
│   ├── harvesttraceability
│   ├── training
│   ├── vietgapchecklist
│   ├── internalassessment
│   ├── knowledge
│   ├── report
│   ├── ai
│   └── admin
│
├── dto
│   ├── auth
│   ├── common
│   ├── farm
│   ├── farmregistration
│   ├── farmmember
│   ├── productionarea
│   ├── plot
│   ├── cropseason
│   ├── farmingactivity
│   ├── material
│   ├── purchase
│   ├── expense
│   ├── harvesttraceability
│   ├── knowledge
│   ├── report
│   └── ai
│
├── service
│   ├── auth
│   │   └── AuthService.java
│   │
│   ├── farm
│   │   └── FarmService.java
│   │
│   ├── farmregistration
│   │   └── FarmRegistrationService.java
│   │
│   ├── knowledge
│   │   └── KnowledgeService.java
│   │
│   ├── expense
│   │   └── ExpenseService.java
│   │
│   └── impl
│       ├── AuthServiceImpl.java
│       ├── FarmServiceImpl.java
│       ├── FarmRegistrationServiceImpl.java
│       ├── KnowledgeServiceImpl.java
│       └── ExpenseServiceImpl.java
│
├── repository
│   ├── auth
│   ├── farm
│   ├── farmregistration
│   ├── farmmember
│   ├── productionarea
│   ├── plot
│   ├── cultivation
│   ├── expense
│   └── knowledge
│
├── mapper
│
├── entity
│   ├── account
│   ├── farm
│   ├── cultivation
│   ├── knowledge
│   ├── expense
│   └── common
│
├── exception
│
└── util
```

Các module UC cần phát triển tiếp: `auth-profile`, `farm-registration`, `farm-members`,
`production-areas`, `plots`, `environmental-assessments`, `crop-seasons`, `tasks`,
`farming-activities`, `materials`, `purchases`, `expenses`, `harvest-traceability`,
`training`, `vietgap-checklists`, `internal-assessments`, `reports`, `ai`, `admin`.

## Chạy local

```bash
mvn spring-boot:run
```

Local sử dụng H2 in-memory. Health check: `GET http://localhost:8080/actuator/health`.

## Chạy với SQL Server

Tạo database bằng script `MyCropDiary_SQLServer.sql`, sau đó đặt biến môi trường và chạy:

```bash
export SPRING_PROFILES_ACTIVE=sqlserver
export DB_URL='jdbc:sqlserver://localhost:1433;databaseName=MyCropDiary;encrypt=true;trustServerCertificate=true'
export DB_USERNAME='sa'
export DB_PASSWORD='your-password'
mvn spring-boot:run
```

## Lưu ý

- HTTP Basic chỉ là cơ chế tạm thời để project skeleton chạy được; thay bằng JWT trước khi làm nghiệp vụ.
- Mọi API thuộc trang trại phải kiểm tra `FarmMember` và `StaffAreaAssignment`.
- Dữ liệu AI chỉ được lấy trong phạm vi farm/production area người dùng có quyền truy cập.
- Không tự động ghi bản nháp AI vào nhật ký sản xuất nếu người dùng chưa xác nhận.
