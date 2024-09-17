FROM ubuntu:18.04

# Install necessary packages
RUN apt-get update && apt-get install -y cmake  \
    libreadline-dev && \
    apt install -y libncurses-dev && \
    apt install -y openjdk-8-jdk   \
    python3-pip  \
    wget  \
    libmlpack-dev \
    libperl-dev \
    python2.7-dev \
    git && \
    python3 -m pip install ROPgadget && \
    cd /tmp/ && \
    wget https://github.com/gabime/spdlog/archive/refs/tags/v1.0.0.tar.gz && \
    tar -zxf v1.0.0.tar.gz && mv spdlog-1.0.0/include/spdlog /usr/include/

# Clone llvm-project
WORKDIR /usr/local/
RUN wget https://github.com/llvm/llvm-project/archive/refs/tags/llvmorg-9.0.1.tar.gz && \
    tar -xf llvmorg-9.0.1.tar.gz && mv llvm-project-llvmorg-9.0.1 llvm-project

# Clone Moss
WORKDIR /usr/local/
RUN git clone https://github.com/BaiGeiQiShi/Moss.git && \
    cp -r Moss Moss-postgres

# Clone Cov
WORKDIR /usr/local/
RUN git clone https://github.com/qixin5/cov.git

# Clone MossBenchmark
WORKDIR /
RUN git clone https://github.com/BaiGeiQiShi/MossBenchmark.git

# Build LLVM
WORKDIR /usr/local/llvm-project
RUN mkdir build && cd build && \
    cmake -DCMAKE_BUILD_TYPE=Release \
    -DLLVM_ENABLE_PROJECTS="clang;clang-tools-extra;compiler-rt;libclc;libcxx;libcxxabi;libunwind;lld;polly" \
    -DLLVM_ENABLE_RTTI=ON \
    -DLLVM_USE_LINKER=gold \
    -G "Unix Makefiles" /usr/local/llvm-project/llvm && \
    make -j && make install

# Build Moss
WORKDIR /usr/local/Moss
RUN mkdir -p /usr/local/Moss/CovBlock_Stmt/build && cd /usr/local/Moss/CovBlock_Stmt/build && cmake .. && make && \
    mkdir -p /usr/local/Moss/CovPath/build && cd /usr/local/Moss/CovPath/build && cmake .. && make && \
    cd /usr/local/Moss/CovPath && ./compile_java

# Build cov
WORKDIR /usr/local/cov
RUN ./compile.sh && bash ./setenv

# Add user for chown-8.2
RUN groupadd mychown && \
    useradd -m -g mychown mychown && \
    passwd -d mychown

# Setup for postgresql
WORKDIR /
RUN cp -r /MossBenchmark/postgresql-12.14 /postgresql-12.14 && \
		adduser postgres && \
		passwd -d postgres

CMD ["/bin/bash"]