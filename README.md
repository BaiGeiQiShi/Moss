# Moss
Moss is a multi-goal debloating technique. In its current implementation, Moss supports an objective function that encodes three goals: size reduction, attack surface reduction, and generality. It leverages a Markov Chain Monte Carlo (MCMC) sampling technique to explore the search space and search for a debloated program that maximizes the objective.



## Installation
check INSTALL.md for installation
### Requirements
* CMake >= 3.10.2
* Clang and LLVM >= 9.0.0
* spdlog >= 1.3.1
* ROPgadget >= 5.8
* Frama-C >= 18.0
* nlohmann/json

Make sure you have installed all the required libraries (shown above). 
Please refer to https://github.com/llvm/llvm-project.git to install `LLVM & Clang`. 
Please refer to https://github.com/gabime/spdlog to install `spdlog`. 
Please refer to https://github.com/JonathanSalwan/ROPgadget to install `ROPgadget`.
Please refer to https://github.com/nlohmann/json to install `json`.

### Linux
Once you have all the requirements, do the following steps.

1. In CMakeLists.txt, change to your own paths the last two `include_directories` (lines ending with "Change to your own path!").
2. You need to seperately install two tools in two folders(```CovBlock_Stmt``` and ```CovPath```).
3. For ```CovBlock_Stmt```, Run `mkdir build && cd build`, `cmake ..` and `make`.
4. For ```CovPath```, Run `mkdir build && cd build`, `cmake ..`, `make` and `cd .. && chmod 755 compile_java && ./compile_java`.

## Installation Test
### Quicktest
### Integration Test

## Usage
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

### Note
It is strongly recommended that you provide Moss with a source file with all the code unexercised by the oracle inputs eliminated. This would make Moss's search space significantly smaller. To produce such a file, please refer to https://github.com/qixin5/debcov.

## Contact
If you have questions, please contact Qi Xin via qxin6@gatech.edu.
