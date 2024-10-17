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

public class CoverageCheckForReducedCode
{
    public static void main(String[] args) {
	File reduced_codef = new File(args[0]);
	File original_codef = new File(args[1]);
	File gcovf = new File(args[2]);
	check(reduced_codef, original_codef, gcovf);
    }

    public static void check(File reduced_codef, File original_codef, File gcovf) {
	List<String> reduced_codef_lines = null;
	try { reduced_codef_lines = FileUtils.readLines(reduced_codef); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (reduced_codef_lines == null) { return; }

	List<String> original_codef_lines = null;
	try { original_codef_lines = FileUtils.readLines(original_codef); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (original_codef_lines == null) { return; }
	
	List<String> gcovf_lines = null;
	try { gcovf_lines = FileUtils.readLines(gcovf); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (gcovf_lines == null) { return; }

	for (String gcovf_line : gcovf_lines) {
	    if (gcovf_line.startsWith("lcount:")) {
		String[] elems = gcovf_line.substring("lcount:".length()).split(",");
		int lno = Integer.parseInt(elems[0]);
		int lc = Integer.parseInt(elems[1]);

		if (lc == 0) {
		    if ("".equals(reduced_codef_lines.get(lno-1).trim())) {}
		    else {
			System.out.println("Non-covered, why preserved? " + lno + ", " + reduced_codef_lines.get(lno-1));
		    }
		}
		else {
		    if ("".equals(reduced_codef_lines.get(lno-1).trim())) {
			if ("".equals(original_codef_lines.get(lno-1).trim())) {}
			else  {
			    System.out.println("Covered, why removed? " + lno + ", " + reduced_codef_lines.get(lno-1));
			}
		    }
		}
	    }
	}
    }
}
