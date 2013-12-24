#!/bin/bash
#testall.sh
# Test all Jenkins plugins.

set -e

for i in Fogbugz AdvancedMercurial FogbugzPlugin GatekeeperPlugin UpmergePlugin
do
    pushd $i
        mvn clean:clean
        mvn test
        mvn install -Dmaven.test.skip=true  # we can skip tests here as they're already run previous
    popd
done
