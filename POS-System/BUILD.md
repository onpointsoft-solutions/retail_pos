# Mobile Meals POS System - Build Instructions

## Overview
This document provides instructions for building and running the Mobile Meals Point of Sale (POS) system.

## Prerequisites
- Java Development Kit (JDK) 8 or higher
- Internet connection for downloading dependencies
- Windows or Linux/macOS operating system

## Quick Start

### Windows Users
1. Open Command Prompt or PowerShell
2. Navigate to the POS-System directory
3. Run the build script:
   ```
   build.bat
   ```
4. Follow the on-screen instructions

### Linux/macOS Users
1. Open Terminal
2. Navigate to the POS-System directory
3. Make the build script executable:
   ```
   chmod +x build.sh
   ```
4. Run the build script:
   ```
   ./build.sh
   ```
5. Follow the on-screen instructions

## Manual Build Process

### 1. Create Directory Structure
```
POS-System/
├── src/
│   └── com/mobilemeals/pos/
│       ├── *.java files
├── lib/
│   ├── gson-2.8.9.jar
│   └── sqlite-jdbc-3.36.0.3.jar
├── bin/
└── MobileMealsPOS.jar
```

### 2. Download Dependencies
The build script automatically downloads these dependencies:
- **Gson 2.8.9** - For JSON parsing
- **SQLite JDBC 3.36.0.3** - For local database operations

### 3. Compile the Source Code
```bash
# Set classpath
export CLASSPATH="src:lib/*:."  # Linux/macOS
set CLASSPATH=src;lib\*;.       # Windows

# Create bin directory
mkdir bin

# Compile all Java files
javac -cp "$CLASSPATH" -d bin src/com/mobilemeals/pos/*.java
```

### 4. Create Executable JAR
```bash
# Create manifest file
echo "Main-Class: com.mobilemeals.pos.MobileMealsPOS" > manifest.txt
echo "Class-Path: lib/gson-2.8.9.jar lib/sqlite-jdbc-3.36.0.3.jar" >> manifest.txt

# Create JAR file
jar -cfm MobileMealsPOS.jar manifest.txt -C bin .
```

## Running the POS System

### Method 1: Using JAR File (Recommended)
```bash
java -jar MobileMealsPOS.jar
```

### Method 2: Using Classpath
```bash
java -cp "src:lib/*:." com.mobilemeals.pos.MobileMealsPOS  # Linux/macOS
java -cp "src;lib*;." com.mobilemeals.pos.MobileMealsPOS    # Windows
```

## Configuration

### API Configuration
The POS system connects to the Mobile Meals backend API. Update the API settings in `RestaurantSession.java`:

```java
private static final String API_BASE_URL = "https://www.mobilemealscenter.co.ke/api/";
```

### Database Configuration
For local operations, the system uses SQLite. Database files are created automatically in the application directory.

### Printer Configuration
The system supports standard Windows/Linux printers. Configure printer settings in the POS application under Settings → Printer Settings.

## Troubleshooting

### Common Issues

#### 1. "javac: command not found"
**Solution:** Install JDK and ensure it's in your PATH
- Windows: Add JDK bin directory to System PATH
- Linux/macOS: Export PATH in ~/.bashrc or ~/.zshrc

#### 2. "ClassNotFoundException: com.google.gson.Gson"
**Solution:** Ensure gson JAR is in lib directory and classpath is correct
```bash
# Verify JAR exists
ls lib/gson-*.jar

# Check classpath
echo $CLASSPATH  # Linux/macOS
echo %CLASSPATH% # Windows
```

#### 3. "Connection refused" or API errors
**Solution:** Check network connectivity and API endpoint
- Verify internet connection
- Check API URL in RestaurantSession.java
- Test API endpoint with curl or browser

#### 4. "Access denied" permissions
**Solution:** Ensure proper file permissions
```bash
# Linux/macOS
chmod +x build.sh
chmod +x MobileMealsPOS.jar

# Windows: Run as Administrator if needed
```

### Debug Mode
To run with debug output:
```bash
java -Djava.util.logging.config.file=logging.properties -jar MobileMealsPOS.jar
```

### Logging
The POS system creates log files in the application directory:
- `pos.log` - General application logs
- `error.log` - Error logs only

## Development Setup

### IDE Configuration
For development in IntelliJ IDEA or Eclipse:

1. **Import Project**
   - File → Import → Existing Project
   - Select POS-System directory

2. **Configure Libraries**
   - Add lib/*.jar files to project libraries
   - Set Java version to 8 or higher

3. **Run Configuration**
   - Main class: `com.mobilemeals.pos.MobileMealsPOS`
   - Classpath: Include lib directory

### Code Structure
```
src/com/mobilemeals/pos/
├── MobileMealsPOS.java          # Main application class
├── OrderManager.java            # Order management
├── MenuManager.java             # Menu management
├── ReportManager.java           # Reporting system
├── RestaurantSession.java       # Session management
├── OrderEntryPanel.java         # Order entry UI
├── ReceiptPrinter.java          # Receipt printing
└── POSApiClient.java            # API communication
```

## Deployment

### Single User Deployment
1. Copy the entire POS-System directory to target machine
2. Ensure Java 8+ is installed
3. Run `build.bat` (Windows) or `./build.sh` (Linux/macOS)
4. Launch with `java -jar MobileMealsPOS.jar`

### Multi-User Deployment
1. Install on each restaurant terminal
2. Configure unique restaurant credentials
3. Set up shared printer if needed
4. Train staff on POS operations

## Support

For technical support:
1. Check this documentation first
2. Review log files for error messages
3. Verify network connectivity
4. Contact development team if issues persist

## Version History
- v1.0.0 - Initial release with basic POS functionality
- Support for order management, menu management, and reporting
- Integration with Mobile Meals backend API
- Receipt printing and kitchen ticket printing
