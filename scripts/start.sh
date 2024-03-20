#!/usr/bin/env bash

if [ -z "$1" ]
then
  echo "start.sh <n_node>";
  exit 1;
fi;

echo "Lancement de "$1" nodes...";

if [ $(ps | grep "rmiregistry" | wc -l) -eq 0 ]
then
  rmiregistry 6090 &
else
  echo "Rmi registry already started";
fi;

export CLASSPATH=$CLASSPATH:'./classes:./lib/ComItf.jar'
for i in $(seq 1 1 "$1")
do
  java ./src/Node.java "$1" "$i"
done