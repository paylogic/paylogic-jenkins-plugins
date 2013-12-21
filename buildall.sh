#!/bin/bash
#buildall.sh
# Build and Deploy all Jenkins plugins.

set -e

export MAVEN_OPTS="-Dmaven.test.skip=true"

for i in Redis Fogbugz AdvancedMercurial FogbugzPlugin GatekeeperPlugin UpmergePlugin
do
    pushd $i
        mvn clean:clean
        mvn deploy
    popd
done
