package moss.covpath;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import org.apache.commons.io.FileUtils;


public class ChownInputGenerator
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

	if (category == 0) {
	    String file_str = RandomFileStringGenerator.getRandomString();
	    try { FileUtils.writeStringToFile(new File(output_file_dpath+"/"+id), file_str); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	    if (getRandom(0,1)==0) { argstr = "qxin6:qxin6"; }
	    else { argstr = "qxin6:sudo"; }

	    argfstr = id;
	}

	else if (category == 1) {
	    String tdpath = output_file_dpath+"/"+id+"/"+id+"/"+id;
	    try { FileUtils.forceMkdir(new File(tdpath)); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    
	    String file_str = RandomFileStringGenerator.getRandomString();
	    try { FileUtils.writeStringToFile(new File(tdpath+"/"+id), file_str); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	    if (getRandom(0,1)==0) { argstr = "qxin6:qxin6"; }
            else { argstr = "qxin6:sudo"; }

	    argfstr = id+"/"+id+"/"+id+"/"+id; //Inner file
	}

	else if (category == 2) {
	    String tdpath = output_file_dpath+"/"+id+"/"+id+"/"+id;
	    try { FileUtils.forceMkdir(new File(tdpath)); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    
	    String file_str = RandomFileStringGenerator.getRandomString();
	    try { FileUtils.writeStringToFile(new File(tdpath+"/"+id), file_str); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	    if (getRandom(0,1)==0) { argstr = "qxin6:qxin6"; }
            else { argstr = "qxin6:sudo"; }

	    argfstr = id; //Parent dir
	}

	else if (category == 3) {
	    String tdpath = output_file_dpath+"/"+id+"/"+id+"/"+id;
	    try { FileUtils.forceMkdir(new File(tdpath)); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    
	    String file_str = RandomFileStringGenerator.getRandomString();
	    try { FileUtils.writeStringToFile(new File(tdpath+"/"+id), file_str); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	    if (getRandom(0,1)==0) { argstr = "-R qxin6:qxin6"; } //Using -R
            else { argstr = "-R qxin6:sudo"; }

	    argfstr = id;
	}

	else if (category == 4) {
	    String file_str = RandomFileStringGenerator.getRandomString();
	    try { FileUtils.writeStringToFile(new File(output_file_dpath+"/"+id), file_str); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	    //Create symbolic link
	    CommandExecutor.executeCmd(output_file_dpath, ("ln -s "+id+" s"+id));
	    
	    if (getRandom(0,1)==0) { argstr = "-h qxin6:qxin6"; }
            else { argstr = "-h qxin6:sudo"; }

	    argfstr = id;
	}

	else if (category == 5) {
	    String file_str = RandomFileStringGenerator.getRandomString();
	    try { FileUtils.writeStringToFile(new File(output_file_dpath+"/"+id), file_str); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	    //Create symbolic link
	    CommandExecutor.executeCmd(output_file_dpath, ("ln -s "+id+" s"+id));
	    
	    if (getRandom(0,1)==0) { argstr = "-h qxin6:qxin6"; }
            else { argstr = "-h qxin6:sudo"; }

	    argfstr = "s"+id; //symbolic file
	}

	else if (category == 6) {
	    String file_str = RandomFileStringGenerator.getRandomString();
	    try { FileUtils.writeStringToFile(new File(output_file_dpath+"/"+id), file_str); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	    //Create symbolic link
	    CommandExecutor.executeCmd(output_file_dpath, ("ln -s "+id+" s"+id));
	    
	    if (getRandom(0,1)==0) { argstr = "qxin6:qxin6"; }
            else { argstr = "qxin6:sudo"; }

	    argfstr = "s"+id;
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
