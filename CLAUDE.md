# Spring React Framework — AI Assistant Guide

## What This Is
A reusable Spring Boot 3.4.1 backend framework with JWT authentication, designed to pair with the React dashboard frontend (`react_framework`). Provides auth, role-based menus, dynamic CRUD, WebSocket support, and audit trails.

## Architecture

```
src/main/java/com/framework/
  config/
    SecurityConfig.java        — SecurityFilterChain: stateless, no CSRF, JWT filter
    CorsConfig.java            — allowCredentials + allowedOriginPatterns("*")
    JacksonConfig.java         — JavaTimeModule, UTC, ISO dates
    WebSocketConfig.java       — Raw WS at /ws (no STOMP)
  security/
    JwtTokenProvider.java      — Generate/validate JWT (jjwt 0.12.6, HMAC-SHA256)
    JwtAuthenticationFilter.java — OncePerRequestFilter, reads "token" cookie
  controller/
    AuthController.java        — POST/GET /auth, POST /auth/logout, POST /auth/permission
    MenuController.java        — GET /menu, GET /adminMenu
    DynamicCrudController.java — GET/POST/PUT/DELETE /{table}/{id}
  model/
    User.java                  — @JsonProperty("first_name"), password hashing, role checks
    Role.java                  — Simple name + description
    MenuItem.java              — @MappedSuperclass (order, name, link, icon, parentId, roles)
    MainMenuItem.java          — main_menu_item table
    AdminMenuItem.java         — admin_menu_item table
  repository/                  — JPA repos: UserRepository, RoleRepository, MainMenuRepository, AdminMenuRepository
  service/
    AuthService.java           — authenticate(email, password), getUserByEmail
    CustomUserDetailsService.java — Spring Security UserDetailsService
    MenuService.java           — Role-filtered hierarchical menu tree builder
    DynamicCrudService.java    — JdbcTemplate CRUD with table whitelist + audit fields
  dto/
    AuthRequest.java           — { username, password }
    AuthResponse.java          — { user: { first_name, last_name, user_name, email, roles } }
    MenuItemDto.java           — { name, link, icon, roles, items: [] }
  exception/
    GlobalExceptionHandler.java — Maps exceptions to { error: "..." } JSON
    ErrorResponse.java         — { error, message }
  websocket/
    FrameworkWebSocketHandler.java  — TextWebSocketHandler, override processMessage()
    WebSocketSessionManager.java   — Thread-safe ConcurrentHashMap session store
```

## Key Conventions

- **No response wrapping**: The server returns raw JSON. The React frontend's `ApiService.js` wraps responses client-side as `{ success, code, data }`.
- **Error format**: Error responses MUST include an `error` field: `{ "error": "Expired" }` or `{ "error": "Unauthorized" }`. The frontend checks `e.data?.error`.
- **JWT via cookie**: Token stored in HttpOnly cookie named `"token"`. Frontend sends `credentials: 'include'`.
- **User JSON fields**: Use `@JsonProperty("first_name")`, `@JsonProperty("last_name")`, `@JsonProperty("user_name")` — frontend reads these exact names.
- **Password never exposed**: `@JsonIgnore` on `password` and `passwordSalt`.
- **Menu items must have `items: []`**: Frontend checks `items.length > 0`, so `MenuItemDto.items` is always initialized.
- **Dynamic CRUD tables**: Whitelist configured in `application.yml` under `app.crud.allowed-tables`. Table names validated against reserved paths and regex `^[a-zA-Z0-9_-]+$`.
- **Audit fields**: `DynamicCrudService` auto-injects `created_at`, `updated_at`, `created_by`, `updated_by` if the table has those columns.
- **Profiles**: `dev` (H2 in-memory), `prod` (MySQL via env vars). Active profile set in `application.yml`.

## API Endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/auth` | No | Login → returns user + sets JWT cookie |
| GET | `/auth` | Yes | Validate token → returns current user |
| POST | `/auth/logout` | Yes | Clear JWT cookie |
| POST | `/auth/permission` | Yes | Check roles: `{ nodes: ["Admin"] }` → `{ "Admin": true }`. Frontend's `HasRole` component uses this. |
| GET | `/menu` | Yes | Main menu tree filtered by user roles |
| GET | `/adminMenu` | Yes | Admin menu tree filtered by user roles |
| GET | `/{table}` | Yes | List all records |
| GET | `/{table}/{id}` | Yes | Get single record |
| POST | `/{table}` | Yes | Create record (auto audit fields) |
| PUT | `/{table}/{id}` | Yes | Update record (auto audit fields) |
| DELETE | `/{table}/{id}` | Yes | Delete record |
| GET | `/{table}/{id}/sec/{csv_ids}` | Yes | Update many-to-many associations |
| WS | `/ws` | No | WebSocket (JSON: `{ cmd, data, success }`) |

## How to Add a New CRUD Table

1. Create the table in `data.sql` (or via migration):
```sql
CREATE TABLE IF NOT EXISTS my_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);
```

2. Add to allowed tables in `application.yml`:
```yaml
app:
  crud:
    allowed-tables:
      - example_table
      - my_table
```

3. The frontend can now use `DynamicSQLDataGrid` with `table="my-table"`.

## How to Add a New Menu Item

Insert into `main_menu_item` or `admin_menu_item` in `data.sql`:
```sql
INSERT INTO main_menu_item (id, item_order, name, link, icon, parent_id, roles)
VALUES (3, 3, 'My Page', '/my-page', 'Description', NULL, NULL);
```
- `parent_id`: Set to another item's `id` for sub-menu nesting
- `roles`: Comma-separated (e.g., `'Admin,User'`). NULL = visible to all.
- `icon`: MUI icon name string (e.g., `'Dashboard'`, `'People'`, `'TableChart'`)

## How to Add Role-Protected Endpoints

Use Spring Security's `@PreAuthorize` on any controller method:
```java
@PreAuthorize("hasRole('Admin')")
@GetMapping("/admin/settings")
public ResponseEntity<...> getSettings() { ... }
```

## How to Extend WebSocket Behavior

Override `processMessage()` in a subclass of `FrameworkWebSocketHandler`:
```java
@Component
public class MyWebSocketHandler extends FrameworkWebSocketHandler {
    @Override
    protected Map<String, Object> processMessage(String cmd, Object data, WebSocketSession session) {
        if ("my-command".equals(cmd)) {
            return Map.of("cmd", cmd, "success", true, "data", myService.process(data));
        }
        return super.processMessage(cmd, data, session);
    }
}
```

## Do NOT

- Wrap responses in `{ success, code, data }` — the frontend does this client-side
- Use `allowedOrigins("*")` in CORS — must use `allowedOriginPatterns("*")` with credentials
- Return error responses without an `error` field — frontend checks `e.data?.error`
- Use `spring-boot-starter-security` defaults (sessions, CSRF) — this is stateless JWT only
- Put table names directly in SQL without validation — always go through `DynamicCrudService.validateTable()`
- Expose `password` or `passwordSalt` in JSON — keep `@JsonIgnore`

## CMS Module

The CMS adds content management capabilities: site settings, page templates, pages, and file uploads.

### CMS Architecture

```
controller/CmsController.java       — /cms endpoints (settings, render, publish, reorder, upload)
service/CmsService.java             — Settings CRUD, page rendering, publishing, menu reorder
service/FileStorageService.java     — File upload/delete, stored in ./uploads/YYYY/MM/
config/WebConfig.java               — Serves /uploads/** as static resources
model/SiteSetting.java              — Key-value site settings entity
model/CmsTemplate.java              — Page template entity (JSON structure)
model/CmsPage.java                  — Content page entity (JSON content)
model/CmsFile.java                  — Uploaded file registry entity
repository/SiteSettingRepository.java — findBySettingKey
repository/CmsTemplateRepository.java — findByIsActiveTrue
repository/CmsPageRepository.java   — findBySlug, findByStatus
repository/CmsFileRepository.java
```

### CMS API Endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/cms/settings` | Yes | All settings as key-value map |
| PUT | `/cms/settings` | Admin | Bulk upsert settings |
| GET | `/cms/settings/{key}` | Yes | Single setting value |
| GET | `/cms/pages/{slug}/render` | **No** | Assembled page for public view |
| PUT | `/cms/pages/{id}/publish` | Admin | Publish page |
| PUT | `/cms/pages/{id}/unpublish` | Admin | Unpublish page |
| PUT | `/cms/menu/{type}/reorder` | Admin | Batch reorder menu items |
| POST | `/cms/upload` | Admin | Multipart file upload |
| DELETE | `/cms/upload/{id}` | Admin | Delete uploaded file |

### Template Structure JSON

```json
{ "sections": [{ "key": "hero", "label": "Hero Section", "blocks": [{ "key": "title", "type": "text", "label": "Title", "required": true }] }] }
```

Block types: `text`, `richtext`, `image`, `html`.

### Page Content JSON

Flat key-value: `{ "sectionKey.blockKey": "value" }`, e.g., `{ "hero.title": "Welcome" }`.

### Reserved Paths

`DynamicCrudService.reservedPaths` includes `"cms"` and `"uploads"` to prevent conflicts with `/{table}` routes.

## Database Tables

| Table | Purpose |
|-------|---------|
| `app_user` | Users (id, user_name, first_name, last_name, email, password, password_salt) |
| `app_role` | Roles (id, name, description) |
| `user_role` | User-Role junction (user_id, role_id) |
| `main_menu_item` | Main sidebar menu (id, item_order, name, link, icon, parent_id, roles) |
| `admin_menu_item` | Admin menu section (same schema as main_menu_item) |
| `site_setting` | CMS key-value settings (setting_key UNIQUE, setting_value, setting_type) |
| `cms_template` | CMS page templates (name, description, structure TEXT, is_active) |
| `cms_page` | CMS pages (title, slug UNIQUE, template_id FK, content TEXT, status) |
| `cms_file` | CMS uploaded files (original_name, stored_name, file_path, mime_type, file_size) |

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `app.jwt.secret` | (base64 key) | HMAC-SHA256 signing key. **Change in production.** |
| `app.jwt.expiration-ms` | 14400000 | Token lifetime (4 hours) |
| `app.crud.allowed-tables` | [example_table, site_setting, cms_template, cms_page, cms_file, main_menu_item, admin_menu_item] | Whitelist for dynamic CRUD. |
| `app.upload.dir` | ./uploads | File upload storage directory |
| `app.upload.max-size` | 10MB | Maximum upload file size |
| `app.upload.allowed-types` | image/png,image/jpeg,image/gif,image/svg+xml,image/webp | Allowed MIME types |
| `spring.profiles.active` | dev | `dev` = H2, `prod` = MySQL |

## Seed Data (dev profile)

- **Admin user**: `admin@localhost` / `admin` with role `Admin`
- **Main menu**: Dashboard (`/`), Example CRUD (`/example/crud`)
- **Admin menu**: Administration > Users, Roles; CMS > Site Settings, Menus, Templates, Pages
- **Example table**: 2 sample records with audit fields
- **CMS defaults**: site_name=Dashboard, site_logo=/logo.svg, 2 starter templates, 1 sample page
