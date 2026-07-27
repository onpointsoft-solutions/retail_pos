# PHP POS System - Installation Guide

## Overview

This is a web-based Point of Sale system that duplicates the functionality of the Java desktop application. It includes sales management, product management, customer management, inventory tracking, and reporting features.

## Requirements

- PHP 8.1 or higher
- MySQL 5.7 or higher
- Apache web server with mod_rewrite enabled
- Web browser (Chrome, Firefox, Safari, Edge)

## Installation Steps

### 1. Copy Files

Copy the entire `php-pos` folder to your web server directory:
- XAMPP: `C:\xampp\htdocs\php-pos`
- WAMP: `C:\wamp64\www\php-pos`
- Linux: `/var/www/html/php-pos`

### 2. Create Database

Create a MySQL database named `retail_pos`:
```sql
CREATE DATABASE retail_pos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Import Schema

Import the database schema:
```bash
mysql -u root -p retail_pos < database/schema.sql
```

Or use phpMyAdmin to import `database/schema.sql`

### 4. Configure Database

Edit `config/database.php` if needed (default settings work for local XAMPP/WAMP):
```php
private static string $host = 'localhost';
private static string $dbname = 'retail_pos';
private static string $username = 'root';
private static string $password = '';
```

### 5. Run Setup Wizard

Open your browser and navigate to:
```
http://localhost/php-pos/setup.php
```

Follow the 3-step setup wizard:
1. **Database Configuration**: Enter your database credentials
2. **Store Information**: Enter store name, address, phone, and tax rate
3. **Admin Account**: Create the admin username and password

### 6. Login

After setup, you'll be redirected to the login page. Login with the credentials you created during setup.

## Directory Structure

```
php-pos/
├── api/                    # REST API endpoints
│   └── index.php          # API router and controllers
├── assets/                # Frontend assets
│   ├── css/
│   │   └── style.css     # Main stylesheet
│   └── js/
│       └── app.js        # Frontend JavaScript
├── config/                # Configuration files
│   ├── config.php        # App settings
│   └── database.php      # Database connection
├── controllers/           # API controllers
│   ├── AuthController.php
│   ├── ProductController.php
│   ├── SaleController.php
│   ├── CustomerController.php
│   ├── CategoryController.php
│   ├── SupplierController.php
│   └── SettingsController.php
├── database/              # Database files
│   └── schema.sql        # Database schema
├── helpers/               # Helper classes
│   └── Auth.php          # Authentication helper
├── models/                # Data models
│   ├── User.php
│   └── Product.php
├── uploads/               # File uploads (create this folder)
├── .htaccess             # Apache rewrite rules
├── index.php             # Main application
├── login.php             # Login page
├── logout.php            # Logout handler
├── setup.php             # Setup wizard
├── README.md             # This file
└── INSTALLATION.md       # Installation guide
```

## Features

### Sales
- Product search and barcode scanning
- Cart management
- Multiple payment methods
- Receipt generation
- Tax calculation
- Discount support

### Products
- Add/edit/delete products
- Product images
- Categories
- Suppliers
- Stock management
- Price management (buying, selling, wholesale)

### Customers
- Customer management
- Loyalty points
- Credit balance
- Purchase history

### Inventory
- Stock tracking
- Low stock alerts
- Inventory movements
- Purchase orders

### Reports
- Daily sales
- Weekly sales
- Monthly sales
- Financial summaries

### Settings
- Store configuration
- Tax rates
- Theme colors
- User management

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login
- `POST /api/auth/logout` - Logout
- `GET /api/auth/me` - Get current user

### Products
- `GET /api/products` - List products
- `POST /api/products` - Create product
- `GET /api/products/{id}` - Get product
- `PUT /api/products/{id}` - Update product
- `DELETE /api/products/{id}` - Delete product

### Sales
- `GET /api/sales` - List sales
- `POST /api/sales` - Create sale
- `GET /api/sales/{id}` - Get sale

### Customers
- `GET /api/customers` - List customers
- `POST /api/customers` - Create customer
- `GET /api/customers/{id}` - Get customer
- `PUT /api/customers/{id}` - Update customer
- `DELETE /api/customers/{id}` - Delete customer

### Categories
- `GET /api/categories` - List categories
- `POST /api/categories` - Create category
- `PUT /api/categories/{id}` - Update category
- `DELETE /api/categories/{id}` - Delete category

### Suppliers
- `GET /api/suppliers` - List suppliers
- `POST /api/suppliers` - Create supplier
- `PUT /api/suppliers/{id}` - Update supplier
- `DELETE /api/suppliers/{id}` - Delete supplier

### Settings
- `GET /api/settings` - Get settings
- `PUT /api/settings` - Update settings

## Security Notes

1. Change the JWT secret in `config/config.php` before production use
2. Enable HTTPS in production
3. Restrict file upload permissions
4. Use strong passwords for admin accounts
5. Regularly update PHP and MySQL

## Troubleshooting

### Setup wizard doesn't load
- Ensure mod_rewrite is enabled in Apache
- Check .htaccess file exists
- Verify PHP version is 8.1+

### Database connection fails
- Verify MySQL is running
- Check database credentials in config/database.php
- Ensure database exists

### API returns 401 Unauthorized
- Check that you're logged in
- Verify JWT token is valid
- Check session is active

### Products not loading
- Check database has products
- Verify API endpoint is accessible
- Check browser console for errors

## Support

For issues or questions, refer to the Java POS documentation as this PHP version mirrors its functionality.
