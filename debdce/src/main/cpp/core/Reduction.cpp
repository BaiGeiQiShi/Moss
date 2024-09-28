#include "Reduction.h"

#include <algorithm>
#include <spdlog/spdlog.h>
#include <iostream>
#include <fstream>

#include "clang/Basic/SourceManager.h"
#include "llvm/Support/Program.h"

#include "OptionManager.h"
#include "Profiler.h"

void Reduction::Initialize(clang::ASTContext &C) {
  Transformation::Initialize(C);
}
