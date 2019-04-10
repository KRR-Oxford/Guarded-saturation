#!/bin/bash

timeout=10m
mem=10g
sleep=10m

version=0.0.2
date=`date +%Y-%m-%d`

basepath=results/ChaseBench/$timeout-$mem-$date
mkdir -p $basepath

for project in 001 010 100
do
	echo "Testing LUBM $project - $timeout $mem"
	rm -f "$basepath/LUBM_$project.log"
	time timeout $timeout java -Xmx$mem -jar guarded-saturation-$version-jar-with-dependencies.jar cb LUBM benchmarks/LUBM/ $project &> "$basepath/LUBM_$project.log"
	echo "Sleeping for $sleep..."
	sleep $sleep
	echo "Testing LUBM $project ST-ONLY - $timeout $mem"
	rm -f "$basepath/LUBM_ST-ONLY_$project.log"
	time timeout $timeout java -Xmx$mem -jar guarded-saturation-$version-jar-with-dependencies.jar cb LUBM benchmarks/LUBM_ST-ONLY/ $project &> "$basepath/LUBM_ST-ONLY_$project.log"
	echo "Sleeping for $sleep..."
	sleep $sleep
done

for project in 10k 100k 500k 1m
do
	echo "Testing doctors $project - $timeout $mem"
	rm -f "$basepath/doctors_$project.log"
	time timeout $timeout java -Xmx$mem -jar guarded-saturation-$version-jar-with-dependencies.jar cb doctors benchmarks/doctors/ $project &> "$basepath/doctors_$project.log"
	echo "Sleeping for $sleep..."
	sleep $sleep
done

for project in 10k 100k 500k 1m
do
	echo "Testing doctors-fd $project - $timeout $mem"
	rm -f "$basepath/doctors-fd_$project.log"
	time timeout $timeout java -Xmx$mem -jar guarded-saturation-$version-jar-with-dependencies.jar cb doctors-fd benchmarks/doctors-fd/ $project &> "$basepath/doctors-fd_$project.log"
	echo "Sleeping for $sleep..."
	sleep $sleep
	echo "Testing doctors-fd $project ST-ONLY - $timeout $mem"
	rm -f "$basepath/doctors-fd_ST-ONLY_$project.log"
	time timeout $timeout java -Xmx$mem -jar guarded-saturation-$version-jar-with-dependencies.jar cb doctors-fd benchmarks/doctors-fd_ST-ONLY/ $project &> "$basepath/doctors-fd_ST-ONLY_$project.log"
	echo "Sleeping for $sleep..."
	sleep $sleep
done

for project in 100 200 300
do
	echo "Testing deep $project - $timeout $mem"
	rm -f "$basepath/deep_$project.log"
	time timeout $timeout java -Xmx$mem -jar guarded-saturation-$version-jar-with-dependencies.jar cb deep benchmarks/deep/$project/ &> "$basepath/deep_$project.log"
	echo "Sleeping for $sleep..."
	sleep $sleep
	echo "Testing deep $project ST-ONLY - $timeout $mem"
	rm -f "$basepath/deep_ST-ONLY_$project.log"
	time timeout $timeout java -Xmx$mem -jar guarded-saturation-$version-jar-with-dependencies.jar cb deep benchmarks/deep/$project\_ST-ONLY/ &> "$basepath/deep_ST-ONLY_$project.log"
	echo "Sleeping for $sleep..."
	sleep $sleep
done


echo "Testing Ontology-256 - $timeout $mem"
rm -f "$basepath/Ontology-256.log"
time timeout $timeout java -Xmx$mem -jar guarded-saturation-$version-jar-with-dependencies.jar cb Ontology-256 benchmarks/Ontology-256/ &> "$basepath/Ontology-256.log"
echo "Sleeping for $sleep..."
sleep $sleep
echo "Testing Ontology-256 ST-ONLY - $timeout $mem"
rm -f "$basepath/Ontology-256_ST-ONLY.log"
time timeout $timeout java -Xmx$mem -jar guarded-saturation-$version-jar-with-dependencies.jar cb Ontology-256 benchmarks/Ontology-256_ST-ONLY/ &> "$basepath/Ontology-256_ST-ONLY.log"
echo "Sleeping for $sleep..."
sleep $sleep


echo "Testing STB-128 - $timeout $mem"
rm -f "$basepath/STB-128.log"
time timeout $timeout java -Xmx$mem -jar guarded-saturation-$version-jar-with-dependencies.jar cb STB-128 benchmarks/STB-128/ &> "$basepath/STB-128.log"
echo "Sleeping for $sleep..."
sleep $sleep
echo "Testing STB-128 ST-ONLY - $timeout $mem"
rm -f "$basepath/STB-128_ST-ONLY.log"
time timeout $timeout java -Xmx$mem -jar guarded-saturation-$version-jar-with-dependencies.jar cb STB-128 benchmarks/STB-128_ST-ONLY/ &> "$basepath/STB-128_ST-ONLY.log"
echo "Sleeping for $sleep..."
sleep $sleep

