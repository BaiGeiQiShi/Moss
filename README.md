# Moss
Moss is a multi-granularity, multi-objective program debloating technique. Moss supports an objective function that quantifies three objectives: size reduction, attack surface reduction, and generality. It leverages a Markov Chain Monte Carlo (MCMC) sampling algorithm to search for a debloated program with the highest objective function value.


## 1.Requirements
* CMake >= 3.10.2
* Clang and LLVM >= 9.0.0
* JDK >= 1.8
* spdlog >= 1.3.1
* ROPgadget >= 5.8
* nlohmann/json
* Ubuntu >= 18.04
* Postgresql == 12.14


## 2. Installation

### 2.1 Create the docker image

Use the `Dockerfile` in `./Docker` to create the docker image.

**Note:** Before using the `Dockerfile`, you can change the parallelism of `make -j` in line 41 of `Dockerfile` according to the number of CPUs available on your device. This is because building LLVM can take several hours, and using multiple CPUs in parallel can reduce this time.

```shell
docker build -t moss .
```

This docker image includes **Moss**, **Moss Benchmark**, **CMake**, **JDK 1.8**, **ROPgadget**, **spdlog**, **Clang && LLVM**, **Postgresql-12.14**, and other essential requirements.

### 2.2 Create and Run the container with this image

```shell
docker run -dit --name moss moss-env /bin/bash
```

### 2.3 Build Dependencies

```bash
# Run inside the docker
## Install packages that require interaction
apt install tcl8.6-dev expect

## Setup Postgresql
cd /postgresql-12.14 && \
chmod -R a+rw . && chown -R postgres . && \
CC=clang CFLAGS="-O3" ./configure --prefix=$(pwd)/pgsql && \
su postgres -c "make" && \
su postgres -c "make install" && \
rm -rf $(pwd)/pgsql/data && \
mkdir $(pwd)/pgsql/data && \
chmod -R a+rw . && chown -R postgres . && \
su postgres -c "pgsql/bin/initdb -D $(pwd)/pgsql/data" && \
su postgres -c "pgsql/bin/pg_ctl -D /postgresql-12.14/pgsql/data/ -l logfile start" && \
sed -n  's/Moss\/Cov/Moss-postgres\/Cov/' start_debloat.py && \
chmod -R a+rw src && cp -r /postgresql-12.14 /tmp/postgresql-12.14
```

### Note:
1. You need to seperately install two tools in two folders(```CovBlock_Stmt``` and ```CovPath```).
2. If you move the path of LLVM, you need to change the CMakeLists.txt in ```CovBlock_Stmt``` and ```CovPath```. In CMakeLists.txt, you should change the last two lines of `include_directories` (lines ending with "Change to your own path!") to your own paths.
3. To compile ```CovPath``` manually, you should run:
```
cd ./CovPath
mkdir build && cd build
cmake ..
make
```
4. To compile ```CovBlock_Stmt``` manually, you should run:
```
cd ./CovBlock_Stmt
mkdir build && cd build
cmake ..
make
cd .. && chmod 755 compile_java && ./compile_java
```

## 3. Quick Test

Run the test experiment to ensure your environment is correct. This command takes a maximum of 1 hour.

```shell
# Test your environment

```


## 4. Repeat whole experiments

``` shell
# Run Moss with must-handle inputs (26 programs)

# Run Moss without must-handle inputs (26 programs)

# Run Debop (26 programs)

# Run Debop-M (26 programs). Debop-M: Debop with must-handle inputs.

# Run Chisel with 22 programs

# Run Razor with 22 programs

# Run the ablation experiment

```


## 5. Usage
check usage.md for basic usage

```
PENBLOAT_BIN [OPTION] ORACLE_FILE SOURCE_FILE
```
**Moss_BIN**: The Moss binary (build/bin/reducer).

**ORACLE_FILE**: The oracle script used to compile source, run tests, and compute scores. It should compute a total of six scores:
1. Size of original program
2. Size of reduced program
3. Number of gadgets in original program
4. Number of gadgets in reduced program
5. Number of total tests
6. Number of tests the program passes
The resulting scores should be redirected to a file named "eval_rslt.txt".
See `test/quicktest/test.sh` for an example.

**SOURCE_FILE**: The program source file(s).

**OPTION**:
```
-m: Number of samples.
-i: Number of sampling iterations. (Default is 100. Consider using a large value.)
-a: Alpha value (weight for attack surface reduction).
-e: Beta value (weight for generality).
-k: K value (for computing density values).
-s: No value needed. Save temp files.
-B: If you want to use MCMC at the BasicBlock level, add this option.
-E: If you want to use MCMC at the Dependency level, add this option, and you must specify the dependency relation file following this option.
-F: If you want to use MCMC at the CovBlock level, add this option, and you must specify the coverage info file following this option.
-M: If you want to use MCMC at the Statement level with unequal select probability, add this option, and you must specify the coverage info file following this option.
-I: If you want to debloating programs with some base inputs(testcases that must pass), add this option, and you must specify the testcase id file following this option.
-T: If you want to debloating a reduced program, please add this option and specify the reduced program.

Note: If you want to use MCMC at the Statement level with equal select probability(i.e., Debop), you don't need to add -B -E -F -M options,  
```


## 6. How to use Moss 

### 6.1 Debloating without must-handle inputs

In the Moss working directory:

```shell
python3 start_debloat.py
```

Where:

- `METHOD`: 
- `PROGNAME`: 
- `version`: 
- `debop_samplenum`:
- `domgad_samplenum`:
- `alphas`:
- `ks`:
- `betas`:
- `CURRDIR`:
- `DEBOP_DIR`:
- `DOMGAD_DIR`:
- `COV`:
- `iternum`:
- `realorcov`:



### Note
It is strongly recommended that you provide Moss with a source file with all the code unexercised by the oracle inputs eliminated. This would make Moss's search space significantly smaller. To produce such a file, please refer to https://github.com/qixin5/debcov.

## Contact
If you have questions, please contact Qi Xin via qxin6@gatech.edu.
