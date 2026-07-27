# Retail POS - Web Version (PHP)

A web-based Point of Sale system that duplicates the functionality of the Java desktop application.

## Features

- **Sales Management**: Cart system, product search, barcode scanning, multiple payment methods
- **Product Management**: Add/edit products with images, categories, suppliers, inventory tracking
- **Customer Management**: Loyalty points, credit balance, purchase history
- **Inventory Management**: Stock movements, purchase orders, low stock alerts
- **Reporting**: Sales reports, inventory reports, financial summaries
- **User Management**: Role-based access (Admin, Cashier, Manager)
- **Settings**: Store configuration, tax rates, receipt printing, theme customization
- **Multi-device Support**: Works on desktop, tablet, and mobile devices

## Tech Stack

- **Backend**: PHP 8.1+, MySQL
- **Frontend**: HTML5, CSS3, JavaScript (Vanilla)
- **Authentication**: JWT tokens
- **Database**: MySQL with InnoDB engine
- **API**: RESTful API

## Installation

1. Copy the files to your web server
2. Import the database schema from `database/schema.sql`
3. Configure database settings in `config/database.php`
4. Run the setup wizard at `http://your-domain/setup.php`
5. Login with the admin credentials created during setup

## Project Structure

```
php-pos/
├── api/              # REST API endpoints
├── assets/           # CSS, JS, images
├── config/           # Configuration files
├── controllers/      # Business logic
├── database/         # Database schema
├── models/           # Data models
├── views/            # UI templates
├── setup.php         # Initial setup wizard
└── index.php         # Main entry point
```

## Default Credentials

After setup, the default admin credentials are:
- Username: admin
- Password: (set during setup)

## License

Proprietary - All rights reserved
