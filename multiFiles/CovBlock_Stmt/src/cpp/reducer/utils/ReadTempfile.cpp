#include "ReadTempfile.h"

#include "clang/Basic/SourceManager.h"
#include "llvm/Support/Program.h"

#include <spdlog/spdlog.h>
#include <stdlib.h>

#include <clang/Basic/Diagnostic.h>
#include <clang/Basic/LangOptions.h>
#include <clang/Analysis/CFG.h>
#include <iostream>
#include <string>
#include <map>
#include <stack>
#include <sstream>

#include "clang/Lex/Lexer.h"
#include "OptionManager.h"
#include "SourceManager.h"
#include "ASTsList.h"

void ReadTempfile::Initialize(clang::ASTContext &Ctx)
{
    Transformation::Initialize(Ctx);
    Context = &Ctx;
    CollectionVisitor = new ReadTempfileElementCollectionVisitor(this);
}

bool ReadTempfile::HandleTopLevelDecl(clang::DeclGroupRef D)
{
    clang::SourceManager &SM = Context->getSourceManager();
    for (clang::DeclGroupRef::iterator I = D.begin(), E = D.end(); I != E; ++I)
    {
        if (SourceManager::IsInHeader(SM, *I))
            continue;
        CollectionVisitor->TraverseDecl(*I);
    }
    return true;
}

/// @brief Complete ASTsList::StmtIndexList
void ReadTempfile::HandleTranslationUnit(clang::ASTContext &Ctx)
{
    bool FilterNullElements = true;
	bool FilterDeclElements = true;

    const clang::SourceManager &SM = Context->getSourceManager();
    
    std::string CurrentFile = SM.getFilename(SM.getLocForStartOfFile(SM.getMainFileID())).str();
    spdlog::get("Logger")->debug("-Get Statements For File: '{}'", CurrentFile);
    std::vector<clang::Stmt *> LocalProgElements;
    std::vector<clang::FunctionDecl *> ProgFuncDecls;
    std::unordered_map<int,int> elem_to_fd;

#pragma region Get Statements
    for (auto const &FD : Functions)
    {
        spdlog::get("Logger")->debug("--{}", FD->getNameInfo().getAsString());

        if (clang::CompoundStmt *FDBodyCS = llvm::dyn_cast<clang::CompoundStmt>(FD->getBody()))
        {
            std::vector<clang::Stmt *> AllPrimitiveChildrenStmts = getAllPrimitiveChildrenStmts(FDBodyCS);
            if (!AllPrimitiveChildrenStmts.empty())
            {
                for (auto const &S : AllPrimitiveChildrenStmts)
                {
                    if (FilterNullElements)
                    { // Ignore Null stmts
                        if (clang::NullStmt *NS = llvm::dyn_cast<clang::NullStmt>(S))
                            continue;
                    }
                    if (FilterDeclElements)
                    { // Ignore Decl stmts
                        if (clang::DeclStmt *DS = llvm::dyn_cast<clang::DeclStmt>(S))
                            continue;
                    }
                    LocalProgElements.emplace_back(S);
                }
            }
        }

        ProgFuncDecls.emplace_back(FD);
    }
    int prog_element_num = LocalProgElements.size();
#pragma endregion Get Statements

#pragma region SyncWithTempfile
    // 1. find the first element of the inputfile in StmtIndexList
	auto it = std::find_if(ASTsList::StmtIndexList.begin(),ASTsList::StmtIndexList.end(), [CurrentFile](std::tuple<std::string,int,std::string,std::vector<int>,int,int,clang::Stmt*>& StmtIndex){
		return std::get<2>(StmtIndex)==CurrentFile;
	});
	if(it == ASTsList::StmtIndexList.end()){
		spdlog::get("Logger")->warn("**Didn't find any element pointing to {} for tempfile.sync", CurrentFile);
		return;
	}
	size_t offset = std::distance(ASTsList::StmtIndexList.begin(), it);
    
    // 2. sync(the sizes of two vectors[LocalProgElements and StmtIndexList[CurrentFile]] are the same)
    int idx=0;
    while(std::get<2>(*it)==CurrentFile){
        clang::SourceLocation Start = SourceManager::GetBeginOfStmt(Context, LocalProgElements[idx]);
        clang::SourceLocation End = SourceManager::GetEndOfStmt(Context, LocalProgElements[idx]);
        llvm::StringRef SStr = SourceManager::GetSourceText(SM, Start, End);

        if(std::get<0>(*it)!=SStr){//sstr not in StmtIndexList
            std::get<1>(*it)=0; //set existence of it to 0(removed)
        }
        // else sstr in StmtIndexList
        it++;
	}
	#pragma endregion
#pragma endregion
}

bool ReadTempfileElementCollectionVisitor::VisitFunctionDecl(clang::FunctionDecl *FD)
{
    spdlog::get("Logger")->debug("--Visit Function Decl: {}",
                                 FD->getNameInfo().getAsString());
    if (FD->isThisDeclarationADefinition())
        Consumer->Functions.emplace_back(FD);
    return true;
}
