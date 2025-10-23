# MockServer Resources Mapping

## For Cloud Desktop with module-name/src+tst structure:

### 1. Transfer resource zips to cloud desktop
```bash
scp *-resources.zip cloud-desktop:/path/to/mockserver/
```

### 2. Run mapping script on cloud desktop
```bash
cd /path/to/mockserver/
./map-resources.sh
```

### 3. Expected structure after mapping:
```
mockserver-core/
├── src/resources/org/mockserver/...     # Main resources
└── tst/resources/org/mockserver/...     # Test resources

mockserver-netty/
├── src/resources/org/mockserver/dashboard/...
└── tst/resources/org/mockserver/...

mockserver-integration-testing/
├── src/resources/org/mockserver/openapi/...
└── tst/resources/org/mockserver/...
```

### 4. Key paths for shouldSupportXmlImports test:
- Test will look for: `org/mockserver/validator/xmlschema/parent.xsd`
- Should be at: `mockserver-core/tst/resources/org/mockserver/validator/xmlschema/parent.xsd`
- Also needs: `mockserver-core/tst/resources/org/mockserver/validator/xmlschema/embedded.xsd`

### 5. Build system classpath adjustment:
Your build system needs to include `tst/resources` in test classpath instead of `src/test/resources`.
