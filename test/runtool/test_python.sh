#!/bin/sh

export BUNDRV=python 
export OUTEXT=py
export TESTBIN=runtool
export BUNJAR="../libbun2.jar"
export CHECKER=python
export OUTLOG1="$BUNDRV.csv"
export OUTLOG2="$BUNDRV.log"
if [ -f $OUTLOG2 ]; then
	rm -f $OUTLOG2
fi

$TESTBIN/test_all.sh $*

