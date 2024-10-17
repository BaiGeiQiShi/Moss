package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import org.apache.commons.io.FileUtils;


public class RmInputGenerator
{
    public static void main(String[] args) {
	String id = args[0];
	String output_arg_dpath = args[1];
	String output_file_dpath = args[2];

	String argstr = null;
	String argfstr = null;
	String ctgstr = null;

	int category = getRandom(0,6);
	ctgstr = ""+category;

	if (category == 0) { //rm file (file does not exist)
	    argstr = "";
	    argfstr = id;
	}

	else if (category == 1) { //rm -r dir
	    argstr = "-r";
	    try { FileUtils.forceMkdir(new File(output_file_dpath+"/"+id)); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    argfstr = id;
	}

	else if (category == 2) { //rm -r -f dir
	    argstr = "-r -f";
	    try { FileUtils.forceMkdir(new File(output_file_dpath+"/"+id)); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    argfstr = id;
	}

	else if (category == 3) { //rm -f dir (error to be generated)
	    argstr = "-f";
	    try { FileUtils.forceMkdir(new File(output_file_dpath+"/"+id)); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    argfstr = id;
	}

	else if (category == 4 || category == 5) { //echo "Y"/"N" | rm -i filei
	    argstr = "-i";
	    String file_str = RandomFileStringGenerator.getRandomString();
            try { FileUtils.writeStringToFile(new File(output_file_dpath+"/"+id), file_str); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    argfstr = id;
	}

	else if (category == 6) { //rm file (a common case)
	    argstr = "";
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
