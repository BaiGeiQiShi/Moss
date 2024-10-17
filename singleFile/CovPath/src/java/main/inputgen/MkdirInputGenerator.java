package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import org.apache.commons.io.FileUtils;


public class MkdirInputGenerator
{
    public static void main(String[] args) {
	String id = args[0];
	String output_arg_dpath = args[1];
	String output_file_dpath = args[2];

	String argstr = null;
	String argfstr = null;
	String ctgstr = null;

	int category = getRandom(0,12);
	ctgstr = ""+category;

	if (category == 0) { //mkdir d1
	    argfstr = id;
	}

	else if (category == 1) { //mkdir d1/d2
	    argfstr = id+"/"+id;
	}

	else if (category == 2) { //mkdir "-m 123124" d1/d2
	    argstr = "-m ";
	    for (int i=0; i<6; i++) { argstr += getRandom(0,7); }
	    argfstr = id+"/"+id;
	}

	else if (category == 3) { //mkdir -m d1/d2
	    argstr = "-m";
	    argfstr = id+"/"+id;
	}
       	
	else if (category >= 4 && category <= 8) { //mkdir "-m 400" d1
	    argstr = "-m ";
	    for (int i=0; i<3; i++) { argstr += getRandom(0,7); }
	    argfstr = id;
	}

	else if (category == 9) { //mkdir -p d1/d2
	    argstr = "-p";
	    argfstr = id+"/"+id;
	}

	else if (category == 10) { //mkdir -p d1/d2/d3/d4
	    argstr = "-p";
	    argfstr = id+"/"+id+"/"+id+"/"+id;
	}

	else if (category == 11) { //mkdir -p d1 (in a context in which d1 has been created)
	    argstr = "-p";
	    argfstr = id;
	}

	else if (category == 12) { //mkdir -p d1/d2 (in a context in which d1 has been created)
	    argstr = "-p";
            argfstr = id+"/"+id;
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
