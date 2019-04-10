#!/bin/bash

timeout=10m
mem=10g
sleep=30m

version=0.0.2
date=`date +%Y-%m-%d`

basepath=results/ChaseBench/$timeout-$mem-$date
mkdir -p $basepath

for filename in benchmarks/OWL/*.owl
do
    basenameF=$(basename "$filename" .owl)
	echo "Testing $basenameF - $timeout $mem"
	rm -f "$basepath/$basenameF.log"
	time timeout $timeout java -Xmx$mem -jar guarded-saturation-$version-jar-with-dependencies.jar owl $filename &> "$basepath/$basenameF.log"
	echo "Sleeping for $sleep..."
	sleep $sleep
done
