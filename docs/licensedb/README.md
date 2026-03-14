<!--
  ~ Copyright Contributors to the SW360 Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  -->

# LicenseDB Integration Documentation

This directory contains documentation for the SW360 ↔ LicenseDB integration,
developed as part of GSoC 2026 ([Discussion #3631](https://github.com/eclipse-sw360/sw360/discussions/3631)).

## Contents

| File | Description |
|---|---|
| [architecture.md](architecture.md) | Integration architecture, data flow diagrams, component overview, field mappings |
| [configuration.md](configuration.md) | How to enable the integration, all config properties, OAuth2 setup, sync scheduling |
| [api.md](api.md) | REST API endpoints, request/response schemas, error codes |
| [user-guide.md](user-guide.md) | How to trigger manual sync, view status, resolve conflicts, best practices |
| [developer-guide.md](developer-guide.md) | Extending transformers, custom conflict strategies, testing guide |

## Quick Start

1. Obtain OAuth2 credentials from your LicenseDB admin
2. Set the required properties in `sw360.properties` (see [configuration.md](configuration.md))
3. Enable the integration: `licensedb.enabled=true`
4. Restart SW360 — sync will run on the next scheduled cron trigger

## Related Issues and PRs

- Issue [#3685](https://github.com/eclipse-sw360/sw360/issues/3685) — Master integration issue
- PR [#3686](https://github.com/eclipse-sw360/sw360/pull/3686) — Configuration properties
- PR [#3898](https://github.com/eclipse-sw360/sw360/pull/3898) — Data transformation layer
- PR [#3894](https://github.com/eclipse-sw360/sw360/pull/3894) — Exception handling

## Contact

For questions about this integration, reach out to:
- [@GMishx](https://github.com/GMishx)
- [@deo002](https://github.com/deo002)
- [@amritkv](https://github.com/amritkv)
