package moss.covpath;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;

public class FilePathCoverage {
    public String fileName;
    public int[] lcountCovered;
    public int[] lcountAll;
    public int[] fcountCovered;
    public int[] fcountAll;

    public FilePathCoverage(String fileName) {
        this.fileName = fileName;
        this.lcountCovered = new int[0];
        this.lcountAll = new int[0];
        this.fcountCovered = new int[0];
        this.fcountAll = new int[0];
    }

    public FilePathCoverage(
        String fileName, 
        ArrayList<Integer> lCovered, ArrayList<Integer> lAll,
        ArrayList<Integer> fCovered, ArrayList<Integer> fAll
    ) {
        this.fileName = fileName;
        lCovered.sort(Comparator.naturalOrder());
        lAll.sort(Comparator.naturalOrder());
        fCovered.sort(Comparator.naturalOrder());
        fAll.sort(Comparator.naturalOrder());
        this.lcountCovered = lCovered.stream().mapToInt(Integer::valueOf).toArray();
        this.lcountAll = lAll.stream().mapToInt(Integer::valueOf).toArray();
        this.fcountCovered = fCovered.stream().mapToInt(Integer::valueOf).toArray();
        this.fcountAll = fAll.stream().mapToInt(Integer::valueOf).toArray();
    }

    public void SetLcount(ArrayList<Integer> lCovered, ArrayList<Integer> lAll) {
        lCovered.sort(Comparator.naturalOrder());
        lAll.sort(Comparator.naturalOrder());
        this.lcountCovered = lCovered.stream().mapToInt(Integer::valueOf).toArray();
        this.lcountAll = lAll.stream().mapToInt(Integer::valueOf).toArray();
    }

    public void SetFcount(ArrayList<Integer> fCovered, ArrayList<Integer> fAll) {
        fCovered.sort(Comparator.naturalOrder());
        fAll.sort(Comparator.naturalOrder());
        this.fcountCovered = fCovered.stream().mapToInt(Integer::valueOf).toArray();
        this.fcountAll = fAll.stream().mapToInt(Integer::valueOf).toArray();
    }

    public boolean IsCovered(int lineNumber, boolean isLcount) {
        int index =  Arrays.binarySearch(
            isLcount ? lcountCovered : fcountCovered,
            lineNumber
        );
        return index >= 0;
    } 
}

