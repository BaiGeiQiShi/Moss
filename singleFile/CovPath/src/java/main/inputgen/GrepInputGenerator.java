package moss.covpath;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class GrepInputGenerator
{
    public static void main(String[] args) {
	File output_argf = new File(args[0]);
	File output_fd = new File(args[1]); //Either a file or a dir

	//Generate an argument string
	String[] argstrs = new String[] {
	    "\"a\"", //0 //input2, multiple
	    "\"a\" -v -H -r", //1 //input_dir, multiple
	    "\"1\" -h -r", //2 //input_dir, one multiple, the other no
	    "-n \"si\"", //3 //input2, multiple
	    "\"1\" -r -l", //4 //input_dir, one multiple, the other no
	    "\"1\" -r -L", //5 //input_dir, one multiple, the other no
	    "\"randomtext\" -r -c", //6 //input_dir, one multiple, the other no
	    "-o [r][a][n][d]*", //7 //input2, contains multiple
	    "\"1\" -r -q", //8 //input_dir, one multiple, the other no
	    "\"1\" -r -s", //9 //input_dir, one multiple, the other no
	    "-v \"a\"", //10 //input2, contains multiple
	    "-i \"Si\"", //11 //input2, contains multiple (one "Si", multiple "si"s)
	    "-w \"Si\"", //12 //input2, contains one (whole word)
	    "-x \"Don't\"", //13 //input2, contains one (whole line)
	    "-F \"randomtext*\"", //14 //input2, contains one (literally as randomtext*)
	    "-E \"randomtext*\"", //15 //input2, multiple (randomtext* as regex)
	    "\"ye \"", //16 //input, multiple
	    "\"cold\"", //17 //input, one
	    "\"not exist\"", //18 //input, no
	    "^D", //19 (starting with D) //input2, multiple
	    ".$", //20 (ending with something, non-empty) //input2, multiple
	    "\\^", //21 (starting with anything) //input2, multiple
	    "\\^$", //22 (empty line) //input2, multiple
	    "^[AEIOU]", //23 (starting with one of AEIOU) //input2, multiple
	    "^[^AEIOU]", //24 (starting with none of AEIOU) //input2, multiple
	    "-E \"free[^[:space:]]+\"", //25 (free following with at least one non-space) //input2, multiple
	    "-E '\\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)'" //26 //input, one
	};

	int selected_idx = getRandom(0, argstrs.length-1);
	String argstr = argstrs[selected_idx];
	try { FileUtils.writeStringToFile(output_argf, argstr); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	
	if (selected_idx == 0) {
	    String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "a", "multi");
	    try { FileUtils.writeStringToFile(output_fd, filestr); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	else if (selected_idx == 1) {
	    String filestr0 = getNewStringWithTargetInserted(getRandomAsciiString(), "a", "multi");
	    String filestr1 = getNewStringWithTargetInserted(getRandomAsciiString(), "a", "multi");
	    try {
		FileUtils.forceMkdir(output_fd);
		FileUtils.writeStringToFile(new File(output_fd.getPath()+"/input"), filestr0);
		FileUtils.writeStringToFile(new File(output_fd.getPath()+"/input2"), filestr1);
	    }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	else if (selected_idx == 2 ||
		 selected_idx == 4 ||
		 selected_idx == 5 ||
		 selected_idx == 8 ||
		 selected_idx == 9) {
	    String filestr0 = getNewStringWithTargetInserted(getRandomAsciiString(), "1", "multi");
	    String filestr1 = getNewStringWithTargetInserted(getRandomAsciiString(), "1", "no");
	    try {
		FileUtils.forceMkdir(output_fd);
		FileUtils.writeStringToFile(new File(output_fd.getPath()+"/input"), filestr0);
		FileUtils.writeStringToFile(new File(output_fd.getPath()+"/input2"), filestr1);
	    }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}
	
	else if (selected_idx == 3) {
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "si", "multi");
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 6) {
	    String filestr0 = getNewStringWithTargetInserted(getRandomAsciiString(), "randomtext", "multi");
	    String filestr1 = getNewStringWithTargetInserted(getRandomAsciiString(), "randomtext", "no");
	    try {
		FileUtils.forceMkdir(output_fd);
		FileUtils.writeStringToFile(new File(output_fd.getPath()+"/input"), filestr0);
		FileUtils.writeStringToFile(new File(output_fd.getPath()+"/input2"), filestr1);
	    }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	else if (selected_idx == 7) {
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "rand", "multi");
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	
	else if (selected_idx == 10) {
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "a", "multi");
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 11) {
	    String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "si", "multi");
	    filestr = getNewStringWithTargetInserted(filestr, "Si", "one");
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	else if (selected_idx == 12) {
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), " Si ", "one"); //Insert whole word
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 13) {
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "Don't\n", "one", 0); //Insert whole line
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 14) {
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "randomtext*", "one");
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 15) {
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "randomtext", "multi");
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 16) {
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "ye ", "multi");
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 17) {
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "cold", "one");
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 18) {
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "not exist", "no"); //A random string with no insertion
	    filestr.replaceAll("not exist", ""); //Eliminate the target string (if any)
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 19) {
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "D", "multi", 0);
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 20) {
	    String rstr = RandomStringUtils.randomAscii(1);
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), rstr, "multi", -1); //Insert at the end
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 21) {
	    String rstr = RandomStringUtils.randomAscii(1);
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), rstr, "multi", 0);
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 22) {
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "\n", "multi", 0); //Insert empty line
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 23) {
	    int rani = getRandom(0,4);
	    String rstr = null;
	    if (rani == 0) { rstr = "A"; }
	    else if (rani == 1) { rstr = "E"; }
	    else if (rani == 2) { rstr = "I"; }
	    else if (rani == 3) { rstr = "O"; }
	    else if (rani == 4) { rstr = "U"; }
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), rstr, "multi", 0); //Insert empty line
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 24) {
	    String rstr = RandomStringUtils.randomAlphanumeric(1);
	    while ("A".equals(rstr) ||
		   "E".equals(rstr) ||
		   "I".equals(rstr) ||
		   "O".equals(rstr) ||
		   "U".equals(rstr)) {
		rstr = RandomStringUtils.randomAlphanumeric(1); //Make it non-AEIOU
	    }

            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), rstr, "multi", 0); //Insert empty line
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 25) {
	    String rstr = RandomStringUtils.randomAlphanumeric(1);
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), ("free"+rstr), "multi");
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        }

	else if (selected_idx == 26) {
            String filestr = getNewStringWithTargetInserted(getRandomAsciiString(), "15.12.141.121", "one");
            try { FileUtils.writeStringToFile(output_fd, filestr); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}
    }

    public static int getRandom(int l, int h) {
	return l + (int) ((h-l+1) * Math.random());
    }

    //At most 100x100 characters
    public static String getRandomAsciiString() {
	int lnum = getRandom(1, 100);
	StringBuilder sb = null;
	for (int i=0; i<lnum; i++) {
	    if (sb == null) { sb = new StringBuilder(); }
	    else { sb.append("\n"); }
	    int cnum = getRandom(1, 100);	    
	    sb.append(RandomStringUtils.randomAscii(cnum));
	}
	return sb.toString();
    }

    public static String getNewStringWithTargetInserted(String oldstr, String target, String type) {
	//-2 means inserting at random position of a line
	return getNewStringWithTargetInserted(oldstr, target, type, -2);
    }

    public static String getNewStringWithTargetInserted(String oldstr, String target, String type, int tidx) {
	String[] oldlines = oldstr.split("\n");
	int oldlines_size = oldlines.length;
	List<Integer> idx_list = new ArrayList<Integer>();
	for (int i=0; i<oldlines_size; i++) { idx_list.add(i); }
	Collections.shuffle(idx_list);

	//How many lines selected for insertion?
	int insert_lc = -1;
	if ("one".equals(type)) { insert_lc = 1; }
	else if ("multi".equals(type)) { insert_lc = 5; }
	else if ("no".equals(type)) { insert_lc = 0; }
	insert_lc = (oldlines_size < insert_lc) ? oldlines_size : insert_lc;

	//============
	/*
	System.out.println("Shuffled index:");
	for (int i=0; i<oldlines_size; i++) {
	    System.out.print(" "+idx_list.get(i).intValue());
	}
	System.out.println();
	System.out.println("Insert_lc: " + insert_lc);
	*/
	//============	
	
	//Generate the new string
	StringBuilder sb = null;
	for (int i=0; i<oldlines_size; i++) {
	    if (i==0) { sb = new StringBuilder(); }
	    else { sb.append("\n"); }

	    boolean target_line = false;
	    for (int j=0; j<insert_lc; j++) {
		//Check up to insert_lc elements in the shuffled list
		if (i == idx_list.get(j).intValue()) {
		    target_line = true;
		    break;
		}
	    }

	    //================
	    //System.out.println("#"+i);
	    //System.out.println("Is target line? " + target_line);
	    //================	    
	    
	    String oldline = oldlines[i];
	    if (target_line) { //Insert target
		int insertidx = -1;
		if (tidx == -2) { insertidx = getRandom(0, oldline.length()); }
		else if (tidx == -1) { insertidx = oldline.length(); } //Insert at the end
		else { insertidx = tidx; }		
		String newline =
		    oldline.substring(0, insertidx) + target + oldline.substring(insertidx);
		sb.append(newline);
		//==============
		//System.out.println("Insertidx: " + insertidx);
		//System.out.println("Newline: " + newline);
		//==============		
		
	    }
	    else {
		sb.append(oldline);
	    }
	}

	return (sb == null) ? "" : sb.toString();
    }
}




