#!/usr/bin/env python3.8
import subprocess,os
#region ENVSandARGS
METHOD={"DEBOP":0,"BASICBLOCK":1,"COVBLOAT":2,"TMCMC":3,"MOSS":4}
PROGNAME="integration1"
version=str.upper("TMCMC")
debop_samplenum=str(10)
domgad_samplenum=str(10)
TIMEOUT="20m"
alphas=list(map(str,[0]))
ks=list(map(str,[50,]))
betas=list(map(str,[0.5]))
CURRDIR=os.getcwd()
DEBOP_DIR="/usr/local/Moss/CovBlock_Stmt"
DEBOP_BIN=f"{DEBOP_DIR}/build/bin/reducer"
DOMGAD_DIR="/usr/local/Moss/CovPath"
COV="/usr/local/cov"
LINEPRINTERBIN=f"{DOMGAD_DIR}/build/bin/instrumenter -g statement test.sh"
# SEARCHBIN=f"java -cp ':{DOMGAD_DIR}/build/java:{DOMGAD_DIR}/lib/java/*:{DOMGAD_DIR}/lib/java/commons-cli-1.5.0/*' moss.covpath.GCovBasedMCMCSearch"
SEARCHBIN=f"java -cp ':{DOMGAD_DIR}/build/java:{DOMGAD_DIR}/lib/java/*:{DOMGAD_DIR}/lib/java/commons-cli-1.5.0/*' -Djava.util.logging.config.file={DOMGAD_DIR}/src/java/main/logging.properties moss.covpath.GCovBasedMCMCSearch"
iternum=str(10)
realorcov="cov"
filter="nodeclstmt"
#endregion ENVSandARGS
def DEBOP(_rid):
    try:
        best=subprocess.check_output(f"timeout -s 9 {TIMEOUT} {DEBOP_BIN} -M Cov_info.txt -T COVBLOATBEST.c -m {debop_samplenum} -i {iternum} -t moss-out.{_rid} -a {alpha} -e {beta} -k {k} -s ./test.sh {PROGNAME}.c > log/{_rid}.txt",shell=True)
    except subprocess.CalledProcessError as e:
        if(e.returncode==137):pass
        else:raise e
    subprocess.run(["/usr/local/bin/getLog.py",f"{CURRDIR}/log/{_rid}.txt", f"{CURRDIR}/log/stat.{_rid}.txt"])
    with open(f"{CURRDIR}/log/stat.{_rid}.txt") as rid:
        best=rid.readline().split()
        if(best[0]!="-1"):
            os.system(f"cp {CURRDIR}/moss-out.{_rid}/{PROGNAME}.c.sample{best[0]}.c {CURRDIR}/DEBOPBEST.c")
        else:
            os.system(f"cp {CURRDIR}/COVBLOATBEST.c {CURRDIR}/DEBOPBEST.c")

def BASICBLOCK(_rid):
    try:
        best=subprocess.check_output(f"timeout -s 9 {TIMEOUT} {DEBOP_BIN} -B -m {debop_samplenum} -i {iternum} -t moss-out.{_rid} -a {alpha} -e {beta} -k {k} -s ./test.sh {PROGNAME}.c > log/{_rid}.txt",shell=True)
    except subprocess.CalledProcessError as e:
        if(e.returncode==137):pass
        else:raise e
    subprocess.run(["/usr/local/bin/getLog.py",f"{CURRDIR}/log/{_rid}.txt", f"{CURRDIR}/log/stat.{_rid}.txt"])
    with open(f"{CURRDIR}/log/stat.{_rid}.txt") as rid:
        best=rid.readline().split()
        if(best[0]!="-1"):
            os.system(f"cp {CURRDIR}/moss-out.{_rid}/{PROGNAME}.c.sample{best[0]}.c {CURRDIR}/BASICBLOCKBEST.c")
        else:
            os.system(f"cp {CURRDIR}/{PROGNAME}.c.cov.origin.c {CURRDIR}/BASICBLOCKBEST.c")

def COVBLOAT(_rid):
    try:
        os.system(f"diff {PROGNAME}.c {CURRDIR}/TMCMCBEST.c");print("\n"*5)
        subprocess.run(f"timeout -s 9 {TIMEOUT} {DEBOP_BIN} -F ./Cov_info.txt -T TMCMCBEST.c -m {debop_samplenum} -i {iternum} -t moss-out.{_rid} -a {alpha} -e {beta} -k {k} -s ./test.sh {PROGNAME}.c > log/{_rid}.txt",shell=True)
    except subprocess.CalledProcessError as e:
        if(e.returncode==137):pass
        else:raise e
    subprocess.run(["/usr/local/bin/getLog.py",f"{CURRDIR}/log/{_rid}.txt", f"{CURRDIR}/log/stat.{_rid}.txt"])
    with open(f"{CURRDIR}/log/stat.{_rid}.txt") as rid:
        best=rid.readline().split()
        if(best[0]!="-1"):
            os.system(f"cp {CURRDIR}/moss-out.{_rid}/{PROGNAME}.c.sample{best[0]}.c {CURRDIR}/COVBLOATBEST.c")
        else:
            os.system(f"cp {CURRDIR}/TMCMCBEST.c {CURRDIR}/COVBLOATBEST.c")

def TMCMC(alpha,beta,k):
    #make identify_path and quantify_path
    os.system("rm -rf identify_path quantify_path moss-out.{realorcov}.{filter}.s{domgad_samplenum}.a{alpha}.b{beta}.k{k}.v3")
    os.system("mkdir identify_path")
    for test in os.listdir(f"{CURRDIR}/tmp/bin.gcov"):
        subprocess.run(["cp",f"{CURRDIR}/tmp/bin.gcov/{test}",f"{CURRDIR}/identify_path/{test[5:].split('.')[0]}"])
    os.system(f"cp -r {CURRDIR}/identify_path {CURRDIR}/quantify_path")

    #generate path_count.txt
    with open(f"{CURRDIR}/tmp/path_counted.txt","w+") as pc:
        for pid,ipath in enumerate(sorted(os.listdir(f"{CURRDIR}/identify_path"))):
            print(f"{pid},{ipath},1,{ipath}",file=pc)

    #run domgad stochasticsearch.sh
    quan_num=str(len(os.listdir(f"{CURRDIR}/quantify_path")))
    os.system(f"echo {quan_num} > {CURRDIR}/tmp/quan_num.txt")

    rid=f"{realorcov}.{filter}.s{domgad_samplenum}.a{alpha}.b{beta}.k{k}.v3"
    os.system(f"cp {CURRDIR}/1 {CURRDIR}/identify_path/1")
    os.system(f"timeout -s 9 {TIMEOUT} {SEARCHBIN} -t {CURRDIR}/tmp/path_counted.txt -i {CURRDIR}/identify_path -s {CURRDIR}/tmp/sample_output -I {iternum} -m {domgad_samplenum} -F {CURRDIR}/programlist -L {CURRDIR}/lines -p {CURRDIR} -n {PROGNAME} -r {alpha} -w  {beta} -k {k} -q {quan_num} -S 2 &>{CURRDIR}/log/{rid}.txt")
    exit()
    subprocess.run(["cp","tmp/sample_output",f"{CURRDIR}/moss-out.{rid}","-r"])
    subprocess.run(["/usr/local/bin/getLog.py",f"{CURRDIR}/log/{rid}.txt", f"{CURRDIR}/log/stat.{rid}.txt"])
    with open(f"{CURRDIR}/log/stat.{rid}.txt") as rid:
        best=rid.readline().split()
        os.system(f"cp {CURRDIR}/tmp/sample_output/{best[0]}.c {CURRDIR}/TMCMCBEST.c")

if(not os.path.isdir(f"{CURRDIR}/log")):
    os.system(f"mkdir {CURRDIR}/log")
    
for k in ks:
    for alpha in alphas:
        for beta in betas:

            os.system(f"{LINEPRINTERBIN} {CURRDIR}/{PROGNAME}.c.real.origin.c > {CURRDIR}/path_generator/line.txt")
            subprocess.run([f"{CURRDIR}/path_generator/generate_cov.py", PROGNAME, COV, DOMGAD_DIR])

#            print(beta);os.system("sleep 1")
            #region init envs and do some cleaning
            os.system(f"echo {PROGNAME}.c.origin.c {PROGNAME}.c | xargs -n 1 cp {PROGNAME}.c.{realorcov}.origin.c")
            subprocess.run(" ".join(["rm","-rf","output.origin","inputfile","*BEST.c"]),shell=True)
            os.system(f"cp programlist tmp")
            # subprocess.run(["source","/etc/profile"])
            #endregion init envs and do some cleaning

            if(version=="MOSS"):
                for subversion in ("TMCMC","COVBLOAT","DEBOP"):
                    try:
                        #os.system(f"echo {alpha} {beta} {subversion} >>check.txt")
                        if(subversion=="TMCMC"):
                            eval(subversion)(alpha,beta,k)
                        else:
                            eval(subversion)(f"{realorcov}.{filter}.s{debop_samplenum}.a{alpha}.b{beta}.k{k}.v{METHOD[subversion]}")
                        #subprocess.run(["cp",f"{CURRDIR}/{subversion}BEST.c",f"{CURRDIR}/{PROGNAME}.c.cov.origin.c"])
                        subprocess.run(["cp",f"{CURRDIR}/{PROGNAME}.c.cov.origin.c",f"{CURRDIR}/{PROGNAME}.c"])
                    except IndexError as ie:
                        print(ie) #just pass this iter if it do nothing

            else:
                rid = f"{realorcov}.{filter}.s{debop_samplenum}.a{alpha}.b{beta}.k{k}.v{METHOD[version]}"
                while(True):
                    try:
                        if(version=="DEBOP"):
                            DEBOP(rid)
                        elif(version=="BASICBLOCK"):
                            BASICBLOCK(rid)
                        elif(version=="COVBLOAT"):
                            COVBLOAT(rid)
                        elif(version=="TMCMC"):
                            TMCMC(alpha,beta,k)
                            rid=f"a{alpha}.b{beta}.k{k}.mcmclog"
                        
                        break
                    except subprocess.CalledProcessError as e:#error and need restart
                        print(e)
