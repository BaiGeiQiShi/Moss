package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import org.apache.commons.io.FileUtils;


public class PathCoverageGenerator
{
    public static PathCoverage getPathCoverage(File covf) {
        List<String> lines = null;
        try { lines = FileUtils.readLines(covf); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        if (lines == null) { return null; }

	String fname = null;
	if (lines.get(0).startsWith("file:")) {
	    fname = lines.get(0).substring("file:".length()).trim();
	}
	if (fname == null) {
	    System.err.println("Parsing error: file name is not shown in the first line.");
	    return null;
	}

	Map<Integer,Integer> fcmap = new HashMap<Integer,Integer>();
	Map<Integer,Integer> lcmap = new HashMap<Integer,Integer>();
	int lines_size = lines.size();
	for (int i=1; i<lines_size; i++) {
	    if (lines.get(i).startsWith("function:")) {
		String[] elems = lines.get(i).substring("function:".length()).split(",");
		fcmap.put(Integer.parseInt(elems[0]), Integer.parseInt(elems[1]));
	    }
	    else if (lines.get(i).startsWith("lcount:")) {
		String[] elems = lines.get(i).substring("lcount:".length()).split(",");
		lcmap.put(Integer.parseInt(elems[0]), Integer.parseInt(elems[1]));
	    }
	    else {
		System.err.println("Unrecgonized line: " + lines.get(i));
	    }
	}

	return new PathCoverage(fname, fcmap, lcmap);
    }


    //pcov is an arbitrary path coverage (for the target program)
    public static PathCoverage getPathCoverageWithZeroCounts(PathCoverage pcov) {
	Map<Integer, Integer> fcmap = new HashMap<Integer, Integer>();
	Map<Integer, Integer> lcmap = new HashMap<Integer, Integer>();
	Set<Integer> pcov_fset = pcov.getFunctionCountMap().keySet();
	Set<Integer> pcov_lset = pcov.getLineCountMap().keySet();
	for (Integer l : pcov_fset) { fcmap.put(l.intValue(), 0); }
	for (Integer l : pcov_lset) { lcmap.put(l.intValue(), 0); }
	return new PathCoverage(pcov.getFileName(), fcmap, lcmap);
    }

    //Generate a merged path coverage by summing the counts
    //Type: 0: binary; 1: real
    public static PathCoverage getMergedPathCoverage(List<PathCoverage> pcovs, int type) {
	String filename = null;
	Map<Integer, Integer> fcmap = new HashMap<Integer, Integer>();
	Map<Integer, Integer> lcmap = new HashMap<Integer, Integer>();

	for (PathCoverage pcov : pcovs) {
	    if (filename == null) { filename = pcov.getFileName(); }
	    else if (!filename.equals(pcov.getFileName())) {
		System.out.println("Inconsistent file names:\n"+filename+"\n"+pcov.getFileName());
		continue;
	    }

	    Map<Integer, Integer> pcov_fcmap = pcov.getFunctionCountMap();
	    Map<Integer, Integer> pcov_lcmap = pcov.getLineCountMap();

	    Iterator<Map.Entry<Integer, Integer>> fc_itr = pcov_fcmap.entrySet().iterator();
	    while (fc_itr.hasNext()) {
		Map.Entry<Integer, Integer> fc_entry = fc_itr.next();
		int fc_key = fc_entry.getKey().intValue();
		int fc_value = fc_entry.getValue().intValue();
		if (type == 0) { fc_value = (fc_value > 0) ? 1 : 0; } //Map to binary
		
		if (fcmap.get(fc_key) == null) {
		    fcmap.put(fc_key, fc_value);
		}
		else {
		    int curr_fc_value = fcmap.get(fc_key);
		    int new_fc_value = curr_fc_value + fc_value;
		    if (type == 0) { new_fc_value = (new_fc_value > 0) ? 1 : 0; } //Remap to binary
		    if (new_fc_value != curr_fc_value) { fcmap.put(fc_key, new_fc_value); }
		}
	    }

	    Iterator<Map.Entry<Integer, Integer>> lc_itr = pcov_lcmap.entrySet().iterator();
	    while (lc_itr.hasNext()) {
		Map.Entry<Integer, Integer> lc_entry = lc_itr.next();
		int lc_key = lc_entry.getKey().intValue();
		int lc_value = lc_entry.getValue().intValue();
		if (type == 0) { lc_value = (lc_value > 0) ? 1 : 0; } //Map to binary
		
		if (lcmap.get(lc_key) == null) {
		    lcmap.put(lc_key, lc_value);
		}
		else {
		    int curr_lc_value = lcmap.get(lc_key);
		    int new_lc_value = curr_lc_value + lc_value;
		    if (type == 0) { new_lc_value = (new_lc_value > 0) ? 1 : 0; } //Remap to binary
		    if (new_lc_value != curr_lc_value) { lcmap.put(lc_key, new_lc_value); }
		}
	    }
	    
	}

	return new PathCoverage(filename, fcmap, lcmap);
    }    
}
