#ifndef OPTION_MANAGER_H
#define OPTION_MANAGER_H

#include <string>
#include <vector>
#include <nlohmann/json.hpp>

#include <spdlog/spdlog.h>

using json = nlohmann::json;

/// \brief Manages and processes all the command-line options that are passed to Chisel
class OptionManager {
public:
  static std::string BinFile;
  static std::string Suffix;// didn't use suffix. Should be used in FileManager
  static std::vector<std::string> InputFiles;
  static std::vector<std::string> BuildCmd;
  static std::vector<std::string> ProgramList;
  static std::string ProgramListFile;
  static std::string InputFile;
  static std::string OracleFile;
  static std::string EvalResultFile;
  static std::string OutputDir;
  static std::string DependencyFile;
  static std::string CoverageFile;
  static std::string CovStatement;
  static std::string BaseInputs;

  /// @brief Compare Input Origin Files with Tempfile(which is generally an already reduced file), and assign stmts' existence according to whether it is reduced or not in Tempfile. 
  /// Attention: One tempfile should only be used in One project, in case of ambiguity.
  /// a tempfile could be like:
  /// ```tempfile.json
  /// {
  ///   "/path/to/source1.c":"/path/to/source1.c.generated_by_another_phase.c",
  ///   "/path/to/source2.c":"/path/to/source2.c.generated_by_another_phase.c"
  /// }
  /// ```
  static std::string TempFile;
  static json Temps;
  
  static std::vector<std::string> DependencyString;
  static json CoverageString;
  static json CovStatementString;
  static std::vector<std::string> BaseInputsString;
  static std::vector<int> BaseTestID;
  
  //SelectingStrategies { 
  //  Statement:0, 
  //  CovStatement:1,
  //  CoverageFile:2, 
  //  BasicBlock:3, 
  //  DependencyFile:4 
  // }
  static int SelectingStrategy;
  static bool Build;
  static bool SaveTemp;
  static bool SaveSample;
  static bool SkipLearning;
  static bool SkipDelayLearning;
  static bool SkipGlobal;
  static bool SkipLocal;
  static bool NoCache;
  static bool SkipGlobalDep;
  static bool SkipLocalDep;
  static bool SkipDCE;
  static bool Profile;
  static bool Debug;
  static bool Stat;
  static bool BasicBlock;
  static int MaxSamples;
  static int MaxIters;
  static float Alpha;
  static float Beta;
  static float K;
  static float GenFactor;
  static float ElemSelectProb;
  
  static void showUsage();
  static void handleOptions(int argc, char *argv[]);
  static void PrintOptions();
};

#endif // OPTION_MANAGER_H
