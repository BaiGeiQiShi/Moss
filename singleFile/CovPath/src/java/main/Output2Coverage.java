package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.io.File;
import org.apache.commons.io.FileUtils;

/* Take in an output file/dir (generated by running the instrumented program with inputs),
  generate coverage files (filter away non-trace text). */
public class Output2Coverage
{
    private static Pattern int_ptn = Pattern.compile("\\d+");
    
    public static void main(String[] args) {
	File output_df = new File(args[0]);
	File cover_d = new File(args[1]);

	if (output_df.isDirectory()) {
	    runOnDirByProgram(output_df, cover_d);
	}
	else if (output_df.isFile()) {
	    runOnFileByProgram(output_df, cover_d);
	}
	else {
	    System.err.println("Fisrt argument should be either a directory or a file.");
	}
    }

    public static void runOnDirByProgram(File output_d, File cover_d) {
	File[] output_fs = output_d.listFiles();
	for (File output_f : output_fs) {
	    runOnFileByProgram(output_f, cover_d);
	}
    }
    
    public static void runOnFileByProgram(File output_f, File cover_d) {
	String cover_dpath = cover_d.getPath();
	List<String> output_flines = null;
	try { output_flines = FileUtils.readLines(output_f); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (output_flines == null) { return; }
	
	//Write instru lines in sb
	int fline_i = 0;
	StringBuilder sb = null;
	Set<String> covered_flines = new HashSet<String>();
	for (String output_fline : output_flines) {
	    fline_i += 1;
	    output_fline = output_fline.trim();
	    int stmt_index = output_fline.indexOf("<STMTID>");
	    int branch_index = output_fline.indexOf("<BRANCHID>");
	    if (stmt_index != -1) {
		boolean valid_suffix = true; //Either as :StmtXXX or :BranchXXX (XXX as number)
		int suffix_index = output_fline.indexOf(":Stmt");
		if (suffix_index == -1) { valid_suffix = false; }
		else { valid_suffix = int_ptn.matcher(output_fline.substring(suffix_index+(":Stmt".length())).trim()).matches(); }
		
		if (valid_suffix) {
		    boolean not_covered = covered_flines.add(output_fline.substring(stmt_index));
		    if (not_covered) {
			if (sb == null) { sb = new StringBuilder(); }
			else { sb.append("\n"); }
			//================
			//System.err.println("***Output Fline***: " + output_fline);
			//================		    
			sb.append(output_fline.substring(stmt_index)); //the line may start with "xxx<STMTID>" where "xxx" is some program output (without newlines)
		    }
		}
		else {
		    System.err.println("From "+output_f.getPath()+", Invalid line (#"+fline_i+"): " + output_fline);
		}
	    }
	    else if (branch_index != -1) {
		boolean valid_suffix = true; //Either as :StmtXXX or :BranchXXX (XXX as number)
		int suffix_index = output_fline.indexOf(":Branch");
		if (suffix_index == -1) { valid_suffix = false; }
		else { valid_suffix = int_ptn.matcher(output_fline.substring(suffix_index+(":Branch".length())).trim()).matches(); }
		
		if (valid_suffix) {
		    boolean not_covered = covered_flines.add(output_fline.substring(stmt_index));
		    if (not_covered) {
			if (sb == null) { sb = new StringBuilder(); }
			else { sb.append("\n"); }
			sb.append(output_fline.substring(branch_index)); //the line may start with "xxx<BRANCHID>" where "xxx" is some program output (without newlines)
		    }
		}
		else {
		    System.err.println("From "+output_f.getPath()+", Invalid line (#"+fline_i+"): " + output_fline);
		}
	    }
	    else {
		int func_begin_index = output_fline.indexOf("<FUNC-BEGIN>");
		int func_end_index = output_fline.indexOf("<FUNC-END>");
		if (func_begin_index == -1 && func_end_index == -1) {
		    System.err.println("From "+output_f.getPath()+", Invalid line (#"+fline_i+"): " + output_fline);
		}
	    }
	}
	
	//Write sb to trace file
	if (sb != null) {
	    File cover_f = new File(cover_dpath+"/"+output_f.getName());
	    try { FileUtils.writeStringToFile(cover_f, sb.toString(), (String) null); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}
    }

}