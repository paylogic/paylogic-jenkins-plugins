#!/bin/bash
#buildall.sh
# Build and Deploy all Jenkins plugins.

set -e

export MAVEN_OPTS="-Dmaven.test.skip=true"

cd Redis
mvn clean:clean
mvn deploy
cd ../Fogbugz
mvn clean:clean
mvn deploy
cd ../AdvancedMercurial/
mvn clean:clean
mvn deploy
cd ../FogbugzPlugin/
mvn clean:clean
mvn deploy
cd ../GatekeeperPlugin/
mvn clean:clean
mvn deploy
cd ../UpmergePlugin/
mvn clean:clean
mvn deploy
