#!/bin/bash

PROGID=$1
SAMPLER_BIN=$2  #Path to the sampler (add "./" if local)
COMPILE_BIN=$3  #Path the compiling script (add "./" if local)

MAXITER=3       #Iteration upper-bound
K=90            #Number of subsequent iterations for sampling termination (by c=0.95 and B=100)


if [ -z $PROGID ] || [ -z ${SAMPLER_BIN} ] || [ -z ${COMPILE_BIN} ]; then
    echo "Missing arguments!"
    exit 1
fi

if [ ! -f pathgen.sh ]; then
    echo "Missing pathgen.sh."
    exit 1
fi


WORKDIR=$(pwd)
GCOV_ANAL_BIN="java -cp :${WORKDIR}/../build/java:${WORKDIR}/../lib/java/* moss.covpath.GCovAnalyzer"
LCOV2GCOV_BIN="java -cp :${WORKDIR}/../build/java:${WORKDIR}/../lib/java/* moss.covpath.LCov2GCov"
ARG_DIR=$WORKDIR/identify_path_input/arg
FILE_DIR=$WORKDIR/identify_path_input/file
ARG_CP_DIR=$WORKDIR/identify_path_input.cp/arg
FILE_CP_DIR=$WORKDIR/identify_path_input.cp/file
INPUT_TMP_DIR=$WORKDIR/identify_path_input.tmp
PATH_DIR=$WORKDIR/identify_path

#Cleanup and setup
rm -fr ${ARG_DIR}/*
rm -fr ${FILE_DIR}/*
rm -fr ${ARG_CP_DIR}/*
rm -fr ${FILE_CP_DIR}/*
rm -fr ${PATH_DIR}/*
rm -fr $PROGID   #Remove any previous instru-compiled binary
if [ ! -d ${INPUT_TMP_DIR} ]; then
    mkdir ${INPUT_TMP_DIR}
else
    rm -fr ${INPUT_TMP_DIR}/*
fi
mkdir ${INPUT_TMP_DIR}/arg
mkdir ${INPUT_TMP_DIR}/file



currk=0
iter=0
ksat=1 #sat (0) or unsat (1) for k-successive bound

while [ ${iter} -lt ${MAXITER} ]; do
    echo "Current Iteration Id: ${iter}"
    echo "Current k: ${currk}"
    if [ ${ksat} -eq 0 ]; then
	echo "Finished Path Identification."
	break
    fi

    cd ${WORKDIR}

    #Clean inputs
    rm -fr ${ARG_DIR}/*
    rm -fr ${FILE_DIR}/*
    rm -fr ${INPUT_TMP_DIR}/arg/*
    rm -fr ${INPUT_TMP_DIR}/file/*
    

    #Generate a new input
    echo "Generating one input"
    ${SAMPLER_BIN} ${iter} ${ARG_DIR} ${FILE_DIR}


    #Copy the new input (because the execution may change it)
    cp -r ${ARG_DIR}/* ${INPUT_TMP_DIR}/arg
    cp -r ${FILE_DIR}/* ${INPUT_TMP_DIR}/file


    #Clean execution dir
    cd ${WORKDIR}
    chmod 755 -R ${WORKDIR}/tmp/* #In case permissions are changed
    rm -fr ${WORKDIR}/tmp/*


    #Execute the input to generate a path
    echo "Generating one path"
    ./pathgen.sh ${iter} ${ARG_DIR} ${FILE_DIR} ${PROGID} ${COMPILE_BIN}   #If successful, pathtmp from tmp directory should be generated

    if [ ! -f ${WORKDIR}/tmp/pathtmp ]; then
	echo "Failed to generate a path file"
	continue
    else
	cd ${WORKDIR}/tmp
    fi


    #Path checking
    echo "Check if path exists"
    pathexist=1
    for pathf in ${PATH_DIR}/*; do
	if diff -q ${pathf} pathtmp &>/dev/null; then
	    pathexist=0
	    break
	fi
    done

    if [ ${pathexist} -eq 0 ]; then
	echo "Path already exists"
	currk=$((currk+1))
	if [ ${currk} -ge $K ]; then
	    ksat=0
	fi

    else
	echo "New Path. Reset current k as 0. Make Copies."
	currk=0
	cp pathtmp ${PATH_DIR}/${iter}
	cp -r ${INPUT_TMP_DIR}/arg/* ${ARG_CP_DIR}/
	cp -r ${INPUT_TMP_DIR}/file/* ${FILE_CP_DIR}/
    fi

    iter=$((iter+1))
done


cd ${WORKDIR}
rm -fr ${INPUT_TMP_DIR}
