package moss.covpath;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class PathCoverage {
    public Map<String, FilePathCoverage> pcMap;

    public PathCoverage() {
        pcMap = new HashMap<String, FilePathCoverage>();
    }

    public PathCoverage(Map<String, FilePathCoverage> _pcMap) {
        this.pcMap = _pcMap;
    }

    public FilePathCoverage RequireCountInfo(String fileName) {
        return pcMap.get(fileName);
    }

    // Check if every line from pc is covered by this coverage
    // The method return true if the caller(this pathcoverage) contains callee(pc).
    public boolean coversByLines(PathCoverage pc) {
        for (Map.Entry<String, FilePathCoverage> fileEntry : pc.pcMap.entrySet()) {
            String filename = fileEntry.getKey();
            FilePathCoverage fileLcount = fileEntry.getValue();
            if (Arrays.equals(fileLcount.lcountCovered, this.pcMap.get(filename).lcountCovered)) {
                return false;
            }
            if (Arrays.equals(fileLcount.lcountAll, this.pcMap.get(filename).lcountAll)) {
                return false;
            }
        }
        return true;
    }

    // Check if every function from pc is covered by this coverage
    public boolean coversByFunctions(PathCoverage pc) {
        for (Map.Entry<String, FilePathCoverage> fileEntry : pc.pcMap.entrySet()) {
            String filename = fileEntry.getKey();
            FilePathCoverage fileFcount = fileEntry.getValue();
            if (Arrays.equals(fileFcount.fcountCovered, this.pcMap.get(filename).fcountCovered)) {
                return false;
            }
            if (Arrays.equals(fileFcount.fcountAll, this.pcMap.get(filename).fcountAll)) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, FilePathCoverage> fileEntry : pcMap.entrySet()) {
            stringBuilder.append("file: ").append(fileEntry.getKey()).append("\n");
            FilePathCoverage filePathCoverage = fileEntry.getValue();
            if (filePathCoverage.fcountAll.length > 0) {
                stringBuilder.append("fcount:\n");
                if (filePathCoverage.fcountCovered.length == 0) {
                    for (int lineNumber : filePathCoverage.fcountAll) {
                        stringBuilder.append(lineNumber).append(":0\n");
                    }
                }
                else {
                    for (int lineNumber : filePathCoverage.fcountAll) {
                        stringBuilder.append(lineNumber).append(":").append(filePathCoverage.IsCovered(lineNumber, false) ? "1" : "0").append("\n");
                    }
                }
            }
            if (filePathCoverage.lcountAll.length > 0) {
                stringBuilder.append("lcount:\n");
                if (filePathCoverage.lcountCovered.length == 0) {
                    for (int lineNumber : filePathCoverage.lcountAll) {
                        stringBuilder.append(lineNumber).append(":0\n");
                    }
                }
                else {
                    for (int lineNumber : filePathCoverage.lcountAll) {
                        stringBuilder.append(lineNumber).append(":").append(filePathCoverage.IsCovered(lineNumber, true) ? "1" : "0").append("\n");
                    }
                }
            }
        }
        return stringBuilder.toString();
    }
}
