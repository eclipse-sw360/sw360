#!/bin/bash
# Script to remove bad commits from branch

cd /c/Users/1111a/sw360

# The commits to remove (test and cleanup commits)
BAD_COMMITS="50054c46d938d53a5ba2cfb4fd0c2c1014267800
49c211a269f409df9e842d9970a4ac00c03fb5f7
deb46600148244f639387c48c730b7a9a513af4f"

# Get the good base commit
BASE_COMMIT="5c09f095b"

# Create new branch without bad commits
git checkout -b clean-final $BASE_COMMIT

# Cherry-pick good commits
git cherry-pick 34e3aa26c b4e9aa2f9 6148c3dac 69de898a0

# Force push
echo "Done! Now push with: git push -f myfork clean-final:feature/3685-licensedb-config"
