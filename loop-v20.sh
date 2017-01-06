#!/bin/bash

#NAME="ResNet3b5"
NAME=$1

echo "begin " $NAME

while true
do
	echo "prepare $i"

	DATE=$(date "+%Y%m%d-%H%M")
	mv Output/${NAME}_train.log* Output/${NAME}-${DATE}.log

	DATAFILE=$(find /cygdrive/e/tmp/v22/ -maxdepth 1 -type f|grep -v gz|shuf|head -1)
	DATA="DataFile=$(cygpath -m $DATAFILE)"
	echo "use $DATAFILE"
	echo "start"
	./cntk.sh $NAME configFile=Config/TrainValueResNetV20.cntk command=train $DATA&
	CNTK=$!
	echo "running " $CNTK $(date)
	sleep 2h
	echo "kill " $CNTK
	/bin/kill $CNTK
	sleep 10s

	ps
	echo "copy $i"
done
