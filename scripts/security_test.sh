#!/bin/bash
echo "=== MockServer Security Assessment ==="

# Critical: Dependencies & Secrets
echo "1. Checking dependencies..."
./mvnw org.owasp:dependency-check-maven:check

echo "2. Scanning for secrets..."
gitleaks detect --source .

# High: Code vulnerabilities
echo "3. Static code analysis..."
semgrep --config=security .

# Medium: Runtime testing (if MockServer is running)
if curl -s http://localhost:1080 > /dev/null; then
    echo "4. Testing TLS configuration..."
    testssl.sh localhost:1080

    echo "5. HTTP security scan..."
    zap-baseline.py -t http://localhost:1080
else
    echo "MockServer not running - skipping runtime tests"
fi

echo "=== Security Assessment Complete ==="