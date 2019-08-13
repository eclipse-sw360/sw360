# Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
#
# SPDX-License-Identifier: EPL-1.0
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# A http://www.eclipse.org/legal/epl-v10.html
#

# The following settings should be adapted to your needs
JAVA_MEMORY_MIN="2g"
JAVA_MEMORY_MAX="4g"

# The following settings should not be touched unless you know what you are doing
# Misconfiguration may be lead to an unusable instance.
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF8"
JAVA_OPTS="$JAVA_OPTS -Dorg.apache.catalina.loader.WebappClassLoader.ENABLE_CLEAR_REFERENCES=false"
JAVA_OPTS="$JAVA_OPTS -Duser.timezone=GMT"
JAVA_OPTS="$JAVA_OPTS -Xms${JAVA_MEMORY_MIN} -Xmx${JAVA_MEMORY_MAX}"
JAVA_OPTS="$JAVA_OPTS -XX:+UseParNewGC"
JAVA_OPTS="$JAVA_OPTS -XX:+UseConcMarkSweepGC"
JAVA_OPTS="$JAVA_OPTS -XX:+CMSParallelRemarkEnabled"
JAVA_OPTS="$JAVA_OPTS -XX:SurvivorRatio=20"

export JAVA_OPTS
