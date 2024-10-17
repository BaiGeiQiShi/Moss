package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class CoverageMerger
{
    public static void main(String[] args) {
	List<File> cover_fs = new ArrayList<File>();
	for (String arg : args) {
	    File cover_df = new File(arg);
	    if (cover_df.isFile()) { cover_fs.add(cover_df); }
	    else if (cover_df.isDirectory()) {
		File[] cfs = cover_df.listFiles();
		for (File cf : cfs) { cover_fs.add(cf);	}
	    }
	}
	String cover_str = getCoverageString(cover_fs);
	System.out.println(cover_str);
    }

    public static String getCoverageString(List<File> cover_fs) {
	Set<String> covered_elems = new HashSet<String>();
	for (File cover_f : cover_fs) {
	    List<String> cover_flines = null;
	    try { cover_flines = FileUtils.readLines(cover_f); }
	    catch (Throwable t) { System.err.println(); t.printStackTrace(); }
	    if (cover_flines != null) {
		for (String cover_fline : cover_flines) {
		    cover_fline = cover_fline.trim();
		    if (!cover_fline.equals("")) {
			covered_elems.add(cover_fline);
		    }
		}
	    }
	}

	StringBuilder sb = null;
	for (String covered_elem : covered_elems) {
	    if (sb == null) { sb = new StringBuilder(); }
	    else { sb.append("\n"); }
	    sb.append(covered_elem);
	}

	return (sb==null) ? "" : sb.toString();
    }
}
