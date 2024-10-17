#!/bin/bash

####################
# NOTE: Put this file in your working directory.
# You may need to modify it. See code below.
####################

id=$1
ARG_DIR=$2
FILE_DIR=$3
PROGID=$4
COMPILE_BIN=$5

if [ ! -f ${ARG_DIR}/${id} ]; then
    exit 1
fi

WORKDIR=$(pwd)
GCOV_ANAL_BIN="java -cp :${WORKDIR}/../build/java:${WORKDIR}/../lib/java/* edu.gatech.cc.domgad.GCovAnalyzer"
LCOV2GCOV_BIN="java -cp :${WORKDIR}/../build/java:${WORKDIR}/../lib/java/* edu.gatech.cc.domgad.LCov2GCov"

echo "Producing instrumented program"
if [ ! -f $PROGID ]; then
    ${COMPILE_BIN} $WORKDIR/src/$PROGID.c $PROGID "-fprofile-instr-generate -fcoverage-mapping -w"
fi

rm -fr $WORKDIR/tmp/*
cp $PROGID tmp/
cd $WORKDIR/tmp

argstr=""
if [ -f ${ARG_DIR}/${id} ]; then
    argstr=`head -n 1 ${ARG_DIR}/${id}`
fi


####################
# NOTE: You may need to change the code below to
# customize how a file should be handled for your program (based on a specific argument)!
# For example, for bzip2-1.0.5, when argstr is "-c", we pass a file "f" as "<f".
# See the path generators we provided for examples.
####################

echo "Executing instrumented program"
if [ -f ${FILE_DIR}/runarg${id}.txt ]; then
    #Obtain the file name to be used as arg
    argf=`head -n 1 ${FILE_DIR}/runarg${id}.txt`
    
    echo "Command: ./$PROGID ${argstr} ${FILE_DIR}/${argf}"
    echo "./$PROGID ${argstr} ${FILE_DIR}/${argf}" >run.sh
    chmod 700 run.sh && ./run.sh
    
else
    echo "Command: ./$PROGID ${argstr}"
    echo "./$PROGID ${argstr}" >run.sh
    chmod 700 run.sh && ./run.sh
fi


echo "Generating path files"
covpath=1     #0: real-count path; 1: binary-count path
llvm-profdata merge -o $PROGID.profdata default.profraw
llvm-cov export -format=lcov ./$PROGID -instr-profile=$PROGID.profdata $WORKDIR/src/$PROGID.c >$PROGID.c.lcov
${LCOV2GCOV_BIN} $PROGID.c.lcov >$PROGID.c.gcov
if [ ${covpath} -eq 1 ]; then
    ${GCOV_ANAL_BIN} $PROGID.c.gcov getbcov >pathtmp
else
    cp $PROGID.c.gcov pathtmp
fi
