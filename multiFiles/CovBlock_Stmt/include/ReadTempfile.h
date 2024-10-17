#ifndef READTEMPFILE_H
#define READTEMPFILE_H

#include <queue>
#include <vector>
#include <nlohmann/json.hpp>

#include "clang/AST/RecursiveASTVisitor.h"

#include "Transformation.h"


using json =  nlohmann::json; 

class ReadTempfileElementCollectionVisitor;


class ReadTempfile : public Transformation {
    friend class ReadTempfileElementCollectionVisitor;
public:
    ReadTempfile() : CollectionVisitor(NULL) {  }
    ~ReadTempfile() { delete CollectionVisitor; }
private:
    void Initialize(clang::ASTContext &Ctx);
    bool HandleTopLevelDecl(clang::DeclGroupRef D);
    void HandleTranslationUnit(clang::ASTContext &Ctx);

    std::vector<clang::FunctionDecl *> Functions;
    std::queue<clang::Stmt *> Queue;

    ReadTempfileElementCollectionVisitor *CollectionVisitor;
    clang::FunctionDecl *CurrentFunction;
};

class ReadTempfileElementCollectionVisitor
    : public clang::RecursiveASTVisitor<ReadTempfileElementCollectionVisitor>
{
    public:
        ReadTempfileElementCollectionVisitor(ReadTempfile *R) : Consumer(R) {}

        bool VisitFunctionDecl(clang::FunctionDecl *FD);

    private:
        ReadTempfile *Consumer;
};

#endif