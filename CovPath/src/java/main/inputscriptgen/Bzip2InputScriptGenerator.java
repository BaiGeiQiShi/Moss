package moss.covpath;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class Bzip2InputScriptGenerator
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
	sb.append("{ timeout -k 9 ${TIMEOUT}s $BIN");
	
	//Add arg
	sb.append(" " + argstr);
	
	//Add file, if needed
	String filestr = null;
	File runargf = new File(input_file_dpath+"/runarg"+i+".txt");
	if (runargf.exists()) {
	    try { filestr = FileUtils.readFileToString(runargf, (String) null); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    filestr = filestr.trim();
	    
	    if ("-c".equals(argstr)) {
		sb.append(" <$INDIR/" + filestr + " >rslt.bz2");
	    }
	    else if ("-d".equals(argstr)) {
		sb.append(" <$INDIR/" + filestr + " >rslt");
	    }
	    else {
		sb.append(" $INDIR/" + filestr);
	    }
	}
	
	sb.append("; } &>$OUTDIR/o"+i);
	sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	
	//******************
	if ("-c".equals(argstr)) {
	    sb.append("\nbzip2 -d <rslt.bz2 >rslt");
	    sb.append("\ncat rslt &>>$OUTDIR/o"+i);
	}
	else if ("-d".equals(argstr)) {
	    sb.append("\ncat rslt &>>$OUTDIR/o"+i);
	}
	else if ("-f".equals(argstr) || "-k".equals(argstr)) {
	    sb.append("\ncp $INDIR/"+filestr+".bz2 ./"); //$INDIR/filestr.bz2 is what's generated
	    sb.append("\nbzip2 -d <"+filestr+".bz2 >rslt");
	    sb.append("\ncat rslt &>>$OUTDIR/o"+i);
	}
	//******************	    

	return sb.toString();
    }
}
