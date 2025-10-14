# Security Policy

## Supported Versions

| Version  | Supported          |
|----------| ------------------ |
| 5.15.0   | :white_check_mark: |
| < 5.15.0 | :x:                |

## JavaScript Template Security

⚠️ **SECURITY WARNING**: JavaScript templates are **disabled by default** due to security vulnerability [CVE-2021-32827](https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2021-32827).

### Risk Assessment
- **CVSS Score**: 9.6 (CRITICAL)
- **Attack Vector**: Cross-site requests + script injection
- **Impact**: Remote code execution on MockServer host

### Enabling JavaScript Templates (NOT RECOMMENDED)

If you must enable JavaScript templates, understand the risks:

**Via Configuration:**
```java
Configuration config = new Configuration()
    .javascriptTemplatesEnabled(true);
```

**Common file locations:**
- Test classes: `mockserver-core/src/test/java/org/mockserver/templates/engine/javascript/`
- MockServer initialization: `mockserver-netty/src/main/java/org/mockserver/netty/MockServer.java`
- Client usage: `mockserver-client-java/src/main/java/org/mockserver/client/MockServerClient.java`
- Integration tests: `mockserver-integration-testing/src/test/java/`

**Via System Property:**
```bash
-Dmockserver.javascriptTemplatesEnabled=true
```

**Via Environment Variable:**
```bash
export MOCKSERVER_JAVASCRIPT_TEMPLATES_ENABLED=true
```

### Security Mitigations

If JavaScript templates are enabled:

1. **Network Isolation**: Run MockServer in isolated environments only
2. **No Web Browsing**: Don't browse the web while MockServer is running
3. **Firewall Rules**: Block external access to MockServer ports
4. **Template Validation**: Use `mockserver.javascriptDisallowedText` to block dangerous functions
5. **Consider Alternatives**: Use Mustache or Velocity templates instead

### Safer Alternatives

- **Mustache Templates**: Safe, logic-less templating
- **Velocity Templates**: More features than Mustache, configurable security
- **Static Responses**: No templating, maximum security

## Reporting a Vulnerability

Reporting a vulnerabilities is very helpful.

Please report a vulnerability via <a href="https://join-mock-server-slack.herokuapp.com" target="_blank">Slack</a> in a direct message to James Bloom.

If that does not received a response fast enough please raise a github issue with your contact details.
