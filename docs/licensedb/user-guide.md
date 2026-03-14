<!--
  ~ Copyright Contributors to the SW360 Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  -->

# LicenseDB Integration — User Guide

## Overview

Once LicenseDB integration is enabled and configured, SW360 automatically keeps
its license and obligation data in sync with LicenseDB. This guide explains how
to trigger syncs manually, monitor sync status, and resolve conflicts.

## Triggering a Manual Sync

You can trigger a sync at any time without waiting for the scheduled cron job.

### Via REST API

```http
POST /api/licensedb/sync
Authorization: Bearer <your-sw360-token>
```

**Response (202 Accepted):**
```json
{
  "status": "STARTED",
  "syncId": "sync-20260314-020000",
  "message": "LicenseDB sync started successfully"
}
```

### What happens during a sync

1. SW360 authenticates with LicenseDB using OAuth2
2. Licenses are fetched in batches (default: 100 per request)
3. Each license is transformed into SW360's internal format
4. Obligations linked to each license are fetched and transformed
5. Conflicts with existing SW360 data are resolved
6. Data is written to CouchDB

## Viewing Sync Status

### Check the latest sync

```http
GET /api/licensedb/sync/status
Authorization: Bearer <your-sw360-token>
```

**Response:**
```json
{
  "lastSyncTime": "2026-03-14T02:00:00Z",
  "status": "SUCCESS",
  "licensesImported": 423,
  "obligationsImported": 891,
  "conflicts": 2,
  "errors": 0
}
```

### Sync status values

| Status | Meaning |
|---|---|
| `STARTED` | Sync is currently running |
| `SUCCESS` | Sync completed with no errors |
| `PARTIAL` | Sync completed but some items had conflicts or warnings |
| `FAILED` | Sync failed — check logs for details |

## Resolving Conflicts

A conflict occurs when a license already exists in SW360 with data that differs
from what LicenseDB returns.

### Viewing conflicts

```http
GET /api/licensedb/sync/conflicts
Authorization: Bearer <your-sw360-token>
```

**Response:**
```json
{
  "conflicts": [
    {
      "licenseShortName": "Apache-2.0",
      "field": "text",
      "sw360Value": "...(existing text)...",
      "licenseDbValue": "...(new text)...",
      "resolution": "PENDING"
    }
  ]
}
```

### Resolving a conflict manually

```http
POST /api/licensedb/sync/conflicts/{licenseShortName}/resolve
Authorization: Bearer <your-sw360-token>
Content-Type: application/json

{
  "strategy": "USE_LICENSEDB"
}
```

**Resolution strategies:**

| Strategy | Behaviour |
|---|---|
| `USE_LICENSEDB` | Overwrite SW360 data with LicenseDB data |
| `KEEP_SW360` | Keep existing SW360 data, ignore LicenseDB value |

## Best Practices

**Do not create licenses manually** once LicenseDB integration is enabled. Manual entries
will be overwritten on the next sync or flagged as conflicts.

**Schedule syncs during off-peak hours.** The default cron (`0 0 2 * * ?`) runs at 2:00 AM.
Adjust this to match your organization's maintenance window.

**Monitor sync logs** after the first sync to catch any mapping issues early, especially
if your LicenseDB instance has custom obligation types.

**Use `on-startup: false`** in production environments to avoid long startup delays.
Reserve `on-startup: true` for development or initial setup.

**Increase batch size cautiously.** Larger batches (e.g., 500) speed up sync but increase
memory usage and the risk of timeout on slow LicenseDB instances.
