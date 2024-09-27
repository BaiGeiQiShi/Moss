#include <sstream>
#include <random>
#include <spdlog/spdlog.h>

#include "AST2Stmts.h"
#include "ASTsList.h"
#include "Frontend.h"
#include "ReadTempfile.h"

using json = nlohmann::json;

std::vector<std::tuple<std::string, int, std::string, std::vector<int>, int,
                       int, clang::Stmt *>>
    ASTsList::StmtIndexList =
        std::vector<std::tuple<std::string, int, std::string, std::vector<int>,
                               int, int, clang::Stmt *>>();
std::vector<double> ASTsList::StmtProbCoefList = std::vector<double>();
std::map<std::vector<int>, std::tuple<int, int, double>>
    ASTsList::CoverageBlocks =
        std::map<std::vector<int>, std::tuple<int, int, double>>();

int ASTsList::AddbackOrRemoveFlag = 1;

int ASTsList::SelectingElementIdx = -1;

// Function to check if two sets overlap
bool doSetsOverlap(const std::vector<int> &set1, const std::vector<int> &set2)
{
  for (int element : set1)
  {
    if (std::find(set2.begin(), set2.end(), element) != set2.end())
    {
      // Found an overlapping element
      return true;
    }
  }
  // No overlapping elements found
  return false;
}

/// @brief Parse AST to Stmts, and append to StmtIndexList.
void ASTsList::InitializeStmtsList()
{
  for (auto &File : OptionManager::InputFiles)
  {
    spdlog::get("Logger")->debug("- Get Statements For File: '{}'", File);
    Frontend::Parse(File, new AST2Stmts()); // will append stmts' informations to StmtIndexList
    if (OptionManager::TempFile.compare("none") != 0)
    {
      std::string tempfile = OptionManager::Temps[File];
      spdlog::get("Logger")->debug("- Sync {} with tempfile: {}", File,
                                   OptionManager::Temps[File]);
      Frontend::Parse(tempfile, new ReadTempfile());
    }
  }
}

/// @brief Compute the selecting probability Coefficient of each statement, and
/// build CoverageBlocks
///          based on SelectingStrategy and StmtIndexList.
///          A lower coeffcient means lower probability to be reduced, but
///          higher probability to be added back.
///          `coef = reduce prob = 1/add prob`
/// @param SelectingStrategy
///          @range: {
///              Statement:0,
///              CovStatement:1,
///              CovFile:2,
///              BasicBlock:3,
///              DependencyChain:4
///          }
///          - Statement: statement's selecting prob coefficient
///          is initialized as 1, which means every statement
///          is equally likely to be selected.
///          - CovStatement: statement's selecting prob coefficient
///          is calculated by the number of covered testcases.
///          - CovFile: statement's selecting prob coefficient is
///          calculated by the number of covered testcases and the
///          size of coverage block, which is the number of statements
///          that have the same covering pattern(cover the same testcases).
///          - BasicBlock: selecting prob coefficient is calculated by
///          the size of basicblock. A larger basicblock is more likely
///          to be selected.
///          - DependencyChain: selecting prob coefficient is calculated
///          by the size of dependency chain. A larger dependency chain
///          is more likely to be selected.
void ASTsList::InitializeStmtsProbCoef()
{
  int SelectingStrategy = OptionManager::SelectingStrategy;
  if (SelectingStrategy == 0)
  {
    ASTsList::StmtProbCoefList = std::vector<double>(StmtIndexList.size(), 1);

    if (OptionManager::BaseInputs.compare("none") != 0)
    {
      for (int i = 0; i != ASTsList::StmtIndexList.size(); i++)
      {
        if (doSetsOverlap(
                std::get<3>(ASTsList::StmtIndexList[i]), OptionManager::BaseTestID))
        {
          ASTsList::StmtProbCoefList[i] = 0;
        }
      }
    }
  }
  if (SelectingStrategy == 1)
  {
    ASTsList::StmtProbCoefList =
        std::vector<double>(ASTsList::StmtIndexList.size());

    // Iterate on each stmt index to update the probability coeffeciency of selecting
    for (int i = 0; i != ASTsList::StmtIndexList.size(); i++)
    {
      // if no covered tests, set coef = 0
      if (std::get<3>(ASTsList::StmtIndexList[i]).size() == 0)
      {
        ASTsList::StmtProbCoefList[i] = 0;
        spdlog::get("Logger")->debug("Prob {} = 0", i);
        spdlog::get("Logger")->warn(
            "\tNo coverage information for program element {}", i);
      }
      else
      {
        // if use baseinputs and cover base inputs, set coef = 0 (never select)
        if (OptionManager::BaseInputs.compare("none") != 0 and doSetsOverlap(
                                                                   std::get<3>(ASTsList::StmtIndexList[i]), OptionManager::BaseTestID))
        {
          ASTsList::StmtProbCoefList[i] = 0;
        }
        // Without BaseInputs, or do not cover baseinputs, then execute as normal coverage statement. If exist, then coef (to reduce) = 1/(covered test number)
        else
        {
          if (std::get<1>(ASTsList::StmtIndexList[i]) == 1)
          { // exist
            ASTsList::StmtProbCoefList[i] =
                1.0 / static_cast<double>(
                          std::get<3>(ASTsList::StmtIndexList[i]).size());
            spdlog::get("Logger")->debug(
                "Prob {} = 1 / {} = {}", i,
                std::get<3>(ASTsList::StmtIndexList[i]).size(),
                ASTsList::StmtProbCoefList[i]);
          }
          // if not exist, then coef (to add back) = (covered test number)
          else
          {
            spdlog::get("Logger")->debug(
                "Prob {} = {}", i,
                std::get<3>(ASTsList::StmtIndexList[i]).size());
            ASTsList::StmtProbCoefList[i] = static_cast<double>(
                std::get<3>(ASTsList::StmtIndexList[i]).size());
          }
        }
      }
    }
  }
  if (SelectingStrategy == 2)
  {
    // calculate the size of coverage block, from covered testcases of each
    // statement
    ASTsList::StmtProbCoefList =
        std::vector<double>(ASTsList::StmtIndexList.size());
    std::map<std::vector<int>, int>
        CoverageBlock; // <covered testcases vector -> size of coverage block>

    // Initialize CoverageBlock
    for (int i = 0; i != ASTsList::StmtIndexList.size(); i++)
    {
      std::vector<int> CoveredTestcases =
          std::get<3>(ASTsList::StmtIndexList[i]);
      // why c++ doesn't have `where` method
      if (CoverageBlock.find(CoveredTestcases) == CoverageBlock.end())
      {
        CoverageBlock[CoveredTestcases] = 1;
      }
      else
      {
        CoverageBlock[CoveredTestcases] += 1;
      }
    }

    // Calculate the selecting prob coefficient of each statement
    for (int i = 0; i != ASTsList::StmtIndexList.size(); i++)
    {
      std::vector<int> CoveredTestcases =
          std::get<3>(ASTsList::StmtIndexList[i]);

      // No coverage information
      if (CoveredTestcases.size() == 0)
      {
        ASTsList::StmtProbCoefList[i] = 0;
        spdlog::get("Logger")->debug("Prob {} = 0", i);
        spdlog::get("Logger")->warn(
            "\tNo coverage information for program element {}", i);
      }
      // BaseInput
      else if (OptionManager::BaseInputs.compare("none") != 0 and doSetsOverlap(
                                                                      std::get<3>(ASTsList::StmtIndexList[i]), OptionManager::BaseTestID))
      {
        ASTsList::StmtProbCoefList[i] = 0;
      }
      // Normal
      else
      {
        spdlog::get("Logger")->debug(
            "Prob {} = {} / {}", i, CoverageBlock[CoveredTestcases],
            std::get<3>(ASTsList::StmtIndexList[i]).size());
        ASTsList::StmtProbCoefList[i] =
            (float)CoverageBlock[CoveredTestcases] / CoveredTestcases.size();
        ASTsList::CoverageBlocks[CoveredTestcases] = std::make_tuple(
            1, CoveredTestcases.size(),
            ASTsList::StmtProbCoefList[i]); // (exist, didn't assign other
                                            // infos)
      }
    }

    // Update CoverageBlock Status
    for (auto &cb : ASTsList::CoverageBlocks)
    {
      // If no covered statements (or all covered statements are related to baseinputs), then current status is `removed`
      // Else, if at lease covers 1 statement, then current status is `exist`
      if (std::any_of(ASTsList::StmtIndexList.begin(), ASTsList::StmtIndexList.end(),
                      [&cb](std::tuple<llvm::StringRef, int, std::string, std::vector<int>, int, int, clang::Stmt *> st)
                      {
                        // any stmts of the coverageblock are not exist
                        if (std::get<3>(st) == cb.first)
                        {
                          return std::get<1>(st) == 0;
                        } // return 1 if stmt is of coverageblock, and not exist
                        else
                        {
                          return false;
                        } // return 0 if stmt is not of coverageblock or is of and exist
                      }))
      {
        std::get<0>(cb.second) = 0;
      } // else assign as 1(exist)
    }
  }
  if (SelectingStrategy == 3)
  {
    // calculate the size of basicblock, from basicblock id of each statement
    std::map<int, int> BasicBlock; // <basicblock id, size of basicblock>
    for (int i = 0; i != ASTsList::StmtIndexList.size(); i++)
    {
      int BasicBlockId = std::get<4>(ASTsList::StmtIndexList[i]);
      if (BasicBlock.find(BasicBlockId) == BasicBlock.end())
      {
        BasicBlock[BasicBlockId] = 1;
      }
      else
      {
        BasicBlock[BasicBlockId] += 1;
      }
    }
    // Calculate the selecting prob coefficient of each statement
    for (int i = 0; i != ASTsList::StmtIndexList.size(); i++)
    {
      int BasicBlockId = std::get<4>(ASTsList::StmtIndexList[i]);
      ASTsList::StmtProbCoefList[i] = BasicBlock[BasicBlockId];
    }
  }
  if (SelectingStrategy == 4)
  {
    // calculate the size of dependency chain, from dependency chain id of each
    // statement
    std::map<int, int>
        DependencyChain; // <dependency chain id, size of dependency chain>
    for (int i = 0; i != ASTsList::StmtIndexList.size(); i++)
    {
      int DependencyChainId = std::get<5>(ASTsList::StmtIndexList[i]);
      if (DependencyChain.find(DependencyChainId) == DependencyChain.end())
      {
        DependencyChain[DependencyChainId] = 1;
      }
      else
      {
        DependencyChain[DependencyChainId] += 1;
      }
    }
    // Calculate the selecting prob coefficient of each statement
    for (int i = 0; i != ASTsList::StmtIndexList.size(); i++)
    {
      int DependencyChainId = std::get<5>(ASTsList::StmtIndexList[i]);
      ASTsList::StmtProbCoefList[i] = DependencyChain[DependencyChainId];
    }
    /// TODO: To be honest, I don't know how to calculate the size of dependency
    /// chain.
  }
}

/**
 * @brief Parse OptionManager::InputFiles into ASTslist, and
 *       convert the ASTslist as a statement index list.
 */
void ASTsList::Initialize()
{
  InitializeStmtsList();
  InitializeStmtsProbCoef();

  if (OptionManager::Debug)
  {
    spdlog::get("Logger")->debug("--Statement Index List");
#pragma region Show collected statement infos.
    int idx = 0;
    for (auto st : ASTsList::StmtIndexList)
    { // st := statement tuple
      spdlog::get("Logger")->debug("---Element {0}: {1}\t", idx,
                                   std::get<0>(st));
      spdlog::get("Logger")->debug(
          "    selecting probability coefficienct: {0}", StmtProbCoefList[idx]);
      idx += 1;
    }
#pragma endregion

#pragma region Show collected coverage block infos
    if (OptionManager::SelectingStrategy == 2)
    {
      idx = 0;
      for (auto cb : ASTsList::CoverageBlocks)
      {
        spdlog::get("Logger")->debug("---Coverage block {}: {}\t", idx,
                                     fmt::join(cb.first, ","));
        spdlog::get("Logger")->debug(
            "    selecting probability coefficienct: {}",
            std::get<2>(cb.second));
        idx += 1;
      }
    }
#pragma endregion
  }
}

/// @brief Select Element index to be removed. Element's type are according to
/// selecting strategy
void ASTsList::SelectElemIdx()
{
  std::random_device rd;
  std::mt19937 RandomGenerator(rd());
  double random =
      std::uniform_real_distribution<double>(0.0, 1.0)(RandomGenerator);

  std::vector<int> flagindices;
  std::vector<double> weights;
  std::vector<double> cumulativeWeights;
  double sumofcoefs = 0;

  spdlog::get("Logger")->debug("-Select Element Index");
  spdlog::get("Logger")->debug("--Selecting Strategy is: {}",
                               OptionManager::SelectingStrategy);
#pragma region pack up for `flaged indices` and `weights`
  if (OptionManager::SelectingStrategy == 0 or
      OptionManager::SelectingStrategy == 1)
  {
    std::uniform_int_distribution<int> dist(0,
                                            ASTsList::StmtIndexList.size() - 1);
    ASTsList::AddbackOrRemoveFlag =
        std::get<1>(ASTsList::StmtIndexList[dist(RandomGenerator)]);
    spdlog::get("Logger")->debug("--Remove or Add-back Flag is {}",
                                 ASTsList::AddbackOrRemoveFlag);

    // pack up for `flaged indices` and `weights`
    spdlog::get("Logger")->debug(
        "--pack up for `flaged indices` and `weights`");
    int idx = 0;
    for (auto si : ASTsList::StmtIndexList)
    {
      spdlog::get("Logger")->debug("---Statement Index's ExistOrRemoved is {}",
                                   std::get<1>(si));
      if (std::get<1>(si) == ASTsList::AddbackOrRemoveFlag)
      {
        flagindices.emplace_back(idx);
        weights.emplace_back(ASTsList::StmtProbCoefList[idx]);
      }
      idx += 1;
    }
  }
  else if (OptionManager::SelectingStrategy == 2)
  {
    std::uniform_int_distribution<int> dist(0, ASTsList::CoverageBlocks.size() -
                                                   1);
    int index_AddbackOrRemoveFlag = dist(RandomGenerator);
    spdlog::get("Logger")->debug(
        "--Index for Remove or Add-back Flag Element is {}",
        index_AddbackOrRemoveFlag);
    ASTsList::AddbackOrRemoveFlag = std::get<0>(
        std::next(ASTsList::CoverageBlocks.begin(), index_AddbackOrRemoveFlag)
            ->second);
    spdlog::get("Logger")->debug("--Remove or Add-back Flag is {}",
                                 ASTsList::AddbackOrRemoveFlag);

    int idx = 0;
    for (auto cb : ASTsList::CoverageBlocks)
    {
      if (std::get<0>(cb.second) == ASTsList::AddbackOrRemoveFlag)
      {
        spdlog::get("Logger")->debug(
            "---[Selected] Coverage Block {}; coef: {}", idx,
            std::get<0>(cb.second), std::get<2>(cb.second));
        flagindices.emplace_back(idx);
        weights.emplace_back(std::get<2>(cb.second));
      }
      else
      {
        spdlog::get("Logger")->debug("---[Ignored] Coverage Block {};", idx,
                                     std::get<0>(cb.second));
      }
      idx += 1;
    }
  }
  else if (OptionManager::SelectingStrategy > 2)
  {
    throw std::logic_error(
        "Not implemented granularity for BasicBlock and DependencyChain");
  }
  if (OptionManager::Debug)
  {
    for (int idx = 0; idx != flagindices.size(); idx++)
    {
      spdlog::get("Logger")->trace("----flag index {}: At {} of all coefs;",
                                   idx, flagindices[idx]);
      spdlog::get("Logger")->trace("----\t Original coef is {}", weights[idx]);
    }
  }
#pragma endregion

  // if flag == ADD_BACK, then select elements according to the possibility of
  // adding-back, which is the reciprocal of coef
  if (ASTsList::AddbackOrRemoveFlag == 0)
  {
    int idx = 0;
    for (double &weight : weights)
    {
      if (weight == 0)
      {
        continue;
      }
      weight = 1 / weight;
      spdlog::get("Logger")->trace("----flag index {}: Flip to {}", idx,
                                   weight);
      idx += 1;
    }
  }
  // else{ Do NOT do anything. }

  spdlog::get("Logger")->debug("---weights: {}", fmt::join(weights, ","));
  sumofcoefs = std::accumulate(weights.begin(), weights.end(), 0.0);
  spdlog::get("Logger")->debug("---sum of coefs: {}", sumofcoefs);

  if (sumofcoefs == 0.0)
  {
    spdlog::get("Logger")->warn("**sum of coefs is 0. Cannot work.");
    exit(1);
  }

  int idx = 0;
  for (double &weight : weights)
  {
    weight /= sumofcoefs;
    spdlog::get("Logger")->trace("----flag index {}: normalize to {}", idx,
                                 weight);
    idx += 1;
  }

  cumulativeWeights = std::vector<double>(weights.size());
  std::partial_sum(weights.begin(), weights.end(), cumulativeWeights.begin());

  size_t selectedIndex = 0;
  /// There are some elements whose ProbCoef(weight)==0(caused by no coverage
  /// info. We remove these code by another tool `Cov`) These 0-probability
  /// elements will not be selected, since selectedIndex will only stop
  /// **first** element greater/equal(>=) with random
  spdlog::get("Logger")->debug("---Random Weight : {}", random);
  while (selectedIndex < cumulativeWeights.size() - 1 &&
         random > cumulativeWeights[selectedIndex])
  {
    spdlog::get("Logger")->debug("----Weight at index {}'s cumulative prob: {}",
                                 selectedIndex,
                                 cumulativeWeights[selectedIndex]);
    selectedIndex += 1;
  }

  spdlog::get("Logger")->debug("Selected Index {} of weights", selectedIndex);
  ASTsList::SelectingElementIdx = flagindices[selectedIndex];

  spdlog::get("Logger")->debug("Selected Index {} of all coefs",
                               ASTsList::SelectingElementIdx);
  return;
}
