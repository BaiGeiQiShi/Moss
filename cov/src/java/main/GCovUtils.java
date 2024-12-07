package cov;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class GCovUtils
{
    /* Note that this map does not include non-code lines (led by "-") determined by gcov. */
    public static Map<Integer,Long> getLineCountMap(List<String> gcov_lines) {
	Map<Integer,Long> lcmap = new HashMap<Integer,Long>();
	for (String gcov_line : gcov_lines) {
	    gcov_line = gcov_line.trim();
            if (gcov_line.startsWith("lcount:")) {
		String[] elems = gcov_line.substring(gcov_line.indexOf(":")+1).split(",");
		lcmap.put(Integer.parseInt(elems[0]), Long.parseLong(elems[1]));
	    }
	}
	return lcmap;
    }

    public static Map<Integer,Long> getFunctionLineCountMap(List<String> gcov_lines) {
	Map<Integer,Long> flcmap = new HashMap<Integer,Long>();
	for (String gcov_line : gcov_lines) {
	    gcov_line = gcov_line.trim();
            if (gcov_line.startsWith("function:")) {
		String[] elems = gcov_line.substring(gcov_line.indexOf(":")+1).split(",");
		flcmap.put(Integer.parseInt(elems[0]), Long.parseLong(elems[1]));
	    }
	}
	return flcmap;
    }

    public static Map<Integer,String> getFunctionLineNameMap(List<String> gcov_lines) {
	Map<Integer,String> flnmap = new HashMap<Integer,String>();
	for (String gcov_line : gcov_lines) {
	    gcov_line = gcov_line.trim();
            if (gcov_line.startsWith("function:")) {
		String[] elems = gcov_line.substring(gcov_line.indexOf(":")+1).split(",");
		if (elems[2].contains(":")) {
		    //Eliminate the leading file name
		    flnmap.put(Integer.parseInt(elems[0]), elems[2].substring(elems[2].indexOf(":")+1));
		}
		else {
		    flnmap.put(Integer.parseInt(elems[0]), elems[2]);
		}
	    }
	}
	return flnmap;
    }

    // One can use this method to obtain all the starting lines for functions having the same name
    public static Map<String,List<Integer>> getFunctionNameLinesMap(List<String> gcov_lines) {
	Map<String,List<Integer>> fnlsmap = new HashMap<String,List<Integer>>();
	for (String gcov_line : gcov_lines) {
	    gcov_line = gcov_line.trim();
            if (gcov_line.startsWith("function:")) {
		String[] elems = gcov_line.substring(gcov_line.indexOf(":")+1).split(",");
		String funcname = null;
		if (elems[2].contains(":")) {
		    funcname = elems[2].substring(elems[2].indexOf(":")+1);
		}
		else {
		    funcname = elems[2];
		}

		if (fnlsmap.get(funcname) == null) {
		    List<Integer> slns = new ArrayList<Integer>();
		    slns.add(Integer.parseInt(elems[0]));
		    fnlsmap.put(funcname, slns);
		}
		else {
		    fnlsmap.get(funcname).add(Integer.parseInt(elems[0]));
		}
	    }
	}
	return fnlsmap;
    }

    public static Map<Integer,String> getInconsistentFunctionLineNameMap(List<String> gcov_lines) {
	Map<Integer,String> flnmap = new HashMap<Integer,String>();
	for (String gcov_line : gcov_lines) {
	    gcov_line = gcov_line.trim();
	    if (gcov_line.startsWith("function:")) {
		String[] elems = gcov_line.split(",");
		if (elems.length == 4 && elems[3].equals("inconsistent")) {
		    String funcname = null;
		    if (elems[2].contains(":")) {
			funcname = elems[2].substring(elems[2].indexOf(":")+1); }
		    else { funcname = elems[2]; }
		    flnmap.put(Integer.parseInt(elems[0].substring("function:".length())), funcname);
		}
	    }
	}
	return flnmap;
    }
}
