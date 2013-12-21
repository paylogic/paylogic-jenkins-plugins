#!/bin/bash
#buildall.sh
# Upload all Jenkins plugins to ci.paylogic.eu jenkins instance.

set -e

for file in */target/*.hpi
do
    echo "uploading $file"
    curl -i -F name=@$file http://ci.paylogic.eu/pluginManager/uploadPlugin
done

curl -i -F Submit=Yes http://ci.paylogic.eu/safeRestart