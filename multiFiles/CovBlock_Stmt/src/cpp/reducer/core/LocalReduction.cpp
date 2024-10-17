#include "LocalReduction.h"

#include <spdlog/spdlog.h>
#include <stdlib.h>
#include <unordered_map>

#include <clang/Basic/Diagnostic.h>
#include <clang/Basic/LangOptions.h>
#include <clang/Analysis/CFG.h>
#include <iostream>
#include <string>
#include <stack>
#include <sstream>
#include <algorithm>

#include "clang/Lex/Lexer.h"

#include "FileManager.h"
#include "OptionManager.h"
#include "Profiler.h"
#include "SourceManager.h"
#include "ASTsList.h"

using BinaryOperator = clang::BinaryOperator;
using BreakStmt = clang::BreakStmt;
using CallExpr = clang::CallExpr;
using ContinueStmt = clang::ContinueStmt;
using CompoundStmt = clang::CompoundStmt;
using DeclGroupRef = clang::DeclGroupRef;
using DeclStmt = clang::DeclStmt;
using FunctionDecl = clang::FunctionDecl;
using GotoStmt = clang::GotoStmt;
using IfStmt = clang::IfStmt;
using LabelStmt = clang::LabelStmt;
using ReturnStmt = clang::ReturnStmt;
using SourceRange = clang::SourceRange;
using SourceLocation = clang::SourceLocation;
using Stmt = clang::Stmt;
using UnaryOperator = clang::UnaryOperator;
using WhileStmt = clang::WhileStmt;
using VarDecl = clang::VarDecl;
using Decl = clang::Decl;
using LabelDecl = clang::LabelDecl;
using Expr = clang::Expr;
using DeclRefExpr = clang::DeclRefExpr;
using ForStmt = clang::ForStmt;
using SwitchStmt = clang::SwitchStmt;
using DoStmt = clang::DoStmt;

// Function to check if two sets overlap, used for baseinputs
bool doSetsOverlap(const std::vector<int> &set1, const std::vector<int> &set2);

void LocalReduction::Initialize(clang::ASTContext &Ctx)
{
	Reduction::Initialize(Ctx);
	CollectionVisitor = new LocalElementCollectionVisitor(this);
}

bool LocalReduction::HandleTopLevelDecl(DeclGroupRef D)
{
	clang::SourceManager &SM = Context->getSourceManager();
	for (DeclGroupRef::iterator I = D.begin(), E = D.end(); I != E; ++I)
	{
		if (SourceManager::IsInHeader(SM, *I))
			continue;
		CollectionVisitor->TraverseDecl(*I);
	}
	return true;
}

/// file is OptionManager::Inputfile
/// selected index is ASTsList::SelectingElementIdx
/// what to do: ASTsList::AddbackOrRemoveFlag
void LocalReduction::HandleTranslationUnit(clang::ASTContext &Ctx)
{
	bool FilterNullElements = true;
	bool FilterDeclElements = true;
	const clang::SourceManager &SM = Context->getSourceManager();

	std::string CurrentFile = SM.getFilename(SM.getLocForStartOfFile(SM.getMainFileID())).str();

	std::vector<clang::Stmt *> LocalProgElements;
	std::vector<clang::FunctionDecl *> ProgFuncDecls;

#pragma region Build LocalProgElements from input file(Origin)
	for (auto const &FD : Functions)
	{
		spdlog::get("Logger")->trace("--{}", FD->getNameInfo().getAsString());

		if (clang::CompoundStmt *FDBodyCS = llvm::dyn_cast<clang::CompoundStmt>(FD->getBody()))
		{
			std::vector<clang::Stmt *> AllPrimitiveChildrenStmts = getAllPrimitiveChildrenStmts(FDBodyCS);
			if (!AllPrimitiveChildrenStmts.empty())
			{
				int idx = 0;
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

					llvm::StringRef SStr;
					clang::SourceLocation Start = SourceManager::GetBeginOfStmt(Context, S);
					clang::SourceLocation End = SourceManager::GetEndOfStmt(Context, S);
					int line_num = SM.getSpellingLineNumber(Start);
					if (End.isInvalid() || Start.isInvalid())
					{
						spdlog::get("Logger")->trace("---Location Invalid");
						continue;
					}
					else
					{
						SStr = SourceManager::GetSourceText(SM, Start, End);
						spdlog::get("Logger")->trace("---ProgElement {}\@line {}: {}", idx, line_num, SStr.str());
					}

					LocalProgElements.emplace_back(S);
					idx += 1;
				}
			}
		}
		ProgFuncDecls.emplace_back(FD);
	}
#pragma endregion

#pragma region Sync LocalProgElements with ASTsList::StmtIndexList
	// 1. find the first element of the inputfile in StmtIndexList
	auto it = std::find_if(ASTsList::StmtIndexList.begin(), ASTsList::StmtIndexList.end(), [CurrentFile](std::tuple<std::string, int, std::string, std::vector<int>, int, int, clang::Stmt *> &StmtIndex)
						   { return std::get<2>(StmtIndex) == CurrentFile; });
	if (it == ASTsList::StmtIndexList.end())
	{
		spdlog::get("Logger")->debug("**Didn't find any element pointing to {} for localreduction", CurrentFile);
		return;
	}

	size_t offset = std::distance(ASTsList::StmtIndexList.begin(), it);
	// 2. sync(the sizes of two vectors[LocalProgElements and StmtIndexList[CurrentFile]] are the same)
	for (int i = 0; i != LocalProgElements.size(); ++i)
	{
		if (std::get<1>(*it) == 0)
		{ // stmtindex is removed in StmtIndexList
			clang::SourceLocation Start = SourceManager::GetBeginOfStmt(Context, LocalProgElements[i]);
			clang::SourceLocation End = SourceManager::GetEndOfStmt(Context, LocalProgElements[i]);
			removeSourceText(Start, End);
		}
		it++;
	}
#pragma endregion

	if (OptionManager::SelectingStrategy == 0 || // select statement
		OptionManager::SelectingStrategy == 1)	 // or coverage-statement
	{
		int local_select_index = -1;

		if (std::get<2>(ASTsList::StmtIndexList[ASTsList::SelectingElementIdx]) == OptionManager::InputFile)
		{ // check if the selected statement is in the inputfile, otherwise just pass the file
			Stmt *TStmt;
			clang::SourceLocation Start, End;
			std::get<0>(ASTsList::StmtIndexList[ASTsList::SelectingElementIdx]);
#pragma region Prepare: find the Stmt in local context
			for (auto S : LocalProgElements)
			{ // find the selected statement in local(this) file
				local_select_index += 1;
				Start = SourceManager::GetBeginOfStmt(Context, S);
				End = SourceManager::GetEndOfStmt(Context, S);
				if (std::get<0>(ASTsList::StmtIndexList[ASTsList::SelectingElementIdx]) == SourceManager::GetSourceText(SM, Start, End).str())
				{
					spdlog::get("Logger")->debug("--Found it: {}", std::get<0>(ASTsList::StmtIndexList[ASTsList::SelectingElementIdx]));
					break;
				}
			}
			if (local_select_index == -1)
			{
				// maybe the file don't have any statements actually... See in Moss/CovBlock_Stmt/test/integration1/subfile1.c
				spdlog::get("Logger")->warn("**Didn't find Selected Statement");
				return;
			}
#pragma endregion

			spdlog::get("Logger")->info("LocalReduction On File: {}", CurrentFile);

			int existflag = std::get<1>(ASTsList::StmtIndexList[ASTsList::SelectingElementIdx]);
			spdlog::get("Logger")->debug("--Reverse Exist/Removed flag for Element {}: {}->{}", ASTsList::SelectingElementIdx, existflag, 1 - existflag);
			std::get<1>(ASTsList::StmtIndexList[ASTsList::SelectingElementIdx]) = 1 - existflag; // set the exist/removed flag to opposite value

			spdlog::get("Logger")->debug("--Add-back or reduce");
			if (std::get<1>(ASTsList::StmtIndexList[ASTsList::SelectingElementIdx]) == 0) // already set to 0(reduced)
			{																			  // Reduce it
				spdlog::get("Logger")->info("--Reducing ProgElement of Index: {}", ASTsList::SelectingElementIdx);
				spdlog::get("Logger")->debug("--Removesourcetext");
				removeSourceText(Start, End);
			}
			else
			{ // Add it back
				spdlog::get("Logger")->info("--Adding back ProgElement of Index: {}", ASTsList::SelectingElementIdx);
				spdlog::get("Logger")->debug("--Replacetext");
				TheRewriter.ReplaceText(SourceRange(Start, End), std::get<0>(ASTsList::StmtIndexList[ASTsList::SelectingElementIdx]));
			}
			spdlog::get("Logger")->debug("--Overwritechangedfiles");
			TheRewriter.overwriteChangedFiles();
		}
		else
		{ // not in this file
			spdlog::get("Logger")->debug("--not in this file");
			return;
		}
	}
	else if (OptionManager::SelectingStrategy == 2)
	{
		std::vector<int> local_select_indices;
		int local_select_index = -1;
		std::vector<int> sorted_covered_testcases = std::next(ASTsList::CoverageBlocks.begin(), ASTsList::SelectingElementIdx)->first;

#pragma region Prepare: find local selected indices
		for (int i = 0; i != LocalProgElements.size(); ++i)
		{
			if (sorted_covered_testcases == std::get<3>(ASTsList::StmtIndexList[i + offset]))
			{
				// For elements in selected coverage block, while also covered by BaseInputs. We exclude these elements by not affecting them.
				if (OptionManager::BaseInputs.compare("none") != 0 and doSetsOverlap(std::get<3>(ASTsList::StmtIndexList[i + offset]), OptionManager::BaseTestID))
				{
					continue;
				}
				
				local_select_indices.emplace_back(i);
			}
		}
#pragma endregion

		if (local_select_indices.size() == 0)
		{
			return;
		}
		else
		{
			spdlog::get("Logger")->info("-Reduce File: {}", CurrentFile);
			spdlog::get("Logger")->info("--reduce local indices: {}", fmt::join(local_select_indices, ","));

			int existflag = std::get<0>(ASTsList::CoverageBlocks[sorted_covered_testcases]);
			spdlog::get("Logger")->debug("--Reverse Exist/Removed flag for Element {}: {}->{}", ASTsList::SelectingElementIdx, existflag, 1 - existflag);
			std::get<0>(ASTsList::CoverageBlocks[sorted_covered_testcases]) = 1 - existflag; // set the exist/removed flag to opposite value

#pragma region do reduce
			spdlog::get("Logger")->debug("--Add-back or reduce");
			for (auto idx : local_select_indices)
			{
				clang::Stmt *TStmt = LocalProgElements[idx];
				clang::SourceLocation Start = SourceManager::GetBeginOfStmt(Context, TStmt);
				clang::SourceLocation End = SourceManager::GetEndOfStmt(Context, TStmt);

				if (std::get<0>(ASTsList::CoverageBlocks[sorted_covered_testcases]) == 0) // already set to 0(reduced)
				{																		  // Reduce it
					spdlog::get("Logger")->debug("--Reducing ProgElement of Index: {}", idx + offset);
					removeSourceText(Start, End);
				}
				else
				{ // Add it back
					spdlog::get("Logger")->debug("--Adding back ProgElement of Index: {}", idx + offset);
					TheRewriter.ReplaceText(SourceRange(Start, End), std::get<0>(ASTsList::StmtIndexList[idx + offset]));
				}

				// don't forget to reverse the stmt of selected covblock
				std::get<1>(ASTsList::StmtIndexList[idx + offset]) = 1 - std::get<1>(ASTsList::StmtIndexList[idx + offset]);
			}
			spdlog::get("Logger")->debug("--Overwritechangedfiles");
			TheRewriter.overwriteChangedFiles();
#pragma endregion
		}
	}
	else if (OptionManager::SelectingStrategy == 3 or OptionManager::SelectingStrategy == 4)
	{
		llvm::errs() << "Not Implemented\n";
		exit(1);
	}
}

bool LocalElementCollectionVisitor::VisitFunctionDecl(FunctionDecl *FD)
{
	// spdlog::get("Logger")->debug("Visit Function Decl: {}",
	// FD->getNameInfo().getAsString());
	if (FD->isThisDeclarationADefinition())
		Consumer->Functions.emplace_back(FD);
	return true;
}

std::vector<int> LocalReduction::split(const std::string &str, char sep)
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
