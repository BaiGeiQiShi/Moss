#!/bin/bash

PROGNAME=integration1
TIMEOUT=1
GDTBIN=ROPgadget
#GDTBIN="python3 /home/qxin6/ROPgadget/ROPgadget.py"

CURRDIR=$(pwd)
SRC=$CURRDIR/$PROGNAME.c
BIN=$CURRDIR/$PROGNAME
ORIGIN_BIN=$CURRDIR/$PROGNAME.real
OUTDIR=$CURRDIR/output
ORIGIN_OUTDIR=$CURRDIR/output.origin
EVALRESULTFILE=$CURRDIR/eval_rslt.txt

#Reset file content
echo "-1" > $EVALRESULTFILE
echo "-1" >> $EVALRESULTFILE
echo "-1" >> $EVALRESULTFILE
echo "-1" >> $EVALRESULTFILE
echo "-1" >> $EVALRESULTFILE
echo "-1" >> $EVALRESULTFILE

#Generate Oracle Bin (if needed)
if [ ! -f ${ORIGIN_BIN} ]; then
    make origin_bin CFLAGS="-w -O3"
fi
#Generate Oracle Outputs (if needed)
if [ ! -d ${ORIGIN_OUTDIR} ]; then
    mkdir ${ORIGIN_OUTDIR}
    make origin_bin CFLAGS="-w -O3"
    ./run_test $BIN ${ORIGIN_OUTDIR} $TIMEOUT
fi

#Compile Reduced Program
make reduced_bin CFLAGS="-w -O3" || exit 1

#Compute Size Reduction (sred)
original_size=`ls -l ${ORIGIN_BIN} | cut -d' ' -f5`
reduced_size=`ls -l ${BIN} | cut -d' ' -f5`

#Compute Gadget Reduction (gred)
original_gdt=`${GDTBIN} --binary ${ORIGIN_BIN} | grep 'Unique gadgets' | cut -d' ' -f4`
reduced_gdt=`${GDTBIN} --binary ${BIN} | grep 'Unique gadgets' | cut -d' ' -f4`

#Compute Generality (gen)
./run_test $BIN $OUTDIR $TIMEOUT
./compare_output ${ORIGIN_OUTDIR} $OUTDIR $CURRDIR/compare.txt
pass_all=`grep 'pass-' $CURRDIR/compare.txt | wc -l`
total_all=$(ls $CURRDIR/testscript | wc -l)
#rm $CURRDIR/compare.txt

echo "${original_size}" > $EVALRESULTFILE
echo "${reduced_size}" >> $EVALRESULTFILE
echo "${original_gdt}" >> $EVALRESULTFILE
echo "${reduced_gdt}" >> $EVALRESULTFILE
echo "${total_all}" >> $EVALRESULTFILE
echo "${pass_all}" >> $EVALRESULTFILE

