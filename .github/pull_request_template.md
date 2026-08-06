[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)

## Summary

<!-- Describe what changed and why in 2-3 sentences. -->


## Type of Change

<!-- Check ONE that applies. -->

- [ ] `feat` — New feature
- [ ] `fix` — Bug fix
- [ ] `refactor` — Code restructuring (no behavior change)
- [ ] `test` — Adding or updating tests
- [ ] `docs` — Documentation only
- [ ] `chore` — Build, CI, dependencies, cleanup
- [ ] `build` — Build system or Docker changes

## Related Issue

<!-- Link the GitHub issue: Fixes #123, Closes #456, or N/A for trivial chores/docs -->

Issue:

---

<!-- ====================================================================
     TYPE-SPECIFIC SECTIONS — Keep the one matching your Type of Change,
     delete the rest.
     ==================================================================== -->

<!-- ───── FOR BUG FIXES (`fix`) ───── -->

### Bug Fix Details

<!-- Delete this section if not a bug fix -->

**Root Cause:**
<!-- What was actually wrong? Not just "it crashed" but WHY it crashed. -->


**Impact Analysis:**
<!-- What else could this bug affect? Did you check related code paths? -->


- [ ] Wrote a failing test that reproduces the bug
- [ ] Test passes after the fix
- [ ] Checked for similar patterns elsewhere in the codebase

<!-- ───── FOR NEW FEATURES (`feat`) ───── -->

### Feature Details

<!-- Delete this section if not a feature -->

**Architecture:**
<!-- Which SW360 layers does this touch? (REST → Service → Handler → Repository → CouchDB)
     How does it fit with existing patterns? -->


**Performance:**
<!-- Any impact on memory, query count, or response time? For DB changes, is pagination used? -->


- [ ] Follows existing SW360 controller/service/handler patterns
- [ ] Integration or unit tests added
- [ ] OpenAPI documentation updated (`@Operation`, `@ApiResponse`)
- [ ] Pagination used for list endpoints (if applicable)

<!-- ───── FOR REFACTORING (`refactor`) ───── -->

### Refactor Details

<!-- Delete this section if not a refactor -->

**What changed structurally and why:**


- [ ] No behavior change (existing tests still pass without modification)
- [ ] No public API signature changes (or documented below in Breaking Changes)

<!-- ────────────────────────────────────────────────────────────────────── -->

## How To Test

<!-- Step-by-step so a reviewer can verify. Include curl commands, test classes, or UI steps. -->

1.

## Checklist

Must:
- [ ] All related issues are referenced in commit messages and in PR
- [ ] Commits include `Signed-off-by` line (`git commit -s`) — [ECA required](https://www.eclipse.org/legal/ECA.php)
- [ ] New files include EPL-2.0 license headers
- [ ] Code is formatted (`mvn spotless:apply`)
- [ ] No new compiler warnings introduced
- [ ] I have **manually tested** these changes against a running SW360 instance (or explained why not)

If applicable:
- [ ] Tests added or updated
- [ ] OpenAPI documentation updated for new/changed REST endpoints
- [ ] DB migration script added (`scripts/migrations/`)
- [ ] New dependencies listed in this PR description
- [ ] Breaking changes documented below

## AI Disclosure

<!-- If NO AI tools were used, delete this entire section. -->
<!-- If AI tools WERE used, fill in ALL fields below. -->

- [ ] This PR includes AI-assisted code

**Tool & model:** <!-- e.g., GitHub Copilot (Claude), ChatGPT-4o -->

**What AI generated vs. what you wrote/changed:**
<!-- Be specific: "AI generated the initial CouchDB view, I rewrote the
     map function to only emit clearing-relevant fields" -->


**Your understanding of the changes (in your own words):**
<!-- Explain the architectural reasoning. This proves you understood what
     the AI produced, not just copy-pasted it. -->


**Edge cases you verified:**
<!-- List at least 2 edge cases you tested or considered. -->

1.
2.

## Breaking Changes

<!-- Delete if none. -->

None.

## Additional Notes

<!-- Screenshots, performance measurements, deployment notes. Delete if not needed. -->
