# How to use
## Example 1: bzip2-1.0.5 from coreutils. Tutorial for setting basic parameters.
```bash
cd /MossBenchmark/bzip2-1.0.5
### Change parameters to modify the tradeoff ability
<< Parameters
version=str.upper("Moss") # version from `METHOD` dictionary in this file
debop_samplenum=str(100000)
domgad_samplenum=str(100000)
TIMEOUT="12h"
alphas=list(map(str,[0.25,0.5,0.75]))
ks=list(map(str,[50,]))
betas=list(map(str,[0.25,0.5,0.75]))
Parameters
### Run `Moss`, with default path of `DEBOP_DIR`, `DOMGAD_DIR`, `COV`.
./start_debloat.py
```

## Example 2: make-3.79 from LSIR. Need to modify the compiling arguments.
```bash
cd /MossBenchmark/make-3.79
### configure the compile.sh for compile path firstly
#### vim compile.sh
### Run Moss, with default path of `DEBOP_DIR, DOMGAD_DIR, COV` and parameters
./start_debloat.py
```

## Example 3: Postgresql-12.14.
```bash
cd /postgresql-12.14
### use start_debloat to wrap the invocation to start_debloat.py, for python script is sometimes not so robust
./start_debloat
```
