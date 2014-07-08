
if [ -z $OUTLOG1 ]; then
        OUTLOG1="/dev/stdout"
else
	if [ -f $OUTLOG1 ]; then
		rm -rf $OUTLOG1
	fi
fi

for file in $*
do
	$TESTBIN/test_each.sh $file
	EXIT_STATUS=$?
	echo "\"$file\", $EXIT_STATUS" >> $OUTLOG1
done


