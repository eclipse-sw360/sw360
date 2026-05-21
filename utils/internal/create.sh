#!/bin/bash

if [ -z "$1" ]; then
  PROJECT_ROOT="$(pwd)/../../"
else
  PROJECT_ROOT="$1"
fi
if [ -z "$2" ]; then
  OUTPUT_DIRECTORY="$(pwd)/wars"
else
  OUTPUT_DIRECTORY="$2"
fi

cat <<EOF
#!/bin/bash

# Configuration
GITLAB_REPO="https://code.siemens.com/api/v4/projects/708/packages/maven"
M2_REPO_PATH="\$HOME/.m2/repository"
EOF
printf 'DESTINATION_FOLDER="%s"\n' "$OUTPUT_DIRECTORY"
cat <<EOF

# Create destination folder if it doesn't exist
mkdir -p "\$DESTINATION_FOLDER"

download_and_copy() {
    local artifact="\$1"
    local custom_filename="\$2"

    echo "Downloading artifact: \$artifact"

    mvn --no-transfer-progress dependency:get \\
        -Dartifact="\$artifact" \\
        -Dtransitive=false \\
        -DremoteRepositories=gitlab-maven::::\$GITLAB_REPO

    if [ \$? -ne 0 ]; then
        echo "Failed to download artifact: \$artifact"
        return 1
    fi

    local groupId=\$(echo "\$artifact" | cut -d':' -f1)
    local artifactId=\$(echo "\$artifact" | cut -d':' -f2)
    local version=\$(echo "\$artifact" | cut -d':' -f3)
    local packaging=\$(echo "\$artifact" | cut -d':' -f4)

    local group_path=\$(echo "\$groupId" | tr '.' '/')

    local default_filename="\$artifactId-\$version.\$packaging"

    # Path to the downloaded artifact
    local artifact_path="\$M2_REPO_PATH/\$group_path/\$artifactId/\$version/\$default_filename"

    if [ ! -f "\$artifact_path" ]; then
        echo "Downloaded artifact not found at expected location: \$artifact_path"
        return 1
    fi

    # Determine the target filename
    local target_filename
    if [ -n "\$custom_filename" ]; then
        target_filename="\$custom_filename.\$packaging"
    else
        target_filename="\$default_filename"
    fi

    echo "Copying \$artifact_path to \$DESTINATION_FOLDER/\$target_filename"
    cp "\$artifact_path" "\$DESTINATION_FOLDER/\$target_filename"

    if [ \$? -eq 0 ]; then
        echo "Successfully copied \$target_filename to destination folder"
    else
        echo "Failed to copy \$target_filename to destination folder"
        return 1
    fi
}

EOF

find $PROJECT_ROOT/backend $PROJECT_ROOT/rest -name "pom.xml" -exec bash -c '
    if grep -q "<packaging>war</packaging>" {}; then
        response=$(echo "\${project.groupId}:\${project.artifactId}:\${project.version}:war \${deploy.name}" | mvn -N -q -DforceStdout help:evaluate -f {} -Dstyle.color=never 2>/dev/null | sed -r "s/\x1B\[[0-9;]*[mK]//g")
        artifact=$(echo $response | awk -F " " "{print \$1}")
        war_name=$(echo $response | awk -F " " "{print \$2}")
        # Skip war modules that do not declare a deploy.name (rare; warn loudly).
        if [ -z "$war_name" ] || [ "$war_name" = "null object or invalid expression" ]; then
            echo "# WARN: {} declares <packaging>war</packaging> but no <deploy.name>; skipping" 1>&2
            exit 0
        fi
        echo "download_and_copy \"$artifact\" \"$war_name\""
    fi
' \;
