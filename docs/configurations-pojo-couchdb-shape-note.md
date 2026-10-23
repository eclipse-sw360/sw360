# Configurations DB Shape Snapshot (Before Core Migration)

Purpose: keep an exact reference of current CouchDB JSON shape vs direct POJO
serialization attempt for `ConfigContainer`, so we can validate the migration
result later.

Date: 2026-08-04  
Source DB/doc: `sw360config / 8ff2b779a868846e45b37c6fda12a137`

## 1) Current stored JSON (thrift-compatible shape)

```json
{
  "_id": "8ff2b779a868846e45b37c6fda12a137",
  "_rev": "10-9bf86fee4071fc02e9ec2d5ddddd60f3",
  "configFor": "SW360_CONFIGURATION",
  "configKeyToValues": {
    "admin.private.project.access.enabled": [
      "true"
    ],
    "attachment.delete.no.of.days": [
      "30"
    ],
    "attachment.store.file.system.location": [
      "/opt/sw360tempattachments"
    ],
    "auto.set.ecc.status": [
      "true"
    ],
    "bulk.release.deleting.enabled": [
      "true"
    ],
    "combined.cli.parser.external.id.correlation.key": [
      ""
    ],
    "component.visibility.restriction.enabled": [
      "true"
    ],
    "disable.clearing.fossology.report.download": [
      "false"
    ],
    "enable.attachment.store.to.file.system": [
      "false"
    ],
    "inherit.attachment.usages": [
      "false"
    ],
    "licenseinfo.spdxparser.use-license-info-from-files": [
      "true"
    ],
    "mainline.state.enabled.for.user": [
      "true"
    ],
    "non.pkg.managed.comps.prop": [
      ""
    ],
    "package.portlet.enabled": [
      "true"
    ],
    "package.portlet.write.access.usergroup": [
      "USER"
    ],
    "release.friendly.url": [
      "http://localhost:3000/components/releases/detail/releaseId"
    ],
    "release.sourcecodeurl.skip.domains": [
      "(?i)\\btrusted\\.(com|de|net)\\b"
    ],
    "rest.apitoken.length": [
      "20"
    ],
    "rest.force.update.enabled": [
      "true"
    ],
    "sbom.import.export.access.usergroup": [
      "USER"
    ],
    "send.component.spreadsheet.export.to.mail.enabled": [
      "false"
    ],
    "send.project.spreadsheet.export.to.mail.enabled": [
      "false"
    ],
    "spdx.document.enabled": [
      "true"
    ],
    "sw360.tool.name": [
      "SW360-Test"
    ],
    "sw360.tool.vendor": [
      "Eclipse Foundation"
    ],
    "vcs.hosts": [
      "[]"
    ]
  }
}
```

## 2) Direct POJO serialization attempt (current)

```json
{
  "configFor": "SW360_CONFIGURATION",
  "configKeyToValues": {
    "admin.private.project.access.enabled": [
      "true"
    ],
    "attachment.delete.no.of.days": [
      "30"
    ],
    "attachment.store.file.system.location": [
      "/opt/sw360tempattachments"
    ],
    "auto.set.ecc.status": [
      "true"
    ],
    "bulk.release.deleting.enabled": [
      "true"
    ],
    "combined.cli.parser.external.id.correlation.key": [
      ""
    ],
    "component.visibility.restriction.enabled": [
      "true"
    ],
    "disable.clearing.fossology.report.download": [
      "false"
    ],
    "enable.attachment.store.to.file.system": [
      "false"
    ],
    "inherit.attachment.usages": [
      "false"
    ],
    "licenseinfo.spdxparser.use-license-info-from-files": [
      "true"
    ],
    "mainline.state.enabled.for.user": [
      "true"
    ],
    "non.pkg.managed.comps.prop": [
      ""
    ],
    "package.portlet.enabled": [
      "true"
    ],
    "package.portlet.write.access.usergroup": [
      "USER"
    ],
    "release.friendly.url": [
      "http://localhost:3000/components/releases/detail/releaseId"
    ],
    "release.sourcecodeurl.skip.domains": [
      "(?i)\\btrusted\\.(com|de|net)\\b"
    ],
    "rest.apitoken.length": [
      "20"
    ],
    "rest.force.update.enabled": [
      "true"
    ],
    "sbom.import.export.access.usergroup": [
      "USER"
    ],
    "send.component.spreadsheet.export.to.mail.enabled": [
      "false"
    ],
    "send.project.spreadsheet.export.to.mail.enabled": [
      "false"
    ],
    "spdx.document.enabled": [
      "true"
    ],
    "sw360.tool.name": [
      "SW360-Test"
    ],
    "sw360.tool.vendor": [
      "Eclipse Foundation"
    ],
    "vcs.hosts": [
      "[]"
    ]
  },
  "id": "8ff2b779a868846e45b37c6fda12a137",
  "revision": "10-9bf86fee4071fc02e9ec2d5ddddd60f3"
}
```

## 3) Exact difference

- Only in stored shape: `_id`, `_rev`
- Only in direct POJO shape: `id`, `revision`
- Same keys: `configFor`, `configKeyToValues`

Conclusion: for `ConfigContainer`, the content payload already matches; the
difference is document identity fields (`_id/_rev` vs `id/revision`).

## 4) Live verification after POJO migration (2026-08-04)

Environment:

- Deployed WAR: `backend-configurations-20.1.0-rc-2.war` → Tomcat `/configurations`
- Handler/repository path uses service-api `ConfigContainer` (no thrift types in configurations module)
- CouchDB doc under test: `sw360config / 8ff2b779a868846e45b37c6fda12a137`
- Internal auth token configured; GET returned configs successfully

### 4.1 Read path

`GET /configurations/api/configurations` returned merged SW360 + UI config map, including:

- `sw360.tool.name` = `SW360-Test`
- `configFor` content from the stored SW360_CONFIGURATION document

`GET /configurations/api/configurations/group/SW360_CONFIGURATION` returned 25 keys from that container only.

Pre-write CouchDB top-level keys (unchanged from section 1):

`["_id", "_rev", "configFor", "configKeyToValues"]`

### 4.2 Write path (update + restore)

1. `PUT /configurations/api/configurations` with body `{"sw360.tool.name":"SW360-Test-POJO-SHAPE"}` as ADMIN → `"SUCCESS"`
2. Re-fetched CouchDB document immediately after write
3. Restored original value `SW360-Test` → `"SUCCESS"`

After POJO-backed write (rev bumped, value changed, **shape preserved**):

```json
{
  "_id": "8ff2b779a868846e45b37c6fda12a137",
  "_rev": "11-5077f51cd9abab6b2d4035d05fafb46a",
  "configFor": "SW360_CONFIGURATION",
  "configKeyToValues": {
    "sw360.tool.name": [
      "SW360-Test-POJO-SHAPE"
    ]
  }
}
```

(Other `configKeyToValues` entries omitted above; present and unchanged in the live document.)

After restore:

| Check | Result |
| --- | --- |
| Top-level keys | `["_id", "_rev", "configFor", "configKeyToValues"]` |
| Has `id` / `revision`? | No |
| Has `_id` / `_rev`? | Yes |
| `_rev` progression | `10-…` → `11-…` (write) → `12-…` (restore) |
| `sw360.tool.name` final | `["SW360-Test"]` |
| API GET after restore | `SW360-Test` |

### 4.3 Conclusion

`DatabaseConnectorCloudant.getDocumentFromPojo` already maps POJO `id`/`revision` onto CouchDB `_id`/`_rev` and strips them from properties. After the configurations service switched to service-api `ConfigContainer`, **stored document shape stayed thrift-compatible** on both read and write. No CouchDB migration / codec layer was required for this entity.
