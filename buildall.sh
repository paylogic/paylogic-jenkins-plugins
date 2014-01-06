#!/bin/bash
#buildall.sh
# Build and Deploy all Jenkins plugins.

set -e

export MAVEN_OPTS="-Dmaven.test.skip=true"

for i in mercurial-plugin Fogbugz AdvancedMercurial FogbugzPlugin GatekeeperPlugin UpmergePlugin
do
    pushd $i
        mvn clean:clean
        mvn install
    popd
done
