package moss.covpath;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class TarInputScriptGenerator
{
    public static String getScriptString(int i, String input_arg_dpath, String input_file_dpath) {
	File argf = new File(input_arg_dpath+"/"+i);
	//Obtain the category number
	int ctgn = -1;
	try {
	    String ctgnstr = FileUtils.readFileToString(new File(input_file_dpath+"/ctg"+i+".txt"), (String) null);
	    ctgn = Integer.parseInt(ctgnstr);
	}
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (ctgn == -1) {
	    System.err.println("Failed to find the category file ctg"+i+".txt");
	    System.err.println("Failed to generate script file for #" + i);
	    return null;
	}

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
	
	//Copy the input file (for oracle generation)
	if (ctgn == 0 || ctgn == 13 || ctgn == 19) {
	    sb.append("cp -r $INDIR/"+i+"_0 "+i+"_0");
	    sb.append("\ncp -r $INDIR/"+i+"_1 "+i+"_1");
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    sb.append(" $BIN " + argstr);
	    String argfstr = getArgfString(new File(input_file_dpath+"/runarg"+i+".txt"));
	    sb.append(" " + argfstr);
	    sb.append(" &>$OUTDIR/o"+i);
	    sb.append("\nrm "+i+"_0");
	    sb.append("\nrm "+i+"_1");
	    
	    //Untar and check rslt files
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	    sb.append("\ntar xf rslt.tar");
	    sb.append("\ncat "+i+"_0 &>>$OUTDIR/o"+i);
	    sb.append("\ncat "+i+"_1 &>>$OUTDIR/o"+i);
	}
	
	else if (ctgn == 1 || ctgn == 14) {
	    sb.append("cp -r $INDIR/" + i + ".tar " + i + ".tar");
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    sb.append(" $BIN " + argstr);
	    String argfstr = getArgfString(new File(input_file_dpath+"/runarg"+i+".txt"));
	    sb.append(" " + argfstr);
	    sb.append(" &>$OUTDIR/o"+i);
	    sb.append("\nrm "+i+".tar");
	    
	    //Check rslt files
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	    sb.append("\ncat "+i+"_0 &>>$OUTDIR/o"+i);
	    sb.append("\ncat "+i+"_1 &>>$OUTDIR/o"+i);
	}
	
	else if (ctgn == 2 || ctgn == 9 || ctgn == 11) {
	    sb.append("cp -r $INDIR/" + i + " " + i);
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    sb.append(" $BIN " + argstr);
	    String argfstr = getArgfString(new File(input_file_dpath+"/runarg"+i+".txt"));
	    sb.append(" " + argfstr);
	    sb.append(" &>$OUTDIR/o"+i);
	    sb.append("\nrm "+i);
	    
	    //Untar and check rslt files
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	    sb.append("\ntar xf rslt.tar");
	    sb.append("\ncat "+i+" &>>$OUTDIR/o"+i);
	}
	
	else if (ctgn == 3) {
	    sb.append("cp -r $INDIR/" + i + ".tar " + i + ".tar");
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    sb.append(" $BIN " + argstr);
	    String argfstr = getArgfString(new File(input_file_dpath+"/runarg"+i+".txt"));
	    sb.append(" " + argfstr);
	    sb.append(" &>$OUTDIR/o"+i);
	    sb.append("\nrm "+i+".tar");
	    
	    //Erroneous operation, check exit value
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	}
	
	else if (ctgn == 4 || ctgn == 5) {
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    sb.append(" $BIN " + argstr);
	    sb.append(" &>$OUTDIR/o"+i);
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	}
	
	else if (ctgn == 6) { //i is a dir
	    sb.append("cp -r $INDIR/" + i + " " + i);
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    sb.append(" $BIN " + argstr);
	    String argfstr = getArgfString(new File(input_file_dpath+"/runarg"+i+".txt"));
	    sb.append(" " + argfstr);
	    sb.append(" &>$OUTDIR/o"+i);
	    sb.append("\nrm -fr "+i+"/*");
	    
	    //Check rslting file
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	    sb.append("\ntar xf rslt.tar -C "+i+" ./1/10"); //Chisel's oracle
	    sb.append("\nfind "+i+" | sort >>$OUTDIR/o"+i);
	}
	
	else if (ctgn == 7 || ctgn == 8) {
	    sb.append("cp -r $INDIR/" + i + ".tar " + i + ".tar");
	    sb.append("\nmkdir "+i); //Need this as tar's output dir!
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    sb.append(" $BIN " + argstr);
	    String argfstr = getArgfString(new File(input_file_dpath+"/runarg"+i+".txt"));
	    sb.append(" " + argfstr);
	    sb.append(" &>$OUTDIR/o"+i);
	    sb.append("\nrm "+i+".tar");
	    
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	    sb.append("\nfind "+i+" | sort >>$OUTDIR/o"+i);
	}
	
	else if (ctgn == 10) {
	    sb.append("cp -r $INDIR/" + i + ".tar " + i + ".tar");
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    sb.append(" $BIN " + argstr);
	    String argfstr = getArgfString(new File(input_file_dpath+"/runarg"+i+".txt"));
	    sb.append(" " + argfstr);
	    sb.append(" &>$OUTDIR/o"+i);
	    sb.append("\nrm "+i+".tar");
	    
	    //Untar and check rslt files
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	    sb.append("\ncat "+i+" &>>$OUTDIR/o"+i);
	}
	
	else if (ctgn == 12) {
	    sb.append("cp -r $INDIR/" + i + ".tar " + i + ".tar");
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    String catargfstr = getArgfString(new File(input_file_dpath+"/catarg"+i+".txt"));
	    sb.append(" bash -c \"cat "+catargfstr+" | $BIN "+argstr+"\"");
	    sb.append(" &>$OUTDIR/o"+i);
	    sb.append("\nrm "+i+".tar");
	    
	    //Check rslt files
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	    sb.append("\ncat "+i+" &>>$OUTDIR/o"+i);		
	}
	
	else if (ctgn == 16) {
	    sb.append("cp -r $INDIR/" + i + ".tar " + i + ".tar");
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    String catargfstr = getArgfString(new File(input_file_dpath+"/catarg"+i+".txt"));
	    sb.append(" bash -c \"cat "+catargfstr+" | $BIN "+argstr+"\"");
	    sb.append(" &>$OUTDIR/o"+i); //To contain untar file content
	    sb.append("\nrm "+i+".tar");
	    
	    //Untar and check rslt files
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	}
	
	else if (ctgn == 17) {
	    sb.append("cp -r $INDIR/" + i + ".tar " + i + ".tar");
	    sb.append("\ncp -r $INDIR/" + i + ".exclude " + i + ".exclude");
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    sb.append(" $BIN " + argstr);
	    String argfstr = getArgfString(new File(input_file_dpath+"/runarg"+i+".txt"));
	    sb.append(" " + argfstr);
	    sb.append(" &>$OUTDIR/o"+i);
	    sb.append("\nrm "+i+".tar");
	    sb.append("\nrm "+i+".exclude");
	    
	    //Check rslt files
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	    sb.append("\ncat "+i+" &>>$OUTDIR/o"+i); //Error message
	}
	
	else if (ctgn == 18) {
	    sb.append("cp -r $INDIR/" + i + ".tar " + i + ".tar");
	    sb.append("\ncp -r $INDIR/" + i + ".exclude " + i + ".exclude");
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    sb.append(" $BIN " + argstr);
	    String argfstr = getArgfString(new File(input_file_dpath+"/runarg"+i+".txt"));
	    sb.append(" " + argfstr);
	    sb.append(" &>$OUTDIR/o"+i);
	    sb.append("\nrm "+i+".tar");
	    sb.append("\nrm "+i+".exclude");
	    
	    //Check rslt files
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	    sb.append("\ncat "+i+"_0 &>>$OUTDIR/o"+i); //Error message
	    sb.append("\ncat "+i+"_1 &>>$OUTDIR/o"+i);
	    sb.append("\ncat "+i+"_2 &>>$OUTDIR/o"+i);
	}
	
	else if (ctgn == 20) {
	    sb.append("cp -r $INDIR/" + i + ".tar " + i + ".tar");
	    sb.append("\ncp -r $INDIR/" + i + "_0.exclude " + i + "_0.exclude");
	    sb.append("\ncp -r $INDIR/" + i + "_1.exclude " + i + "_1.exclude");
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    sb.append(" $BIN " + argstr);
	    String argfstr = getArgfString(new File(input_file_dpath+"/runarg"+i+".txt"));
	    sb.append(" " + argfstr);
	    sb.append(" &>$OUTDIR/o"+i);
	    sb.append("\nrm "+i+".tar");
	    sb.append("\nrm "+i+"_0.exclude");
	    sb.append("\nrm "+i+"_1.exclude");
	    
	    //Check rslt files
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	    sb.append("\ncat "+i+"_0 &>>$OUTDIR/o"+i); //Error message
	    sb.append("\ncat "+i+"_1 &>>$OUTDIR/o"+i); //Error message
	}
	
	else if (ctgn == 21) {
	    sb.append("cp -r $INDIR/" + i + ".tar " + i + ".tar");
	    sb.append("\ncp -r $INDIR/" + i + ".exclude " + i + ".exclude");
	    sb.append("\ntimeout -k 9 ${TIMEOUT}s");
	    sb.append(" $BIN " + argstr);
	    String argfstr = getArgfString(new File(input_file_dpath+"/runarg"+i+".txt"));
	    sb.append(" " + argfstr);
	    sb.append(" &>$OUTDIR/o"+i);
	    sb.append("\nrm "+i+".tar");
	    sb.append("\nrm "+i+".exclude");
	    
	    //Check rslt files
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	    sb.append("\ntest -d " + i);
	    sb.append("\n\necho \"$?\" >>$OUTDIR/o"+i);
	    sb.append("\ncat "+i+"/bar &>>$OUTDIR/o"+i); //Error message
	}
	
	return sb.toString();
    }

    public static String getArgfString(File argf) {
	String filestr = null;
	if (!argf.exists()) { return filestr; }
	List<String> flines = null;
	try { flines = FileUtils.readLines(argf, (String) null); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (flines != null) {
	    for (String fline : flines) {
		if (filestr == null) { filestr = ""; }
		else { filestr += " "; }
		filestr += fline.trim();
	    }
	}
	return filestr;
    }
}
