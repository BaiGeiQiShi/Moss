#!/bin/bash

PROGNAME=integration1
TIMEOUT=1
GDTBIN=ROPgadget
DOMGAD=/usr/local/Moss/CovPath
STMT_COUNTER="${DOMGAD}/build/bin/instrumenter -S"
debdce=/usr/local/debdce/build/bin/debdce
WORKDIR=$(pwd)
EVALRESULTFILE=$WORKDIR/size_rslt.txt
MAKEFILE=$WORKDIR/Makefile


BIN=$WORKDIR/$PROGNAME
ORIGIN_BIN=$WORKDIR/$PROGNAME.origin
BASE_BIN=$WORKDIR/$PROGNAME.base

#Reset file content (original binary bytes; reduced binary bytes; original gadgets; reduced gadgets; original #stmts; reduced #stmts)
echo "-1" > $EVALRESULTFILE
echo "-1" >> $EVALRESULTFILE
echo "-1" >> $EVALRESULTFILE
echo "-1" >> $EVALRESULTFILE
echo "-1" >> $EVALRESULTFILE
echo "-1" >> $EVALRESULTFILE

if [ ! -f ${BASE_BIN} ]; then
    make -f $MAKEFILE base_bin CFLAGS="-w -O3" || exit 1
fi

#Generate Oracle Bin (if needed)
if [ ! -f ${ORIGIN_BIN} ]; then
    make -f $MAKEFILE origin_real_bin CFLAGS="-w -O3" || exit 1
fi


#Compile Reduced Program
make -f $MAKEFILE reduced_bin CFLAGS="-w -O3" || exit 1

#Count Binary Bytes
original_size=$((`ls -l ${ORIGIN_BIN} | cut -d' ' -f5`-`ls -l $BASE_BIN | cut -d' ' -f5`))
reduced_size=$((`ls -l ${BIN} | cut -d' ' -f5`-`ls -l $BASE_BIN | cut -d' ' -f5`))

#Count Gadgets
original_gdt=`${GDTBIN} --binary ${ORIGIN_BIN} | grep 'Unique gadgets' | cut -d' ' -f4`
reduced_gdt=`${GDTBIN} --binary ${BIN} | grep 'Unique gadgets' | cut -d' ' -f4`

#Count Stmts
original_stmts=0
reduced_stmts=0
while IFS= read -r filename; do
    if [ -f "$filename" ]; then
        echo "Processing $filename..." > /dev/null
        statements_info=$($STMT_COUNTER $filename)
        echo "Statements information for $filename:" > /dev/null
        echo "$statements_info" > /dev/null

        # Extract and sum the statement count from the output
        original_stmts=$((original_stmts + statements_info))
    else
        echo "File $filename not found."
    fi

    echo > /dev/null
    
    reduced_filename="${filename%%.*}"  # Remove last part after dot
    reduced_filename="$reduced_filename.c"  # Add .c extension
    if [ -f "$reduced_filename" ]; then
        echo "Processing additional file $reduced_filename..." > /dev/null
        reduced_statements_info=$($STMT_COUNTER $reduced_filename)
        echo "Statements information for $reduced_filename:" > /dev/null
        echo "$reduced_statements_info" > /dev/null

        reduced_stmts=$((reduced_stmts + reduced_statements_info))

    else
        echo "Additional file $reduced_filename not found." > /dev/null
    fi
done < "programlist"

#Output to file
echo "${original_size}" > $EVALRESULTFILE
echo "${reduced_size}" >> $EVALRESULTFILE
echo "${original_gdt}" >> $EVALRESULTFILE
echo "${reduced_gdt}" >> $EVALRESULTFILE
echo "${original_stmts}" >> $EVALRESULTFILE
echo "${reduced_stmts}" >> $EVALRESULTFILE
