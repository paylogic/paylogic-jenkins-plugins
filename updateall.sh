#!/bin/bash

for i in */src
do
    pushd $i
    cd ..
        git pull origin master
    popd
done
