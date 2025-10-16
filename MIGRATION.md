# Migration Guide

## Migrating to MockServer 5.15.0+

### Jakarta EE Migration (Breaking Change)

MockServer 5.15.0+ migrates from Java EE to Jakarta EE. This affects servlet-based integrations.

#### Required Changes:

**Import Changes:**
```java
// Before (Java EE)
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// After (Jakarta EE)  
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
```

**Dependency Updates:**
```xml
<!-- Before -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
</dependency>

<!-- After -->
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>6.0.0</version>
</dependency>
```

### JavaScript Engine Migration (Breaking Change)

**GraalJS is now mandatory:**
- Nashorn fallback removed for security reasons
- **BREAKING**: JavaScript templates only work with GraalJS
- No fallback to other JavaScript engines

**Required Dependencies:**
```xml
<dependency>
    <groupId>org.graalvm.js</groupId>
    <artifactId>js</artifactId>
    <version>23.0.9</version>
</dependency>
<dependency>
    <groupId>org.graalvm.js</groupId>
    <artifactId>js-scriptengine</artifactId>
    <version>23.0.9</version>
</dependency>
```

**Migration Impact:**
- If GraalJS not available → JavaScript templates fail completely
- No automatic fallback to Nashorn/Rhino
- JavaScript templates disabled by default for security

**Enabling JavaScript Templates:**
```java
Configuration config = new Configuration()
    .javascriptTemplatesEnabled(true);
```

### Spring Framework Update

**Spring 5.x → 6.x:**
- Requires Java 17+
- Jakarta EE compatible
- May affect Spring Boot applications

### Security Changes

**Templates Disabled by Default:**
- JavaScript templates: disabled (CVE-2021-32827)
- Velocity templates: disabled
- Use Mustache templates for safer templating

**Migration Path:**
1. Update imports (javax → jakarta)
2. Update dependencies
3. Enable templates if needed (understand security risks)
4. Test thoroughly
