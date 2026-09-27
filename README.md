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
├── security
├── controller
├── dto
│   ├── auth
│   ├── common
│   └── farm
├── service
│   └── impl
├── repository
├── mapper
├── entity
├── exception
└── util
```

Các module UC cần phát triển tiếp: `auth-profile`, `farm-registration`, `farm-members`,
`production-areas`, `plots`, `environmental-assessments`, `crop-seasons`, `tasks`,
`farming-activities`, `materials`, `purchases`, `expenses`, `harvest-traceability`,
`training`, `vietgap-checklists`, `internal-assessments`, `reports`, `ai`, `admin`.

## Chạy local (Development)

Có thể chạy trực tiếp bằng Maven Wrapper đi kèm dự án (không cần cài đặt Maven trước):

**Trên Windows (PowerShell / Command Prompt):**
```powershell
.\mvnw.cmd spring-boot:run
```

**Trên Linux / macOS (Bash):**
```bash
./mvnw spring-boot:run
```

*(Hoặc dùng lệnh `mvn spring-boot:run` nếu máy đã cài Maven sẵn)*

Local mặc định sử dụng H2 in-memory:
- **Swagger UI (Interactive API Docs & Test):** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **H2 Console:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:mycropdiary`, User: `sa`, Password: *(trống)*)
- **Health check:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

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
