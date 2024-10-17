package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class CoverageFilter
{
    public static void main(String[] args) {
	File cover_f = new File(args[0]);
	List<String> cover_flines = null;
	try { cover_flines = FileUtils.readLines(cover_f, (String) null); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (cover_flines == null) { return; }

	StringBuilder sb = null;
	for (String cover_fline : cover_flines) {
	    cover_fline = cover_fline.trim();
	    if (cover_fline.startsWith("<STMTID>")) { //Only consider <STMTID> lines
		if (sb == null) { sb = new StringBuilder(); }
		else { sb.append("\n"); }
		sb.append(cover_fline);
	    }
	}

	if (sb != null) {
	    System.out.println(sb.toString());
	}
    }
}
