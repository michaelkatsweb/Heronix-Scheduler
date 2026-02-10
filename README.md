// ============================================================================
// FILE: README.md (VS Code Specific Section)
// LOCATION: /heronix-pro/README.md
// ============================================================================

# Heronix Scheduling System - VS Code Setup

## Prerequisites

### 1. Install Java Development Kit (JDK)
```bash
# Download JDK 17 or higher
# Windows: https://adoptium.net/
# macOS: brew install openjdk@17
# Linux: sudo apt install openjdk-17-jdk

# Verify installation
java -version
javac -version
```

### 2. Install Maven
```bash
# Windows: Download from https://maven.apache.org/download.cgi
# macOS: brew install maven
# Linux: sudo apt install maven

# Verify installation
mvn -version
```

### 3. Install VS Code Extensions

Open VS Code and install these extensions:
1. Press `Ctrl+Shift+X` (or `Cmd+Shift+X` on Mac)
2. Search and install:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Maven for Java
   - Lombok Annotations Support

Or use the command palette:
```
Press F1 → Type "Extensions: Show Recommended Extensions"
Click "Install All" for workspace recommendations
```

## Project Setup in VS Code

### Step 1: Open Project
```bash
# Clone or create the project folder
mkdir heronix-pro
cd heronix-pro

# Open in VS Code
code .
```

### Step 2: Trust the Workspace
- VS Code will ask if you trust the workspace
- Click "Yes, I trust the authors"

### Step 3: Let Java Extension Initialize
- VS Code will automatically detect it's a Java/Maven project
- Wait for "Importing projects..." in the status bar to complete
- You'll see "Java projects: 1" in the bottom right

### Step 4: Configure Java Home (if needed)
If Java is not detected:
1. Press `Ctrl+Shift+P` (or `Cmd+Shift+P`)
2. Type "Java: Configure Java Runtime"
3. Select your JDK 17+ installation

### Step 5: Build the Project
```bash
# Option 1: Use VS Code task
Press Ctrl+Shift+P → "Tasks: Run Task" → "Maven: Clean Install"

# Option 2: Use terminal
mvn clean install

# Option 3: Use Maven sidebar
Click Maven icon in sidebar → heronix-pro → Lifecycle → install
```

## Running the Application in VS Code

### Method 1: Using Debug Panel (Recommended)
1. Press `F5` or click the Run icon (▶️) in the sidebar
2. Select "Launch Heronix Scheduler Application"
3. Application starts with debugger attached

### Method 2: Using Spring Boot Dashboard
1. Click Spring Boot icon in VS Code sidebar
2. Find "heronix-pro" app
3. Click the ▶️ play button next to it

### Method 3: Using Maven Command
```bash
# In VS Code terminal (Ctrl+`)
mvn spring-boot:run
```

### Method 4: Using Task Runner
1. Press `Ctrl+Shift+P`
2. Type "Tasks: Run Task"
3. Select "Spring Boot: Run"

## Debugging Tips

### Set Breakpoints
1. Click to the left of line numbers (red dot appears)
2. Press `F5` to start debugging
3. Use debug toolbar to step through code:
   - `F10` - Step Over
   - `F11` - Step Into
   - `Shift+F11` - Step Out
   - `F5` - Continue

### Debug Console
- View variables in the "Variables" panel
- Use "Watch" panel for custom expressions
- Check "Call Stack" to see execution path

### Hot Reload (Spring DevTools)
Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

Now code changes auto-reload without restarting!

## Useful VS Code Shortcuts

### General
- `Ctrl+Shift+P` - Command Palette
- `Ctrl+P` - Quick Open File
- `Ctrl+` - Toggle Terminal
- `Ctrl+B` - Toggle Sidebar

### Java Specific
- `F12` - Go to Definition
- `Alt+F12` - Peek Definition
- `Shift+F12` - Show All References
- `F2` - Rename Symbol
- `Ctrl+.` - Quick Fix (generates getters, imports, etc.)
- `Ctrl+Space` - IntelliSense/Autocomplete

### Refactoring
- Right-click → "Refactor..."
- Extract Method/Variable/Constant
- Generate Constructors/Getters/Setters (with Lombok)

### Code Navigation
- `Ctrl+Shift+O` - Go to Symbol in File
- `Ctrl+T` - Go to Symbol in Workspace
- `Alt+Left/Right` - Navigate Back/Forward

## Maven Commands in VS Code

### Using Command Palette
1. Press `Ctrl+Shift+P`
2. Type "Maven"
3. Select desired command

### Using Maven Sidebar
1. Click Maven icon (M) in sidebar
2. Expand your project
3. Run lifecycle goals:
   - `clean` - Remove target folder
   - `compile` - Compile source code
   - `test` - Run unit tests
   - `package` - Create JAR file
   - `install` - Install to local repository

### Custom Maven Commands
Add to `.vscode/tasks.json` for quick access

## Testing in VS Code

### Run All Tests
1. Open Test Explorer (flask icon in sidebar)
2. Click "Run All Tests" button

### Run Single Test
1. Open test file
2. Click ▶️ icon next to test method
3. Or right-click → "Run Test"

### Debug Tests
1. Click 🐛 icon next to test method
2. Or right-click → "Debug Test"

## Database Viewing (H2 Console)

### Access H2 Console
1. Start application
2. Open browser: http://localhost:9590/h2-console
3. JDBC URL: `jdbc:h2:file:./data/heronix`
4. Username: `admin`
5. Password: `admin123`

### Using VS Code Database Extension
1. Install "Database Client" extension
2. Click database icon in sidebar
3. Add connection:
   - Type: H2
   - Host: localhost
   - Database: ./data/heronix
   - User: admin
   - Password: admin123

## Troubleshooting

### "Java extension not initialized"
```bash
# Clean Java workspace
1. Ctrl+Shift+P → "Java: Clean Java Language Server Workspace"
2. Restart VS Code
```

### "Cannot find main class"
```bash
# Rebuild project
mvn clean install
# Reload VS Code window
Ctrl+Shift+P → "Developer: Reload Window"
```

### "Port 8080 already in use"
```bash
# Find and kill process
# Windows:
netstat -ano | findstr :8080
taskkill /PID <pid> /F

# Mac/Linux:
lsof -ti:8080 | xargs kill -9
```

### Maven Dependencies Not Resolving
```bash
# Force update
mvn clean install -U

# Or in VS Code
Ctrl+Shift+P → "Maven: Force Update Snapshots"
```

## Recommended VS Code Settings

### Enable Auto Save
```json
"files.autoSave": "afterDelay",
"files.autoSaveDelay": 1000
```

### Format on Save
```json
"editor.formatOnSave": true,
"[java]": {
    "editor.formatOnSave": true
}
```

### Show Whitespace
```json
"editor.renderWhitespace": "all"
```

## Keyboard Shortcuts Reference

| Action | Windows/Linux | macOS |
|--------|--------------|-------|
| Run App | F5 | F5 |
| Build | Ctrl+Shift+B | Cmd+Shift+B |
| Terminal | Ctrl+` | Ctrl+` |
| Go to Definition | F12 | F12 |
| Find All References | Shift+F12 | Shift+F12 |
| Rename | F2 | F2 |
| Quick Fix | Ctrl+. | Cmd+. |

## Next Steps

1. ✅ Setup complete? Run the project: Press `F5`
2. 📖 Read the full documentation in `/docs`
3. 🔧 Start coding! Begin with domain models
4. 🧪 Write tests as you go
5. 🐛 Use debugger to troubleshoot

## Useful Links

- [VS Code Java Documentation](https://code.visualstudio.com/docs/languages/java)
- [Spring Boot in VS Code](https://code.visualstudio.com/docs/java/java-spring-boot)
- [Maven in VS Code](https://code.visualstudio.com/docs/java/java-build)

## Support

Having issues? Check:
1. Java extension logs: View → Output → Select "Language Support for Java"
2. Maven logs: View → Output → Select "Maven for Java"
3. Application logs: Check `logs/heronix.log`