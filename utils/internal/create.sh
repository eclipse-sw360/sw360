#!/bin/bash

if [ -z "$1" ]; then
  PROJECT_ROOT="$(pwd)/../../"
else
  PROJECT_ROOT="$1"
fi

printf '#!/bin/bash\n'

find $PROJECT_ROOT/backend -name "pom.xml" -exec sh -c '
    if grep -q "<packaging>war</packaging>" {}; then
        groupId=$(mvn -B -f {} help:evaluate -Dexpression=project.groupId -q -DforceStdout -Dstyle.color=never 2>/dev/null | sed -r "s/\x1B\[[0-9;]*[mK]//g")
        artifactId=$(mvn -B -f {} help:evaluate -Dexpression=project.artifactId -q -DforceStdout -Dstyle.color=never 2>/dev/null | sed -r "s/\x1B\[[0-9;]*[mK]//g")
        version=$(mvn -B -f {} help:evaluate -Dexpression=project.version -q -DforceStdout -Dstyle.color=never 2>/dev/null | sed -r "s/\x1B\[[0-9;]*[mK]//g")
        echo "mvn --no-transfer-progress dependency:get -Dartifact=$groupId:$artifactId:$version:war -Dtransitive=false -DremoteRepositories=gitlab-maven::::https://code.siemens.com/api/v4/projects/708/packages/maven"
    fi
' \;

find $PROJECT_ROOT/rest -name "pom.xml" -exec sh -c '
    if grep -q "<packaging>war</packaging>" {}; then
        groupId=$(mvn -B -f {} help:evaluate -Dexpression=project.groupId -q -DforceStdout -Dstyle.color=never 2>/dev/null | sed -r "s/\x1B\[[0-9;]*[mK]//g")
        artifactId=$(mvn -B -f {} help:evaluate -Dexpression=project.artifactId -q -DforceStdout -Dstyle.color=never 2>/dev/null | sed -r "s/\x1B\[[0-9;]*[mK]//g")
        version=$(mvn -B -f {} help:evaluate -Dexpression=project.version -q -DforceStdout -Dstyle.color=never 2>/dev/null | sed -r "s/\x1B\[[0-9;]*[mK]//g")
        echo "mvn --no-transfer-progress dependency:get -Dartifact=$groupId:$artifactId:$version:war -Dtransitive=false -DremoteRepositories=gitlab-maven::::https://code.siemens.com/api/v4/projects/708/packages/maven"
    fi
' \;
