# Moss
Moss is a multi-granularity, multi-objective program debloating technique. Moss supports an objective function that quantifies three objectives: size reduction, attack surface reduction, and generality. It leverages a Markov Chain Monte Carlo (MCMC) sampling algorithm to search for a debloated program with the highest objective function value.

### Note:
Moss starts debloating from the source file with all the code unexercised by the oracle inputs eliminated. 
This makes Moss's search space significantly smaller. 
We use [debcov](https://github.com/qixin5/debcov) to produce such a file. To use debcov conveniently in our experiments, we renamed it to cov and integrated it into this repository.


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

**Note:** Before using the `Dockerfile`, you can change the parallelism of `make -j` in line 32 of `Dockerfile` according to the number of CPUs available on your device. This is because building LLVM can take several hours, and using multiple CPUs in parallel can reduce this time.

```shell
docker build -t moss-env .
```

This docker image includes **Moss**, **Moss Benchmark**, **CMake**, **JDK 1.8**, **ROPgadget**, **spdlog**, **Clang && LLVM**, **Postgresql-12.14**, and other essential requirements.

### 2.2 Create and run the container with this image

```shell
docker run -dit --name moss moss-env /bin/bash
```

### 2.3 Build Dependencies

```bash
# Inside the docker container
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
chmod -R a+rw src && cp -r /postgresql-12.14 /tmp/postgresql-12.14
```

### Note:
1. Currently, we have implemented two versions of Moss for single-file and multi-file debloating. In each version, you need to seperately install two tools in two folders (i.e., ```CovBlock_Stmt``` and ```CovPath```).
3. If you move the path of LLVM, you need to change the CMakeLists.txt in ```CovBlock_Stmt``` and ```CovPath```. In CMakeLists.txt, you should change the last two lines of `include_directories` (lines ending with "Change to your own path!") to your own paths.
4. To compile ```CovPath``` manually, you should run:
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
Run the test experiment to ensure your environment is correct. This command takes a few minutes.
```shell
# Test your environment
cd ./quicktest
python3 start_debloat.py
```
After running, check that you have three `moss-out` directories containing the sample C files and three debloating logs (end with `.txt`) in `./log`. These logs should show that Moss generated **1** sample at each stage. If you've found all these, the running was successful.


## 4. Usage
```
MOSS_BIN [OPTION] ORACLE_FILE SOURCE_FILE
```
**MOSS_BIN**: The Moss binary (CovPath or CovBlock_Stmt).

**ORACLE_FILE**: The oracle script used to compile source, run tests, and compute scores. It should compute a total of six scores:
1. Size of original program.
2. Size of reduced program.
3. Number of gadgets in original program.
4. Number of gadgets in reduced program.
5. Number of total inputs.
6. Number of inputs that the program can correctly handle.

**SOURCE_FILE**: The program source file(s).

**OPTION**:
```
# CovPath
-t: Path to trace count file
-i: Path to the identify path folder
-s: Path to save samples
-m: Max samples number
-I: Max iterations number
-f: The single file program to be reduced. #Can not work with -L together
-F: A file that indicates a list of file to be reduced. #Can not work with -f together
-l: A file that indicates line information
-L: A file that indicates the program file list and corresponding lines information file
-p: Temporary working directory of the debloating process
-n: The program name
-r: Alpha value (weight for attack surface reduction)
-w: Beta value (weight for generality)
-k: K value (for computing density values)
-q: Total inputs used for path quantification
-S: Set the size reduction type (0: covered lines; 1: executable bytes; 2: covered stmts)
-B: A file that indicates the must-handle inputs
-U: Timeout of debloating process

# CovBlock_Stmt
-m: Max samples number
-i: Max iterations number
-a: Alpha value (weight for attack surface reduction)
-e: Beta value (weight for generality)
-k: K value (for computing density values)
-s: If use this option, Moss will save each generated sample program
-F: If you want to use MCMC at the CovBlock level, add this option, and you must specify the coverage info file following this option
-M: If you want to use MCMC at the Statement level with unequal select probability, add this option, and you must specify the coverage info file following this option
-I: A file that indicates the must-handle inputs.
-T: If you want to debloat a program that has already been debloated, please add this option and specify the reduced program
-U: Timeout of debloating process
```

### Note:
We also integrated Debop into Moss. If you don't use either the -F option or the -M option, the default setting of CovBloct_Stmt is Debop.


## 5. Reproduce our experiments
In the docker container, we have cloned the Moss benchmark. To reproduce our experiments, you can execute the following cli command:

``` shell
# 1.Choose an arbitrary program to debloat
## Choose an arbitrary program (from 25 programs) to debloat
cd /MossBenchmark/$PROGRAM
## To debloat PostgreSQL, you need to use this following command instead. Since PostgreSQL can only be executed under the postgresql user, we created a separate copy of PostgreSQL in the root directory to facilitate file permission modifications.
cd ./postgresql-12.14

# 2.Run Moss without must-handle inputs
python3 start_debloat.py

# 3.Run Moss with must-handle inputs
python3 start_debloat_must.py

# 4.Run Debop
python3 start_debloat_debop.py

# 5.Run Debop-M (Debop with must-handle inputs).
python3 start_debloat_debopm.py

# 6.Run the ablation experiment
python3 start_debloat-s12.py  #Moss-s1,2
python3 start_debloat-s13.py  #Moss-s1,3
python3 start_debloat-s23.py  #Moss-s2,3

# 7.Run Chisel with 22 programs
./run_chisel

# 8.Run Razor with 22 programs
./run_razor
```

If you want to know more details about the benchmark, please refer to [MossBenchmark](https://github.com/BaiGeiQiShi/MossBenchmark).


## Contact
If you have questions, please contact Jinran Tang via jinrantang@whu.edu.cn.
