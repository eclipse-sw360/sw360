# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

# The following settings should be adapted to your needs
JAVA_MEMORY_MIN="3g"
JAVA_MEMORY_MAX="6g"

# The following settings should not be touched unless you know what you are doing
# Misconfiguration may be lead to an unusable instance.
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF8"
JAVA_OPTS="$JAVA_OPTS -Dorg.apache.catalina.loader.WebappClassLoader.ENABLE_CLEAR_REFERENCES=false"
JAVA_OPTS="$JAVA_OPTS -Duser.timezone=GMT"
JAVA_OPTS="$JAVA_OPTS -Xms${JAVA_MEMORY_MIN} -Xmx${JAVA_MEMORY_MAX}"
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC"
JAVA_OPTS="$JAVA_OPTS -XX:+CMSParallelRemarkEnabled"
JAVA_OPTS="$JAVA_OPTS -XX:SurvivorRatio=20"

export JAVA_OPTS
