# Copyright Helio Chissini de Castro, 2023
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0


import os

import requests
from rich import print

""" Use current Github API to list packages
    in registry and remove all but last 3 or custom
    set number of packages.
    Reference: https://docs.github.com/en/rest/packages/packages?apiVersion=2022-11-28#about-github-packages
"""

dry_run: bool = True if os.getenv("INPUT_DRY_RUN") == "true" else False
keep = int(os.getenv("INPUT_KEEP"))
org = os.getenv("GITHUB_REPOSITORY_OWNER")
packages = os.getenv("INPUT_PACKAGES").split("\n")
token = os.getenv("INPUT_TOKEN")

headers = {
    "Accept": "application/vnd.github+json",
    "Authorization": f"Bearer {token}",
    "X-GitHub-Api-Version": "2022-11-28",
}

# Assembly organization packages url string
pkg_url: str = f"https://api.github.com/orgs/{org}/packages"


def delete_packages():
    for package in packages:
        print(f":package: {package}")
        url = f"{pkg_url}/container/{package.replace('/', '%2F')}/versions?per_page=100"
        response = requests.get(url, headers=headers)

        if response.status_code == 404:
            print(f":cross_mark: Not found - {url}")
            continue

        # Sort all images on id.
        images = sorted(response.json(), key=lambda x: x["id"], reverse=True)

        # Slice and remove all
        if len(images) > keep:
            for image in images[keep + 1 :]:
                url = f"{pkg_url}/container/{package.replace('/', '%2F')}/versions/{image['id']}"

                # Never remove latest or non snapshot tagged images
                if restrict_delete_tags(image["metadata"]["container"]["tags"]):
                    print(
                        f":package: Skip tagged {package} id {image['id']} tags {image['metadata']['container']['tags']}"
                    )
                    continue

                if not dry_run:
                    response = requests.delete(url, headers=headers)
                    if response.status_code != 204:
                        print(
                            f":cross_mark: Failed to delete package {package} version id {image['id']}."
                        )
                        continue
                print(
                    f":white_heavy_check_mark: Deleted package {package} version id {image['id']}."
                )


def restrict_delete_tags(tags: list) -> bool:
    if not tags:
        return False
    for tag in tags:
        if tag == "latest":
            return True
        elif "nightly" in tag:
            return True
    return False


if __name__ == "__main__":
    delete_packages()
