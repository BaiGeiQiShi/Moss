package edu.gatech.cc.debaug;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class LineFileParser
{
    public static Map<Integer, Integer> getStartEndLineMap(File linef, String granu) {
	Map<Integer, Integer> lmap = new HashMap<Integer, Integer>();
	List<String> linef_lines = null;
	try { linef_lines = FileUtils.readLines(linef, (String) null); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (linef_lines == null) { return lmap; }

	for (String linef_line : linef_lines) {
	    linef_line = linef_line.trim();
	    String[] elems = null;
	    if (linef_line.startsWith(granu+":")) {
		elems = linef_line.substring((granu+":").length()).split(",");
	    }
	    else {
		continue;
	    }

	    int sl = Integer.parseInt(elems[0]);
	    int el = Integer.parseInt(elems[1]);
	    if (lmap.get(sl) == null) {
		lmap.put(sl, el);
	    }
	    else {
		//E.g., l0 has two ending lines l2 (compound) & l4 (if). We save l4 in the map.
		//if (c) { //l0
		//  s0;    //l1
		//} else { //l2
		//  s1;    //l3
		//}        //l4
		int el_curr = lmap.get(sl).intValue();
		if (el > el_curr) { lmap.put(sl, el); }
	    }
	}
	
	return lmap;
    }

    public static Map<Integer, String> getStartPropertyLineMap(File linef, String granu) {
	Map<Integer, String> lmap = new HashMap<Integer, String>();
	List<String> linef_lines = null;
        try { linef_lines = FileUtils.readLines(linef, (String) null); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        if (linef_lines == null) { return lmap; }

        for (String linef_line : linef_lines) {
            linef_line = linef_line.trim();
            String[] elems = null;
            if (linef_line.startsWith(granu+":")) {
                elems = linef_line.substring((granu+":").length()).split(",");
            }
            else {
                continue;
            }

	    int sl = Integer.parseInt(elems[0]);
	    if (elems.length > 2) {
		if (lmap.get(sl) == null) {
		    lmap.put(sl, elems[2]);
		}
	    }
	}

	return lmap;
    }

    //funtion name -> a string marking starting and ending lines (e.g., "100,150")
    public static Map<String, String> getFunctionNameStartEndLineMap(File linef) {
	Map<String, String> fmap = new HashMap<String, String>();
	List<String> linef_lines = null;
        try { linef_lines = FileUtils.readLines(linef, (String) null); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (linef_lines == null) { return fmap; }

	for (String linef_line : linef_lines) {
	    linef_line = linef_line.trim();
	    if (linef_line.startsWith("function:")) {
		String[] elems = linef_line.substring("function:".length()).split(",");
		fmap.put(elems[2], elems[0]+","+elems[1]);
	    }
	}
	return fmap;
    }
}
