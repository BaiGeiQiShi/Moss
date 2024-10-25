package moss.covpath;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class DateInputScriptGenerator
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
	
	File ctgf = new File(input_file_dpath+"/ctg"+i+".txt");
	String ctgstr = null;
	try { ctgstr = FileUtils.readFileToString(ctgf, (String) null); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	
	
	//For file encoded in arg, replace the leading directory with "$INDIR"
	String old_dpath = null;
	if (argstr.contains("--file=")) {
	    String[] elems = argstr.split(" ");
	    for (String elem : elems) {
		if (elem.startsWith("--file=")) {
		    String old_fpath = elem.substring("--file=".length());
		    old_dpath = old_fpath.substring(0, old_fpath.lastIndexOf("/"));
		    break;
		}
	    }
	}
	if (old_dpath != null) {
	    argstr = argstr.replace(old_dpath, "$INDIR");
	}
	
	//Initial script
	StringBuilder sb = new StringBuilder(InputScriptInitializer.getInputString());
	sb.append("timeout -k 9 ${TIMEOUT}s $BIN");
	
	//Add arg
	sb.append(" " + argstr);
	
	//Add file, if needed
	File runargf = new File(input_file_dpath+"/runarg"+i+".txt");
	if (runargf.exists()) {
	    String filestr = null;
	    try { filestr = FileUtils.readFileToString(runargf, (String) null); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    filestr = filestr.trim();
	    
	    sb.append(" $INDIR/" + filestr);
	}
	else {
	    //No need for files
	}
	
	sb.append(" &>tmp.txt");
	
	//******************
	sb.append("\n\nexit_val=$?");
	if (ctgstr != null && ("0".equals(ctgstr) || "68".equals(ctgstr))) { //noarg or -r
	    sb.append("\nwhile read -r line; do");
	    sb.append("\nspacenum=`echo ${line} | grep -o \" \" | wc -l`");
	    sb.append("\necho ${spacenum} >>$OUTDIR/o"+i);
	    sb.append("\ndone <tmp.txt");
	    sb.append("\necho \"${exit_val}\" >>$OUTDIR/o"+i);
	}
	else {
	    sb.append("\ncat tmp.txt &>$OUTDIR/o"+i);
	    sb.append("\necho \"${exit_val}\" >>$OUTDIR/o"+i);
	}
	//******************	    
	
	return sb.toString();
    }
}
