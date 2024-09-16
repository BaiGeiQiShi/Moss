# Parameters:
#  CFLAGS: indicate compiling options(by default is `-w -O3`)

usage:
	# This Makefile is invoked by Oracle(test.sh).
	echo

PGO: integration1.c.real.origin.c subfile0.c.real.origin.c subfile1.c.real.origin.c
	clang -fprofile-instr-generate -fcoverage-mapping -o integration1 -I ./include $^

#all: integration1.o subfile0.o subfile1.o

%.o: %.c
	clang $(CFLAGS) -I ./include -c $< -o $@

clean:
	rm -rf *.o integration1 output output.origin tmp/*

origin_bin: integration1.o subfile0.o subfile1.o
	@# We capture compiling process in /CovBlock_Stmt/src/cpp/reducer/utils/Integration.cpp, 
	@# which need compile each source file individually. So compile pattern is like this. 
	clang -o integration1 $^


origin_real_bin: integration1.c.real.origin.c subfile0.c.real.origin.c subfile1.c.real.origin.c
	clang $(CFLAGS) -o integration1.origin -I ./include $^

reduced_bin: integration1.c subfile0.c subfile1.c
	clang $(CFLAGS) -o integration1 -I ./include $^

base_bin: integration1.c.base.origin.c subfile0.c.base.origin.c subfile1.c.real.origin.c
	clang $(CFLAGS) -o integration1.base -I ./include $^

.PHONY: clean all reduced_bin origin_bin usage PGO

# `origin_bin` and `origin_real_bin` are actually the same bin with different names.
# But when calculating the binary size, names will affect size either.
# Therefore, origin_bin and reduced_bin are used to calculate the size.
# and origin_real_bin are used to execute and get output 
