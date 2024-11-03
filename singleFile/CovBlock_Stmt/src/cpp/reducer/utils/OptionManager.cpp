#include "OptionManager.h"
#include <iostream>
#include <fstream>
#include <getopt.h>
#include <string>
#include <chrono>

#include "llvm/Support/FileSystem.h"
#include "llvm/Support/Program.h"
#include "llvm/Support/raw_ostream.h"

#include "FileManager.h"


const std::string usage_simple("Usage: debop [OPTIONS]... ORACLE PROGRAM");
const std::string error_message("Try 'debop --help' for more information.");

void OptionManager::showUsage() {
  llvm::errs()
      << usage_simple << "\n"
      << "Options:"
      << "\n"
      << "  --help                 Show this help message\n"
      << "  --output_dir OUTDIR    Output directory\n"
      << "  --build                Integrate Debop with build system\n"
      << "  --save_temp            Save intermediate results\n"
      << "  --skip_learning        Disable decision tree learning\n"
      << "  --skip_delay_learning  Learn a new model for every iteration\n"
      << "  --skip_global          Skip global-level reduction\n"
      << "  --skip_local           Skip function-level reduction\n"
      << "  --no_cache             Do not cache intermediate results\n"
      << "  --skip_local_dep       Disable local dependency checking\n"
      << "  --skip_global_dep      Disable global dependency checking\n"
      << "  --skip_dce             Do not perform static unreachability "
         "analysis\n"
      << "  --no_profile           Do not print profiling report\n"
      << "  --debug                Print debug information\n"
      << "  --stat                 Count the number of statements\n"
      << "  --max_samples MAX_SAMPLES   The max number of samples generated\n"
      << "  --max_iters MAX_ITERS   The max number of sampling iterations\n"
      << "  --alpha ALPHA   The weight (b/w 0 and 1 inclusive) of AR-Score (attack surface reduction score). The weight of SR-Score (size reduction score) is (1-ALPHA). R-Score is ((1-ALPHA)*SR-Score + ALPHA*AR-Score)\n"
      << "  --beta BETA   The weight (b/w 0 and 1 inclusive) of G-Score. The weight of R-Score is (1-BETA). O-Score is ((1-BETA)*R-Score + BETA*G-Score)\n"
      << "  --kvalue KVALUE The k-value for computing density value\n"
      << "  --genfactor GENFACTOR The discounting factor for computing generality\n"
      << "  --elem_select_prob The probability of selecting an element for mutation\n"
      << "  --basic_block          Use MCMC at the Basic Block level, otherwise at the statement level\n"
      << "  --dependency_file     Read the dependency graph file generated by Frama-c\n"
      << "  --coverage_file     Read the coverage_file generated by Cov\n"
      << "  --base_inputs     Inputs that must pass\n"
      << "  --temp_file     Record the reduced program generated by the first stage of Moss\n"
      << "  --cov_statement     Use MCMC at the statement level with unequal prob\n"
      << "  --timeout     Set the timeout of the debloating process\n";
}

static struct option long_options[] = {
    {"help", no_argument, 0, 'h'},
    {"output_dir", required_argument, 0, 't'},
    {"build", no_argument, 0, 'b'},
    {"save_temp", no_argument, 0, 's'},
    {"skip_learning", no_argument, 0, 'D'},
    {"skip_delay_learning", no_argument, 0, 'd'},
    {"skip_global", no_argument, 0, 'g'},
    {"skip_local", no_argument, 0, 'l'},
    {"no_cache", no_argument, 0, 'c'},
    {"skip_local_dep", no_argument, 0, 'L'},
    {"skip_global_dep", no_argument, 0, 'G'},
    {"skip_dce", no_argument, 0, 'C'},
    {"no_profile", no_argument, 0, 'p'},
    {"debug", no_argument, 0, 'v'},
    {"stat", no_argument, 0, 'S'},
    {"max_samples", required_argument, 0, 'm'},
    {"max_iters", required_argument, 0, 'i'},
    {"alpha", required_argument, 0, 'a'},
    {"beta", required_argument, 0, 'e'},
    {"kvalue", required_argument, 0, 'k'},
    {"genfactor", required_argument, 0, 'z'},
    {"elem_select_prob", required_argument, 0, 'j'},
    {"basic_block", no_argument, 0, 'B'},
    {"dependency_file", required_argument, 0, 'E'},
    {"coverage_file", required_argument, 0, 'F'},
    {"base_inputs", required_argument, 0, 'I'},
    {"temp_file", required_argument, 0, 'T'},
    {"cov_statement", required_argument, 0, 'M'},
    {"timeout", required_argument, 0, 'U'},
    {0, 0, 0, 0}};

static const char *optstring = "ho:t:sDdglcLGCpvSm:i:a:e:k:z:j:BE:F:I:T:M:U:";

std::string OptionManager::BinFile = "";
std::vector<std::string> OptionManager::InputFiles;
std::vector<std::string> OptionManager::BuildCmd;
std::vector<std::string> OptionManager::DependencyString;
std::vector<std::string> OptionManager::CoverageString;
std::vector<std::string> OptionManager::BaseInputsString;
std::vector<std::string> OptionManager::TempFileString;
std::vector<std::string> OptionManager::CovStatementString;
std::string OptionManager::InputFile = "";
std::string OptionManager::OracleFile = "";
std::string OptionManager::EvalResultFile = "eval_rslt.txt";
std::string OptionManager::OutputDir = "debop-out";
std::string OptionManager::DependencyFile = "none";
std::string OptionManager::CoverageFile = "none";
std::string OptionManager::CovStatement = "none";
std::string OptionManager::BaseInputs = "none";
std::string OptionManager::TempFile = "none";
bool OptionManager::Build = false;
bool OptionManager::SaveTemp = false;
bool OptionManager::SaveSample = false;
bool OptionManager::SkipLearning = false;
bool OptionManager::SkipDelayLearning = false;
bool OptionManager::SkipGlobal = false;
bool OptionManager::SkipLocal = false;
bool OptionManager::NoCache = false;
bool OptionManager::SkipGlobalDep = true; //default skip dependency check
bool OptionManager::SkipLocalDep = true; //default skip dependency check
bool OptionManager::SkipDCE = false;
bool OptionManager::Profile = true;
bool OptionManager::Debug = false;
bool OptionManager::Stat = false;
bool OptionManager::BasicBlock = false;
int OptionManager::MaxSamples = 5;
int OptionManager::MaxIters = 100;
float OptionManager::Alpha = 0.5;
float OptionManager::Beta = 0.5;
float OptionManager::K = 1;
float OptionManager::GenFactor = 1;
float OptionManager::ElemSelectProb = -1;
std::chrono::minutes OptionManager::Timeout = std::chrono::minutes(1);
bool OptionManager::UseTimeout = false;

void OptionManager::handleOptions(int argc, char *argv[]) {
  llvm::ErrorOr<std::string> Program = llvm::sys::findProgramByName(argv[0]);
  BinFile = FileManager::Readlink(Program.get());
  

  char c;
  while ((c = getopt_long(argc, argv, optstring, long_options, 0)) != -1) {
    switch (c) {
    case 'h':
      showUsage();
      exit(0);

    case 't':
      OptionManager::OutputDir = std::string(optarg);
      break;

    case 'b':
      Build = true;
      break;

    case 's':
      OptionManager::SaveTemp = true;
      OptionManager::SaveSample = true;
      break;

    case 'D':
      OptionManager::SkipLearning = true;
      break;

    case 'd':
      OptionManager::SkipDelayLearning = true;
      break;

    case 'g':
      OptionManager::SkipGlobal = true;
      break;

    case 'l':
      OptionManager::SkipLocal = true;
      break;

    case 'c':
      OptionManager::NoCache = true;
      break;

    case 'L':
      OptionManager::SkipLocalDep = true;
      break;

    case 'G':
      OptionManager::SkipGlobalDep = true;
      break;

    case 'C':
      OptionManager::SkipDCE = true;
      break;

    case 'p':
      OptionManager::Profile = false;
      break;

    case 'v':
      OptionManager::Debug = true;
      break;

    case 'S':
      OptionManager::Stat = true;
      break;

    case 'B':
      OptionManager::BasicBlock = true;
      break;

    case 'm':
      OptionManager::MaxSamples = std::stoi(optarg);
      break;

    case 'i':
      OptionManager::MaxIters = std::stoi(optarg);
      break;

    case 'a':
      OptionManager::Alpha = std::stof(optarg);
      break;
      
    case 'e':
      OptionManager::Beta = std::stof(optarg);
      break;

    case 'k':
      OptionManager::K = std::stof(optarg);
      break;

    case 'z':
      OptionManager::GenFactor = std::stof(optarg);
      break;
      
    case 'j':
      OptionManager::ElemSelectProb = std::stof(optarg);
      break;

    case 'E':
      OptionManager::DependencyFile = std::string(optarg);
      break;

    case 'F':
      OptionManager::CoverageFile = std::string(optarg);
      break;

    case 'I':
      OptionManager::BaseInputs = std::string(optarg);
      break;

    case 'T':
      OptionManager::TempFile = std::string(optarg);
      break;

    case 'M':
      OptionManager::CovStatement = std::string(optarg);
      break;

    case 'U':
      OptionManager::Timeout = std::chrono::minutes(std::stoi(optarg));
      break;

    default:
      llvm::errs() << "Invalid option.\n";
      llvm::errs() << usage_simple << "\n";
      llvm::errs() << error_message << "\n";
      exit(1);
    }
  }

  if (optind + 2 > argc && !OptionManager::Stat) {
    llvm::errs() << "debop: You must specify the oracle and input.\n";
    llvm::errs() << usage_simple << "\n";
    llvm::errs() << error_message << "\n";
    exit(1);
  } else if (optind + 1 > argc && OptionManager::Stat) {
    llvm::errs() << "debop: You must specify the input.\n";
    exit(1);
  }

  int i;
  if (Stat)
    i = optind;
  else
    i = optind + 1;

  if (Build) {
    // integrate Debop with build system
    // input files will be captured by IntegrationManager
    for (; i < argc; i++)
      BuildCmd.push_back(std::string(argv[i]));
  } else {
    for (; i < argc; i++) {
      std::string Input = std::string(argv[i]);
      if (!llvm::sys::fs::exists(Input)) {
        llvm::errs() << "The specified input file " << Input
                     << " does not exist.\n";
        exit(1);
      }
      InputFiles.push_back(Input);
    }
  }

  // Use the timeout
  if(OptionManager::Timeout != std::chrono::minutes(1)){
      OptionManager::UseTimeout = true;
  }
	
  // When Debop only computes statistics, it does not need to check the oracle
  if (OptionManager::Stat)
    return;

  // If use debop with basicblock graph
  if(OptionManager::BasicBlock){
      if((OptionManager::DependencyFile.compare("none") != 0) || (OptionManager::CoverageFile.compare("none") != 0) || (OptionManager::CovStatement.compare("none") != 0)){
          llvm::errs() << "You can use one granularity level at once! ";
          exit(1);
      }
  }

  // If use debop with dependency graph(Ignore empty line in DependencyFile)
  if (OptionManager::DependencyFile.compare("none") != 0){
      // If all exits dependency and basicblock and coverage
      if(OptionManager::CoverageFile.compare("none") != 0 || OptionManager::BasicBlock || (OptionManager::CovStatement.compare("none") != 0)){
          llvm::errs() << "You cannot use basic block level, coverage level and dependency level at once! ";
          exit(1);
      }else if(OptionManager::ElemSelectProb != -1){
          llvm::errs() << "You cannot use ElemSelectProb at dependency level! ";
          exit(1);
      }
      std::ifstream dependencyFile(OptionManager::DependencyFile,std::ios::in);
      std::string templine;
      if (!dependencyFile.is_open())
      {
          std::cout << "Failed to open the dependency file!" << std::endl;
	  exit(1);
      }
      while(getline(dependencyFile,templine)){
          if (!templine.empty())
          OptionManager::DependencyString.push_back(templine);
      }
      dependencyFile.close();
  }

  // If use debop with coverage file(Ignore empty line in CoverageFile)
  if (OptionManager::CoverageFile.compare("none") != 0){
      // If all exits dependency and basicblock and coverage
      if(OptionManager::DependencyFile.compare("none") != 0 || OptionManager::BasicBlock || (OptionManager::CovStatement.compare("none") != 0)){
          llvm::errs() << "You can use one granularity level at once! ";
          exit(1);
      }else if(OptionManager::ElemSelectProb != -1){
          llvm::errs() << "You cannot use ElemSelectProb at coverage level! ";
          exit(1);
      }
      std::ifstream coverageFile(OptionManager::CoverageFile,std::ios::in);
      std::string templine;
      if (!coverageFile.is_open())
      {
          std::cout << "Failed to open the coverage file!" << std::endl;
          exit(1);
      }
      while(getline(coverageFile,templine)){
          if (!templine.empty())
              OptionManager::CoverageString.push_back(templine);
      }
      coverageFile.close();
  }

    // If use debop with statement level and use file(Ignore empty line in CoverageFile)
    if (OptionManager::CovStatement.compare("none") != 0){
        if(OptionManager::DependencyFile.compare("none") != 0 || OptionManager::CoverageFile.compare("none") != 0 || OptionManager::BasicBlock){
            llvm::errs() << "You cannot use one granularity level at once! ";
            exit(1);
        }else if(OptionManager::ElemSelectProb != -1){
            llvm::errs() << "You cannot use ElemSelectProb at Statement with unequal prob level! ";
            exit(1);
        }

        std::ifstream covStatement(OptionManager::CovStatement,std::ios::in);
        std::string templine;
        if (!covStatement.is_open())
        {
            std::cout << "Failed to open the coverage file!" << std::endl;
            exit(1);
        }
        while(getline(covStatement,templine)){
            if (!templine.empty())
                OptionManager::CovStatementString.push_back(templine);
        }
        covStatement.close();
    }

  //If use base inputs(Ignore empty line in BaseInputs file)
  if (OptionManager::BaseInputs.compare("none") != 0){
      std::ifstream baseInputs(OptionManager::BaseInputs,std::ios::in);
      std::string templine;
      if (!baseInputs.is_open())
      {
          std::cout << "Failed to open the BaseInputs file!" << std::endl;
          exit(1);
      }
      while(getline(baseInputs,templine)){
          if (!templine.empty())
              OptionManager::BaseInputsString.push_back(templine);
      }
      baseInputs.close();
  }


  // If use the reduced program generated by TMCMC(domgad)
  if (OptionManager::TempFile.compare("none") != 0){
      std::ifstream tempFile(OptionManager::TempFile,std::ios::in);
      std::string templine;
      if (!tempFile.is_open())
      {
          std::cout << "Failed to open the TempFile file!" << std::endl;
          exit(1);
      }
      while(getline(tempFile,templine)){
          OptionManager::TempFileString.push_back(templine);
      }
      tempFile.close();
  }

  OptionManager::OracleFile = std::string(argv[optind]);

  if (!llvm::sys::fs::exists(OptionManager::OracleFile)) {
    llvm::errs() << "The specified oracle file " << OptionManager::OracleFile
                 << " does not exist.\n";
    exit(1);
  } else if (!llvm::sys::fs::can_execute(OptionManager::OracleFile)) {
    llvm::errs() << "The specified oracle file " << OptionManager::OracleFile
                 << " is not executable.\n";
    exit(1);
  } else if (llvm::sys::ExecuteAndWait(OptionManager::OracleFile,
                                       {OptionManager::OracleFile})) {
    llvm::errs() << "The specified oracle file " << OptionManager::OracleFile
                 << " cannot execute correctly.\n";
    exit(1);
  }

  llvm::SmallString<128> OutputDirVec(OutputDir);
  llvm::sys::fs::make_absolute(OutputDirVec);
  OutputDir = OutputDirVec.str();
}
