#include "LocalReduction.h"

#include <spdlog/spdlog.h>
#include <stdlib.h>
#include <unordered_map>

#include "clang/Lex/Lexer.h"

#include "FileManager.h"
#include "OptionManager.h"
#include "Profiler.h"
#include "SourceManager.h"

using ArraySubscriptExpr = clang::ArraySubscriptExpr;
using BinaryOperator = clang::BinaryOperator;
using BreakStmt = clang::BreakStmt;
using CallExpr = clang::CallExpr;
using CastExpr = clang::CastExpr;
using ContinueStmt = clang::ContinueStmt;
using CompoundStmt = clang::CompoundStmt;
using CXXOperatorCallExpr = clang::CXXOperatorCallExpr;
using CXXNewExpr = clang::CXXNewExpr;
using CXXDeleteExpr = clang::CXXDeleteExpr;
using CXXThrowExpr = clang::CXXThrowExpr;
using Decl = clang::Decl;
using DeclGroupRef = clang::DeclGroupRef;
using DeclRefExpr = clang::DeclRefExpr;
using DeclStmt = clang::DeclStmt;
using DoStmt = clang::DoStmt;
using Expr = clang::Expr;
using ForStmt = clang::ForStmt;
using FunctionDecl = clang::FunctionDecl;
using GotoStmt = clang::GotoStmt;
using IfStmt = clang::IfStmt;
using LabelDecl = clang::LabelDecl;
using LabelStmt = clang::LabelStmt;
using MemberExpr = clang::MemberExpr;
using NullStmt = clang::NullStmt;
using ParenExpr = clang::ParenExpr;
using ReturnStmt = clang::ReturnStmt;
using SourceRange = clang::SourceRange;
using SourceLocation = clang::SourceLocation;
using Stmt = clang::Stmt;
using SwitchCase = clang::SwitchCase;
using SwitchStmt = clang::SwitchStmt;
using UnaryOperator = clang::UnaryOperator;
using VarDecl = clang::VarDecl;
using WhileStmt = clang::WhileStmt;



void LocalReduction::Initialize(clang::ASTContext &Ctx) {
  Reduction::Initialize(Ctx);
  CollectionVisitor = new LocalElementCollectionVisitor(this);
}

bool LocalReduction::HandleTopLevelDecl(DeclGroupRef D) {
  clang::SourceManager &SM = Context->getSourceManager();
  for (DeclGroupRef::iterator I = D.begin(), E = D.end(); I != E; ++I) {
    if (SourceManager::IsInHeader(SM, *I))
      continue;
    CollectionVisitor->TraverseDecl(*I);
  }
  return true;
}

void LocalReduction::HandleTranslationUnit(clang::ASTContext &Ctx) {
  for (auto const &FD : Functions) {
    //spdlog::get("Logger")->info("Reduce Function {} at {}",
    //FD->getNameInfo().getAsString(), OptionManager::InputFile);
    
    HandleFunctionDecl(FD);
    //spdlog::get("Logger")->info("Finish Reducing Function {} at {}",
    //FD->getNameInfo().getAsString(), OptionManager::InputFile);
  }
}

void LocalReduction::HandleFunctionDecl(FunctionDecl *fd) {
  if (fd->isThisDeclarationADefinition() && fd->hasBody()) {
    RemoveIfDangling(fd->getBody());
  }
  TheRewriter.overwriteChangedFiles();
}

/* If the stmt is a dangling parent stmt (one of do, for, if, switch, while stmts with a side-effect-free condition and an empty body), remove it, and return true. Otherwise, keep it, and return false. */
bool LocalReduction::RemoveIfDangling(Stmt* stmt) {

  //============
  /*
  SourceLocation Begin = SourceManager::GetBeginOfStmt(Context, stmt);
  SourceLocation End = SourceManager::GetEndOfStmt(Context, stmt);
  clang::SourceManager &SM = Context->getSourceManager();
  llvm::StringRef SStr = SourceManager::GetSourceText(SM, Begin, End);
  spdlog::get("Logger")->info("Stmt Visited: {};", SStr.str());
  */
  //============
  
  if (CompoundStmt* cs = llvm::dyn_cast<CompoundStmt>(stmt)) {
    bool sef = true;
    CompoundStmt::const_body_iterator bi, be;
    for (bi=cs->body_begin(), be=cs->body_end(); bi!=be; bi++) {
      sef &= RemoveIfDangling(*bi);
    }
    return sef;
  }
  
  else if (DoStmt* ds = llvm::dyn_cast<DoStmt>(stmt)) {
    bool sef = IsSideEffectFree(ds->getCond());
    sef &= RemoveIfDangling(ds->getBody());
    if (sef) {
      SourceLocation BeginStmt = SourceManager::GetBeginOfStmt(Context, ds);
      SourceLocation EndStmt = SourceManager::GetEndOfStmt(Context, ds);
      removeSourceText(BeginStmt, EndStmt);
    }
    return sef;
  }
  
  else if (ForStmt* fs = llvm::dyn_cast<ForStmt>(stmt)) {
    bool sef = IsSideEffectFree(fs->getInit()); //E.g., "i=0" rather than "int i=0"
    sef &= IsSideEffectFree(fs->getCond());
    sef &= RemoveIfDangling(fs->getBody());
    if (sef) {
      SourceLocation BeginStmt = SourceManager::GetBeginOfStmt(Context, fs);
      SourceLocation EndStmt = SourceManager::GetEndOfStmt(Context, fs);
      removeSourceText(BeginStmt, EndStmt);
    }
    return sef;
  }

  else if (IfStmt* if_stmt = llvm::dyn_cast<IfStmt>(stmt)) {
    bool sef = IsSideEffectFree(if_stmt->getCond());
    //================
    /*
    Stmt* cond = if_stmt->getCond();
    SourceLocation BeginExpr = SourceManager::GetBeginOfStmt(Context, cond);
    SourceLocation EndExpr = SourceManager::GetEndOfStmt(Context, cond);
    clang::SourceManager &SM = Context->getSourceManager();
    llvm::StringRef SStr = SourceManager::GetSourceText(SM, BeginExpr, EndExpr);
    spdlog::get("Logger")->info("If Stmt Cond: {}; Is Side Effect Free? {}", SStr.str(), std::to_string(sef));
    */
    //================    
    sef &= RemoveIfDangling(if_stmt->getThen());
    if (if_stmt->hasElseStorage()) {
      sef &= RemoveIfDangling(if_stmt->getElse());
    }
    if (sef) {
      SourceLocation BeginStmt = SourceManager::GetBeginOfStmt(Context, if_stmt);
      SourceLocation EndStmt = SourceManager::GetEndOfStmt(Context, if_stmt);
      removeSourceText(BeginStmt, EndStmt);
    }
    return sef;
  }

  else if (LabelStmt* ls = llvm::dyn_cast<LabelStmt>(stmt)) {
    RemoveIfDangling(ls->getSubStmt());
    return false; //Shouldn't be removed, even with an empty body. Otherwise, a goto label cannot be directed (which leads to a compiling error).
  }
  
  else if (SwitchCase* sc = llvm::dyn_cast<SwitchCase>(stmt)) {
    RemoveIfDangling(sc->getSubStmt());
    return false; //Shouldn't be removed, even with an empty body. Otherwise, a switch cond will be directed to default (which can change program's original semantics).
  }
  
  else if (SwitchStmt* ss = llvm::dyn_cast<SwitchStmt>(stmt)) {
    bool sef = IsSideEffectFree(ss->getCond());
    sef &= RemoveIfDangling(ss->getBody());
    if (sef) {
      SourceLocation BeginStmt = SourceManager::GetBeginOfStmt(Context, ss);
      SourceLocation EndStmt = SourceManager::GetEndOfStmt(Context, ss);
      removeSourceText(BeginStmt, EndStmt);
    }
    return sef;
  }

  else if (WhileStmt* ws = llvm::dyn_cast<WhileStmt>(stmt)) {
    bool sef = IsSideEffectFree(ws->getCond());
    sef &= RemoveIfDangling(ws->getBody());
    if (sef) {
      SourceLocation BeginStmt = SourceManager::GetBeginOfStmt(Context, ws);
      SourceLocation EndStmt = SourceManager::GetEndOfStmt(Context, ws);
      removeSourceText(BeginStmt, EndStmt);
    }
    return sef;
  }

  else if (NullStmt* ns = llvm::dyn_cast<NullStmt>(stmt)) {
    return true;
  }

  else {
    return false; //Be conservative
  }
}

/* We use this to check whether a condition is side-effect free. */
bool LocalReduction::IsSideEffectFree(Stmt* stmt) {
  if (CallExpr* callexpr = llvm::dyn_cast<CallExpr>(stmt)) {
    return false; //We're currently conservative on this.
  }

  std::vector<Stmt *> children = getAllChildren(stmt);
  for (auto const &s : children) {
    if (BinaryOperator* bop = llvm::dyn_cast<BinaryOperator>(s)) {
      if (bop->isAssignmentOp()) {
	return false;
      }
    }

    else if (auto *OpCallExpr = llvm::dyn_cast<CXXOperatorCallExpr>(s)) {
      clang::OverloadedOperatorKind OpKind = OpCallExpr->getOperator();
      if (OpKind == clang::OO_Equal || OpKind == clang::OO_PlusEqual ||
	  OpKind == clang::OO_MinusEqual || OpKind == clang::OO_StarEqual ||
	  OpKind == clang::OO_SlashEqual || OpKind == clang::OO_AmpEqual ||
	  OpKind == clang::OO_PipeEqual || OpKind == clang::OO_CaretEqual ||
	  OpKind == clang::OO_LessLessEqual ||
	  OpKind == clang::OO_GreaterGreaterEqual ||
	  OpKind == clang::OO_PlusPlus || OpKind == clang::OO_MinusMinus ||
	  OpKind == clang::OO_PercentEqual || OpKind == clang::OO_New ||
	  OpKind == clang::OO_Delete || OpKind == clang::OO_Array_New ||
	  OpKind == clang::OO_Array_Delete) {
	return false;
      }
    }

    else if (UnaryOperator* uop = llvm::dyn_cast<UnaryOperator>(stmt)) {
      clang::UnaryOperator::Opcode OC = uop->getOpcode();
      if (OC == clang::UO_PostInc || OC == clang::UO_PostDec ||
	  OC == clang::UO_PreInc || OC == clang::UO_PreDec) {
	return false;
      }
    }

    else if (llvm::isa<CXXNewExpr>(stmt) || llvm::isa<CXXDeleteExpr>(stmt) || llvm::isa<CXXThrowExpr>(stmt)) {
      return false;
    }
  }

  return true;
}

bool LocalElementCollectionVisitor::VisitFunctionDecl(FunctionDecl *FD) {
  spdlog::get("Logger")->debug("Visit Function Decl: {}",
                               FD->getNameInfo().getAsString());
  if (FD->isThisDeclarationADefinition())
    Consumer->Functions.emplace_back(FD);
  return true;
}
