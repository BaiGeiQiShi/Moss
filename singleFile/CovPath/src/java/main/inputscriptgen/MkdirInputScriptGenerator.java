package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class MkdirInputScriptGenerator
{
    public static String getScriptString(int i, String input_arg_dpath, String input_file_dpath) {
	File argf = new File(input_arg_dpath+"/"+i);
	String argstr = null;
	try { argstr = FileUtils.readFileToString(argf, (String) null); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (argstr == null) {
	    System.err.println("Arg File Not Found for #" + i);
	    System.err.println("Failed to generate script file for #" + i);
	    return null;
	}
	argstr = argstr.trim();
	
	//Get ctg number
	File ctgf = new File(input_file_dpath+"/ctg"+i+".txt");
	String ctgstr = null;
	if (ctgf.exists()) {
	    try { ctgstr = FileUtils.readFileToString(ctgf, (String) null); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    ctgstr = ctgstr.trim();
	}
	
	//Initial script
	StringBuilder sb = new StringBuilder(InputScriptInitializer.getInputString());
	if ("11".equals(ctgstr) || "12".equals(ctgstr)) {
	    sb.append("mkdir " + i + "\n");
	}
	sb.append("timeout -k 9 ${TIMEOUT}s $BIN");
	
	//Add arg
	sb.append(" " + argstr);
	
	//Add file
	File runargf = new File(input_file_dpath+"/runarg"+i+".txt");
	String filestr = null;
	if (runargf.exists()) {
	    try { filestr = FileUtils.readFileToString(runargf, (String) null); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    filestr = filestr.trim();
	    
	    sb.append(" " + filestr); //NOTE: Don't use $INDIR here, as we're not going to mkdir in $INDIR
	}
	else {
	    //No need for files -- this should be always the case
	}
	
	sb.append(" &>$OUTDIR/o"+i);
	sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	
	//******************
	//Special oracle: printing file/dir permissions, number of components, & owners
	if (filestr != null) {
	    if ("1".equals(ctgstr) || "2".equals(ctgstr) ||
		"3".equals(ctgstr) || "11".equals(ctgstr)) {
		//Do nothing, error message is enough.
	    }
	    else {
		sb.append("\ninfo=`ls -ald "+filestr+" | tr -s ' ' | cut -d ' ' -f 1,2,3,4`"); //tr converts every multi-space into single-space
		sb.append("\necho ${info} >>$OUTDIR/o"+i);
	    }
	}
	//******************	    
	
	return sb.toString();
    }
}
