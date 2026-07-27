# BizFlow POS REST API Backend

PHP 8+ REST API for the BizFlow POS desktop sync system.
Zero composer dependencies — manual JWT HS256, PDO/MySQL.

---

## Requirements

| Requirement | Version |
|-------------|---------|
| PHP         | 8.0+    |
| MySQL       | 5.7+ / MariaDB 10.4+ |
| Apache / Nginx | with mod_rewrite / try_files |
| php-pdo     | enabled |
| php-pdo_mysql | enabled |
| php-json    | enabled |
| php-mbstring | enabled |

---

## Quick Start

### 1. Create the database

```sql
CREATE DATABASE retail_pos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Import the schema and seed data

```bash
mysql -u root -p retail_pos < backend/sql/schema.sql
mysql -u root -p retail_pos < backend/sql/seed.sql
```

### 3. Configure environment variables

Set these variables in your web server config (Apache VirtualHost or `.env` loaded by the server), **not** in committed files:

| Variable    | Default       | Description               |
|-------------|---------------|---------------------------|
| `DB_HOST`   | `localhost`   | MySQL hostname             |
| `DB_PORT`   | `3306`        | MySQL port                 |
| `DB_NAME`   | `retail_pos`  | Database name              |
| `DB_USER`   | `root`        | Database username          |
| `DB_PASS`   | *(empty)*     | Database password          |
| `JWT_SECRET`| *(insecure default)* | **Change this in production!** |
| `REQUIRE_AUTH` | `true` | Require a valid JWT for sync endpoints |

**Apache VirtualHost example:**
```apache
<VirtualHost *:80>
    ServerName pos.example.com
    DocumentRoot /var/www/pos/backend

    SetEnv DB_HOST     localhost
    SetEnv DB_NAME     retail_pos
    SetEnv DB_USER     pos_user
    SetEnv DB_PASS     supersecret
    SetEnv JWT_SECRET  your-very-long-random-secret-here

    <Directory /var/www/pos/backend>
        AllowOverride All
        Require all granted
    </Directory>
</VirtualHost>
```

**Nginx example (with php-fpm):**
```nginx
server {
    listen 80;
    server_name pos.example.com;
    root /var/www/pos/backend;
    index index.php;

    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }

    location ~ \.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/run/php/php8.2-fpm.sock;
        fastcgi_param JWT_SECRET "your-very-long-random-secret-here";
        fastcgi_param DB_PASS "supersecret";
    }
}
```

### 4. Set directory permissions

```bash
# Backups directory must be writable by the web server
mkdir -p backend/backups
chmod 750 backend/backups
chown www-data:www-data backend/backups
```

---

## Default Credentials

| Username | Password    | Role  |
|----------|-------------|-------|
| admin    | admin123    | ADMIN |
| cashier  | cashier123  | CASHIER |

⚠️ **Change both passwords immediately after first login.**

---

## File Structure

```
backend/
├── .htaccess                    URL rewriting
├── index.php                    Router & dispatcher
├── config/
│   ├── config.php               JWT secret, app constants
│   └── database.php             PDO singleton
├── middleware/
│   ├── AuthMiddleware.php       JWT verification, role checks
│   └── CorsMiddleware.php       CORS headers
├── controllers/
│   ├── AuthController.php       Login / refresh / logout
│   ├── ProductController.php    Product CRUD
│   ├── SaleController.php       Sales CRUD
│   ├── CustomerController.php   Customer CRUD
│   ├── SupplierController.php   Supplier CRUD
│   ├── PurchaseOrderController.php  PO CRUD
│   ├── InventoryController.php  Stock movements
│   ├── UserController.php       User management (admin)
│   ├── SyncController.php       Bulk sync upload / download
│   ├── SettingsController.php   App settings
│   └── BackupController.php     Backup file upload
├── helpers/
│   ├── JwtHelper.php            Manual HS256 JWT
│   ├── Response.php             JSON response helpers
│   └── Validator.php            Input validation
└── sql/
    ├── schema.sql               Full MySQL schema
    └── seed.sql                 Default users, categories, settings
```

---

## API Reference

### Authentication

All protected endpoints require:
```
Authorization: Bearer <token>
```

#### POST /api/auth/login
```json
{ "username": "admin", "password": "admin123" }
```
Response:
```json
{
  "access_token": "eyJ...",
  "token_type": "Bearer",
  "expires_in": 86400,
  "user": { "id": "...", "username": "admin", "full_name": "Administrator", "role": "ADMIN" }
}
```

#### POST /api/auth/refresh
Pass the current token in the body or Authorization header.

#### POST /api/auth/logout
Requires auth. Writes audit log.

---

### Incremental Sync

All list endpoints accept `?since=ISO8601_TIMESTAMP` for incremental sync:

```
GET /api/products?since=2024-01-01T00:00:00Z
GET /api/sales?since=2024-06-01T12:00:00Z
```

### Bulk Sync

#### POST /api/sync/upload
```json
{
  "entity_type": "products",
  "records": [
    { "id": "uuid", "name": "Widget", "sku": "WGT-001", "selling_price": 9.99, "updated_at": "2024-01-01T12:00:00Z" }
  ]
}
```
Response:
```json
{ "success": 1, "conflicts": [], "errors": [] }
```

Conflict resolution: if the server record has a **newer** `updated_at`, the client version is discarded and reported in `conflicts`.

#### GET /api/sync/download/{entity_type}?since=ISO_TIMESTAMP
Returns all records of that type updated after `since`.

Supported entity types: `products`, `sales`, `customers`, `suppliers`, `purchase_orders`, `inventory_movements`, `users`, `settings`

#### GET /api/sync/status
Returns record counts by sync_status per entity.

---

### Products

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/products | List (since, page, per_page, status, category_id, search) |
| POST | /api/products | Create |
| GET | /api/products/{id} | Get by ID |
| PUT/PATCH | /api/products/{id} | Update |
| DELETE | /api/products/{id} | Soft delete |

### Sales

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/sales | List (since, cashier_id, date_from, date_to) |
| POST | /api/sales | Create sale with items |
| GET | /api/sales/{id} | Get with items |

### Customers / Suppliers

Standard CRUD with `?search=` and `?since=` query params.

### Purchase Orders

CRUD with nested items. Statuses: `PENDING`, `ORDERED`, `PARTIAL`, `RECEIVED`, `CANCELLED`.

### Inventory Movements

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/inventory/movements | List (since, product_id, type, date_from, date_to) |
| POST | /api/inventory/movements | Record movement (updates product stock) |

Movement types: `SALE`, `PURCHASE`, `ADJUSTMENT`, `RETURN`, `TRANSFER`, `DAMAGE`, `OPENING`

### Users (Admin only)

Standard CRUD. Passwords hashed with bcrypt cost 12.

### Settings

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/settings | Get all settings |
| PUT/PATCH | /api/settings | Update settings (admin only) |

### Backup

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/backup/upload | Upload backup file (multipart, field: `backup`) |
| GET | /api/backup/list | List uploaded backups |

### Health Check

```
GET /api/health
```
```json
{ "status": "ok", "app": "BizFlow POS API", "version": "2.0.0", "timestamp": "2024-..." }
```

---

## Security Notes

1. **JWT_SECRET** must be a long random string — minimum 32 characters. Store only in environment variables.
2. All queries use PDO prepared statements — no SQL injection vectors.
3. Passwords hashed with `PASSWORD_BCRYPT` cost 12.
4. Account lockout after 5 failed login attempts (15-minute lockout).
5. Backup uploads are restricted to `.db`, `.sqlite`, `.zip`, `.gz`, `.sql`, `.bak` extensions.
6. CORS is wide-open (`*`) by default for desktop app compatibility — restrict in production.
7. The `backups/` directory is outside the web root by default — adjust `BACKUP_DIR` if needed.

---

## BizFlow POS Licensing

Licensing tables and the Starter, Business, and Enterprise plans are created
automatically when a `/license/*` endpoint is first called. The standalone
schema is also available at `sql/licensing.sql`.

Public endpoints are `GET /license/plans` and `POST` requests to
`/license/trial`, `/license/activate`, and `/license/validate`.

The `/license/issue`, `/license/renew`, and `/license/revoke` endpoints require
a signed-in backend administrator. From the cPanel terminal:

```bash
php tools/issue_license.php BUSINESS "Customer Shop" 12 5 owner@example.com 0712345678
php tools/renew_license.php BIZF-XXXXX-XXXXX-XXXXX-XXXXX 12
```

### Multi-business isolation

Each issued license creates a unique `business_id`. Activation returns a signed
sync token containing that business identity, and all sync uploads, downloads,
status counts, child records, settings, and product-image folders are restricted
to it. Computers using the same license share data; computers using another
license cannot read or overwrite that data.

`TenantManager` migrates existing databases automatically. On the first
activation of an existing installation, records without a `business_id` are
claimed by that license. Back up the database first and activate the original
business license before issuing licenses for additional businesses. See
`sql/multi_tenant.sql` for deployment notes. The configured MySQL account needs
`ALTER`, `CREATE`, and `INDEX` permissions during this one-time migration.

License keys are stored only as SHA-256 hashes. Copy a newly issued key from
the command output immediately.

---

## Troubleshooting

**404 on all routes**  
→ Ensure `mod_rewrite` is enabled and `AllowOverride All` is set.

**500 / "Database connection failed"**  
→ Check `DB_*` environment variables and MySQL credentials.

**401 on all requests after login**  
→ Confirm your HTTP client sends `Authorization: Bearer <token>` header, and that Apache is not stripping it (add `SetEnvIf Authorization "(.*)" HTTP_AUTHORIZATION=$1` to VirtualHost if needed).

**JWT "Token has expired"**  
→ Default expiry is 24 hours (`JWT_EXPIRY`). Use `/api/auth/refresh` to get a new token.
