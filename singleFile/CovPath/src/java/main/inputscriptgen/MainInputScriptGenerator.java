package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class MainInputScriptGenerator
{
    public static void main(String[] args) {
	String progname = args[0];
	String input_dpath = args[1];
	String output_dpath = args[2];
	String target_ids_fpath = null;
	if (args.length > 3) { target_ids_fpath = args[3]; }
	String input_arg_dpath = input_dpath + "/arg";
	String input_file_dpath = input_dpath + "/file";

	//If target_ids_fpath is null after this, take all ids.
	Set<Integer> target_ids = null;
	if (target_ids_fpath != null) {
	    target_ids = getTargetIds(new File(target_ids_fpath));
	}

	for (int i=0; i<Integer.MAX_VALUE; i++) {
	    if (target_ids != null) { //If target_ids is null, then take every input.
		if (!target_ids.contains(i)) { continue; }
	    }
	    File argf = new File(input_arg_dpath+"/"+i);
            if (!argf.exists()) { break; }

	    String script_str = null;
	    if ("bzip2-1.0.5".equals(progname)) {
		script_str = Bzip2InputScriptGenerator.getScriptString(i, input_arg_dpath, input_file_dpath);
	    }
	    else if ("chown-8.2".equals(progname)) {
		script_str = ChownInputScriptGenerator.getScriptString(i, input_arg_dpath, input_file_dpath);
	    }
	    else if ("date-8.21".equals(progname)) {
		script_str = DateInputScriptGenerator.getScriptString(i, input_arg_dpath, input_file_dpath);
	    }
	    else if ("grep-2.19".equals(progname)) {
		script_str = GrepInputScriptGenerator.getScriptString(i, input_arg_dpath, input_file_dpath);
	    }
	    else if ("gzip-1.2.4".equals(progname)) {
		script_str = GzipInputScriptGenerator.getScriptString(i, input_arg_dpath, input_file_dpath);
	    }
	    else if ("mkdir-5.2.1".equals(progname)) {
		script_str = MkdirInputScriptGenerator.getScriptString(i, input_arg_dpath, input_file_dpath);
	    }
	    else if ("rm-8.4".equals(progname)) {
		script_str = RmInputScriptGenerator.getScriptString(i, input_arg_dpath, input_file_dpath);
	    }
	    else if ("sort-8.16".equals(progname)) {
		script_str = SortInputScriptGenerator.getScriptString(i, input_arg_dpath, input_file_dpath);
	    }
	    else if ("tar-1.14".equals(progname)) {
		script_str = TarInputScriptGenerator.getScriptString(i, input_arg_dpath, input_file_dpath);
	    }
	    else if ("uniq-8.16".equals(progname)) {
		script_str = UniqInputScriptGenerator.getScriptString(i, input_arg_dpath, input_file_dpath);
	    }

	    if (script_str != null) {
		File outputf = new File(output_dpath + "/test_" + i);
		try { FileUtils.writeStringToFile(outputf, script_str); }
		catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    }
	}
    }

    //All ids in one line, separated by comma.
    public static Set<Integer> getTargetIds(File target_ids_f) {
	String target_ids_str = null;
	try { target_ids_str = FileUtils.readFileToString(target_ids_f, (String) null); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (target_ids_str == null) { return new HashSet<Integer>(); }

	Set<Integer> target_ids = new HashSet<Integer>();
	String[] elems = target_ids_str.split(",");
	for (String elem : elems) {
	    target_ids.add(Integer.parseInt(elem.trim()));
	}
	return target_ids;
    }
}
