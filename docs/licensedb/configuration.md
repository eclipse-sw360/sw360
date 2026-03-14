<!--
  ~ Copyright Contributors to the SW360 Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  -->

# LicenseDB Integration — Configuration Guide

## Prerequisites

- SW360 instance running (see main [README](../../README.md))
- A running LicenseDB instance (see [LicenseDB GitHub](https://github.com/fossology/LicenseDb))
- OAuth2 client credentials issued by your LicenseDB instance

## Enabling the Integration

The integration is **disabled by default**. To enable it, set the following
property in your `sw360.properties`:

```properties
licensedb.enabled=true
```

This property acts as a master switch. All sync operations are skipped when set to `false`.

## Configuration Properties Reference

All properties are set in `sw360.properties`. There are three copies of this file
depending on your deployment:

| Deployment Type | File Location |
|---|---|
| Local / bare metal | `libraries/datahandler/src/main/resources/sw360.properties` |
| Build configuration | `build-configuration/resources/sw360.properties` |
| Docker | `scripts/docker-config/etc_sw360/sw360.properties` |

### Connection Settings

| Property | Default | Description |
|---|---|---|
| `licensedb.enabled` | `false` | Master enable/disable switch |
| `licensedb.api.url` | *(none)* | Base URL of your LicenseDB instance e.g. `https://licensedb.example.com` |
| `licensedb.api.version` | `v1` | LicenseDB API version |
| `licensedb.connection.timeout` | `30000` | Connection timeout in milliseconds |
| `licensedb.connection.read-timeout` | `60000` | Read timeout in milliseconds |

### OAuth2 Settings

| Property | Default | Description |
|---|---|---|
| `licensedb.oauth.client.id` | *(none)* | OAuth2 client ID issued by LicenseDB |
| `licensedb.oauth.client.secret` | *(none)* | OAuth2 client secret — keep this secure |

> ⚠️ **Security Note**: Never commit `client.secret` to version control.
> Use environment variable substitution or a secrets manager in production.

### Sync Settings

| Property | Default | Description |
|---|---|---|
| `licensedb.sync.cron` | `0 0 2 * * ?` | Cron expression for automatic sync schedule |
| `licensedb.sync.batch-size` | `100` | Number of licenses fetched per API request |
| `licensedb.sync.on-startup` | `false` | If `true`, triggers a full sync when SW360 starts |

## OAuth2 Client Setup

LicenseDB uses the **OAuth2 Client Credentials** flow (machine-to-machine).

### Step 1: Create a client in LicenseDB

Log in to your LicenseDB admin panel and create a new OAuth2 client application.
Note down the `client_id` and `client_secret` values provided.

### Step 2: Add credentials to sw360.properties

```properties
licensedb.oauth.client.id=your-client-id-here
licensedb.oauth.client.secret=your-client-secret-here
```

### Step 3: Verify token endpoint

SW360 will automatically call LicenseDB's token endpoint at:
```
{licensedb.api.url}/oauth/token
```

## Sync Schedule Configuration

The `licensedb.sync.cron` property uses standard Spring cron format:

```
┌─────────────  second (0–59)
│ ┌───────────  minute (0–59)
│ │ ┌─────────  hour (0–23)
│ │ │ ┌───────  day of month (1–31)
│ │ │ │ ┌─────  month (1–12)
│ │ │ │ │ ┌───  day of week (0–7, 0 and 7 = Sunday)
│ │ │ │ │ │
0 0 2 * * ?   ← default: every day at 2:00 AM
```

### Common schedule examples

| Schedule | Cron Expression |
|---|---|
| Every day at 2:00 AM (default) | `0 0 2 * * ?` |
| Every 6 hours | `0 0 */6 * * ?` |
| Every Monday at midnight | `0 0 0 ? * MON` |
| Every hour | `0 0 * * * ?` |

## Minimal Working Configuration

```properties
# Enable integration
licensedb.enabled=true

# LicenseDB instance URL
licensedb.api.url=https://licensedb.example.com
licensedb.api.version=v1

# OAuth2 credentials
licensedb.oauth.client.id=sw360-client
licensedb.oauth.client.secret=your-secret-here

# Sync schedule (daily at 2 AM)
licensedb.sync.cron=0 0 2 * * ?
licensedb.sync.batch-size=100
licensedb.sync.on-startup=false

# Timeouts
licensedb.connection.timeout=30000
licensedb.connection.read-timeout=60000
```
