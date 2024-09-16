# Dockerfile
Because of the LLVM project which cause too much time to build, we divide the setup procedure into two files: Dockerfile for downloading projects, and a setup script to build each project
## Dockerfile
```Dockerfile
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

# Clone cov
WORKDIR /usr/local/
RUN git clone https://github.com/qixin5/cov.git

# Clone MossBenchmark
WORKDIR /
RUN git clone https://github.com/BaiGeiQiShi/MossBenchmark.git

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
```
## setup script
```bash
# run inside the docker
# Build llvm
cd /usr/local/llvm-project && mkdir build && cd build && \ 
cmake -DCMAKE_BUILD_TYPE=Release -DLLVM_ENABLE_PROJECTS="clang;clang-tools-extra;compiler-rt;libclc;libcxx;libcxxabi;libunwind;lld;polly" -DLLVM_ENABLE_RTTI=ON -DLLVM_USE_LINKER=gold -G "Unix Makefiles" /usr/local/llvm-project/llvm && \
make -j && make install
 
# Build Moss
cd /usr/local/Moss && \
git checkout db1180207ee347a414ddc5c8808c7995ff4bd881 && \
mkdir -p /usr/local/Moss/CovBlock_Stmt/build && cd /usr/local/Moss/CovBlock_Stmt/build && cmake .. && make && \
mkdir -p /usr/local/Moss/CovPath/build && cd /usr/local/Moss/CovPath/build && cmake .. && make && \
cd .. && ./compile_java

# Build Moss-postgres
cd /usr/local/Moss-postgres
mkdir -p /usr/local/Moss-postgres/{CovBlock_Stmt,CovPath}/build && \
cd /usr/local/Moss-postgres/CovBlock_Stmt/build && cmake .. && make && \
cd /usr/local/Moss-postgres/CovPath/build && cmake .. && make && \
cd .. && ./compile_java

# Build cov
cd /usr/local/cov && \
./compile.sh && bash ./setenv

# Setup Postgresql
## install packages that require interaction
apt install tcl8.6-dev expect
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

# Linux
Tested on Ubuntu 18.04
## Update package lists and install necessary packages
```bash
sudo apt-get update
# Note: package names and versions can change according to the system
sudo apt-get install -y cmake libreadline-dev libncurses-dev openjdk-8-jdk python3-pip wget libmlpack-dev libperl-dev python2.7-dev git  tcl8.6-dev expect
```
## Install ROPgadget using pip
```bash
python3 -m pip install ROPgadget
```
## Download and install spdlog
```bash
wget https://github.com/gabime/spdlog/archive/refs/tags/v1.0.0.tar.gz -P /tmp/
tar -zxf /tmp/v1.0.0.tar.gz -C /tmp/
sudo mv /tmp/spdlog-1.0.0/include/spdlog /usr/include/
```

## Clone llvm-project and build
```bash
cd /usr/local/
wget https://github.com/llvm/llvm-project/archive/refs/tags/llvmorg-9.0.1.tar.gz
tar -xf llvmorg-9.0.1.tar.gz
sudo mv llvm-project-llvmorg-9.0.1 llvm-project
cd /usr/local/llvm-project && mkdir build && cd build && \
cmake -DCMAKE_BUILD_TYPE=Release -DLLVM_ENABLE_PROJECTS="clang;clang-tools-extra;compiler-rt;libclc;libcxx;libcxxabi;libunwind;lld;polly" -DLLVM_ENABLE_RTTI=ON -DLLVM_USE_LINKER=gold -G "Unix Makefiles" /usr/local/llvm-project/llvm && \
make -j$(nproc) && make install
```
**NOTE**: Compile llvm projects from source may cause **memory exhaust**. If so, use swapfile to walk around.
### use swapfile (optional)
```bash
#in host machine
mkdir /swapfile && cd /swapfile && sudo dd if=/dev/zero of=swap bs=1024 count=40000000 && \
sudo mkswap -f  swap && sudo swapon swap
```

## Clone Moss and build
```bash
cd /usr/local/ && \
git clone https://github.com/BaiGeiQiShi/Moss.git && \
cd Moss && \
git checkout db1180207ee347a414ddc5c8808c7995ff4bd881 && \
sudo cp -r Moss /usr/local/Moss-postgres && \
cd /usr/local/Moss && \
mkdir -p CovBlock_Stmt/build && cd CovBlock_Stmt/build && cmake .. && make && \
mkdir -p CovPath/build && cd CovPath/build && cmake .. && make && \
cd .. && ./compile_java
```

## Clone cov
```bash
cd /usr/local/ && \
git clone https://github.com/qixin5/cov.git
cd /usr/local/cov && \
./compile.sh && source ./setenv
```
## Clone MossBenchmark
```
cd / && git clone https://github.com/BaiGeiQiShi/MossBenchmark.git
```

## Add user for chown-8.2
```
sudo groupadd mychown
sudo useradd -m -g mychown mychown
sudo passwd -d mychown
```

## Setup PostgreSQL
```
cd /tmp/postgresql-12.14 && \
sudo chown -R a+rw . && sudo chown -R postgres . && \
CC=clang CFLAGS="-O3" ./configure --prefix=$(pwd)/pgsql && \
su postgres -c "make" && \
su postgres -c "make install" && \
sudo rm -rf $(pwd)/pgsql/data && \
sudo mkdir $(pwd)/pgsql/data && \
su postgres -c "pgsql/bin/initdb -D $(pwd)/pgsql/data" && \
su postgres -c "pgsql/bin/pg_ctl -D /tmp/postgresql-12.14/pgsql/data/ -l logfile start" && \
sudo chmod -R a+rw src && sudo cp -r /tmp/postgresql-12.14 /tmp/postgresql-12.14
```
