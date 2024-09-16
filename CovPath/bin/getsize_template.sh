#!/bin/bash

####################
# NOTE: Put this file in your working directory.
# Provide a value for PROGID.
# Change GDTBIN to specify how to invoke ROPgadget (see how it's used below).
# Depending on how long to execute an input, you may need to change TIMEOUT.
####################

PROGID=XXX                                            #Provide a program id
TIMEOUT=1                                             #In seconds
GDTBIN="python3 /home/qxin6/ROPgadget/ROPgadget.py"   #Change it to your own path!
PROG_COUNTER_BIN="../build/bin/instrumenter -S"

WORKDIR=$(pwd)
SRC=$WORKDIR/$PROGID.c.reduced.c
ORIGIN_SRC=$WORKDIR/src/$PROGID.c
BIN=$WORKDIR/$PROGID.reduced
ORIGIN_BIN=$WORKDIR/$PROGID.origin



#Reset file content (original binary bytes; reduced binary bytes; original gadgets; reduced gadgets; original #stmts; reduced #stmts)
echo "-1" > size_rslt.txt
echo "-1" >> size_rslt.txt
echo "-1" >> size_rslt.txt
echo "-1" >> size_rslt.txt
echo "-1" >> size_rslt.txt
echo "-1" >> size_rslt.txt

#Generate Oracle Bin (if needed)
if [ ! -f ${ORIGIN_BIN} ]; then
    ./compile.sh ${ORIGIN_SRC} ${ORIGIN_BIN} "-w" || exit 1
fi

#Compile Reduced Program
./compile.sh $SRC $BIN "-w" || exit 1

#Count Binary Bytes
original_size=`ls -l ${ORIGIN_BIN} | cut -d' ' -f5`
reduced_size=`ls -l ${BIN} | cut -d' ' -f5`

#Count Gadgets
original_gdt=`${GDTBIN} --binary ${ORIGIN_BIN} | grep 'Unique gadgets' | cut -d' ' -f4`
reduced_gdt=`${GDTBIN} --binary ${BIN} | grep 'Unique gadgets' | cut -d' ' -f4`

#Count Stmts
original_stmts=-1
if [ -f ${WORKDIR}/original_stmt_num.txt ]; then
    original_stmts=`head -n 1 ${WORKDIR}/original_stmt_num.txt`
else
    original_stmts=`${PROG_COUNTER_BIN} ${ORIGIN_SRC}`
    echo ${original_stmts} >${WORKDIR}/original_stmt_num.txt
fi
reduced_stmts=`${PROG_COUNTER_BIN} ${SRC}`

#Output to file
echo "${original_size}" > size_rslt.txt
echo "${reduced_size}" >> size_rslt.txt
echo "${original_gdt}" >> size_rslt.txt
echo "${reduced_gdt}" >> size_rslt.txt
echo "${original_stmts}" >> size_rslt.txt
echo "${reduced_stmts}" >> size_rslt.txt
