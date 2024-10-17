#ifndef AST2STMTS_H
#define AST2STMTS_H

#include <queue>
#include <vector>
#include <nlohmann/json.hpp>

#include "clang/AST/RecursiveASTVisitor.h"

#include "Transformation.h"
#include "ASTsList.h"
#include "OptionManager.h"

using json =  nlohmann::json; 

class AST2StmtsElementCollectionVisitor;
/**
 * @brief Convert ASTs to statements.
 *        Will be called by ASTsList::InitializeStmtsList
*/
class AST2Stmts : public Transformation {
    friend class AST2StmtsElementCollectionVisitor;
public:
    AST2Stmts() : CollectionVisitor(NULL) {  }
    ~AST2Stmts() { delete CollectionVisitor; }
private:
    void Initialize(clang::ASTContext &Ctx);
    bool HandleTopLevelDecl(clang::DeclGroupRef D);
    void HandleTranslationUnit(clang::ASTContext &Ctx);

    std::vector<clang::FunctionDecl *> Functions;
    std::queue<clang::Stmt *> Queue;

    AST2StmtsElementCollectionVisitor *CollectionVisitor;
    clang::FunctionDecl *CurrentFunction;
    std::vector<int> split(const std::string &str, char sep);
};

class AST2StmtsElementCollectionVisitor
    : public clang::RecursiveASTVisitor<AST2StmtsElementCollectionVisitor>
{
    public:
        AST2StmtsElementCollectionVisitor(AST2Stmts *R) : Consumer(R) {}

        bool VisitFunctionDecl(clang::FunctionDecl *FD);

    private:
        AST2Stmts *Consumer;
};
#endif // AST2STMTS_H
