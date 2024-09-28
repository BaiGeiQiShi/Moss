#ifndef REDUCTION_H
#define REDUCTION_H

#include <vector>

#include "ProbabilisticModel.h"
#include "Transformation.h"

using DDElement = llvm::PointerUnion<clang::Decl *, clang::Stmt *>;
using DDElementVector = std::vector<DDElement>;
using DDElementSet = std::set<DDElement>;

/// \brief Represents a general reduction step
class Reduction : public Transformation {
public:
  Reduction() {}
  ~Reduction() {}

protected:
  virtual void Initialize(clang::ASTContext &Ctx);

};

#endif // REDUCTION_H
