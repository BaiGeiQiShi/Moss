#!/usr/bin/env python3.8
from enum import Enum
import os
import re
import subprocess
import sys
# import numpy as np

#region error wrappers
'''
Error code:
    7: Domgad Stopped unexpected
    8: Init SR score is Not Nero
    9: Initial G score is Not 1
    10: All testcases are BaseInput
'''
class ErrorCode(Enum):
    DSError=7
    ISNZError=8
    IGN1Error=9
    ABIError=10
    IL=11
def mail_With_Same_Subject_Content(*args):
    subprocess.run(["/usr/local/bin/sendemail.py",args[0],*args])
#endregion error wrappers

def logAnalysePass(GetAccepts:bool,inputfile:str,outputfile:str, GetAll=False):
    try:
       r=subprocess.check_call(f"cat {inputfile} | grep 'All test cases are base inputs, illegal!'",shell=True)
    except subprocess.CalledProcessError as err:
        if(err.returncode == 0):
            mail_With_Same_Subject_Content(f"All testcases are base inputs===>{os.path.abspath(inputfile)}")
            exit(ErrorCode.ABIError)
    with open(inputfile,"r") as input, open(outputfile,"w+") as output:
            Acceptes=[]
            AllScores=[]
            tmpScores=[]
            Initscores=[];inited=False
            BestScores=[]
            args=[]
            while l:=input.readline():
                #region Find_input_args
                if(len(args)<3):
                    for arg in ("Weight kr:","Weight w:", "K-value:"):
                        if(re.match(arg,l)):
                            args.append(eval(l.split(":")[1]))
                    #endregion
                #region Get Best
                if(l.startswith("Best Sample Id:")):
                    Initscores.clear()
                    for i in range(6):
                        Initscores.append(l[:-1].split()[-1])
                        l=input.readline()
                    BestScores=list(Initscores)
                    if(not inited):
                        Initscores.append("1")
                        AllScores.append(tuple(Initscores))
                        Initscores.clear()
                        inited=True
                #endregion

                #region Get_sample
                elif(re.match("Generate sample id: ",l)):
                    tmpScores.append(l[len("Generate sample id: "):-1])
                elif(re.match("Size Reduction: ",l)):
                    tmpScores.append(l[len("Size Reduction: "):-1])
                elif(re.match("AttkSurf Reduction: ",l)):
                    tmpScores.append(l[len("AttkSurf Reduction: "):-1])
                elif(re.match("Reduction: ",l)):
                    tmpScores.append(l[len("Reduction: "):-1])
                elif(re.match("Generality: ",l)):
                    tmpScores.append(l[len("Generality: "):-1])
                elif(re.match("OScore: ",l)):
                    tmpScores.append(l[len("OScore: "):-1])
                elif(re.match("Reject",l)):
                    tmpScores.append("0")
                elif(re.match("Accept",l)):
                    tmpScores.append("1")
                #endregion
                

                if(len(tmpScores)==7):
                    AllScores.append(tuple(tmpScores))
                    # if(tmpScores[0]!=BestScores[0]):
                    #     BestScores=list(tmpScores)
                    tmpScores.clear()
            else:
                if(len(tmpScores)!=0):
                    AllScores.append(tuple(tmpScores))
    
    if(not GetAccepts):
        #scores: sample, SR, AR, R, G, O, Accept
        Samples = {}
        for scores in AllScores:        
            Samples.setdefault(eval(scores[0]),[]).append(list(scores[3:5]))
            try:
                #region DiffSample
                if(not GetAll):
                    Samples[eval(scores[0])][-1].append(abs(eval(scores[3])-eval(Samples[eval(scores[0])-1][-1][0])))
                    Samples[eval(scores[0])][-1].append(abs(eval(scores[4])-eval(Samples[eval(scores[0])-1][-1][1])))
                #endregion
                #region GetAll
                elif(GetAll):
                    Samples[eval(scores[0])][-1].append(eval(scores[0]))
                    Samples[eval(scores[0])][-1].append(eval(scores[1]))
                    Samples[eval(scores[0])][-1].append(abs(eval(scores[2])))
                    Samples[eval(scores[0])][-1].append(abs(eval(scores[3])))
                    Samples[eval(scores[0])][-1].append(abs(eval(scores[4])))
                    Samples[eval(scores[0])][-1].append(abs(eval(scores[5])))
                #endregion
            except:
                pass
            if(scores[-1]=="1"):Samples[eval(scores[0])][-1].append(1)
            else:Samples[eval(scores[0])][-1].append(0)

        with open(outputfile,"w+") as output:
            output.seek(0)
            for samplenum in Samples:
                for ss in Samples[samplenum]:
                    print(*ss[2:],sep="\t",file=output)
        BestScores_String="\t".join(BestScores)
        os.system(f"sed -i '1i\{BestScores_String}' {outputfile}")
        subprocess.run(["/usr/local/bin/sendemail.py", f"{inputfile} done", f"{inputfile} done",f"{os.path.abspath(inputfile)}"])
    elif(GetAccepts):
        #region DiffAccepts
        Samples = []
        for scores in AllScores:
            if(scores[-1]=="1"):
                Samples.append(list(scores[3:5]))
                try:
                    Samples[-1].append(abs(eval(scores[3])-eval(Samples[-2][0])))
                    Samples[-1].append(abs(eval(scores[4])-eval(Samples[-2][1])))
                except:
                    pass
        with open(outputfile,"w") as output:
            for sample in Samples:
                print(*sample[2:],file=output)
        #endregion
    return args
            


if __name__ == "__main__":
    inputfile=sys.argv[1]
    outputfile=sys.argv[2]
    _, b, _ = logAnalysePass(GetAccepts=False,GetAll=True,inputfile=inputfile,outputfile=outputfile)
        
        
