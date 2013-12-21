#!/bin/bash

for i in */src
do
    pushd $i
    cd ..
        git checkout master
        git pull origin master
    popd
done
