#include "AST2Stmts.h"

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

void AST2Stmts::Initialize(clang::ASTContext &Ctx)
{
    Transformation::Initialize(Ctx);
    Context = &Ctx;
    CollectionVisitor = new AST2StmtsElementCollectionVisitor(this);
}

bool AST2Stmts::HandleTopLevelDecl(clang::DeclGroupRef D)
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
void AST2Stmts::HandleTranslationUnit(clang::ASTContext &Ctx)
{
    bool FilterNullElements = true;
    bool FilterDeclElements = true;
    const clang::SourceManager &SM = Context->getSourceManager();

    std::string CurrentFile = SM.getFilename(SM.getLocForStartOfFile(SM.getMainFileID())).str();
    spdlog::get("Logger")->info("-Get Statements For File: '{}'", CurrentFile);
    std::vector<clang::Stmt *> ProgElements;
    std::vector<clang::FunctionDecl *> ProgFuncDecls;
    std::unordered_map<int, int> elem_to_fd;

#pragma region Get Statements
    for (auto const &FD : Functions)
    {
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
                    ProgElements.emplace_back(S);
                }
            }
        }

        ProgFuncDecls.emplace_back(FD);
    }
    int prog_element_num = ProgElements.size();
#pragma endregion Get Statements

#pragma region complete statement information
    /// @brief Complete ASTsList::StmtIndexList with statement elements
    /// element is a tuple of <stmtrepr, exist=1/non-exist=0,
    /// AST/File id, sorted covered testcase array, basicblock id, dependency chain id >
    /// @attention: I didn't implement last two elements in the tuple(basicblock id, dependency chain id),
    ///             because we didn't use them in our paper.
    /// @author: sfgong@whu.edu.cn

    // 1. selecting strategy
    // 2. get covered testcases by specific selecting strategy
    // 3. Foreach statement, make tuple, and append to StmtIndexList
    int &selecting_strategy = OptionManager::SelectingStrategy;
    auto &stmtIndexList = ASTsList::StmtIndexList;
    std::map<int, std::vector<int>> CoverageInformation; // <key: line, value: testcases>
    if (selecting_strategy == 1 || selecting_strategy == 2 || OptionManager::BaseInputs.compare("none") != 0)
    {

        json &RawCoverageInformation = selecting_strategy == 2 ? OptionManager::CoverageString
                                                               : OptionManager::CovStatementString;
        spdlog::get("Logger")->debug("--RawCoverageInformation size: {}", RawCoverageInformation[CurrentFile].size());
        for (auto line : RawCoverageInformation[CurrentFile].items())
        {
            CoverageInformation[std::stoi(line.key())] = line.value().get<std::vector<int>>();
        }
    }

    for (int i = 0; i < prog_element_num; i++)
    {
        clang::Stmt *S = ProgElements[i];
        llvm::StringRef SStr;
        clang::SourceLocation Start = SourceManager::GetBeginOfStmt(Context, S);
        clang::SourceLocation End = SourceManager::GetEndOfStmt(Context, S);
        int line_num = SM.getSpellingLineNumber(Start);
        if (End.isInvalid() || Start.isInvalid())
        {
            spdlog::get("Logger")->debug("---ProgElement {}: Location Invalid", std::to_string(i));
            continue;
        }
        else
        {
            SStr = SourceManager::GetSourceText(SM, Start, End);
            spdlog::get("Logger")->info("---ProgElement {}\@line {}: {}", std::to_string(i), line_num, SStr.str());
        }

        std::vector<int> covered_testcases = std::vector<int>();
        if (selecting_strategy == 1 || selecting_strategy == 2 || (selecting_strategy == 0 && OptionManager::BaseInputs.compare("none") != 0))
        {
            covered_testcases = CoverageInformation[line_num];

            std::string covered_testcases_repr = fmt::format("{}", fmt::join(covered_testcases, ","));
            spdlog::get("Logger")->info("----covered testcases for {}: {}", std::to_string(i), covered_testcases_repr);
        }
        /// TODO: set basicblock id and dependency chain id

        ASTsList::StmtIndexList.emplace_back(std::make_tuple(SStr.str(), 1, CurrentFile, covered_testcases, -1, -1, S));

    }

#pragma endregion complete statement information
}

/// Astonishingly, there is no split function in C++.
std::vector<int> AST2Stmts::split(const std::string &str, char sep)
{
    std::vector<int> tokens;

    int i;
    std::stringstream ss(str);
    while (ss >> i)
    {
        tokens.push_back(i);
        while (ss.peek() == sep || ss.peek() == ' ')
        {
            ss.ignore();
        }
    }

    return tokens;
}

bool AST2StmtsElementCollectionVisitor::VisitFunctionDecl(clang::FunctionDecl *FD)
{
    spdlog::get("Logger")->debug("--Visit Function Decl: {}",
                                 FD->getNameInfo().getAsString());
    if (FD->isThisDeclarationADefinition())
        Consumer->Functions.emplace_back(FD);
    return true;
}
