## 🍽️ Mobile Meals POS System - Maven Project

A professional Point of Sale system built with Maven, featuring **Mobile Meals brand colors** and modern UI design that seamlessly integrates with the existing web and mobile applications.

---

## ✨ **Key Features**

### 🎨 **Mobile Meals Brand Integration**
- **Exact Color Matching** - Primary blue (#2980b9) and complementary colors
- **Consistent Theming** - Matches web and mobile app design
- **Modern UI Framework** - FlatLaf with custom Mobile Meals theme
- **Material Design Icons** - Professional iconography throughout
- **Responsive Design** - Adapts to different screen sizes

### 📊 **Complete POS Functionality**
- **📊 Dashboard** - Real-time statistics and analytics
- **📋 Order Management** - Complete order lifecycle
- **📱 Menu Management** - Dynamic menu with categories
- **📈 Reporting** - Comprehensive sales analytics
- **⚙️ Settings** - Configurable restaurant preferences
- **🎨 Theme Support** - Light, Dark, IntelliJ, macOS themes

### 🔧 **Technical Excellence**
- **Maven Build System** - Professional dependency management
- **Java 11+** - Modern Java with latest features
- **Structured Logging** - SLF4J with Logback
- **Comprehensive Testing** - JUnit 5 with Mockito
- **API Integration** - Full backend connectivity

---

## 🚀 **Quick Start**

### **Prerequisites**
- **Java 11+** installed
- **Apache Maven 3.6+** installed
- **Git** for version control

### **Build & Run**
```bash
# Clone and build
git clone <repository-url>
cd POS-System

# Linux/macOS
chmod +x build-maven.sh
./build-maven.sh

# Windows
build-maven.bat

# Or use Maven directly
mvn clean package
java -jar target/MobileMealsPOS-1.0.0-fat.jar
```

---

## 🎨 **Mobile Meals Brand Colors**

### **Primary Palette**
```css
--primary-color: #2980b9;     /* Main Blue */
--primary-dark: #1f618d;        /* Dark Blue */
--primary-light: #3498db;       /* Light Blue */
--accent-color: #e74c3c;        /* Red Accent */
--success-color: #27ae60;        /* Green Success */
--warning-color: #f39c12;        /* Orange Warning */
```

### **Order Status Colors**
```css
--pending: #f39c12;              /* Orange */
--confirmed: #3498db;             /* Blue */
--preparing: #2980b9;            /* Primary Blue */
--ready: #27ae60;                 /* Green */
--delivered: #27ae60;             /* Green */
--cancelled: #95a5a6;             /* Gray */
```

---

## 📱 **UI Components**

### **Themed Components**
```java
// Mobile Meals themed buttons
JButton primary = ThemeManager.createPrimaryButton("Submit Order");
JButton success = ThemeManager.createSuccessButton("Complete");
JButton danger = ThemeManager.createDangerButton("Cancel");

// Styled labels
JLabel header = ThemeManager.createHeaderLabel("Dashboard");
JLabel subHeader = ThemeManager.createSubHeaderLabel("Today's Orders");
```

### **Custom Features**
- **🎨 Theme Manager** - Dynamic theme switching
- **📊 Status Cards** - Color-coded statistics
- **🔄 Real-time Updates** - Live data synchronization
- **🖨️ Receipt Printing** - Professional receipt format
- **📱 Mobile Integration** - Seamless backend sync

---

## 🔧 **Maven Configuration**

### **Dependencies**
```xml
<!-- Core -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>

<!-- UI Framework -->
<dependency>
    <groupId>com.formdev</groupId>
    <artifactId>flatlaf</artifactId>
    <version>3.2.5</version>
</dependency>

<!-- Icons -->
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-materialdesign2</artifactId>
    <version>12.3.1</version>
</dependency>
```

### **Build Profiles**
- **🔧 Development** - Local API, debug logging
- **🚀 Production** - Live API, optimized logging
- **🧪 Testing** - Test API, minimal logging

---

## 📊 **Application Structure**

```
src/main/java/com/mobilemeals/pos/
├── 📱 MobileMealsPOS.java          # Main application
├── 🎨 ui/
│   └── ThemeManager.java            # Theme management
├── 📋 OrderManager.java            # Order management
├── 🍽️ MenuManager.java             # Menu management
├── 📈 ReportManager.java           # Reporting system
├── 🔐 RestaurantSession.java       # Session management
├── 📝 OrderEntryPanel.java         # Order entry UI
├── 🖨️ ReceiptPrinter.java          # Receipt printing
└── 🌐 POSApiClient.java            # API communication
```

---

## 🎯 **Key Features**

### **📊 Dashboard**
- **Real-time Statistics** - Today's orders, revenue, pending items
- **Recent Orders** - Quick access to latest orders
- **Quick Actions** - New order, refresh, view all
- **Status Indicators** - Color-coded status displays

### **📋 Order Management**
- **Complete Lifecycle** - Order creation to delivery
- **Status Tracking** - Real-time order status
- **Customer Information** - Contact and delivery details
- **Order Actions** - Edit, cancel, print, export

### **🍽️ Menu Management**
- **Category Organization** - Structured menu layout
- **Dynamic Pricing** - Real-time price updates
- **Availability Control** - Toggle item availability
- **Bulk Operations** - Add, edit, delete multiple items

### **📈 Reports & Analytics**
- **Sales Reports** - Daily, weekly, monthly analytics
- **Order Reports** - Order status and performance
- **Revenue Reports** - Financial analysis
- **Menu Performance** - Best-selling items analysis

---

## 🌐 **Backend Integration**

### **API Endpoints**
```java
// Order Management
POST /api/orders/           // Create order
GET  /api/orders/           // Get orders
PATCH /api/orders/{id}/     // Update status

// Menu Management
GET  /api/restaurants/menu/ // Get menu
POST /api/restaurants/menu/ // Add item
PUT  /api/restaurants/menu/{id}/ // Update item

// Reports
GET  /api/reports/sales/   // Sales report
GET  /api/reports/orders/  // Order report
```

### **Real-time Synchronization**
- **🔄 Auto-sync** - 30-second background refresh
- **📱 Mobile App Integration** - Seamless data sharing
- **🌐 WebSocket Support** - Real-time updates
- **💾 Offline Mode** - Local caching with sync

---

## 🖨️ **Printing Features**

### **Receipt Printing**
- **🧾 Professional Format** - Restaurant branding and details
- **📊 Itemized List** - Quantity, description, pricing
- **💰 Tax Breakdown** - Automatic VAT calculation
- **👤 Customer Info** - Delivery details and contact

### **Kitchen Tickets**
- **🍽️ Large Print** - Easy to read in busy kitchen
- **⏰ Order Priority** - Sequence and timing
- **📝 Special Instructions** - Custom notes
- **🚚 Delivery Info** - Customer details

---

## 🔐 **Security Features**

### **Authentication**
- **🔐 Secure Login** - Username/password authentication
- **🎫 Session Management** - Automatic timeout and refresh
- **🔑 API Tokens** - Bearer token authentication
- **👥 Role-Based Access** - Different permission levels

### **Data Protection**
- **🔒 Local Encryption** - Sensitive data protection
- **🌐 Secure Communication** - HTTPS API calls
- **📝 Audit Logging** - Comprehensive activity tracking
- **💾 Data Backup** - Automatic data synchronization

---

## 📞 **Support & Maintenance**

### **Troubleshooting**
- **🔧 Build Issues** - Check Maven configuration
- **📦 Dependency Problems** - Verify Maven Central access
- **🎨 Theme Issues** - Check FlatLaf compatibility
- **⚡ Performance** - Monitor JVM memory usage

### **Regular Updates**
- **🔄 Dependency Updates** - Monthly security patches
- **✨ Feature Updates** - Regular feature releases
- **⚡ Performance** - Continuous optimization
- **🔒 Security** - Immediate security updates

---

## 🎯 **Build Commands**

```bash
# Development build
./build-maven.sh dev

# Production build
./build-maven.sh prod

# Build without tests
./build-maven.sh dev true

# Maven commands
mvn clean package
mvn test
mvn site

# Run application
java -jar target/MobileMealsPOS-1.0.0-fat.jar
```

---

## 📱 **Mobile App Integration**

### **Consistent Experience**
- **🎨 Color Matching** - Exact color reproduction
- **📱 Icon Consistency** - Material Design icons
- **🔄 Layout Similarity** - Familiar user experience
- **📊 Data Synchronization** - Real-time data sharing

### **Order Flow Integration**
```
Customer App → Backend → POS System → Kitchen → Rider → Customer
     ↓              ↓          ↓          ↓        ↓        ↓
  Order Place   → API Call  → Kitchen   → Prepare → Pickup → Delivery
```

---

## 🎉 **Success Metrics**

### **Performance**
- **⚡ Fast Startup** - < 3 seconds application launch
- **🔄 Real-time Updates** - < 1 second data sync
- **💾 Efficient Memory** - < 512MB typical usage
- **🌐 Reliable API** - 99.5% uptime target

### **User Experience**
- **🎨 Intuitive Interface** - Minimal training required
- **📱 Mobile Consistency** - Seamless cross-platform experience
- **🖨️ Professional Output** - High-quality receipts
- **📊 Actionable Insights** - Useful business analytics

---

**🍽️ Mobile Meals POS System**  
*Professional Point of Sale with Mobile Meals Branding*  

**© 2024 Mobile Meals Center. All rights reserved.** System

## Overview
A desktop Point of Sale (POS) system for restaurants using Java AWT, designed to seamlessly integrate with the existing Mobile Meals Center backend.

## Features
- Order management and tracking
- Real-time order status updates
- Payment processing integration
- Restaurant menu management
- Customer order processing
- Kitchen order display
- Sales reporting and analytics
- Inventory management
- Staff management

## Requirements
- Java 8+
- Java AWT for UI
- Network access for API integration
- Database connectivity
- Local storage for offline mode

## Integration
- REST API integration with Django backend
- Real-time WebSocket connections
- Print support for receipts
- Barcode scanner support
- Cash drawer integration
- Payment terminal integration

## Installation
1. Ensure Java 8+ is installed
2. Extract the POS application
3. Configure database settings
4. Set up API endpoints
5. Launch the application

## Support
For technical support and documentation, contact the development team.
