#!/bin/bash

PROGID=$1
kr=$2               #Weight balancing size red & attksurf red
kg=$3               #Weight balancing red & gen
kvalue=$4           #k for computing density score
samplenum=3


if [ -z $PROGID ] || [ -z ${kr} ] || [ -z ${kg} ] || [ -z ${kvalue} ]; then
    echo "Missing arguments!"
    exit 1
fi


WORKDIR=$(pwd)
SEARCHBIN="java -cp :../build/java:../lib/java/* edu.gatech.cc.domgad.GCovBasedMCMCSearch"
LINEPRINTERBIN="../build/bin/instrumenter -g statement test.sh"
EMPTYGENBIN="java -cp :../build/java:../lib/java/* edu.gatech.cc.domgad.EmptyProgramGenerator"


#Clean up
if [ -d sample_output ]; then
    rm -fr sample_output/*
fi
if [ -d errcode ]; then
    rm -fr errcode/*
fi
if [ -d tmp ]; then
    rm -fr tmp/*
fi
if [ -d domgad-out ]; then
    rm -fr domgad-out
fi
if [ -d progcounter-out ]; then
    rm -fr progcounter-out
fi

rm -fr ${PROGID}.c.reduced.c
rm -fr ${PROGID}.origin
rm -fr ${PROGID}.reduced
rm -fr mcmclog.txt
rm -fr original_stmt_num.txt
rm -fr size_rslt.txt



$LINEPRINTERBIN $WORKDIR/src/$PROGID.c >$WORKDIR/line.txt

quan_num=`ls ${WORKDIR}/quantify_path | wc -l`
echo ${quan_num} >${WORKDIR}/quan_num.txt

$SEARCHBIN $WORKDIR/path_counted.txt $WORKDIR/identify_path $WORKDIR/sample_output ${samplenum} $WORKDIR/src/$PROGID.c $WORKDIR/line.txt $WORKDIR $PROGID ${kr} ${kg} ${kvalue} ${quan_num} >mcmclog.txt

$EMPTYGENBIN $WORKDIR/src/$PROGID.c $WORKDIR/line.txt $WORKDIR/identify_path/0 >$WORKDIR/sample_output/-1.c
