# SW360 Archive Format (`.tar.gz`)

The archival service produces and consumes a single TAR.GZ bundle per archive
operation. This document is the contract — both the archive flow (writes) and
the restore flow (reads) must follow it.

## File name

```
sw360_archive_<bundleId>.tar.gz
```

`bundleId` is the same value that appears in `manifest.json` and the
`ArchivalRecord` registry entry.

## Layout

```
sw360_archive_<bundleId>.tar.gz
├── manifest.json                        <- read first by restore preview
│
├── projects/
│   └── <projectId>/
│       ├── project.json                 <- the Project document
│       ├── changelogs.json              <- full audit history
│       ├── obligations.json             <- accepted license obligations
│       ├── clearing-requests.json
│       └── attachments/
│           ├── <attachmentId>.bin       <- raw binary
│           └── <attachmentId>.meta.json <- filename, mimetype, sha, etc.
│
├── components/
│   └── <componentId>/
│       ├── component.json
│       ├── changelogs.json
│       ├── attachments/
│       └── releases/
│           └── <releaseId>/
│               ├── release.json
│               ├── changelogs.json
│               ├── spdx.json            <- if exists
│               ├── attachments/
│               └── packages/
│                   └── <packageId>/
│                       ├── package.json
│                       └── changelogs.json
│
├── releases/                            <- for standalone-archived releases
│   └── <releaseId>/
│       └── (same shape as nested release above)
│
└── packages/                            <- for orphan packages only
    └── <packageId>/
        ├── package.json
        ├── changelogs.json
        └── moderation-requests.json


## Notes

1. **`manifest.json` is always at the root** and is the first file written.
   Restore preview reads only this file.
2. **Attachments are raw binary** (`<id>.bin`), never base64 inside JSON.
   They are streamed in directly so multi-GB files do not load into memory.
   The sibling `<id>.meta.json` carries filename, content type, SHA, uploader,
   and upload timestamp.
3. **One folder per entity ID**, even if the bundle has a single entity —
   the layout stays consistent across bundle sizes.
4. **Nested vs. standalone** — a Release archived as part of its parent
   Component lives under `components/<id>/releases/<id>/`. A Release archived
   on its own lives under `releases/<id>/`. Restore handles both.
5. **Orphan Packages** (Packages with no live parent Release) live under
   `packages/<id>/`. Packages with a live parent Release are bundled with that
   Release.

## Versioning

`manifest.json` carries a `manifestVersion` field. Bump it whenever the layout
or manifest shape changes in a breaking way; restore reads it first and refuses
to operate on a manifest version it does not understand.
