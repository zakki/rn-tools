#!/bin/sh

DIR=$(cygpath -u  /cygdrive/e/Data/gtp/)

for i in $@
#for i in $DIR/cgos_0{00..03}
#for i in $DIR/cgos_0{04..08}
#for i in $DIR/cgos_0{09..13}
#for i in $DIR/cgos_0{08,13}
do
	echo $i $(basename $i)
	mv data.txt $(basename $i)_
	rm tmplist_*
	split -d -a 3 -l 1000 $i tmplist_
	for j in tmplist_*
	do
		echo $j
		if [ ! -e $(basename $i)_${j#tmplist_}.txt ]
		then
		(cat $(cat $j|sed "s#^\./#${DIR}#")) | timeout -sKILL 4h ~/git/ray-8.0/win/x64/Release/ray --thread 6 --playout 1000 > log.txt 2>> err.log
		STATUS=$?
		if [ "$STATUS" -eq 124 ];
		then
			echo "TIMEOUT $i $j" >> timeout.log
		fi
		mv data.txt $(basename $i)_${j#tmplist_}.txt
		fi
	done
done
