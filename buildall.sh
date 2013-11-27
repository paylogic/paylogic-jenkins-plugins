#!/bin/bash
#buildall.sh
# Build and Deploy all Jenkins plugins.

set -e

export MAVEN_OPTS="-Dmaven.test.skip=true"

cd Fogbugz
mvn deploy
cd ../AdvancedMercurial/
mvn deploy
cd ../FogbugzPlugin/
mvn deploy
cd ../GatekeeperPlugin/
mvn deploy
cd ../UpmergePlugin/
mvn deploy
