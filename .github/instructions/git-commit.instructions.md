---
applyTo: "**"
---

# SW360 Git Commit & Contribution Instructions

> **Eclipse SW360 follows conventional commits with signed-off commits (DCO)**

## Commit Message Format

```
<type>(<scope>): <description>

[optional body with more details]

Signed-off-by: Your Name <your.email@example.com>
```

### Rules
- Use the conventional commit format: `<type>(<scope>): <description>`
- **REQUIRED**: All commits must include `Signed-off-by` line (use `git commit -s`)
- Keep the subject line concise (under 72 characters, ideally under 50)
- Use imperative mood (e.g., "add" not "added" or "adds")
- Don't end the subject with a period
- Use lowercase for the first word unless it's a proper noun (e.g., `KeyCloak`, `CouchDB`)
- Separate subject from body with a blank line
- Wrap body at 72 characters

### Types
| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat(rest): add endpoint for project dependencies` |
| `fix` | Bug fix | `fix(backend): null check for license text` |
| `docs` | Documentation only | `docs(rest): add OpenAPI examples for MAP parameters` |
| `style` | Formatting, no code change | `style(rest): fix indentation in controller` |
| `refactor` | Code restructure, no behavior change | `refactor(backend): extract validation logic` |
| `test` | Adding/fixing tests | `test(rest): add integration tests for search API` |
| `chore` | Build, CI, dependencies, cleanup | `chore(deps): bump spring-boot from 3.5.2 to 3.5.3` |
| `build` | Build system changes | `build(docker): update base image to Java 21` |

### Common Scopes (SW360-specific)
| Scope | Description |
|-------|-------------|
| `rest` | REST API (resource-server) |
| `backend` | Backend Thrift services |
| `deps` | Dependency updates |
| `deps-dev` | Dev dependency updates |
| `KeyCloak` | Keycloak integration |
| `component` | Component service |
| `release` | Release service |
| `project` | Project service |
| `license` | License service |
| `obligation` | Obligation service |
| `vulnerability` | Vulnerability service |
| `LicenseInfo` | License info parsing |
| `CR` | Clearing requests |
| `ECC` | Export control |
| `importer` | Import functionality |
| `docker` | Docker configuration |

### Examples from Project History
```bash
# Feature
feat(rest): add endpoint for getting project dependencies
feat(KeyCloak): Use UserRepository

# Fix
fix(rest): Add shortname field to license REST API response
fix(LicenseInfo): null check for license text
fix(obligation): fix the pagination /obligations

# Chore
chore(deps): bump spring-boot from 3.5.2 to 3.5.3
chore(license): Add final modifier to CONTENT_TYPE constant
chore(rest): Remove outdated TODO comment

# Docs
docs(rest): Add OpenAPI examples for MAP value parameters
```

---

## Branch Naming Conventions

- **Always create** new branch from `https://github.com/eclipse-sw360/sw360/main`
- Use kebab-case (lowercase with hyphens)
- Follow the pattern: `<type>/<issue-number>-<short-description>`

### Branch Types
| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat/1234-add-dark-mode` |
| `fix` | Bug fix | `fix/5678-null-pointer-in-search` |
| `docs` | Documentation | `docs/9012-update-api-docs` |
| `refactor` | Code refactor | `refactor/3456-cleanup-controller` |
| `chore` | Maintenance | `chore/7890-update-dependencies` |

---

## Git Workflow

### Before Starting Work
```bash
# Sync with upstream main
git fetch upstream
git checkout main
git rebase upstream/main

# Create feature branch
git checkout -b feat/1234-my-feature
```

### Committing Changes
```bash
# Stage changes
git add .

# Commit with sign-off (REQUIRED)
git commit -s -m "feat(rest): add new endpoint for X"

# For additional changes, amend the commit
git add .
git commit --amend -s
```

### Before Pushing
```bash
# Rebase on latest main
git fetch upstream
git rebase upstream/main

# Push to your fork (siemens remote)
git push siemens feat/1234-my-feature

# If you amended, force push with lease
git push siemens feat/1234-my-feature --force-with-lease
```

### Remotes Setup
```bash
# Upstream (eclipse-sw360 - read-only for PRs)
git remote add upstream https://github.com/eclipse-sw360/sw360.git

# Your fork (siemens - push here)
git remote add siemens https://github.com/siemens/sw360.git
```

---

## Pull Request Guidelines

### Before Creating PR
- [ ] All commits are signed-off (`Signed-off-by:` line present)
- [ ] Branch is rebased on latest `main`
- [ ] All tests pass locally (`mvn test`)
- [ ] Code is formatted (`mvn spotless:apply`)
- [ ] New files have EPL-2.0 license headers

### PR Description Template
Use the following template when creating a PR:

```markdown
[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)

> Please provide a summary of your changes here.
> * Which issue is this pull request belonging to and how is it solving it? (*Refer to issue here*)
> * Did you add or update any new dependencies that are required for your change?

Issue: #<issue-number>

### Suggest Reviewer
> You can suggest reviewers here with an @mention.

### How To Test?
> How should these changes be tested by the reviewer?
> Have you implemented any additional tests?

### Checklist
Must:
- [ ] All related issues are referenced in commit messages and in PR
- [ ] If code is AI-generated, mention the tool and model used (e.g., GitHub Copilot, GPT-4)
```

### PR Description Tips
- Link related issues using keywords: `Fixes #123`, `Closes #456`, `Resolves #789`
- Provide a clear description of changes
- Add screenshots for UI changes
- List breaking changes if any

### After PR is Open
- Ensure all CI checks pass (GitHub Actions, Eclipse IP check)
- Respond to review comments promptly
- Squash commits if requested by reviewers
- Keep the PR focused and small when possible

### Eclipse Contributor Agreement
- You must have signed the [Eclipse Contributor Agreement (ECA)](http://www.eclipse.org/legal/ECA.php)
- Your commits must include `Signed-off-by` with the same email as your ECA

> **⚠️ Common Pitfall: ECA Email Mismatch**
>
> Many contributors sign the ECA with one email but have a different email configured in Git.
> The git commit author email **must match** the email used to sign the ECA.
>
> **AI Hint**: Before committing, verify the user's Git email configuration:
> ```bash
> # Check current Git email configuration
> git config user.email
> git config user.name
>
> # If email doesn't match ECA, configure it:
> git config user.email "your.eca.email@example.com"
> git config user.name "Your Full Name"
> ```
>
> **Troubleshooting Checklist**:
> - [ ] Verify `git config user.email` matches the email used for ECA sign-up
> - [ ] If email was wrong when you committed, amend with: `git commit --amend --reset-author -s`

---

## Quick Reference

```bash
# Standard commit with sign-off
git commit -s -m "feat(rest): add search endpoint"

# Amend last commit
git commit --amend -s

# Interactive rebase to squash
git rebase -i HEAD~3

# Push to siemens fork
git push siemens my-branch

# Force push after rebase/amend
git push siemens my-branch --force-with-lease
```
