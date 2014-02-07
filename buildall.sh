#!/bin/bash
#buildall.sh
# Build all Jenkins plugins.

set -e

export MAVEN_OPTS="-Dmaven.test.skip=true"

for i in Fogbugz fogbugz-plugin GatekeeperPlugin ssh-slaves-plugin
do
    pushd $i
        mvn clean:clean
        mvn install
    popd
done
