#!/usr/bin/env python3
# -----------------------------------------------------------------------------
# Copyright Shivamrut<gshivamrut@gmail.com>, 2026.
# Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# -----------------------------------------------------------------------------
"""Report how far the Thrift -> service-api POJO migration has progressed.

Counts *type usages*, not import lines: for every production source file the
Thrift and service-api imports are resolved to their simple names and those
names are then counted in the file body. Counting import lines instead would
undercount by roughly 6x, because a type imported once is typically used many
times.

`import pkg.*;` wildcards are resolved too, via a type index built from the
`.thrift` IDL (Thrift java is generated at build time, so there is no source to
read) and from the service-api sources. Skipping that step hides every type in
the 24 files that wildcard-import `org.eclipse.sw360.datahandler.thrift`,
ComponentDatabaseHandler among them, and understates the remaining work by
roughly a quarter.

Usage
-----
    python3 scripts/thrift-migration-report.py                  # plain text
    python3 scripts/thrift-migration-report.py --markdown       # GitHub summary
    python3 scripts/thrift-migration-report.py --baseline DIR    # add a delta

The numbers are an estimate meant for tracking direction and relative size.
They are not a precise count and this script never fails a build.
"""

from __future__ import annotations

import argparse
import os
import re
import sys

THRIFT_PKG = "org.eclipse.sw360.datahandler.thrift."
POJO_PKG = "org.eclipse.sw360.datahandler.services."

DEFAULT_ROOTS = ("backend", "libraries", "rest")

# Code whose only purpose is to bridge Thrift and POJOs. It is deleted wholesale
# at the end of the migration rather than converted, so it is reported apart
# from the work that actually remains.
SCAFFOLDING_MARKERS = (
    "/common/utils/converter/",
    "/thriftbridge/",
    "/datahandler/thrift/",
    "RestMapper.java",
    "ThriftAdapter.java",
    "ServiceRestAdapter.java",
)

IMPORT_RE = re.compile(r"^import\s+(?:static\s+)?([\w.]+);", re.MULTILINE)
WILDCARD_RE = re.compile(r"^import\s+([\w.]+)\.\*;", re.MULTILINE)
BLOCK_COMMENT_RE = re.compile(r"/\*.*?\*/", re.DOTALL)
LINE_COMMENT_RE = re.compile(r"//[^\n]*")

THRIFT_DECL_RE = re.compile(r"^(?:struct|enum|exception|union|service)\s+(\w+)", re.MULTILINE)
NAMESPACE_RE = re.compile(r"^namespace\s+java\s+([\w.]+)", re.MULTILINE)
PACKAGE_RE = re.compile(r"^package\s+([\w.]+);", re.MULTILINE)


def build_type_index(root_dir: str) -> dict[str, set[str]]:
    """Map java package -> simple type names, so `import pkg.*;` can be resolved.

    Without this, every type reached through a wildcard import is invisible:
    24 files use `import org.eclipse.sw360.datahandler.thrift.*;`, including
    ComponentDatabaseHandler, and their Thrift usage would go uncounted.

    Thrift types are generated at build time so there is no java source to read;
    they come from the `.thrift` IDL instead. POJO types are read from service-api.
    """
    index: dict[str, set[str]] = {}

    for dirpath, _, filenames in os.walk(root_dir):
        if "/target/" in dirpath:
            continue
        for filename in filenames:
            path = os.path.join(dirpath, filename)
            try:
                if filename.endswith(".thrift"):
                    text = open(path, encoding="utf-8", errors="replace").read()
                    namespace = NAMESPACE_RE.search(text)
                    if namespace:
                        index.setdefault(namespace.group(1), set()).update(
                            THRIFT_DECL_RE.findall(text)
                        )
                elif filename.endswith(".java") and "/service-api/" in path:
                    text = open(path, encoding="utf-8", errors="replace").read()
                    package = PACKAGE_RE.search(text)
                    if package:
                        index.setdefault(package.group(1), set()).add(filename[:-5])
            except OSError:
                continue
    return index


def strip_noise(source: str) -> str:
    """Drop imports and comments so they are not counted as usages."""
    body = "\n".join(
        line for line in source.split("\n") if not line.lstrip().startswith("import ")
    )
    body = BLOCK_COMMENT_RE.sub("", body)
    return LINE_COMMENT_RE.sub("", body)


def count_usages(source: str, body: str, package: str,
                 type_index: dict[str, set[str]]) -> int:
    """Usages of types from `package`: by simple name if imported, else fully qualified.

    Handles single-type imports, `import pkg.*;` wildcards (resolved through
    `type_index`) and inline fully-qualified references.

    A single-type import shadows an on-demand (wildcard) import of the same simple
    name, as in the JLS. Modelling that matters here: a migrated file typically adds
    `import ...services.common.RequestStatus;` while keeping `import ...thrift.*;`
    for other types, and without the shadowing rule the name would be charged to
    both sides, hiding the very progress this report exists to show.
    """
    explicit = {
        imported.rsplit(".", 1)[-1]: imported for imported in IMPORT_RE.findall(source)
    }
    names = {
        simple
        for simple, fqn in explicit.items()
        if fqn.startswith(package) and simple and simple[0].isupper()
    }
    for wildcard in WILDCARD_RE.findall(source):
        if wildcard + "." == package or wildcard.startswith(package):
            names |= {
                candidate
                for candidate in type_index.get(wildcard, set())
                if candidate not in explicit and candidate and candidate[0].isupper()
            }
    total = sum(len(re.findall(r"\b%s\b" % re.escape(name), body)) for name in names)
    return total + len(re.findall(re.escape(package), body))


def is_production_source(path: str) -> bool:
    return (
        path.endswith(".java")
        and "/target/" not in path
        and "/src/test/" not in path
        and "/src/main/java/" in path
    )


def module_of(path: str, root_dir: str) -> str:
    """Group by maven module, e.g. backend/common or rest/resource-server."""
    rel = os.path.relpath(path, root_dir).replace(os.sep, "/")
    parts = rel.split("/")
    return "/".join(parts[:2]) if len(parts) >= 2 else rel


def scan(root_dir: str, roots: tuple[str, ...]) -> dict:
    type_index = build_type_index(root_dir)
    modules: dict[str, dict[str, int]] = {}
    scaffolding = 0
    files_with_thrift = 0
    files_total = 0

    for top in roots:
        for dirpath, _, filenames in os.walk(os.path.join(root_dir, top)):
            for filename in filenames:
                path = os.path.join(dirpath, filename)
                if not is_production_source(path):
                    continue
                try:
                    with open(path, encoding="utf-8", errors="replace") as handle:
                        source = handle.read()
                except OSError:
                    continue

                body = strip_noise(source)
                thrift = count_usages(source, body, THRIFT_PKG, type_index)
                pojo = count_usages(source, body, POJO_PKG, type_index)
                if not thrift and not pojo:
                    continue

                files_total += 1
                if thrift:
                    files_with_thrift += 1
                    if any(marker in path for marker in SCAFFOLDING_MARKERS):
                        scaffolding += thrift

                bucket = modules.setdefault(
                    module_of(path, root_dir), {"thrift": 0, "pojo": 0}
                )
                bucket["thrift"] += thrift
                bucket["pojo"] += pojo

    return {
        "modules": modules,
        "thrift": sum(m["thrift"] for m in modules.values()),
        "pojo": sum(m["pojo"] for m in modules.values()),
        "scaffolding": scaffolding,
        "files_with_thrift": files_with_thrift,
        "files_total": files_total,
    }


def percent_migrated(result: dict) -> float:
    """POJO share of convertible type usage.

    Scaffolding is excluded from the Thrift side: it disappears at teardown
    without anyone converting it, so counting it would understate progress.
    """
    convertible = result["thrift"] - result["scaffolding"]
    denominator = result["pojo"] + max(convertible, 0)
    return 100.0 * result["pojo"] / denominator if denominator else 100.0


def signed(value: int) -> str:
    return f"{value:+d}" if value else "0"


def render(result: dict, baseline: dict | None, markdown: bool) -> str:
    out: list[str] = []
    head, cell = ("| ", " |") if markdown else ("  ", "")

    if markdown:
        out.append("## 🧵 Thrift migration progress")
        out.append("")
        out.append(f"**{percent_migrated(result):.1f}% migrated** "
                   f"— {result['files_with_thrift']} of {result['files_total']} "
                   f"production files still reference Thrift.")
        out.append("")
        out.append("| Module | Thrift | POJO |")
        out.append("|---|---:|---:|")
    else:
        out.append(f"Thrift migration progress: {percent_migrated(result):.1f}% migrated")
        out.append(f"  {result['files_with_thrift']} of {result['files_total']} "
                   f"production files still reference Thrift")
        out.append("")
        out.append(f"  {'MODULE':<32}{'THRIFT':>9}{'POJO':>9}")

    for name, counts in sorted(
        result["modules"].items(), key=lambda item: -item[1]["thrift"]
    ):
        if not counts["thrift"] and not counts["pojo"]:
            continue
        if markdown:
            out.append(f"| `{name}` | {counts['thrift']} | {counts['pojo']} |")
        else:
            out.append(f"  {name:<32}{counts['thrift']:>9}{counts['pojo']:>9}")

    if markdown:
        out.append(f"| **total** | **{result['thrift']}** | **{result['pojo']}** |")
    else:
        out.append(f"  {'total':<32}{result['thrift']:>9}{result['pojo']:>9}")

    out.append("")
    out.append(
        ("_Of the Thrift total, %d usages live in bridge scaffolding "
         "(converters, RestMappers, adapters) that is deleted rather than "
         "converted._" % result["scaffolding"])
        if markdown
        else "  of which %d are bridge scaffolding (deleted, not converted)"
        % result["scaffolding"]
    )

    if baseline:
        d_thrift = result["thrift"] - baseline["thrift"]
        d_pojo = result["pojo"] - baseline["pojo"]
        d_pct = percent_migrated(result) - percent_migrated(baseline)
        verdict = (
            "removes Thrift usage 🎉" if d_thrift < 0
            else "adds Thrift usage ⚠️" if d_thrift > 0
            else "leaves Thrift usage unchanged"
        )
        out.append("")
        if markdown:
            out.append(f"### This change {verdict}")
            out.append("")
            out.append("| | Thrift | POJO | Migrated |")
            out.append("|---|---:|---:|---:|")
            out.append(f"| base | {baseline['thrift']} | {baseline['pojo']} "
                       f"| {percent_migrated(baseline):.1f}% |")
            out.append(f"| head | {result['thrift']} | {result['pojo']} "
                       f"| {percent_migrated(result):.1f}% |")
            out.append(f"| **delta** | **{signed(d_thrift)}** | **{signed(d_pojo)}** "
                       f"| **{d_pct:+.2f} pp** |")
        else:
            out.append(f"  vs base: thrift {signed(d_thrift)}  pojo {signed(d_pojo)}  "
                       f"({d_pct:+.2f} pp) — {verdict}")

    del head, cell
    return "\n".join(out)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("roots", nargs="*", default=list(DEFAULT_ROOTS),
                        help="top-level directories to scan (default: %s)"
                             % " ".join(DEFAULT_ROOTS))
    parser.add_argument("--markdown", action="store_true",
                        help="emit a GitHub-flavoured markdown summary")
    parser.add_argument("--baseline", metavar="DIR",
                        help="a checkout to compare against, to show the delta")
    parser.add_argument("--repo-root", default=".", help="repository root to scan")
    args = parser.parse_args()

    roots = tuple(args.roots) or DEFAULT_ROOTS
    result = scan(args.repo_root, roots)

    baseline = None
    if args.baseline:
        if os.path.isdir(args.baseline):
            baseline = scan(args.baseline, roots)
        else:
            print(f"note: baseline {args.baseline!r} not found, "
                  "reporting absolute numbers only", file=sys.stderr)

    print(render(result, baseline, args.markdown))
    return 0


if __name__ == "__main__":
    # Never fail a build: this is a report, not a gate.
    try:
        sys.exit(main())
    except Exception as error:  # noqa: BLE001
        print(f"thrift-migration-report failed: {error}", file=sys.stderr)
        sys.exit(0)
