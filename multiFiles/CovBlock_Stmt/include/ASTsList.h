#ifndef ASTS_LIST_H
#define ASTS_LIST_H

#include <string>
#include <vector>
#include <map>
#include <utility>

#include "clang/AST/Stmt.h"

#include "OptionManager.h"

/// @brief Parse OptionManager::InputFiles into ASTslist, and 
///        convert the ASTslist as a statement index list.
class ASTsList {
    public:
        /// < stmtrepr, exist=1/non-exist=0, AST/File, sorted covered testcase array, basicblock id, dependency chain id, stmt ptr >
        static std::vector<std::tuple<std::string, int, std::string, std::vector<int>, int, int, clang::Stmt*>> StmtIndexList;
        /// selecting probility Coefficient of each statement
        static std::vector<double> StmtProbCoefList;
        /// sorted covered testcases => exist/removed, covered testcase number, selecting coefficiency
        static std::map<std::vector<int>, std::tuple<int, int, double>> CoverageBlocks; 
        
        /// 1:add back; 0:remove. 
        /// This flag is only useful when using CoverageBlock Selecting Strategy(`OptionManager::SelectingStrategy`).
        /// when the flag is 1, select elements according to the possibility of removing(coef, weight)
        /// when the flag is 0, select elements according to the possibility of adding-back, which is the reciprocal of coef
        static int AddbackOrRemoveFlag; 

        /// @brief Selected Element Index to reduce
        /// When using CoverageBlock Selecting Strategy(`OptionManager::SelectingStrategy==2`), 
        /// the Selecting Element Index only indicate to part of elements. That is, codes are to 
        /// be remove if the flag is 0, or to be added-back when flag is 1.
        ///
        /// For Statement/Coveraged Statement level, the element is StmtIndexList[SelectingElementIdx]
        /// For CoverageBlock level, the element is std::next(ASTsList::CoverageBlocks.begin(),ASTsList::SelectingElementIdx)
        static int SelectingElementIdx;

        static void InitializeStmtsList();
        static void InitializeStmtsProbCoef();
        static void Initialize();

        static void SelectElemIdx();

};

#endif // ASTS_LIST_H