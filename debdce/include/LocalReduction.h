#ifndef LOCAL_REDUCTION_H
#define LOCAL_REDUCTION_H

#include <queue>
#include <vector>

#include "clang/AST/RecursiveASTVisitor.h"

#include "Reduction.h"

class LocalElementCollectionVisitor;

/// \brief Represents a local reduction phase
///
/// In local reduction phase, local statements are reduced
/// hierarchically with respect to AST.
class LocalReduction : public Reduction {
  friend class LocalElementCollectionVisitor;

public:
  LocalReduction() : CollectionVisitor(NULL) {}
  ~LocalReduction() { delete CollectionVisitor; }

private:
  void Initialize(clang::ASTContext &Ctx);
  bool HandleTopLevelDecl(clang::DeclGroupRef D);
  void HandleTranslationUnit(clang::ASTContext &Ctx);
  void HandleFunctionDecl(clang::FunctionDecl *FD);
  //bool RemoveIfDangling(const clang::Stmt *Stmt);
  bool RemoveIfDangling(clang::Stmt *Stmt);
  //bool IsSideEffectFree(const clang::Stmt *Stmt);
  bool IsSideEffectFree(clang::Stmt *Stmt);

  std::set<clang::Stmt *> RemovedElements;
  std::vector<clang::FunctionDecl *> Functions;
  std::queue<clang::Stmt *> Queue;

  LocalElementCollectionVisitor *CollectionVisitor;
  clang::FunctionDecl *CurrentFunction;
};

class LocalElementCollectionVisitor
    : public clang::RecursiveASTVisitor<LocalElementCollectionVisitor> {
public:
  LocalElementCollectionVisitor(LocalReduction *R) : Consumer(R) {}

  //bool shouldTraversePostOrder() const { return true; } //Visit in post order
  bool VisitFunctionDecl(clang::FunctionDecl *FD);

private:
  LocalReduction *Consumer;
};

#endif // LOCAL_REDUCTION_H
