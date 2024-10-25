#!/bin/bash

GCOV_MERGER_BIN=../bin/gcovbasedcoveragemerger

$GCOV_MERGER_BIN binary bin.gcov > merged.bin.gcov
