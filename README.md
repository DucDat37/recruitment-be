# Recruitment BE

Backend của hệ thống tuyển dụng tích hợp AI phân tích CV và gợi ý việc làm (đồ án tốt nghiệp).

## Tài liệu dự án

Tài liệu dùng chung cho Backend, Frontend, AI và thiết kế nằm tại [recruitment-docs](https://github.com/yopaz-ducdt/recruitment-docs). Xem [đặc tả API](https://github.com/yopaz-ducdt/recruitment-docs/blob/main/docs/api_specifications.md), [kiến trúc hệ thống](https://github.com/yopaz-ducdt/recruitment-docs/blob/main/docs/system_architecture.md), [database schema](https://github.com/yopaz-ducdt/recruitment-docs/blob/main/docs/database_schema.md) và [roadmap](https://github.com/yopaz-ducdt/recruitment-docs/blob/main/docs/project_roadmap_and_decisions.md).

## Công nghệ sử dụng

- Java 21, Spring Boot 4.1.1
- Spring Web MVC, Spring Data JPA (Hibernate 7), Spring Security, Spring Validation
- MySQL 8.0, Flyway (quản lý migration)
- JWT với thư viện jjwt 0.12.6, mã hóa mật khẩu bằng BCrypt
- Gửi email qua Gmail SMTP
- springdoc-openapi (Swagger UI)
- Lombok

## Yêu cầu môi trường

- JDK 21
- MySQL 8.0.16 trở lên
- Maven không cần cài sẵn, dự án có sẵn `mvnw`
- Tài khoản Gmail và mật khẩu ứng dụng (App Password), nếu muốn gửi email thật

## Cài đặt và chạy

**1. Tạo database**

```sql
CREATE DATABASE recruitment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Các bảng được tạo tự động khi app khởi động, nhờ Flyway chạy migration `src/main/resources/db/migration/V1__init.sql`.

**2. Cấu hình biến môi trường**

Cấu hình nằm trong `src/main/resources/application.yaml`. Các giá trị nhạy cảm được đọc từ biến môi trường:

| Biến | Ý nghĩa | Mặc định |
|---|---|---|
| `DB_USERNAME` | Tài khoản MySQL | `root` |
| `DB_PASSWORD` | Mật khẩu MySQL | **Bắt buộc**, không có giá trị mặc định |
| `JWT_SECRET` | Khóa ký JWT, dạng base64, tối thiểu 256 bit (ví dụ tạo bằng `openssl rand -base64 48`) | **Bắt buộc**, không có giá trị mặc định |
| `MAIL_USERNAME` | Địa chỉ Gmail dùng để gửi email | Trống |
| `MAIL_PASSWORD` | Mật khẩu ứng dụng của Gmail | Trống |
| `APP_MAIL_MODE` | `smtp` để gửi email thật, `log` để chỉ ghi nội dung ra console | `smtp` |
| `APP_FRONTEND_URL` | Địa chỉ frontend, dùng để tạo link trong email xác thực và đặt lại mật khẩu | `http://localhost:5173` |
| `APP_CORS_ALLOWED_ORIGINS` | Các origin được phép gọi API từ trình duyệt, cách nhau bằng dấu phẩy, không có `/` ở cuối | `http://localhost:5173` |

Trên IntelliJ, đặt biến môi trường trong **Run → Edit Configurations → Environment variables**, các biến cách nhau bằng dấu chấm phẩy:

```
MAIL_USERNAME=your.account@gmail.com;MAIL_PASSWORD=xxxx xxxx xxxx xxxx
```

**3. Chạy ứng dụng**

```bash
./mvnw spring-boot:run
```

Server chạy tại `http://localhost:8080`. Swagger UI nằm tại `http://localhost:8080/swagger-ui.html`.

## Xác thực và phân quyền

- Access token (JWT) có hạn 15 phút, gửi trong header `Authorization: Bearer <token>`.
- Refresh token có hạn 14 ngày, được xoay vòng mỗi lần làm mới. Nếu một refresh token đã thu hồi bị gửi lại, toàn bộ phiên của người dùng đó sẽ bị thu hồi.
- Các vai trò: `CANDIDATE`, `RECRUITER`, `ADMIN`. Không thể đăng ký vai trò `ADMIN` qua API.
- Đăng nhập sai 5 lần liên tiếp sẽ khóa tạm thời tài khoản 15 phút.
- Người dùng phải xác thực email trước khi đăng nhập.
- Token xác thực email có hạn 24 giờ, token đặt lại mật khẩu có hạn 30 phút. Mỗi token chỉ dùng được một lần.

## API hiện có (giai đoạn 1)

**Công khai**

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/auth/register` | Đăng ký tài khoản, gửi email xác thực |
| POST | `/api/auth/verify-email` | Xác thực email bằng token |
| POST | `/api/auth/resend-verification` | Gửi lại email xác thực |
| POST | `/api/auth/login` | Đăng nhập, trả về access token và refresh token |
| POST | `/api/auth/refresh` | Làm mới cặp token |
| POST | `/api/auth/logout` | Đăng xuất (thu hồi refresh token) |
| POST | `/api/auth/forgot-password` | Gửi email đặt lại mật khẩu |
| POST | `/api/auth/reset-password` | Đặt lại mật khẩu bằng token |

**Yêu cầu đăng nhập**

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/me` | Thông tin tài khoản hiện tại |
| POST | `/api/me/password` | Đổi mật khẩu |
| POST | `/api/me/logout-all` | Đăng xuất khỏi mọi thiết bị |

**Chỉ ADMIN**

| Method | Endpoint | Mô tả |
|---|---|---|
| PATCH | `/api/admin/users/{userId}/status` | Đổi trạng thái tài khoản (`ACTIVE`, `LOCKED`, `DISABLED`) |

## Định dạng lỗi

Mọi lỗi trả về JSON theo cấu trúc sau:

```json
{ "code": "EMAIL_ALREADY_EXISTS", "message": "Email đã được sử dụng", "fields": null }
```

- `code`: mã lỗi để frontend xử lý.
- `message`: mô tả bằng tiếng Việt.
- `fields`: chỉ có khi lỗi validation, chứa tên trường và thông báo tương ứng.

## Cấu trúc thư mục

```
src/main/java/com/example/recruitment/
├── common/        Tiện ích dùng chung (TokenUtils)
├── config/        Cấu hình Security, OpenAPI, AppProperties
├── controller/    REST controller
├── dto/           Request và response (auth, admin)
├── entity/        Entity JPA và enum
├── exception/     ApiException và xử lý lỗi toàn cục
├── mapper/        Mapping giữa entity và DTO
├── repository/    Spring Data JPA repository
├── security/      JWT service và filter xác thực
└── service/       Nghiệp vụ, gửi email
```

Migration CSDL nằm tại `src/main/resources/db/migration/`.

## Lưu ý bảo mật

- Không commit mật khẩu database, `JWT_SECRET` hoặc `MAIL_PASSWORD` lên Git. `application.yaml` chỉ đọc các giá trị này từ biến môi trường. Nếu thiếu `DB_PASSWORD` hoặc `JWT_SECRET`, app sẽ không khởi động.
- Khi `APP_MAIL_MODE=log`, log sẽ chứa link có token xác thực. Chỉ dùng chế độ này khi phát triển.

## Quy chuẩn Git & Đa Tác Nhân AI (Agent Rules)

- **Quy trình Git & Bắt buộc tạo PR**: **Tuyệt đối không commit trực tiếp vào nhánh `main`**. Mọi tính năng, sửa lỗi hay cập nhật cấu hình phải tạo nhánh riêng (`feat/<tên>`, `fix/<tên>`, `chore/<tên>`) và tạo **Pull Request (PR)** để được review và kiểm tra trước khi merge vào `main`.
- **Ngôn ngữ bắt buộc**: **Tiếng Anh 100%** cho mã nguồn Java, tên class/method/variable, chú thích code (Javadoc, inline comments), và Git commit messages theo chuẩn Conventional Commits.
- **Tài liệu quy chuẩn cho AI Coding Agents**:
  - [`AGENTS.md`](file:///home/ducdt/Other/recruitment-be/AGENTS.md): Quy chuẩn trung tâm bắt buộc cho mọi AI Agent làm việc trên BE.
  - [`CODEX.md`](file:///home/ducdt/Other/recruitment-be/CODEX.md): Chỉ dẫn dành riêng cho OpenAI Codex CLI.
  - [`CLAUDE.md`](file:///home/ducdt/Other/recruitment-be/CLAUDE.md): Chỉ dẫn dành riêng cho Claude Code.
  - [`.cursorrules`](file:///home/ducdt/Other/recruitment-be/.cursorrules): Cấu hình ngữ cảnh cho Cursor IDE.
  - [`.github/copilot-instructions.md`](file:///home/ducdt/Other/recruitment-be/.github/copilot-instructions.md): Hướng dẫn cho GitHub Copilot.
  - [`.gemini/rules/be-agents-rules.md`](file:///home/ducdt/Other/recruitment-be/.gemini/rules/be-agents-rules.md): Hướng dẫn cho Google Antigravity / Gemini.

## Trạng thái phát triển

- [x] Giai đoạn 1: xác thực và phân quyền (đã cài đặt, đã kiểm thử đăng ký, xác thực email, đăng nhập và `/api/me`)
- [ ] Giai đoạn 2 đến 11: chưa bắt đầu

CORS chỉ cho phép các origin trong `APP_CORS_ALLOWED_ORIGINS` gọi `/api/**`. Khi deploy, đặt biến này và `APP_FRONTEND_URL` bằng domain thật của frontend.
