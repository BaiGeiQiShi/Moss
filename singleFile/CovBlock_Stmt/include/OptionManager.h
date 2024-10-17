#ifndef OPTION_MANAGER_H
#define OPTION_MANAGER_H

#include <string>
#include <vector>

/// \brief Manages and processes all the command-line options that are passed to Chisel
class OptionManager {
public:
  static std::string BinFile;
  static std::vector<std::string> InputFiles;
  static std::vector<std::string> BuildCmd;
  static std::string InputFile;
  static std::string OracleFile;
  static std::string EvalResultFile;
  static std::string OutputDir;
  static std::string DependencyFile;
  static std::string CoverageFile;
  static std::string CovStatement;
  static std::string BaseInputs;
  static std::string TempFile;
  static std::vector<std::string> TempFileString;
  static std::vector<std::string> DependencyString;
  static std::vector<std::string> CoverageString;
  static std::vector<std::string> CovStatementString;
  static std::vector<std::string> BaseInputsString;
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
};

#endif // OPTION_MANAGER_H
