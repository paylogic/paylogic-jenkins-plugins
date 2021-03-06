SHELL := /bin/bash
PATH := $(PWD)/env/bin:$(PATH)
BRANCH_NAME := master
MAVEN_OPTS := -Dmaven.test.skip=true
MODULES := Fogbugz fogbugz-plugin GatekeeperPlugin ssh-slaves-plugin

upload:
	$(if $(URL),,echo 'Usage: make upload URL=<my-jenkins-url> [PACKAGE=<mypackage>].'; exit 1)
	$(foreach hpi_file,$(if $(PACKAGE),$(wildcard $(PACKAGE)/target/*.hpi),$(wildcard */target/*.hpi)), curl -i -F name=@$(hpi_file) $(URL)/pluginManager/uploadPlugin)
	curl -i -F Submit=Yes $(URL)/safeRestart

pull:
	-git pull
	git submodule init
	git submodule sync
	git submodule update
	$(foreach module, $(MODULES), (pushd $(module) && git checkout $(BRANCH_NAME) && git pull origin $(BRANCH_NAME));)

build:
	$(foreach module, $(MODULES), (export MAVEN_OPTS=$(MAVEN_OPTS) && pushd $(module) && mvn clean:clean && mvn install);)

test:
	$(foreach module, $(MODULES), (pushd $(module) && mvn clean:clean && mvn test && mvn install);)
