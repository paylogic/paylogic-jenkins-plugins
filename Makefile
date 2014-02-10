SHELL := /bin/bash
PATH := $(PWD)/env/bin:$(PATH)
BRANCH_NAME := master

upload:
	test -v URL || (echo 'Usage: make upload URL=something. ' && exit 1 )
	$(foreach hpi_file, $(wildcard */target/*.hpi), curl -i -F name=$(hpi_file) $(URL)/pluginManager/uploadPlugin)

pull:
	-git pull
	git submodule init
	git submodule sync
	git submodule update

	pushd Fogbugz && git checkout $(BRANCH_NAME) && git pull origin $(BRANCH_NAME)
	pushd fogbugz-plugin && git checkout $(BRANCH_NAME) && git pull origin $(BRANCH_NAME)
	pushd GatekeeperPlugin && git checkout $(BRANCH_NAME) && git pull origin $(BRANCH_NAME)