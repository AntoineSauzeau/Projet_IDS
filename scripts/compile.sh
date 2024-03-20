#!/usr/bin/env bash

mkdir -p lib classes

INTERFACE_FILES="Communication_itf.java"
ROI_FILES="Communication_impl.java"
OTHER_FILES="Memory.java Utility.java Node.java"

cd src
javac -d ../classes/ $INTERFACE_FILES
javac -d ../classes/ $ROI_FILES
cd ../classes
jar cvf ../lib/ComItf.jar ${INTERFACE_FILES//'.java'/'.class'}
jar cvf ../lib/ComRoi.jar ${ROI_FILES//'.java'/'.class'}
cd ../src
javac -d ../classes/ -classpath '../lib/ComItf.jar:../lib/ComRoi.jar' $OTHER_FILES
