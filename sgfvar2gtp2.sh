#!/bin/bash
#find igokifu/kgs-19* igokifu/GoGoDWinter2015/Database/ -name '*.sgf'|while read sgf
find $@ -name '*.sgf'|while read sgf
do
	echo $sgf
	OUT="$(basename $sgf).gtp"
	perl sgfvar2gtp2.pl $sgf > $OUT
	if [ ! -s $OUT ]
	then
		rm $OUT
	fi
done
