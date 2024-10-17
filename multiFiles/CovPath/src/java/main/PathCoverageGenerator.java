package moss.covpath;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

public class PathCoverageGenerator {
    public static PathCoverage getPathCoverage(File covf) {
        Map<String, FilePathCoverage> pcMap = new HashMap<String, FilePathCoverage>();
        ArrayList<Integer> lineCovered = new ArrayList<Integer>();
        ArrayList<Integer> lineAll = new ArrayList<Integer>();
        ArrayList<Integer> funcCovered = new ArrayList<Integer>();
        ArrayList<Integer> funcAll = new ArrayList<Integer>();
        String currentfile = null;
        try (BufferedReader br = new BufferedReader(new FileReader(covf))){
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length < 2) {
                    continue;
                }
                String key = parts[0].trim();
                String[] values = parts[1].split(",");
                if (key.equals("file")) {
                    if (currentfile != null && pcMap.containsKey(currentfile)) {
                        FilePathCoverage filePathCoverage = pcMap.get(currentfile);
                        lineCovered.sort(Comparator.naturalOrder());
                        lineAll.sort(Comparator.naturalOrder());
                        funcCovered.sort(Comparator.naturalOrder());
                        funcAll.sort(Comparator.naturalOrder());
                        filePathCoverage.SetFcount(funcCovered, funcAll);
                        filePathCoverage.SetLcount(lineCovered, lineAll);
                        lineCovered.clear();
                        lineAll.clear();
                        funcCovered.clear();
                        funcAll.clear();
                    }
                    currentfile = values[0].trim();
                    if (currentfile.endsWith(".c")) {
                        pcMap.put(currentfile, new FilePathCoverage(currentfile));
                    }
                }
                else if (key.equals("function")) {
                    if (currentfile != null && pcMap.containsKey(currentfile)) {
                        Integer lineNumber = Integer.parseInt(values[0].trim());
                        boolean exist = Integer.parseInt(values[1].trim()) == 1;
                        funcAll.add(lineNumber);
                        if (exist) {
                            funcCovered.add(lineNumber);
                        }
                    }
                }
                else if (key.equals("lcount")) {
                    if (currentfile != null && pcMap.containsKey(currentfile)) {
                        Integer lineNumber = Integer.parseInt(values[0].trim());
                        boolean exist = Integer.parseInt(values[1].trim()) == 1;
                        lineAll.add(lineNumber);
                        if (exist) {
                            lineCovered.add(lineNumber);
                        }
                    }
                }
            }
            if (currentfile != null && pcMap.containsKey(currentfile)) {
                FilePathCoverage filePathCoverage = pcMap.get(currentfile);
                lineCovered.sort(Comparator.naturalOrder());
                lineAll.sort(Comparator.naturalOrder());
                funcCovered.sort(Comparator.naturalOrder());
                funcAll.sort(Comparator.naturalOrder());
                filePathCoverage.SetFcount(funcCovered, funcAll);
                filePathCoverage.SetLcount(lineCovered, lineAll);
                lineCovered.clear();
                lineAll.clear();
                funcCovered.clear();
                funcAll.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new PathCoverage(pcMap);
    }

    // pcov is an arbitrary path coverage (for the target program)
    public static PathCoverage getPathCoverageWithZeroCounts(PathCoverage pcov) {
        Map<String, FilePathCoverage> pcMapWithZeroCount = new HashMap<String, FilePathCoverage>();
        ArrayList<Integer> lineCovered = new ArrayList<Integer>();
        ArrayList<Integer> lineAll = new ArrayList<Integer>();
        ArrayList<Integer> funcCovered = new ArrayList<Integer>();
        ArrayList<Integer> funcAll = new ArrayList<Integer>();
        for (Map.Entry<String, FilePathCoverage> fileEntry : pcov.pcMap.entrySet()) {
            String fileName = fileEntry.getKey();
            FilePathCoverage filePathCoverage = fileEntry.getValue();
            lineAll = (ArrayList<Integer>)Arrays.stream(filePathCoverage.lcountAll).boxed().collect(Collectors.toList());
            funcAll = (ArrayList<Integer>)Arrays.stream(filePathCoverage.fcountAll).boxed().collect(Collectors.toList());
            FilePathCoverage newFilePathCoverage = new FilePathCoverage(
                fileName,
                lineCovered, lineAll,
                funcCovered, funcAll
            );
            pcMapWithZeroCount.put(fileName, newFilePathCoverage);
        }
        return new PathCoverage(pcMapWithZeroCount);
    }

    // Generate a merged path coverage by summing the counts
    // Type: 0: binary; 1: real
    public static PathCoverage getMergedPathCoverage(List<PathCoverage> pcovs, int type) {
        if (type != 0) {
            throw new UnsupportedOperationException("Functions not implemented when the value of the parameter type is equal to 1.");
        }
        Map<String, FilePathCoverageMerging> mergingPcMap = new HashMap<String, FilePathCoverageMerging>();                
        for (PathCoverage pcov : pcovs) {
            for (Map.Entry<String, FilePathCoverage> filEntry : pcov.pcMap.entrySet()) {
                String fileName = filEntry.getKey();
                FilePathCoverage filePathCoverage = filEntry.getValue();
                if (!mergingPcMap.containsKey(fileName)) {
                    mergingPcMap.put(fileName, new FilePathCoverageMerging(fileName));
                }
                FilePathCoverageMerging mergedFilePathCoverage = mergingPcMap.get(fileName);
                Set<Integer> lineCovered = (HashSet<Integer>)Arrays.stream(filePathCoverage.lcountCovered).boxed().collect(Collectors.toSet());
                Set<Integer> lineAll = (HashSet<Integer>)Arrays.stream(filePathCoverage.lcountAll).boxed().collect(Collectors.toSet());
                Set<Integer> funcCovered = (HashSet<Integer>)Arrays.stream(filePathCoverage.fcountCovered).boxed().collect(Collectors.toSet());
                Set<Integer> funcAll = (HashSet<Integer>)Arrays.stream(filePathCoverage.fcountAll).boxed().collect(Collectors.toSet());
                mergedFilePathCoverage.lcountCovered.addAll(lineCovered);
                mergedFilePathCoverage.lcountAll.addAll(lineAll);
                mergedFilePathCoverage.fcountCovered.addAll(funcCovered);
                mergedFilePathCoverage.fcountAll.addAll(funcAll);
            }
        }
        Map<String, FilePathCoverage> mergedPcMap = new HashMap<String, FilePathCoverage>();            
        for (Map.Entry<String, FilePathCoverageMerging> filEntry : mergingPcMap.entrySet()) {
            mergedPcMap.put(filEntry.getKey(), filEntry.getValue().ConvertToFilePathCoverage());
        }
        return new PathCoverage(mergedPcMap);
    }
}

class FilePathCoverageMerging {
    public String fileName;
    public Set<Integer> lcountCovered;
    public Set<Integer> lcountAll;
    public Set<Integer> fcountCovered;
    public Set<Integer> fcountAll;
    
    public FilePathCoverageMerging(String fileName) {
        this.fileName = fileName;
        this.lcountCovered = new HashSet<Integer>();
        this.lcountAll = new HashSet<Integer>();
        this.fcountCovered = new HashSet<Integer>();
        this.fcountAll = new HashSet<Integer>();
    }

    public FilePathCoverage ConvertToFilePathCoverage() {
        ArrayList<Integer> lineCovered = new ArrayList<Integer>(lcountCovered);
        ArrayList<Integer> lineAll = new ArrayList<Integer>(lcountAll);
        ArrayList<Integer> funcCovered = new ArrayList<Integer>(fcountCovered);
        ArrayList<Integer> funcAll = new ArrayList<Integer>(fcountAll);
        return new FilePathCoverage(
            fileName, 
            lineCovered, lineAll, 
            funcCovered, funcAll
        );
    }
}
