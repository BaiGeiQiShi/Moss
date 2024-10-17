package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;

public class TarInputGenerator
{
    public static void main(String[] args) {
	String id = args[0];
	String output_argdpath = args[1];
	String output_filedpath = args[2];

	int category = getRandom(0,21);
	String argstr = null;
	if (category == 0 || category == 13 || category == 19) { //tar cf foo.tar foo bar
	    //Option string
	    argstr = "cf rslt.tar";

	    //Generate input files
	    String fctnt0 = RandomFileStringGenerator.getRandomString();
	    String fctnt1 = RandomFileStringGenerator.getRandomString();
	    File f0 = new File(output_filedpath+"/"+id+"_0");
	    File f1 = new File(output_filedpath+"/"+id+"_1");
	    try { 
		FileUtils.writeStringToFile(f0, fctnt0);
		FileUtils.writeStringToFile(f1, fctnt1);
	    }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    
	    //Generate runarg file
	    String runargfctnt = id+"_0\n"+id+"_1"; //In two lines
	    File runargf = new File(output_filedpath+"/runarg"+id+".txt");
	    try { FileUtils.writeStringToFile(runargf, runargfctnt); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}
	
	else if (category == 1 || 
		 category == 14) { //tar xf foo.tar (foo.tar contains foo & bar)
	    argstr = "xf";
	    
	    //Generate input sub-files
	    String fctnt0 = RandomFileStringGenerator.getRandomString();
	    String fctnt1 = RandomFileStringGenerator.getRandomString();
	    File f0 = new File(output_filedpath+"/"+id+"_0");
	    File f1 = new File(output_filedpath+"/"+id+"_1");
	    try { 
		FileUtils.writeStringToFile(f0, fctnt0);
		FileUtils.writeStringToFile(f1, fctnt1);
	    }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	    //Generate the input tar file
	    String cmdstr = "tar cf "+id+".tar "+id+"_0 "+id+"_1";
	    CommandExecutor.executeCmd(output_filedpath, cmdstr);

            //Generate runarg file
            String runargfctnt = id+".tar";
            File runargf = new File(output_filedpath+"/runarg"+id+".txt");
            try { FileUtils.writeStringToFile(runargf, runargfctnt); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}
	
	else if (category == 2) { //tar cf foo.tar foo
	    //Option string
	    argstr = "cf rslt.tar";

	    //Generate input files
	    String fctnt0 = RandomFileStringGenerator.getRandomString();
	    File f0 = new File(output_filedpath+"/"+id);
	    try { FileUtils.writeStringToFile(f0, fctnt0); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    
	    //Generate runarg file
	    String runargfctnt = ""+id;
	    File runargf = new File(output_filedpath+"/runarg"+id+".txt");
	    try { FileUtils.writeStringToFile(runargf, runargfctnt); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	else if (category == 3) { //tar xf foo.tar bar (bar is unknown)
	    argstr = "xf";
	    
	    //Generate input sub-file
	    String fctnt0 = RandomFileStringGenerator.getRandomString();
	    File f0 = new File(output_filedpath+"/"+id);
	    try { FileUtils.writeStringToFile(f0, fctnt0); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	    //Generate the input tar file
	    String cmdstr = "tar cf "+id+".tar "+id;
            CommandExecutor.executeCmd(output_filedpath, cmdstr);

            //Generate runarg file
            String runargfctnt = id+".tar bar"; //bar is purposely made unknown
            File runargf = new File(output_filedpath+"/runarg"+id+".txt");
            try { FileUtils.writeStringToFile(runargf, runargfctnt); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	else if (category == 4) { //tar v
	    argstr = "v";
	}

	else if (category == 5) { //tar tx
	    argstr = "tx";
	}
	
	else if (category == 6) { //tar cf foo.tar -C foo . (foo is a dir)
	    argstr = "cf rslt.tar -C";
	    
	    //Generate input directories
	    makeDirectories(output_filedpath+"/"+id);

            //Generate runarg file
            String runargfctnt = id+" ."; //SPACE-AND-DOT is needed
            File runargf = new File(output_filedpath+"/runarg"+id+".txt");
            try { FileUtils.writeStringToFile(runargf, runargfctnt); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	else if (category == 7 || category == 8) { //tar xf foo.tar -C foo ./1/10
	    argstr = "xf";
	    
	    //Generate input directories
	    makeDirectories(output_filedpath+"/"+id);

	    //Generate input file
	    String cmdstr3 = "tar cf "+id+".tar -C "+id+" .";
	    CommandExecutor.executeCmd(output_filedpath, cmdstr3);

	    //Generate runarg file (For running, need to make an empty dir mkdir named *id*!!!)
            String runargfctnt = id+".tar -C "+id+" ./1/10"; //No \n, as id is not input!!!
            File runargf = new File(output_filedpath+"/runarg"+id+".txt");
            try { FileUtils.writeStringToFile(runargf, runargfctnt); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	else if (category == 9 || category == 11) { //tar cf foo.tar foo
	    argstr = "cf rslt.tar";
	    
	    //Generate input file
            String fctnt0 = RandomFileStringGenerator.getRandomString();
            File f0 = new File(output_filedpath+"/"+id);
            try { FileUtils.writeStringToFile(f0, fctnt0); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

            //Generate runarg file
            String runargfctnt = ""+id;
            File runargf = new File(output_filedpath+"/runarg"+id+".txt");
            try { FileUtils.writeStringToFile(runargf, runargfctnt); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	else if (category == 10) { //tar xf foo.tar
	    argstr = "xf";
	    
	    //Generate input sub-file
            String fctnt0 = RandomFileStringGenerator.getRandomString();
            File f0 = new File(output_filedpath+"/"+id);
            try { FileUtils.writeStringToFile(f0, fctnt0); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	    //Generate input file
            String cmdstr = "tar cf "+id+".tar "+id;
            CommandExecutor.executeCmd(output_filedpath, cmdstr);

            //Generate runarg file
            String runargfctnt = id+".tar";
            File runargf = new File(output_filedpath+"/runarg"+id+".txt");
            try { FileUtils.writeStringToFile(runargf, runargfctnt); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}
	
	else if (category == 12) { //cat foo.tar | ./tar x
	    argstr = "x";

	    //Generate input sub-file
            String fctnt0 = RandomFileStringGenerator.getRandomString();
            File f0 = new File(output_filedpath+"/"+id);
            try { FileUtils.writeStringToFile(f0, fctnt0); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

            //Generate input file
            String cmdstr = "tar cf "+id+".tar "+id;
            CommandExecutor.executeCmd(output_filedpath, cmdstr);

	    //Generate catarg file
            String catargfctnt = id+".tar";
            File catargf = new File(output_filedpath+"/catarg"+id+".txt");
            try { FileUtils.writeStringToFile(catargf, catargfctnt); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	else if (category == 16) { //cat foo.tar | ./tar Ox
	    argstr = "Ox";
	    
	    //Generate input sub-file
            String fctnt0 = RandomFileStringGenerator.getRandomString();
            File f0 = new File(output_filedpath+"/"+id);
            try { FileUtils.writeStringToFile(f0, fctnt0); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

            //Generate input file
            String cmdstr = "tar cf "+id+".tar "+id;
            CommandExecutor.executeCmd(output_filedpath, cmdstr);

	    //Generate catarg file
            String catargfctnt = id+".tar";
            File catargf = new File(output_filedpath+"/catarg"+id+".txt");
            try { FileUtils.writeStringToFile(catargf, catargfctnt); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }	    
	}

	else if (category == 17) { //tar xf foo.tar -X foo.exclude
	    argstr = "xf";

	    //Generate input sub-file & exclude file
            String fctnt0 = RandomFileStringGenerator.getRandomString();
            String fctnt1 = ""+id;
            File f0 = new File(output_filedpath+"/"+id);
            File f1 = new File(output_filedpath+"/"+id+".exclude");
            try { 
		FileUtils.writeStringToFile(f0, fctnt0);
		FileUtils.writeStringToFile(f1, fctnt1);
	    }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    
	    //Generate input tar file
	    String cmdstr = "tar cf "+id+".tar "+id;
            CommandExecutor.executeCmd(output_filedpath, cmdstr);

	    //Generate runarg file
	    String runargfctnt = id+".tar -X\n"+id+".exclude";
            File runargf = new File(output_filedpath+"/runarg"+id+".txt");
            try { FileUtils.writeStringToFile(runargf, runargfctnt); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	else if (category == 18) { //tar xf foo.tar foo bar -X foo.exclude
	    argstr = "xf";

	    //Generate input sub-files & exclude file
            String fctnt0 = RandomFileStringGenerator.getRandomString();
            String fctnt1 = RandomFileStringGenerator.getRandomString();
            String fctnt2 = RandomFileStringGenerator.getRandomString();
            String fctnt3 = id+"_0";
            File f0 = new File(output_filedpath+"/"+id+"_0");
            File f1 = new File(output_filedpath+"/"+id+"_1");
            File f2 = new File(output_filedpath+"/"+id+"_2");
            File f3 = new File(output_filedpath+"/"+id+".exclude");
            try { 
		FileUtils.writeStringToFile(f0, fctnt0);
		FileUtils.writeStringToFile(f1, fctnt1);
		FileUtils.writeStringToFile(f2, fctnt2);
		FileUtils.writeStringToFile(f3, fctnt3);
	    }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    

	    //Generate input tar file
	    String cmdstr = "tar cf "+id+".tar "+id+"_0 "+id+"_1 "+id+"_2";
            CommandExecutor.executeCmd(output_filedpath, cmdstr);

	    //Generate runarg file
	    String runargfctnt = id+".tar "+id+"_0 "+id+"_1 -X\n"+id+".exclude";
            File runargf = new File(output_filedpath+"/runarg"+id+".txt");
            try { FileUtils.writeStringToFile(runargf, runargfctnt); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	else if (category == 20) { //tar xf foo.tar -X foo.exclude -X bar.exclude
	    argstr = "xf";

            //Generate input sub-files & exclude files
            String fctnt0 = RandomFileStringGenerator.getRandomString();
            String fctnt1 = RandomFileStringGenerator.getRandomString();
	    String fctnt2 = id+"_0";
	    String fctnt3 = id+"_1";
            File f0 = new File(output_filedpath+"/"+id+"_0");
            File f1 = new File(output_filedpath+"/"+id+"_1");
            File f2 = new File(output_filedpath+"/"+id+"_0.exclude");
            File f3 = new File(output_filedpath+"/"+id+"_1.exclude");
            try {
                FileUtils.writeStringToFile(f0, fctnt0);
                FileUtils.writeStringToFile(f1, fctnt1);
                FileUtils.writeStringToFile(f2, fctnt2);
                FileUtils.writeStringToFile(f3, fctnt3);
            }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

            //Generate the input tar file
            String cmdstr = "tar cf "+id+".tar "+id+"_0 "+id+"_1";
            CommandExecutor.executeCmd(output_filedpath, cmdstr);
	    
	    //Generate runarg file
            String runargfctnt = id+".tar -X\n"+id+"_0.exclude -X\n"+id+"_1.exclude";
            File runargf = new File(output_filedpath+"/runarg"+id+".txt");
            try { FileUtils.writeStringToFile(runargf, runargfctnt); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	else if (category == 21) { //tar xf foo.tar foo -X foobar.exclude
	    argstr = "xf";

            String fctnt0 = RandomFileStringGenerator.getRandomString();
	    String fctnt1 = id+"/bar";

	    //Generate input sub-files and exclude files
	    File d0 = new File(output_filedpath+"/"+id);
            File f0 = new File(output_filedpath+"/"+id+"/bar");
            File f1 = new File(output_filedpath+"/"+id+".exclude");
            try {
		FileUtils.forceMkdir(d0);
                FileUtils.writeStringToFile(f0, fctnt0);
                FileUtils.writeStringToFile(f1, fctnt1);
            }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    
	    //Generate input tar file
	    String cmdstr = "tar cf "+id+".tar "+id;
            CommandExecutor.executeCmd(output_filedpath, cmdstr);

	    //Generate runarg file
	    String runargfctnt = id+".tar "+id+" -X\n"+id+".exclude";
            File runargf = new File(output_filedpath+"/runarg"+id+".txt");
            try { FileUtils.writeStringToFile(runargf, runargfctnt); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	//Write argument string to file
	if (argstr != null) {
	    File output_argf = new File(output_argdpath+"/"+id);
	    try { FileUtils.writeStringToFile(output_argf, argstr); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}
	else {
	    System.err.println("No arg string generated.");
	}

	//Write category # to file (used for generating oracles for input-based approach)
	File output_ctgf = new File(output_filedpath+"/ctg"+id+".txt");
	try { FileUtils.writeStringToFile(output_ctgf, (""+category)); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
    }

    public static int getRandom(int l, int h) {
	return l + (int) ((h-l+1) * Math.random());
    }

    public static void makeDirectories(String dpath) {
	try {
	    FileUtils.forceMkdir(new File(dpath));
	    FileUtils.forceMkdir(new File(dpath+"/1"));
	    FileUtils.forceMkdir(new File(dpath+"/2"));
	    FileUtils.forceMkdir(new File(dpath+"/3"));
	    FileUtils.forceMkdir(new File(dpath+"/1/10"));
	    FileUtils.forceMkdir(new File(dpath+"/1/11"));
	    FileUtils.forceMkdir(new File(dpath+"/1/10/100"));
	    FileUtils.forceMkdir(new File(dpath+"/1/10/101"));
	    FileUtils.forceMkdir(new File(dpath+"/1/10/102"));
	}
	catch (Throwable t) {
	    System.err.println(t);
	    t.printStackTrace();
	}
    }
}
