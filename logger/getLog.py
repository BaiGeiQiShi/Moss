#!/usr/bin/env python3
from sys import argv
import re
import os
import subprocess
logfile,outputfile=argv[1:]
with open(logfile) as Log, open(outputfile,"w") as Output:
    firstline = Log.readline()
    if(firstline.startswith("Program name: ")):
        subprocess.run(["/usr/local/bin/getLog_DomgadFormatted.py",logfile,outputfile])         
    elif(firstline.startswith("Oracle:") or firstline.startswith("Input:") or firstline.startswith("-base")):
            subprocess.run(["/usr/local/bin/getLog_DebopFormatted.py",logfile,outputfile])
    else:
        raise RuntimeError(f"{logfile} is neither Debop-format nor Domgad-format")
