#ifndef PROFILER_H
#define PROFILER_H


#include "llvm/Support/Timer.h"
#include "OptionManager.h"

/// \brief Keeps track of performance information that is used in preparing the report
class Profiler {
public:
  static void Initialize();
  static Profiler *GetInstance();
  static void Finalize();

  /// I have to profiling the project about its size, attack surface and generality. So I add some features to `Profiler` class
  /// info of last sample: <iter, sample, SR, AR, R, G, O, D scores, accept(1)/reject(0)>
  static std::tuple<int, int, double, double, double, double, double, double, bool> last_sample_info; 
  /// info of current iteration: <iter, sample, SR, AR, R, G, O, D scores, accept(1)/reject(0)>
  static std::tuple<int, int, double, double, double, double, double, double, bool> curr_iter_info; 
  /// info of best sample: <iter, sample, SR, AR, R, G, O, D scores, accept/reject=1>
  static std::tuple<int, int, double, double, double, double, double, double, bool> best_sample_info; 

  static void Init();
  /// @brief evaluate the project, and give scores to last_sample_info and best_sample_info
  static void getEvalResult();
  static void EvalReducedProject();
  
  void incrementGlobalReductionCounter();
  void incrementSuccessfulGlobalReductionCounter();
  void incrementLocalReductionCounter();
  void incrementSuccessfulLocalReductionCounter();
  void setBestSampleId(int Id);
  int getBestSampleId();
  void setBestSizeRScore(float SRScore);
  float getBestSizeRScore();
  void setBestAttackSurfaceRScore(float ARScore);
  float getBestAttackSurfaceRScore();
  void setBestRScore(float RScore);
  float getBestRScore();
  void setBestGScore(float GScore);
  float getBestGScore();
  void setBestOScore(float OScore);
  float getBestOScore();

  int getGlobalReductionCounter() { return GlobalReductionCounter; }
  int getSuccessfulGlobalReductionCounter() {
    return SuccessfulGlobalReductionCounter;
  }
  int getLocalReductionCounter() { return LocalReductionCounter; }
  int getSuccessfulLocalReductionCounter() {
    return SuccessfulLocalReductionCounter;
  }

  llvm::Timer &getChiselTimer() { return ChiselTimer; }
  llvm::Timer &getLearningTimer() { return LearningTimer; }
  llvm::Timer &getOracleTimer() { return OracleTimer; }

  llvm::TimeRecord &getChiselTimeRecord() { return ChiselTimeRecord; }
  llvm::TimeRecord &getLearningTimeRecord() { return LearningTimeRecord; }
  llvm::TimeRecord &getOracleTimeRecord() { return OracleTimeRecord; }

  void beginChisel();
  void endChisel();

  void beginOracle();
  void endOracle();

  void beginLearning();
  void endLearning();

private:
  Profiler() {}
  ~Profiler() {}

  static Profiler *Instance;

  int GlobalReductionCounter = 0;
  int SuccessfulGlobalReductionCounter = 0;
  int LocalReductionCounter = 0;
  int SuccessfulLocalReductionCounter = 0;
  int BestSampleId = -1;
  float BestSRScore = -1;
  float BestARScore = -1;
  float BestRScore = -1;
  float BestGScore = -1;
  float BestOScore = -1;


  llvm::TimeRecord ChiselTimeRecord;
  llvm::TimeRecord LearningTimeRecord;
  llvm::TimeRecord OracleTimeRecord;

  llvm::Timer ChiselTimer;
  llvm::Timer LearningTimer;
  llvm::Timer OracleTimer;
};

#endif // PROFILER_H
