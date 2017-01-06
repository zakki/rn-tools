#!/bin/sh
CNTK=/cygdrive/c/programs/CNTK-2-0-beta6-0-Windows-64bit-GPU/cntk/cntk/CNTK.exe

NAME=$1
MODEL="Output\\${NAME}"
shift

#mpiexec $HOSTS -p 8677 $(cygpath -w $CNTK) $@ parallelTrain=true ModelDir=$MODEL Name=$NAME
$CNTK $@ ModelDir=$MODEL Name=$NAME&
PID=$!
trap "kill $PID" TERM INT
echo "wait" $PID
wait $PID
