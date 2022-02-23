#!/bin/bash

set -e
HOST="http://localhost:8080"
IN=$1
FILE=${IN#"../rules/"}
OUT=analyzes/${FILE%".dlgp"}.txt
DIR=`dirname $OUT`
mkdir -p $DIR

java -Xmx10G -jar kiabora-1.2.0.jar -r -g -p 'rr' -P -f $IN > $OUT

URL=$HOST"?path="$OUT
echo "see the graph at $URL"
xdg-open $URL
