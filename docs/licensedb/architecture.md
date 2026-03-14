<!--
  ~ Copyright Contributors to the SW360 Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  -->

# LicenseDB Integration — Architecture

## Overview

LicenseDB is an open-source license and obligation database maintained by the FOSSology project.
This integration establishes LicenseDB as the **sole source of truth** for all license and
obligation data within SW360, replacing manual license creation and direct SPDX/OSADL imports.

## Goals

- Remove fragmented, manual license management from SW360
- Fetch licenses and obligations directly from LicenseDB via REST API
- Ensure consistent, up-to-date license data across all SW360 instances
- Support scheduled and on-demand synchronization

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                        SW360                            │
│                                                         │
│  ┌─────────────┐    ┌──────────────┐   ┌────────────┐  │
│  │ REST Client │───▶│ Transformers │──▶│  CouchDB   │  │
│  │(HTTP + Auth)│    │  (Mapping)   │   │ (Storage)  │  │
│  └──────┬──────┘    └──────────────┘   └────────────┘  │
│         │                                               │
│  ┌──────▼──────┐    ┌──────────────┐                   │
│  │ OAuth2      │    │   Conflict   │                   │
│  │ Token Mgr   │    │   Resolver   │                   │
│  └─────────────┘    └──────────────┘                   │
│                                                         │
│  ┌──────────────────────────────────────┐               │
│  │         Sync Scheduler               │               │
│  │  (Cron-based or manual trigger)      │               │
│  └──────────────────────────────────────┘               │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTPS + OAuth2
                       ▼
           ┌───────────────────────┐
           │       LicenseDB       │
           │  (FOSSology Project)  │
           │  /licenses            │
           │  /obligations         │
           └───────────────────────┘
```

## Component Descriptions

### REST Client
Handles all outbound HTTP communication to the LicenseDB API. Responsible for:
- Acquiring and refreshing OAuth2 access tokens
- Paginated fetching of licenses and obligations
- Respecting connection and read timeouts
- Raising typed exceptions on failure

### Transformers
Convert LicenseDB's data format into SW360's internal data model.
Two transformers exist:

| Transformer | Input | Output |
|---|---|---|
| `LicenseTransformer` | `LicenseDBLicense` | SW360 `License` |
| `ObligationTransformer` | `LicenseDBObligation` | SW360 `Obligation` |

See [Field Mapping](#field-mapping) below for details.

### OAuth2 Token Manager
Implements the **Machine-to-Machine (M2M) OAuth2 client credentials flow**:
1. SW360 sends `client_id` + `client_secret` to LicenseDB's token endpoint
2. Receives a bearer token
3. Attaches token to all subsequent API requests
4. Refreshes token before expiry

### Conflict Resolver
Handles situations where a license fetched from LicenseDB already exists in SW360
with differing data. Strategies include:
- **Overwrite**: LicenseDB data always wins
- **Skip**: Existing SW360 data is preserved
- **Flag**: Conflict is logged for manual review

### Sync Scheduler
Triggers synchronization on a configurable cron schedule (default: daily at 2:00 AM).
Also supports manual sync via REST API endpoint.

## Data Flow

```
LicenseDB API
     │
     │  1. Fetch licenses (paginated)
     ▼
REST Client
     │
     │  2. Pass raw LicenseDB objects
     ▼
Transformers
     │
     │  3. SW360-compatible License/Obligation objects
     ▼
Conflict Resolver
     │
     │  4. Resolved, deduplicated objects
     ▼
CouchDB (SW360 Database)
```

## Field Mapping

### License Fields

| LicenseDB Field | SW360 Field |
|---|---|
| `shortname` | `shortname` |
| `fullname` | `fullname` |
| `text` | `text` |
| `url` | `url` |
| `licenseType` | `licenseType` |
| `osiApproved` | `osiApproved` |

### Obligation Fields

| LicenseDB Field | SW360 Field |
|---|---|
| `text` | `text` |
| `title` | `title` |
| `type` | `obligationType` |
| `level` | `obligationLevel` |

## Location in SW360 Codebase

```
rest/resource-server/src/main/java/org/eclipse/sw360/rest/resourceserver/licensedb/
├── client/          # REST client + OAuth2 token management
├── transformer/     # LicenseTransformer, ObligationTransformer
├── resolver/        # Conflict resolution strategies
└── scheduler/       # Cron sync scheduler
```
