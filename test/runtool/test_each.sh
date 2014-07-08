#!/bin/bash
#Usage: libbuntest file.k

if [ -z $SRCPATH ]; then
	SRCPATH=$1
fi

if [ -z $BUNJAR ]; then
	BUNJAR="libbun2.jar"
fi

if [ -z $BUNDRV ]; then
        echo "set BUNDRV"
        exit 1
fi

if [ -z $OUTEXT ]; then
	echo "set OUTEXT"
	exit 1
fi

if [ -z $OUTLOG2 ]; then
	#OUTLOG2="test-$BUNDRV.log"
	OUTLOG2="/dev/stdout"
fi

SRCFILE=`basename $SRCPATH`
OUTFILE="$SRCFILE.$OUTEXT"

if [ -f $OUTFILE ]; then
	rm -f $OUTFILE
fi

CHECKED=" $SRCFILE"

echo "$SRCFILE" >> $OUTLOG2
echo "========" >> $OUTLOG2
echo "java -ea -jar $BUNJAR -d $BUNDRV -o $OUTFILE $SRCPATH" >> $OUTLOG2
java -ea -jar $BUNJAR -d $BUNDRV -o $OUTFILE $SRCPATH >> $OUTLOG2
EXIT_JAVA=$?
echo "EXIT_STATUS=$EXIT_JAVA" >> $OUTLOG2

if [ $EXIT_JAVA -eq 0 ]; then
	if [ -z $CHECKER ]; then
		exit 0
	fi
	echo "$CHECKER $OUTFILE" >> $OUTLOG2
	$CHECKER $OUTFILE >> $OUTLOG2
	EXIT_CHECKER=$?
	echo "EXIT_STATUS=$EXIT_CHECKER" >> $OUTLOG2
	exit $EXIT_CHECKER
else
	echo 1
fi

