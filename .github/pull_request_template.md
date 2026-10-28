[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)

## Summary

<!-- Describe what changed and why in 2-3 sentences. -->

## Type of Change

- [ ] `feat` — New feature
- [ ] `fix` — Bug fix
- [ ] `refactor` — Code restructuring (no behavior change)
- [ ] `test` — Adding or updating tests
- [ ] `docs` — Documentation only
- [ ] `chore` — Build, CI, dependencies, cleanup
- [ ] `build` — Build system or Docker changes

## Related Issue

Issue:

## Suggest Reviewer

<!-- Suggest reviewers here with an @mention, or leave blank. -->

## How To Test

1.

## Required checklist
- [ ] All related issues are referenced in commit messages and in PR
- [ ] Commits include `Signed-off-by` line (`git commit -s`) — [ECA required](https://www.eclipse.org/legal/ECA.php)
- [ ] New files include EPL-2.0 license headers
- [ ] Code is formatted (`mvn spotless:apply`)
- [ ] I have manually tested these changes against a running SW360 instance (or explained why not)

<details>
<summary>Additional details and quality checks</summary>

### Bug Fix Details

**Root Cause:**

**Impact Analysis:**

- [ ] Wrote a failing test that reproduces the bug
- [ ] Test passes after the fix
- [ ] Checked for similar patterns elsewhere in the codebase

### Feature Details

**Architecture:**

**Performance:**

- [ ] Follows existing SW360 controller/service/handler patterns
- [ ] Integration or unit tests added
- [ ] OpenAPI documentation updated (`@Operation`, `@ApiResponse`)
- [ ] Pagination used for list endpoints (if applicable)

### Refactor Details

**What changed structurally and why:**

- [ ] No behavior change (existing tests still pass without modification)
- [ ] No public API signature changes (or documented below in Breaking Changes)

### Checklist

If applicable:
- [ ] Tests added or updated
- [ ] OpenAPI documentation updated for new/changed REST endpoints
- [ ] DB migration script added (`scripts/migrations/`)
- [ ] New dependencies listed in this PR description
- [ ] Breaking changes documented below

### AI Disclosure

- [ ] This PR includes AI-assisted code

**Tool & model:**

**What AI generated vs. what you wrote/changed:**

**Your understanding of the changes (in your own words):**

**Edge cases you verified:**

1.
2.

**Breaking changes:**

None.

**Additional notes:**

</details>
