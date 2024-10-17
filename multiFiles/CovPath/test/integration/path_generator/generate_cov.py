#!/usr/bin/env python3

import os,time,shutil
from sys import argv,version
from multiprocessing import Pool, Manager
import subprocess
from progress.bar import Bar

PROGRAM,COV,DOMGAD_DIR=argv[1:]
SRCs=['integration1.c.real.origin.c', 'subfile0.c.real.origin.c', 'subfile1.c.real.origin.c']
CURRDIR=os.getcwd()
merge_sh=f"{CURRDIR}/path_generator/merge_bin_gcov_files.sh"
getbin=f"{CURRDIR}/path_generator/get_bin_gcov_file.sh"
compile=f"{CURRDIR}/compile.sh"
INDIR=f"{CURRDIR}/testscript/kn"
INDIR_CP=f"{CURRDIR}/inputfile"
OUTDIR=f"{CURRDIR}/output.origin/kn"
TMP=f"{CURRDIR}/tmp"
LINEPRINTERBIN=f"{DOMGAD_DIR}/build/bin/instrumenter -g statement /dev/null"

print(f"generating {PROGRAM}.c.cov.origin.c")

subprocess.run(["rm","-rf","lines","programlist"])
subprocess.run(["touch","lines","programlist"])

#clean input folder
subprocess.run(["rm","-rf",INDIR_CP])
subprocess.run(["mkdir","-p",INDIR_CP])

#clean output folder #why wouldn't put output folder to 
subprocess.run(["rm","-rf",OUTDIR])
subprocess.run(["mkdir","-p",OUTDIR])

#use $CURRDIR/tmp for execution
subprocess.run(["rm","-rf",TMP])
subprocess.run(["mkdir","-p",TMP])

#region copy inputs
os.system(f"cp -r {CURRDIR}/input.origin/* {INDIR_CP}")
os.system(f"cp integration1.c.real.origin.c subfile1.c.real.origin.c subfile0.c.real.origin.c {TMP}/")
#endregion

os.system(f'make PGO -f {CURRDIR}/Makefile  && mv {CURRDIR}/{PROGRAM} {TMP}')

os.chdir(TMP)
for dir in ("lcov","real.gcov","bin.gcov"):
    os.mkdir(f"{TMP}/{dir}")

bar = Bar('Processing',max=len(os.listdir(INDIR)))


def DoTestcase(args):
    testcase=args
    bar.next()
    os.mkdir("%s/tmp/%s"%(CURRDIR,testcase))
    os.chdir("%s/tmp/%s"%(CURRDIR,testcase))

    current_env = os.environ.copy()
    current_env["LLVM_PROFILE_FILE"]=f"{TMP}/{testcase}/{testcase}-%p.profraw"
    subprocess.run([f"{CURRDIR}/testscript/kn/{testcase}",f"{TMP}/{PROGRAM}",OUTDIR,"1",INDIR_CP], env=current_env)
    os.system(f"llvm-profdata merge -o {TMP}/{testcase}/{testcase}.profdata {TMP}/{testcase}/*.profraw")
    os.system(f"llvm-cov export -format=lcov {TMP}/{PROGRAM} -instr-profile={TMP}/{testcase}/{testcase}.profdata "+" ".join(f"{TMP}/{fn}" for fn in SRCs)+ f" > {TMP}/{testcase}/{PROGRAM}.lcov")

    os.system(f"{COV}/bin/lcov2gcov {TMP}/{testcase}/{PROGRAM}.lcov > {TMP}/{testcase}/{PROGRAM}.real.gcov")
    os.system(f"{COV}/bin/gcovanalyzer {TMP}/{testcase}/{PROGRAM}.real.gcov getbcov > {TMP}/{testcase}/{PROGRAM}.bin.gcov")

    os.system(f"mv {TMP}/{testcase}/{PROGRAM}.lcov {TMP}/lcov/{testcase}.lcov")
    os.system(f"mv {TMP}/{testcase}/{PROGRAM}.real.gcov {TMP}/real.gcov/{testcase}.real.gcov")
    os.system(f"mv {TMP}/{testcase}/{PROGRAM}.bin.gcov {TMP}/bin.gcov/{testcase}.bin.gcov")

    os.chdir(TMP)
    os.chmod("%s/tmp/%s"%(CURRDIR,testcase),755)
    #os.system("rm -rf %s/tmp/%s"%(CURRDIR,testcase))

with Pool(1) as pool:
    pool.map(DoTestcase, os.listdir(INDIR))

bar.finish()
#get cov.origin.c
subprocess.run([merge_sh,COV,TMP])
os.system(f"{CURRDIR}/path_generator/bin_gcov_spliter.py merged.bin.gcov")

cov_files = []
with open("merged.bin.gcov") as merged:
    cov_files = [fn[5:-1] for fn in merged.readlines() if fn.startswith("file:") and fn.endswith(".c\n")]

for cf in cov_files:
    coved_cf = cf.replace(".c.real.origin.c",".c.cov.origin.c")
    print(coved_cf)

    os.system(f"{LINEPRINTERBIN} {cf} > {coved_cf}.line.txt")
    os.system(' '.join([f"{COV}/bin/gcovbasedcoderemover",cf, f"{coved_cf}.line.txt",f"{cf}.bin.gcov","false",">",coved_cf]))
    os.system("cp "+coved_cf+" "+TMP+"/"+os.path.basename(coved_cf))
    os.system(f"echo '{coved_cf}:{coved_cf}.line.txt' >> {CURRDIR}/lines")
    os.system(f"echo '{coved_cf}' >> {CURRDIR}/programlist")

# shutil.copyfile(f"{PROGRAM}.c.cov.origin.c",f"{CURRDIR}/{PROGRAM}.c.cov.origin.c")

#get cov_info.txt
shutil.copyfile(f"{CURRDIR}/extract_info.c",f"{TMP}/extract_info.c")
os.system(" ".join(["g++","extract_info.c","-o","extract_info"]))
os.system(" ".join(["./extract_info",">",f"{CURRDIR}/Cov_info.json"]))
# os.system(" ".join(["sort","-n",f"{CURRDIR}/Cov_info.txt","-o",f"{CURRDIR}/Cov_info.txt"]))



