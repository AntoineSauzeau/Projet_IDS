#!/usr/bin/env bash

export CLASSPATH=$CLASSPATH:'./classes:./lib/ComItf.jar'

if [ -z "$1" ]
then
  echo "start.sh <n_node>";
  exit 1;
fi;

echo "Lancement de "$1" nodes...";

#if [ $(ps | grep "rmiregistry" | wc -l) -eq 0 ]
if [ $(netstat -tulpn | grep "LISTEN" | grep "rmiregistry" | wc -l) -eq 0 ]
then
  rmiregistry 6090 &
else
  echo "Rmi registry already started";
fi;

debug=false
if [ "$2" = "true" ]
then
  debug=true
fi

for i in $(seq 1 1 "$1")
do
  #java ./src/Node.java "$1" "$i" "$2" &
  gnome-terminal -- java ./src/Node.java "$1" "$i" "$2"
done