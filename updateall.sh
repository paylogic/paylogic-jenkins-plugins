#!/bin/bash

git submodule init
git submodule update

for i in */src
do
    pushd $i
    cd ..
        BRANCH_NAME=master
        git checkout $BRANCH_NAME
        git pull origin $BRANCH_NAME
    popd
done
