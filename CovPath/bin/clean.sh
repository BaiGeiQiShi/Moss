#!/bin/bash

PROGID=$1

if [ -z ${PROGID} ]; then
    echo "Missing program id."
    exit 1
fi

rm -fr identify_path/*
rm -fr identify_path_input/file/*
rm -fr identify_path_input/arg/*
rm -fr identify_path_input.cp/file/*
rm -fr identify_path_input.cp/arg/*

rm -fr quantify_path/*
rm -fr quantify_path_input/file/*
rm -fr quantify_path_input/arg/*
rm -fr quantify_path_input.cp/file/*
rm -fr quantify_path_input.cp/arg/*

rm -fr sample_output/*
rm -fr errcode/*
rm -f *.txt
rm -fr tmp/*
rm -fr domgad-out
rm -fr progcounter-out

rm -fr $PROGID.c.reduced.c
rm -fr $PROGID.c.cov.c 
rm -fr $PROGID.reduced 
rm -fr $PROGID.origin 
rm -fr $PROGID
