package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import org.apache.commons.io.FileUtils;


public class Bzip2InputGenerator
{
    public static void main(String[] args) {
	String id = args[0];
	String output_arg_dpath = args[1];
	String output_file_dpath = args[2];

	String argstr = null;
	String argfstr = null;
	String ctgstr = null;

	int category = getRandom(0,5);
	ctgstr = ""+category;

	if (category == 0) {
	    argstr = "-c";

	    //Generate input file
	    String file_str = RandomFileStringGenerator.getRandomString();
	    try { FileUtils.writeStringToFile(new File(output_file_dpath+"/"+id), file_str); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    argfstr = id;
	}

	else if (category == 1) {
	    argstr = "-d";

            //Generate non-bz2 file
            String file_str = RandomFileStringGenerator.getRandomString();
            try { FileUtils.writeStringToFile(new File(output_file_dpath+"/"+id), file_str); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

            //Generate bz2 file
            CommandExecutor.executeCmd(output_file_dpath, ("bzip2 -k "+id));
            argfstr = id+".bz2";	    
	}

	else if (category == 2) {
	    argstr = "-f";

	    //Generate input file
	    String file_str = RandomFileStringGenerator.getRandomString();
	    try { FileUtils.writeStringToFile(new File(output_file_dpath+"/"+id), file_str); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    argfstr = id;
	}

	else if (category == 3) {
	    argstr = "-t";

	    //Generate input file
	    String file_str = RandomFileStringGenerator.getRandomString();
	    try { FileUtils.writeStringToFile(new File(output_file_dpath+"/"+id), file_str); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    argfstr = id;
	}

	else if (category == 4) {
	    argstr = "-t";

            //Generate non-bz2 file
            String file_str = RandomFileStringGenerator.getRandomString();
            try { FileUtils.writeStringToFile(new File(output_file_dpath+"/"+id), file_str); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

            //Generate bz2 file
            CommandExecutor.executeCmd(output_file_dpath, ("bzip2 -k "+id));
            argfstr = id+".bz2";	    
	}

	else if (category == 5) {
	    argstr = "-k";

	    //Generate input file
	    String file_str = RandomFileStringGenerator.getRandomString();
	    try { FileUtils.writeStringToFile(new File(output_file_dpath+"/"+id), file_str); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    argfstr = id;
	}
	
	//Write to arg file
	try { FileUtils.writeStringToFile(new File(output_arg_dpath+"/"+id), argstr); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	//Write to runarg${id}.txt
	try { FileUtils.writeStringToFile(new File(output_file_dpath+"/runarg"+id+".txt"), argfstr); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	//Write to ctg${id}.txt
	try { FileUtils.writeStringToFile(new File(output_file_dpath+"/ctg"+id+".txt"), ctgstr); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	
    }

    public static int getRandom(int l, int h) {
	return l + (int) ((h-l+1) * Math.random());
    }
}
