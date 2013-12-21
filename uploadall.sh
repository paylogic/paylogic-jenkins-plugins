#!/bin/bash
#buildall.sh
# Upload all Jenkins plugins to jenkins instance.

if [ -z "$1" ]
then
    echo "Usage: uploadall.sh http://jenkins.company.com"
    exit 1
fi

set -e

for file in */target/*.hpi
do
    echo "uploading $file"
    curl -i -F name=@$file $1/pluginManager/uploadPlugin
done

curl -i -F Submit=Yes $1/safeRestart
