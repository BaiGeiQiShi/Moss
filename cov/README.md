# Coverage-based profiling and debloating

This is a repository containing code and instructions for performing coverage-based profiling and producing debloated programs based on the profiling result.

## Get started

1. Clone this repository
2. run `. setenv`
3. run `./compile.sh`

**NOTE**: You need to run `. setenv` each time you use the tool. Otherwise, set `COV_HOME` as a persistent environment variable whose value is the path to this directory (e.g., by adding an export line in `~/.profile`).

## llvm-cov

Check out `https://llvm.org/docs/CommandGuide/llvm-cov.html`.

### Usage

1. Compile the source code with Clang. Add "-fprofile-instr-generate -fcoverage-mapping" as a compiling option.

2. Execute the program with inputs. And you will get a file named `default.profraw`.

3. Produce a .lcov file.

  ```
  llvm-profdata merge -o PROGNAME.profdata default.profraw
  llvm-cov export -format=lcov PATH_TO_BIN -instr-profile=PROGNAME.profdata PATH_TO_SOURCE_FILE >PROGNAME.lcov
  ```
  
  - PROGNAME: a name for your program
  - PATH_TO_BIN: path to the program binary
  - PATH_TO_SOURCE_FILE: path to the source file
  
  
4. Convert .lcov into .gcov (needed by the coverage-based debloating tool).
  
  ```
  LCOV2GCOV_BIN PROGNAME.lcov >PROGNAME.real.gcov
  GCOV_ANAL_BIN PROGNAME.real.gcov getbcov >PROGNAME.bin.gcov
  ```
  
  - LCOV2GCOV_BIN: path to bin/lcov2gcov
  - GCOV_ANAL_BIN: path to bin/gcovanalyzer
  
5. Merge multiple .gcov files into a single file (with line counts aggregated).

  ```
  GCOV_MERGER_BIN binary BIN_GCOV_DIR >PROGNAME.bin.merged.gcov
  ```
  
  - GCOV_MERGER_BIN: path to bin/gcovbasedcoveragemerger
  - binary: a string literal
  - BIN_GCOV_DIR: path to directory containing .bin.gcov files
  
  
  
### Example

1. Get into `profile_example`.
   ```
   cd profile_example
   ```

2. Create some directories holding coverage files.
   ```
   mkdir lcov real.gcov bin.gcov
   ```

3. Compile `test.c` with instrumentation.
   ```
   clang -fprofile-instr-generate -fcoverage-mapping -w -o test test.c
   ```

4. Execute `test` with input `5`.
   ```
   ./test 5
   ```

5. Get coverage files and move them to the target directories.
   ```
   ./get_bin_gcov_file.sh

   mv test.lcov lcov/test_5.lcov
   mv test.real.gcov real.gcov/test_5.real.gcov
   mv test.bin.gcov bin.gcov/test_5.bin.gcov
   ```

6. Remove the old `default.profraw` (important) and execute `test` with input `0`.
   ```
   rm default.profraw
   ./test 0
   ```

7. Get coverage files and move them to the target directories.
   ```
   ./get_bin_gcov_file.sh

   mv test.lcov lcov/test_0.lcov
   mv test.real.gcov real.gcov/test_0.real.gcov
   mv test.bin.gcov bin.gcov/test_0.bin.gcov
   ```

8. Merge files in bin.gcov.
   ```
   ./merge_bin_gcov_files.sh
   ```

9. Check the result.
   - In `bin.gcov/test_5.bin.gcov`, you should see `lcount:4,0`, `lcount:7,0`, and `lcount:10,1`.
   - In `bin.gcov/test_0.bin.gcov`, you should see `lcount:4,0`, `lcount:7,1`, and `lcount:10,0`.
   - In `merged.bin.gcov`, you should see `lcount:4,0`, `lcount:7,1`, and `lcount:10,1`.

10. Clean the directory.
    ```
    ./clean.sh
    ```

## Cov

Coverage-based debloater

### Usage

```
bin/gcovbasedcoderemover CODE_FILE STMT_LINE_INFO_FILE GCOV_FILE
```

- CODE_FILE: the original program's source file.
- STMT_LINE_INFO_FILE: the file showing, for each statement, its starting and ending line numbers and its type.
- GCOV_FILE: a merged, binary-count gcov file (e.g., the .bin.gcov file used in the llvm-cov example).

NOTE: Code for producing STMT_LINE_INFO_FILE has not been included in the repo. I have however made such files for the 10 utilities in `stmt_line_info_files_for_ten_utilities`. These are files generated for the merged source code files in ChiselBench. You should not modify the original source code. Otherwise, the info files I created would be INVALID!


### Example

1. Get into `debloating_example`.
   ```
   cd debloating_example
   ```

   In this directory, you will find three files: `test.c`, `line.txt`, and `test_5.bin.gcov`. `test.c` is the source file; `line.txt` is the info file; and `test_5.bin.gcov` is a .gcov file containing binary counts (this file is generated basically by running the instrumented program with input 5; see the llvm-cov example for details).

2. Debloat the source code.
   ```
   ../bin/gcovbasedcoderemover test.c line.txt test_5.bin.gcov [true/false/none] > test_reduced.c
   ```

   You can pass `true`, `false`, or nothing for the last argument, which specifies whether the reduced program contains `printf` statements for instrumentation purpose.

   Check `test_reduced.c` and you should see the original last two `printf` statements deleted or replaced with `printf("Line XXX deleted.\n")` if you turn on the instrumentation mode.
