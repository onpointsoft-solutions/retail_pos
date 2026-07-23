# Mobile Meals POS System - Maven Build Guide

## 🚀 Maven-Based POS System with Mobile Meals Branding

A comprehensive Point of Sale system built with Maven, featuring Mobile Meals brand colors and modern UI design using FlatLaf.

## ✨ Key Features

### 🎨 **Mobile Meals Brand Integration**
- **Consistent Color Scheme** - Matches web and mobile app branding
- **Primary Colors** - Blue (#2980b9) with complementary colors
- **Modern UI** - FlatLaf look and feel with custom theming
- **Responsive Design** - Adapts to different screen sizes
- **Icon Integration** - Material Design icons throughout

### 📊 **Enhanced POS Features**
- **Dashboard** - Real-time statistics and analytics
- **Order Management** - Complete order lifecycle management
- **Menu Management** - Dynamic menu with categories
- **Reporting** - Comprehensive sales and performance reports
- **Settings** - Configurable restaurant preferences
- **Theme Support** - Multiple theme options (Light, Dark, IntelliJ, macOS)

### 🔧 **Technical Improvements**
- **Maven Build System** - Professional dependency management
- **Modern Java** - Java 11+ with latest libraries
- **FlatLaf UI** - Modern, clean interface
- **Material Icons** - Professional iconography
- **Structured Logging** - SLF4J with Logback
- **Testing Framework** - JUnit 5 with Mockito

## 🛠️ Build Requirements

### Prerequisites
- **Java 11+** (JDK 11 or higher)
- **Apache Maven 3.6+**
- **Git** (for version control)

### Dependencies
```xml
<!-- Core Libraries -->
- Gson 2.10.1 (JSON Processing)
- Apache HttpClient 5.3 (HTTP Communication)
- SQLite JDBC 3.45.1.0 (Local Database)

<!-- UI Framework -->
- FlatLaf 3.2.5 (Modern Look & Feel)
- Ikonli 12.3.1 (Material Design Icons)

<!-- Logging & Testing -->
- SLF4J 2.0.9 + Logback 1.4.11
- JUnit 5.10.0 + Mockito 5.6.0
```

## 🏗️ Build Instructions

### 1. Clone Repository
```bash
git clone <repository-url>
cd POS-System
```

### 2. Build with Maven
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package application
mvn package

# Install to local repository
mvn install
```

### 3. Run Application
```bash
# Run from JAR
java -jar target/MobileMealsPOS-1.0.0-fat.jar

# Or run with Maven
mvn exec:java -Dexec.mainClass="com.mobilemeals.pos.MobileMealsPOS"
```

## 🎨 Mobile Meals Brand Colors

### Primary Color Palette
```css
--primary-color: #2980b9;     /* Main Blue */
--primary-dark: #1f618d;        /* Dark Blue */
--primary-light: #3498db;       /* Light Blue */
--accent-color: #e74c3c;        /* Red Accent */
--success-color: #27ae60;        /* Green Success */
--warning-color: #f39c12;        /* Orange Warning */
--info-color: #3498db;          /* Blue Info */
--light-color: #ecf0f1;         /* Light Gray */
--dark-color: #2c3e50;          /* Dark Gray */
--text-color: #34495e;           /* Text Gray */
```

### Order Status Colors
```css
--pending: #f39c12;              /* Orange */
--confirmed: #3498db;             /* Blue */
--preparing: #2980b9;            /* Primary Blue */
--ready: #27ae60;                 /* Green */
--picked_up: #3498db;             /* Light Blue */
--delivering: #1f618d;            /* Dark Blue */
--delivered: #27ae60;             /* Green */
--cancelled: #95a5a6;             /* Gray */
```

## 📱 UI Components

### Theme Manager
```java
// Apply Mobile Meals theme
ThemeManager.getInstance().initializeTheme();

// Create themed components
JButton primaryButton = ThemeManager.createPrimaryButton("Submit");
JButton successButton = ThemeManager.createSuccessButton("Complete");
JButton dangerButton = ThemeManager.createDangerButton("Cancel");

// Create styled labels
JLabel header = ThemeManager.createHeaderLabel("Dashboard");
JLabel subHeader = ThemeManager.createSubHeaderLabel("Today's Orders");
JLabel body = ThemeManager.createBodyLabel("Order details");
```

### Custom Components
- **Themed Buttons** - Primary, Success, Warning, Danger, Secondary
- **Styled Labels** - Header, Sub-header, Body, Caption
- **Enhanced Inputs** - Text fields with Mobile Meals styling
- **Card Panels** - Consistent card-based layout
- **Status Indicators** - Color-coded status displays

## 🔧 Configuration

### Maven Profiles
```bash
# Development (default)
mvn compile -Pdev

# Production
mvn compile -Pprod

# Testing
mvn compile -Ptest
```

### Environment Variables
```properties
# Development
api.base.url=http://localhost:8000/api/
log.level=DEBUG

# Production
api.base.url=https://www.mobilemealscenter.co.ke/api/
log.level=INFO
```

## 📊 Application Structure

```
src/main/java/com/mobilemeals/pos/
├── MobileMealsPOS.java          # Main application
├── ui/
│   └── ThemeManager.java        # Theme management
├── OrderManager.java            # Order management
├── MenuManager.java             # Menu management
├── ReportManager.java           # Reporting system
├── RestaurantSession.java       # Session management
├── OrderEntryPanel.java         # Order entry UI
├── ReceiptPrinter.java          # Receipt printing
└── POSApiClient.java            # API communication
```

## 🎯 Build Profiles

### Development Profile
- **API URL**: `http://localhost:8000/api/`
- **Log Level**: DEBUG
- **Database**: Local SQLite
- **Theme**: Light theme with debug info

### Production Profile
- **API URL**: `https://www.mobilemealscenter.co.ke/api/`
- **Log Level**: INFO
- **Database**: Encrypted SQLite
- **Theme**: Optimized production theme

### Testing Profile
- **API URL**: `https://test.mobilemealscenter.co.ke/api/`
- **Log Level**: WARN
- **Database**: In-memory SQLite
- **Theme**: Neutral testing theme

## 🚀 Deployment

### Single JAR Deployment
```bash
# Build fat JAR with all dependencies
mvn clean package

# Deploy JAR file
cp target/MobileMealsPOS-1.0.0-fat.jar /opt/mobilemeals-pos/
```

### Multi-Module Deployment
```bash
# Build with dependencies
mvn clean package

# Deploy with lib directory
cp target/MobileMealsPOS-1.0.0.jar /opt/mobilemeals-pos/
cp -r target/lib/ /opt/mobilemeals-pos/lib/
```

### Docker Deployment
```dockerfile
FROM openjdk:11-jre-slim
COPY target/MobileMealsPOS-1.0.0-fat.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 🧪 Testing

### Run Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=OrderManagerTest

# Run with coverage
mvn test jacoco:report
```

### Test Structure
```
src/test/java/com/mobilemeals/pos/
├── OrderManagerTest.java
├── MenuManagerTest.java
├── ReportManagerTest.java
└── ThemeManagerTest.java
```

## 📝 Logging Configuration

### Logback Configuration
```xml
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>logs/pos.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

## 🔌 Plugin Configuration

### Key Maven Plugins
- **maven-compiler-plugin** - Java 11 compilation
- **maven-shade-plugin** - Fat JAR creation
- **maven-surefire-plugin** - Test execution
- **maven-dependency-plugin** - Dependency management
- **maven-resources-plugin** - Resource filtering

## 🎨 Theme Customization

### Custom Theme Creation
```java
// Create custom theme
public class MobileMealsTheme extends FlatLightLaf {
    @Override
    public String getName() {
        return "MobileMeals";
    }
    
    @Override
    public boolean isDark() {
        return false;
    }
}
```

### Theme Switching
```java
// Switch themes dynamically
ThemeManager.getInstance().setTheme("dark");
ThemeManager.getInstance().setTheme("light");
ThemeManager.getInstance().setTheme("intellij");
```

## 📱 Integration Features

### API Integration
- **RESTful API** - Full backend integration
- **Real-time Updates** - WebSocket support
- **Offline Mode** - Local caching
- **Sync Management** - Automatic data synchronization

### Mobile App Consistency
- **Color Matching** - Exact color reproduction
- **Icon Consistency** - Material Design icons
- **Layout Similarity** - Familiar user experience
- **Data Synchronization** - Real-time data sharing

## 🚀 Performance Optimizations

### JVM Optimization
```bash
# Production JVM settings
java -Xms512m -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar MobileMealsPOS-1.0.0-fat.jar
```

### Memory Management
- **Efficient Caching** - Smart data caching
- **Lazy Loading** - On-demand data loading
- **Resource Cleanup** - Proper memory management
- **Connection Pooling** - Efficient database connections

## 🔒 Security Features

### Data Protection
- **API Encryption** - HTTPS communication
- **Local Encryption** - Sensitive data protection
- **Session Management** - Secure user sessions
- **Audit Logging** - Comprehensive activity tracking

## 📞 Support & Maintenance

### Troubleshooting
- **Build Issues** - Check Maven configuration
- **Dependency Problems** - Verify Maven Central access
- **Theme Issues** - Check FlatLaf compatibility
- **Performance** - Monitor JVM memory usage

### Regular Updates
- **Dependency Updates** - Regular security patches
- **Feature Updates** - Monthly feature releases
- **Performance** - Continuous optimization
- **Security** - Immediate security updates

---

## 🎯 Quick Start Commands

```bash
# Clone and build
git clone <repo-url>
cd POS-System
mvn clean package

# Run application
java -jar target/MobileMealsPOS-1.0.0-fat.jar

# Development mode
mvn compile exec:java

# Test application
mvn test

# Generate reports
mvn site
```

---

**© 2024 Mobile Meals Center. All rights reserved.**  
*Professional POS System with Mobile Meals Branding*
