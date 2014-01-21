#!/bin/bash

git pull
git submodule init
git submodule sync
git submodule update

for i in */src
do
    pushd $i
    cd ..
        echo "Updating $PWD ..."
        BRANCH_NAME=master
        git checkout $BRANCH_NAME
        git pull origin $BRANCH_NAME
    popd
done
