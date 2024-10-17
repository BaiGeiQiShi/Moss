package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class ChownInputScriptGenerator
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
	
	//Initial script
	StringBuilder sb = new StringBuilder(InputScriptInitializer.getInputString());
	sb.append("timeout -k 9 ${TIMEOUT}s $BIN");
	
	//Add arg
	sb.append(" " + argstr);
	
	//Add file, if needed
	File runargf = new File(input_file_dpath+"/runarg"+i+".txt");
	String filestr = null;
	if (runargf.exists()) {
	    try { filestr = FileUtils.readFileToString(runargf, (String) null); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    filestr = filestr.trim();
	    
	    sb.append(" $INDIR/" + filestr);
	}
	else {
	    //No need for files
	}
	
	sb.append(" &>$OUTDIR/o"+i);
	sb.append("\necho \"$?\" >>$OUTDIR/o"+i);
	
	//******************
	//Special oracle: printing file/dir owners
	if (filestr != null) {
	    sb.append("\nown0=`ls -al $INDIR/"+filestr+" | tr -s ' ' | cut -d ' ' -f 3`"); //tr converts every multi-space into single-space
	    sb.append("\nown1=`ls -al $INDIR/"+filestr+" | tr -s ' ' | cut -d ' ' -f 4`");
	    sb.append("\necho \"${own0} ${own1}\" >>$OUTDIR/o"+i);
	}
	//******************	    
	    
	return sb.toString();
    }
}
