<!--
  ~ Copyright Contributors to the SW360 Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  -->

# LicenseDB Integration — API Documentation

## Authentication

All LicenseDB endpoints require a valid SW360 bearer token.

```http
Authorization: Bearer <sw360-access-token>
```

---

## Endpoints

### Trigger Manual Sync

```
POST /api/licensedb/sync
```

Starts a full synchronization from LicenseDB immediately, independent of the
configured cron schedule.

**Request:** No body required.

**Response `202 Accepted`:**
```json
{
  "status": "STARTED",
  "syncId": "sync-20260314-020000",
  "message": "LicenseDB sync started successfully"
}
```

**Response `503 Service Unavailable`** (LicenseDB unreachable):
```json
{
  "error": "LicenseDB connection failed",
  "details": "Connection timed out after 30000ms"
}
```

---

### Get Sync Status

```
GET /api/licensedb/sync/status
```

Returns the result of the most recent sync operation.

**Response `200 OK`:**
```json
{
  "lastSyncTime": "2026-03-14T02:00:00Z",
  "status": "SUCCESS",
  "licensesImported": 423,
  "obligationsImported": 891,
  "conflicts": 2,
  "errors": 0,
  "durationMs": 4821
}
```

---

### List Conflicts

```
GET /api/licensedb/sync/conflicts
```

Returns all unresolved conflicts from the last sync.

**Response `200 OK`:**
```json
{
  "conflicts": [
    {
      "licenseShortName": "Apache-2.0",
      "field": "text",
      "sw360Value": "existing license text...",
      "licenseDbValue": "updated license text...",
      "resolution": "PENDING"
    }
  ]
}
```

---

### Resolve a Conflict

```
POST /api/licensedb/sync/conflicts/{licenseShortName}/resolve
```

Resolves a specific conflict for a license.

**Path parameter:** `licenseShortName` — e.g., `Apache-2.0`

**Request body:**
```json
{
  "strategy": "USE_LICENSEDB"
}
```

**Available strategies:**

| Value | Effect |
|---|---|
| `USE_LICENSEDB` | Overwrite SW360 data with LicenseDB value |
| `KEEP_SW360` | Retain existing SW360 data, discard LicenseDB value |

**Response `200 OK`:**
```json
{
  "licenseShortName": "Apache-2.0",
  "resolution": "USE_LICENSEDB",
  "status": "RESOLVED"
}
```

---

## Error Codes

| HTTP Status | Meaning |
|---|---|
| `202` | Sync started successfully |
| `200` | Request processed successfully |
| `400` | Invalid request body or unknown strategy |
| `401` | Missing or invalid SW360 bearer token |
| `404` | License short name not found in conflicts list |
| `503` | LicenseDB instance unreachable |
