#ifndef REDUCTION_HELPER_H
#define REDUCTION_HELPER_H

#include <string>
#include <vector>
#include <map>

/// @brief Store reduction information for Localreduction::HandleTranslationUnit
class ReductionHelper {
    private:
        std::vector<std::string> ReduceFiles;
        std::string CurrentReduceFile;
        std::map<std::string, std::vector<int>> ReduceStmts;
        std::vector<int> IterScores;
        int AcceptOrReject;
    public:
        static bool GetIterScores;
        static int GetAcceptOrReject;
        static std::vector<int> RandElements;

}

#endif // REDUCTION_HELPER_H