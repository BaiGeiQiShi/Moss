#include "Profiler.h"

#include <iostream>
#include <fstream>
#include <spdlog/spdlog.h>

#include "llvm/Support/Timer.h"
#include "llvm/Support/Program.h"

/// info of last sample: <iter, sample, SR, AR, R, G, O, D scores, accept(1)/reject(0)>
std::tuple<int, int, double, double, double, double, double, double, bool> Profiler::last_sample_info = std::make_tuple(-1,-1,-1,-1,-1,-1,-1,-1,1);
/// info of current iteration: <iter, sample, SR, AR, R, G, O, D scores, accept(1)/reject(0)>
std::tuple<int, int, double, double, double, double, double, double, bool> Profiler::curr_iter_info = std::make_tuple(-1,-1,-1,-1,-1,-1,-1,-1,1);
/// info of best sample: <iter, sample, SR, AR, R, G, O, D scores, accept/reject=1>
std::tuple<int, int, double, double, double, double, double, double, bool> Profiler::best_sample_info = std::make_tuple(-1,-1,-1,-1,-1,-1,-1,-1,1);

Profiler *Profiler::Instance;

void Profiler::Initialize() {
  Instance = new Profiler();

  Instance->ChiselTimer.init("ChiselTimer", "");
  Instance->OracleTimer.init("OracleTimer", "");
  Instance->LearningTimer.init("LearningTimer", "");
}

Profiler *Profiler::GetInstance() {
  if (!Instance)
    Initialize();
  return Instance;
}

#pragma region heritage
void Profiler::Finalize() { delete Instance; }

void Profiler::incrementGlobalReductionCounter() { GlobalReductionCounter++; }

void Profiler::incrementSuccessfulGlobalReductionCounter() {
  SuccessfulGlobalReductionCounter++;
}

void Profiler::incrementLocalReductionCounter() { LocalReductionCounter++; }

void Profiler::incrementSuccessfulLocalReductionCounter() {
  SuccessfulLocalReductionCounter++;
}

void Profiler::setBestSampleId(int Id) {
  BestSampleId = Id;
}

int Profiler::getBestSampleId() {
  return std::get<1>(Profiler::best_sample_info);
}

void Profiler::setBestSizeRScore(float SRScore) {
  BestSRScore = SRScore;
}

float Profiler::getBestSizeRScore() {
  return std::get<2>(Profiler::best_sample_info);
}

void Profiler::setBestAttackSurfaceRScore(float ARScore) {
  BestARScore = ARScore;
}

float Profiler::getBestAttackSurfaceRScore() {
  return std::get<3>(Profiler::best_sample_info);
}

void Profiler::setBestRScore(float RScore) {
  BestRScore = RScore;
}

float Profiler::getBestRScore() {
  return std::get<4>(Profiler::best_sample_info);
}

void Profiler::setBestGScore(float GScore) {
  BestGScore = GScore;
}

float Profiler::getBestGScore() {
  return std::get<5>(Profiler::best_sample_info);
}

void Profiler::setBestOScore(float OScore) {
  BestOScore = OScore;
}

float Profiler::getBestOScore() {
  return std::get<6>(Profiler::best_sample_info);
}

void Profiler::beginChisel() {
  assert(ChiselTimer.isInitialized());
  ChiselTimer.startTimer();
}

void Profiler::endChisel() {
  assert(ChiselTimer.isRunning());
  ChiselTimer.stopTimer();
  ChiselTimeRecord += ChiselTimer.getTotalTime();
  ChiselTimer.clear();
}

void Profiler::beginOracle() {
  assert(OracleTimer.isInitialized());
  OracleTimer.startTimer();
}

void Profiler::endOracle() {
  assert(OracleTimer.isRunning());
  OracleTimer.stopTimer();
  OracleTimeRecord += OracleTimer.getTotalTime();
  OracleTimer.clear();
}

void Profiler::beginLearning() {
  assert(LearningTimer.isInitialized());
  LearningTimer.startTimer();
}

void Profiler::endLearning() {
  assert(LearningTimer.isRunning());
  LearningTimer.stopTimer();
  LearningTimeRecord += LearningTimer.getTotalTime();
  LearningTimer.clear();
}
#pragma endregion heritage



/// @brief evaluate the project, and update scores of last_sample_info. first two indices are managed in Main.cpp
/// If the project cannot compile or cannot pass the baseinput, 
/// iter and sample stay still, scores<-(-1, ...), accept/reject=reject(0) 
void Profiler::getEvalResult() {
  Profiler::GetInstance()->beginOracle();
  int Status = llvm::sys::ExecuteAndWait(OptionManager::OracleFile,
                                         {OptionManager::OracleFile});

  Profiler::GetInstance()->endOracle();
  if (Status != 0) {
    // std::get<0>(Profiler::curr_iter_info);   // Iter
    // std::get<1>(Profiler::curr_iter_info);   // Sample
    std::get<2>(Profiler::curr_iter_info)=-1.0; // SR
    std::get<3>(Profiler::curr_iter_info)=-1.0; // AR
    std::get<4>(Profiler::curr_iter_info)=-1.0; // R
    std::get<5>(Profiler::curr_iter_info)=-1.0; // G
    std::get<6>(Profiler::curr_iter_info)=-1.0; // O
    std::get<7>(Profiler::curr_iter_info)=-1.0; // D
    std::get<8>(Profiler::curr_iter_info)=false;// Reject
    spdlog::get("Logger")->info("Compiling Failure for Original Program. Abort");
    return;
  }

  #pragma region Read in the scores
  float* rslt = new float[6];
  rslt[0] = -1; rslt[1] = -1;
  rslt[2] = -1; rslt[3] = -1;
  rslt[4] = -1; rslt[5] = -1;
  std::ifstream infile(OptionManager::EvalResultFile);
  std::string Line0 = "-1", Line1 = "-1", Line2 = "-1", Line3 = "-1", Line4 = "-1", Line5 = "-1";
  if (infile.good())
  {
    for (int i = 0; i < 6; i++)
    {
      std::string line = "";
      if (std::getline(infile, line))
      {
        if (i == 0){Line0 = line;}
        else if (i == 1){Line1 = line;}
        else if (i == 2){Line2 = line;}
        else if (i == 3){Line3 = line;}
        else if (i == 4){Line4 = line;}
        else if (i == 5){Line5 = line;}
      }
      else{break;}
    }
  }
  infile.close();
  rslt[0] = std::stof(Line0);
  rslt[1] = std::stof(Line1);
  rslt[2] = std::stof(Line2);
  rslt[3] = std::stof(Line3);
  rslt[4] = std::stof(Line4);
  rslt[5] = std::stof(Line5);

  float curr_srscore = (rslt[0] == -1 || rslt[1] == -1) ? -1 :
    (rslt[0] - rslt[1]) / rslt[0];
  curr_srscore = curr_srscore<0? 0: curr_srscore;

  float curr_arscore = (rslt[2] == -1 || rslt[3] == -1) ? -1 :
    (rslt[2] - rslt[3]) / rslt[2];
  curr_arscore = curr_arscore < 0 ? 0 : curr_arscore;

  float curr_rscore = (curr_srscore == -1 || curr_arscore == -1) ? -1 :
    (1-OptionManager::Alpha)*curr_srscore + OptionManager::Alpha * curr_arscore;

  float curr_gscore = (rslt[4] == -1 || rslt[5] == -1) ? -1 :
    rslt[5] / rslt[4];

  if (curr_gscore != -1) { curr_gscore *= OptionManager::GenFactor; }

  float curr_oscore = (curr_rscore == -1 || curr_gscore == -1) ? -1 :
    (1-OptionManager::Beta) * curr_rscore + OptionManager::Beta * curr_gscore;

  float curr_dscore = (curr_oscore == -1) ? -1 :
    exp(OptionManager::K * curr_oscore);
  delete[] rslt;
  #pragma endregion

  #pragma region update current iteration information
  std::get<2>(Profiler::curr_iter_info)=curr_srscore; // SR
  std::get<3>(Profiler::curr_iter_info)=curr_arscore; // AR
  std::get<4>(Profiler::curr_iter_info)=curr_rscore; // R
  std::get<5>(Profiler::curr_iter_info)=curr_gscore; // G
  std::get<6>(Profiler::curr_iter_info)=curr_oscore; // O
  std::get<7>(Profiler::curr_iter_info)=curr_dscore; // D
  std::get<8>(Profiler::curr_iter_info)=true;// Reject
  #pragma endregion 
}

/// @brief Evaluate the original program
void Profiler::Init(){
	if(std::get<0>(best_sample_info) == -1 && std::get<6>(best_sample_info)==-1){//best sample is -1 and it's o-score is 0(initial mark, since it's generally impossible)
		Profiler::getEvalResult();

    Profiler::last_sample_info = Profiler::curr_iter_info;
    Profiler::best_sample_info = Profiler::curr_iter_info;
		spdlog::get("Logger")->info("");
		spdlog::get("Logger")->info("Initial SR-Score: {}; Initial AR-Score: {}; Initial R-Score: {}; Initial G-Score: {}; Initial O-Score: {}; Initial D-Score: {}",
									std::get<2>(Profiler::best_sample_info), std::get<3>(Profiler::best_sample_info), 
                  std::get<4>(Profiler::best_sample_info), std::get<5>(Profiler::best_sample_info), 
                  std::get<6>(Profiler::best_sample_info), std::get<7>(Profiler::best_sample_info));
    
	}
}

void Profiler::EvalReducedProject(){
  spdlog::get("Logger")->info("Current Best Sample Id: {}", std::get<1>(Profiler::best_sample_info));
  spdlog::get("Logger")->info("Current Best SR-Score: {}; AR-Score: {}; R-Score: {}; G-Score: {}; O-Score: {}; D-Score: {}",
                              std::get<2>(Profiler::best_sample_info), std::get<3>(Profiler::best_sample_info),
                              std::get<4>(Profiler::best_sample_info), std::get<5>(Profiler::best_sample_info),
                              std::get<6>(Profiler::best_sample_info), std::get<7>(Profiler::best_sample_info));

  Profiler::getEvalResult();
}