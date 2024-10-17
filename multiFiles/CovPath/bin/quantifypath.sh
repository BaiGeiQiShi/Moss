#!/bin/bash

PROGID=$1
SAMPLER_BIN=$2
COMPILE_BIN=$3  #Three arguments: src, bin, options (as one string)

EDBOUND=8321    #Computed based on epsilon=0.03 and delta=0.95



if [ -z $PROGID ] || [ -z ${SAMPLER_BIN} ] || [ -z ${COMPILE_BIN} ]; then
    echo "Missing arguments!"
    exit 1
fi

if [ ! -f pathgen.sh ]; then
    echo "Missing pathgen.sh."
    exit 1
fi


WORKDIR=$(pwd)
GCOV_ANAL_BIN="java -cp :${WORKDIR}/../build/java:${WORKDIR}/../lib/java/* edu.gatech.cc.domgad.GCovAnalyzer"
LCOV2GCOV_BIN="java -cp :${WORKDIR}/../build/java:${WORKDIR}/../lib/java/* edu.gatech.cc.domgad.LCov2GCov"
ARG_DIR=$WORKDIR/quantify_path_input/arg
FILE_DIR=$WORKDIR/quantify_path_input/file
ARG_CP_DIR=$WORKDIR/quantify_path_input.cp/arg
FILE_CP_DIR=$WORKDIR/quantify_path_input.cp/file
PATH_DIR=$WORKDIR/quantify_path

#Cleanup and setup
rm -fr ${ARG_DIR}/*
rm -fr ${FILE_DIR}/*
rm -fr ${ARG_CP_DIR}/*
rm -fr ${FILE_CP_DIR}/*
rm -fr ${PATH_DIR}/*
rm -fr $PROGID   #Remove any previous instru-compiled binary



iter=0
while [ ${iter} -lt ${EDBOUND} ]; do
    echo "Current Iteration Id: ${iter}; # of Epsilon-Delta Bounded Iterations: ${EDBOUND}"

    cd ${WORKDIR}

    #Clean inputs
    rm -fr ${ARG_DIR}/*
    rm -fr ${FILE_DIR}/*


    #Generate a new input
    echo "Generating one input"
    ${SAMPLER_BIN} ${iter} ${ARG_DIR} ${FILE_DIR}


    #Copy the new input (because the execution may change it)
    cp -r ${ARG_DIR}/* ${ARG_CP_DIR}/
    cp -r ${FILE_DIR}/* ${FILE_CP_DIR}/


    #Clean execution dir
    cd ${WORKDIR}
    chmod 755 -R ${WORKDIR}/tmp/* #In case permissions are changed
    rm -fr ${WORKDIR}/tmp/*


    #Execute the input to generate a trace
    echo "Generating one trace"
    ./pathgen.sh ${iter} ${ARG_DIR} ${FILE_DIR} ${PROGID} ${COMPILE_BIN}   #If successful, pathtmp from tmp directory should be generated

    if [ ! -f ${WORKDIR}/tmp/pathtmp ]; then
	echo "Failed to generate a path file"
	continue
    fi

    cp ${WORKDIR}/tmp/pathtmp ${PATH_DIR}/${iter}
    iter=$((iter+1))
done



#Path counting
cd ${WORKDIR}
>path_counted.txt
pid=0
for ippath in identify_path/*; do
    ipid=$(basename ${ippath})
    count=0
    qpids=""

    for qppath in quantify_path/*; do
        if diff -q ${qppath} ${ippath} &>/dev/null; then #Identical path
            count=$((count+1))
            qpid=$(basename ${qppath})
            if [ -z ${qpids} ]; then
                qpids=${qpid}
            else
                qpids="${qpids},${qpid}"
            fi
        fi
    done

    echo "${pid},${ipid},${count},${qpids}" >>path_counted.txt
    pid=$((pid+1))
done
