## Summary
This PR adds the foundational configuration infrastructure for LicenseDB integration as specified in issue #3685.

## Changes Made

### 1. Core Constants (SW360Constants.java)
Added 10 LicenseDB configuration constants:
- `licensedb.enabled` - Enable/disable integration (default: false)
- `licensedb.api.url` - LicenseDB API base URL
- `licensedb.api.version` - API version (default: v1)
- `licensedb.oauth.client.id` - OAuth 2.0 client ID
- `licensedb.oauth.client.secret` - OAuth 2.0 client secret
- `licensedb.sync.cron` - Sync schedule (default: 0 0 2 * * ?)
- `licensedb.sync.batch-size` - Batch size (default: 100)
- `licensedb.sync.on-startup` - Auto-sync on startup (default: false)
- `licensedb.connection.timeout` - Connection timeout (default: 30000ms)
- `licensedb.connection.read-timeout` - Read timeout (default: 60000ms)

### 2. Unit Tests (LicenseDBConstantsTest.java)
- 10 test methods covering all constants
- Validates constant names, values, and prefixes
- 100% code coverage for new constants

### 3. Property Files Updated
Updated sw360.properties in 3 locations:
- `libraries/datahandler/src/main/resources/sw360.properties`
- `build-configuration/resources/sw360.properties`
- `scripts/docker-config/etc_sw360/sw360.properties`

## Testing
- [x] All unit tests pass
- [x] Code compiles successfully
- [x] No breaking changes (all properties disabled by default)
- [x] Follows existing code style
- [x] Signed commits for ECA compliance

## Verification
```bash
# Compile the module
mvn clean compile -pl libraries/datahandler -am

# Run tests
mvn test -pl libraries/datahandler
```

## Backward Compatibility
✅ All new properties are disabled by default (`licensedb.enabled=false`)
✅ No changes to existing functionality
✅ No breaking changes to API or data models

## Related
- Closes #3685
- Part of GSoC 2026 project (Discussion #3631)

/cc @GMishx @deo002
