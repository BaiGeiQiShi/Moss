#!/bin/bash

LCOV2GCOV_BIN=../bin/lcov2gcov
GCOV_ANAL_BIN=../bin/gcovanalyzer

llvm-profdata merge -o test.profdata default.profraw
llvm-cov export -format=lcov ./test -instr-profile=test.profdata test.c >test.lcov

$LCOV2GCOV_BIN test.lcov >test.real.gcov
$GCOV_ANAL_BIN test.real.gcov getbcov >test.bin.gcov
