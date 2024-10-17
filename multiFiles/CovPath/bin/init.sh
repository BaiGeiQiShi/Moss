#!/bin/bash

PROGID=$1

if [ -z $PROGID ]; then
    echo "Missing program id."
    exit 1
fi

#Path Identification
if [ ! -d identify_path ]; then
    mkdir identify_path
fi

if [ ! -d identify_path_input ]; then
    mkdir identify_path_input
fi

if [ ! -d identify_path_input/file ]; then
    mkdir identify_path_input/file
fi

if [ ! -d identify_path_input/arg ]; then
    mkdir identify_path_input/arg
fi

if [ ! -d identify_path_input.cp ]; then
    mkdir identify_path_input.cp
fi

if [ ! -d identify_path_input.cp/file ]; then
    mkdir identify_path_input.cp/file
fi

if [ ! -d identify_path_input.cp/arg ]; then
    mkdir identify_path_input.cp/arg
fi



#Path Quantification
if [ ! -d quantify_path ]; then
    mkdir quantify_path
fi

if [ ! -d quantify_path_input ]; then
    mkdir quantify_path_input
fi

if [ ! -d quantify_path_input/file ]; then
    mkdir quantify_path_input/file
fi

if [ ! -d quantify_path_input/arg ]; then
    mkdir quantify_path_input/arg
fi

if [ ! -d quantify_path_input.cp ]; then
    mkdir quantify_path_input.cp
fi

if [ ! -d quantify_path_input.cp/file ]; then
    mkdir quantify_path_input.cp/file
fi

if [ ! -d quantify_path_input.cp/arg ]; then
    mkdir quantify_path_input.cp/arg
fi


#Others
if [ ! -d sample_output ]; then
    mkdir sample_output
fi

if [ ! -d tmp ]; then
    mkdir tmp
fi
