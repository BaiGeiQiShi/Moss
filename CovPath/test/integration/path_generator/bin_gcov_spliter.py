#!/usr/bin/env python3
import os,sys
gcovfile = sys.argv[1]

with open(gcovfile) as gcf: 
    lines = gcf.readlines()
    # print(lines)
    start=0;end=0
    while(start<len(lines)):
        if(lines[start].startswith("file:") and (lines[start].endswith(".c\n") or lines[start].endswith(".cpp\n"))):
            end=start+1
            partial_name = lines[start][5:-1]+".bin.gcov" #"file:/foo/bar\n" => "/foo/bar.bin.gcov"
            while(end != len(lines) and not lines[end].startswith("file:")):
                end+=1
            # print(partial_name)
            # print(f"start:{start}; end:{end}")
            with open(partial_name,"w") as p:
                p.writelines(lines[start:end])
            start=end
