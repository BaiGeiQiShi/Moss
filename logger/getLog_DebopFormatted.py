#!/usr/bin/env python3.8
'''
For debop format(start with "Oracle...")
'''
from sys import argv
import os
import subprocess
from enum import Enum
logfile,outputfile=argv[1:]
a,b,k=0,0,0
with open(logfile) as Log, open(outputfile,"w+") as Output:
    log = Log.readlines()
    current_scores=[];best_score=[]
    init_check=False
    if(not log[0].startswith("Oracle:") and not log[0].startswith("Input:") and not log[0].startswith("-base")):
        raise RuntimeError("this file is not debop-format")
    for line_idx,line in enumerate(log):
        #region Get Init Paras
        if(line.startswith("Alpha Value")):a=eval(line[:-1].split(":")[-1]);continue
        if(line.startswith("Beta Value")):b=eval(line[:-1].split(":")[-1]);continue
        if(line.startswith("K Value")):k=eval(line[:-1].split(":")[-1]);continue
        if(line.startswith("Initial SR-Score")):
            current_scores=[-1,-1]+list(map(float,(p.split(":")[1] for p in line[:-1].split(";"))))
            print(*current_scores,sep="\t",file=Output,flush=True);current_scores.clear();continue
        #endregion
        #region Main Iteration Part
        try:
            #region completing @best_score
            if(len(best_score)!=7):
                if(line.startswith("Current Best Sample Id")):
                    best_score.append(str(int(line[:-1].split(":")[-1])));continue
                if(line.startswith("Current Best SR-Score")):
                    best_score+=list(map(float,(p.split(":")[1] for p in line[:-1].split(";"))));continue
            #endregion
            #region compare best with comfront one
            elif(len(best_score)==7 and line.startswith("Current Best Sample Id")):
                if(best_score[0]!=line[:-1].split()[-1]):# a new best sample. Update best_score
                    best_score.clear();best_score.append(line[:-1].split(":")[-1]);continue
            #endregion
            #region completing @current score and print to @outputfile
            if(line.startswith("Current Iteration:")):
                current_scores+=list(map(int,(p.split(":")[1] for p in line[:-1].split(";"))));continue
            if(len(current_scores)!=8):
                if(line.startswith("SR-Score:")):
                    current_scores+=list(map(float,(p.split(":")[1] for p in line[:-1].split(";"))))
                    print(*current_scores,sep="\t",file=Output,end="\n",flush=True)
                    #update best_score in case the last sample is best
                    if(len(log)-line_idx<30 and float(current_scores[-2])>float(best_score[-2])):
                        best_score=list(current_scores[1:])
                    current_scores.clear();continue
            #endregion

        except Exception as e:
            print(e,line_idx,line)
        #endregion
        #region ErrorHandle
        '''
        Error code:
            12117: CovBloat Stopped unexpected
            12118: Init SR score is Not Nero
            12119: Initial G score is Not 1
        '''
        #region error wrappers
        class ErrorCode(Enum):
            CBSError=12117
            ISNZError=12118
            IGN1Error=12119
        def mail_With_Same_Subject_Content(*args):
            subprocess.run(["/usr/local/bin/sendemail.py",args[0],*args])
        #endregion error wrappers
        if(line.startswith("Some bugs occur during random selecting!")):
            mail_With_Same_Subject_Content(f"covBloat stop unexpected==>'{os.path.abspath(logfile)}'",os.path.abspath(logfile))
            exit(ErrorCode.CBSError)
        if(not init_check and len(current_scores)==8):
            if(current_scores[2]!=0):#init SR is not 0
                mail_With_Same_Subject_Content(f"'{os.path.abspath(logfile)}' get wrong input .c file at SR")
                exit(ErrorCode.ISNZError)
            if(current_scores[5]!=1):#init G is not 1
                mail_With_Same_Subject_Content(f"'{os.path.abspath(logfile)}' get wrong input .c file at G")
                exit(ErrorCode.IGN1Error)
            init_check=True
        if(len(log)<20):
            mail_With_Same_Subject_Content(f"{os.path.abspath(logfile)} is {len(log)} lines. Some bugs may occur",os.path.abspath(logfile))
            exit(1)
        #endregion ErrorHandle
    best_score_string='\t'.join(str(item) for item in best_score)
    subprocess.run(f"sed -i '1i{best_score_string}\n' {outputfile}",shell=True)
    # print("Best sample: ","\t".join([str(item) for item in best_score]))
    mail_With_Same_Subject_Content(f"'{logfile}' is done. \nBest sample:{best_score}",os.path.abspath(logfile))
    exit(0)
