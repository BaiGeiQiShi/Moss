package moss.covpath;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class UniqInputGenerator
{
    public static void main(String[] args) {
        String id = args[0];
        String output_arg_dpath = args[1];
        String output_file_dpath = args[2];	

	//Generate an argument string
	String[] argstrs = new String[] { "", "-c", "-d", "-u", "-i", "-f", "-s", "-w" };
	String argstr = argstrs[getRandom(0, argstrs.length-1)];
	if ("-f".equals(argstr) || "-s".equals(argstr) || "-w".equals(argstr)) {
	    int argv = getRandom(1,10);
	    argstr += " " + argv;
	}

	//Generate a file string
	String filestr = RandomFileStringGenerator.getRandomString();

	//Write to files
	try { FileUtils.writeStringToFile(new File(output_arg_dpath+"/"+id), argstr); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	try { FileUtils.writeStringToFile(new File(output_file_dpath+"/"+id), filestr); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	try { FileUtils.writeStringToFile(new File(output_file_dpath+"/runarg"+id+".txt"), id); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
    }

    public static int getRandom(int l, int h) {
	return l + (int) ((h-l+1) * Math.random());
    }
}
