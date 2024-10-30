FROM ubuntu:18.04

# Install necessary packages
RUN ln -s /usr/bin/python2.7 /usr/bin/python
RUN apt-get update && apt-get install -y cmake  \
    libreadline-dev && \
    apt install -y python3.8 && \
    update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.8 1 && \
    apt install -y libncurses-dev && \
    apt install -y openjdk-8-jdk   \
    python3-pip  \
    wget  \
    vim \
    libmlpack-dev \
    libperl-dev \
    python2.7-dev \
    git && \
    python3 -m pip install ROPgadget
RUN apt install -y nlohmann-json-dev \
    python-pip \
    python-pefile && \
    pip install pyelftools==0.29 && \
    pip install capstone==5.0.0 && \
    pip install progress

# Clone llvm-project
WORKDIR /usr/local/
RUN wget https://github.com/llvm/llvm-project/archive/refs/tags/llvmorg-9.0.1.tar.gz && \
    tar -xf llvmorg-9.0.1.tar.gz && mv llvm-project-llvmorg-9.0.1 llvm-project

# Build LLVM
WORKDIR /usr/local/llvm-project
RUN mkdir build && cd build && \
    cmake -DCMAKE_BUILD_TYPE=Release \
    -DLLVM_ENABLE_PROJECTS="clang;clang-tools-extra;compiler-rt;libclc;libcxx;libcxxabi;libunwind;lld;polly" \
    -DLLVM_BUILD_LLVM_DYLIB=ON \ 
    -DLLVM_LINK_LLVM_DYLIB=ON \
    -DLLVM_ENABLE_RTTI=ON \
    -DLLVM_USE_LINKER=gold \
    -G "Unix Makefiles" /usr/local/llvm-project/llvm && \
    make -j8 && make install
ENV CC=clang
ENV LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib

# Install spdlog
RUN wget https://github.com/gabime/spdlog/archive/refs/tags/v1.9.2.tar.gz && \
    tar -xf v1.9.2.tar.gz && mv spdlog-1.9.2/include/spdlog /usr/include/

# Clone Moss
WORKDIR /usr/local/
RUN git clone https://github.com/BaiGeiQiShi/Moss.git && \
    cp -r /usr/local/Moss/logger/* /usr/local/bin/

# Build Moss
WORKDIR /usr/local/Moss
RUN mkdir -p /usr/local/Moss/multiFiles/CovBlock_Stmt/build && cd /usr/local/Moss/multiFiles/CovBlock_Stmt/build && cmake .. && make && \
    mkdir -p /usr/local/Moss/multiFiles/CovPath/build && cd /usr/local/Moss/multiFiles/CovPath/build && cmake .. && make && \
    cd /usr/local/Moss/multiFiles/CovPath && chmod 755 ./compile_java  && ./compile_java

RUN mkdir -p /usr/local/Moss/singleFile/CovBlock_Stmt/build && cd /usr/local/Moss/singleFile/CovBlock_Stmt/build && cmake .. && make && \
    mkdir -p /usr/local/Moss/singleFile/CovPath/build && cd /usr/local/Moss/singleFile/CovPath/build && cmake .. && make && \
    cd /usr/local/Moss/singleFile/CovPath && chmod 755 ./compile_java  && ./compile_java

# Clone MossBenchmark
WORKDIR /
RUN git clone https://github.com/BaiGeiQiShi/MossBenchmark.git

# Build cov
WORKDIR /usr/local/Moss/cov
RUN chmod 755 setenv && chmod 755 compile.sh && ./compile.sh && ./setenv && chmod 755 bin/*

# Build debdce
WORKDIR /usr/local/Moss/debdce
RUN mkdir -p build && cd build && cmake .. && make

# Add user for chown-8.2
RUN groupadd mychown && \
    useradd -m -g mychown mychown && \
    passwd -d mychown

# Setup for postgresql
WORKDIR /
RUN cp -r /MossBenchmark/postgresql-12.14 /postgresql-12.14 && \
		adduser postgres && \
		passwd -d postgres

# Clone Chisel and Razor
WORKDIR /
RUN git clone https://github.com/aspire-project/chisel.git
RUN git clone https://github.com/cxreet/razor.git

# Build Chisel
WORKDIR /
RUN mkdir -p /chisel/build && cd /chisel/build && CXX=clang cmake .. && make
ENV PATH=$PATH:/chisel/build/bin

# Build Razor
WORKDIR /
RUN cd /razor/tracers && python build.py dynamorio && python build.py pin

CMD ["/bin/bash"]
