# SPDX-FileCopyrightText: 2024 Helio Chissini de Castro <heliocastro@gmail.com>
#
# SPDX-License-Identifier: MIT


import hashlib
import os
import sys

import requests
from requests import Response
from urllib.parse import quote

""" Use current GitHub API to check if a container image with the
    given name and version exists.
"""

token = os.getenv("INPUT_TOKEN")
github_repository = os.getenv("GITHUB_REPOSITORY")
name = os.getenv("INPUT_NAME")
base_version = os.getenv("INPUT_VERSION")
build_args = os.getenv("BUILD_ARGS")
invalidate_cache = True if os.getenv("INVALIDATE_CACHE") else False
unique_id = hashlib.sha256(build_args.encode()).hexdigest() if build_args else "uniq"
owner, repository = github_repository.split('/')

# We base the version on the base_version and the unique_id
version = f"{base_version}-sha.{unique_id[:8]}"

# In case of need invalidate the cache from images we just return the version
if invalidate_cache:
    print(version)
    sys.exit(0)

headers: dict[str, str] = {
    "Accept": "application/vnd.github+json",
    "Authorization": f"Bearer {token}",
    "X-GitHub-Api-Version": "2022-11-28",
}

url: str = f"https://api.github.com/users/{owner}"
response = requests.get(url, headers=headers)
data: Response = response.json()

encoded_name = quote(f"{repository}/{name}", safe='')
if data.get("type") == "Organization":
    url = (
        f"https://api.github.com/orgs/{owner}/packages/container/{encoded_name}/versions"
    )
else:
    url = f"https://api.github.com/user/packages/container/{encoded_name}/versions"

response = requests.get(url, headers=headers)
if response.status_code == 404:
    print("none")
else:
    data = response.json()
    versions = [
        item
        for sublist in [v["metadata"]["container"]["tags"] for v in data]
        if sublist
        for item in sublist
    ]
    if version in versions:
        print("found")
    else:
        print(version)
