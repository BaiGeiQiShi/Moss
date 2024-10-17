package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class RmInputScriptGenerator
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
	sb.append("timeout -k 9 ${TIMEOUT}s");
	
	//Add echo (if needed) and file
	File runargf = new File(input_file_dpath+"/runarg"+i+".txt");
	String filestr = null;
	if (runargf.exists()) {
	    try { filestr = FileUtils.readFileToString(runargf, (String) null); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    filestr = filestr.trim();
	    
	    if ("4".equals(ctgstr)) {
		sb.append(" bash -c \"echo Y | $BIN "+argstr+" $INDIR/"+filestr+"\"");
	    }
	    else if ("5".equals(ctgstr)) {
		sb.append(" bash -c \"echo N | $BIN "+argstr+" $INDIR/"+filestr+"\"");
	    }
	    else {
		sb.append(" $BIN "+argstr+" $INDIR/"+filestr);
	    }
	}
	else {
	    //No need for files
	}
	
	sb.append(" &>$OUTDIR/o"+i);
	sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	
	//******************
	//Special oracle: check existance of $INDIR/filestr, both as a file and as a dir.
	if (filestr != null) {
	    sb.append("\ntest -f $INDIR/"+filestr);
	    sb.append("\necho \"$?\" >>$OUTDIR/o"+i);
	    sb.append("\ntest -d $INDIR/"+filestr);
	    sb.append("\necho \"$?\" >>$OUTDIR/o"+i);
	}
	//******************	    
	
	return sb.toString();
    }
}
