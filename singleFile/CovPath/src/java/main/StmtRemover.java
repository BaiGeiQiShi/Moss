package moss.covpath;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class StmtRemover
{
    Set<String> covered_stmt_ids;

    public StmtRemover(Set<String> covered_sids) {
	covered_stmt_ids = covered_sids;
    }

    /* Ignore any lines but those starting with <STMTID>. */
    public StmtRemover(String instru_output_fctnt) {
	covered_stmt_ids = new HashSet<String>();
	String[] instru_output_flines = instru_output_fctnt.trim().split("\n");
	for (String instru_output_fline : instru_output_flines) {
	    instru_output_fline = instru_output_fline.trim();
	    if (instru_output_fline.startsWith("<STMTID>")) {
		covered_stmt_ids.add(instru_output_fline.substring("<STMTID>".length()).trim());
	    }
	}
    }

    /* Ignore any lines but those starting with <STMTID>. */
    public StmtRemover(File instru_output_f) {
	covered_stmt_ids = new HashSet<String>();
	List<String> instru_output_flines = null;
	try { instru_output_flines = FileUtils.readLines(instru_output_f); }
	catch (Throwable t) { System.err.println(); t.printStackTrace(); }
	if (instru_output_flines != null) {
	    for (String instru_output_fline : instru_output_flines) {
		instru_output_fline = instru_output_fline.trim();
		if (instru_output_fline.startsWith("<STMTID>")) {
		    covered_stmt_ids.add(instru_output_fline.substring("<STMTID>".length()).trim());
		}
	    }
	}
    }

    //code_f is the instrumented code file (using writeStringToFile method)
    public String getCodeAfterRemoval(File code_f) {
	return getCodeAfterRemoval(code_f, false);
    }
    
    //code_f is the instrumented code file (using writeStringToFile method)
    public String getCodeAfterRemoval(File code_f, boolean keep_instru_print) {
	List<String> code_flines_old = null;
	try { code_flines_old = FileUtils.readLines(code_f); }
	catch (Throwable t) { System.err.println(); t.printStackTrace(); }
	if (code_flines_old == null) { return null; }

	List<String> code_flines_new = new ArrayList<String>();
	int size = code_flines_old.size();
	for (int i=0; i<size; ) {
	    String code_fline = code_flines_old.get(i).trim();
	    if (code_fline.startsWith("//") && code_fline.endsWith("-BEGIN")) {
		String stmt_id = code_fline.substring(2, code_fline.lastIndexOf("-BEGIN")).trim();

		//To remove the stmt, check two things:
		//(1) the stmt id itself is NOT shown in the covered set and
		//(2) NO stmt ids used within the stmt are shown in the covered set.
		//Need to check (2) because there can be GOTO labels used in the stmt,
		//in which case (1) might be true but (2) might not.
		if (!covered_stmt_ids.contains(stmt_id)) { //Check (1)
		    //Use the loop below to (a) find the -END line and (b) check (2).
		    int stmt_end_fline_index = -1;
		    boolean has_covered_child_stmts = false;
		    for (int j=i+1; j<size; j++) {
			String code_fline_j = code_flines_old.get(j).trim();
			//Check (2)
			if (code_fline_j.startsWith("//") && code_fline_j.endsWith("-BEGIN")) {
			    String child_stmt_id = code_fline_j.substring(2, code_fline_j.lastIndexOf("-BEGIN")).trim();
			    if (covered_stmt_ids.contains(child_stmt_id)) {
				has_covered_child_stmts = true;
			    }
			}
			//Found the -END line
			if (code_fline_j.equals("//"+stmt_id+"-END")) {
			    stmt_end_fline_index = j;
			    break;
			}
		    }
		    if (stmt_end_fline_index != -1) { //If not found (which shouldn't happen), keep the stmt
			if (!has_covered_child_stmts) { //Satisfies (2)
			    i = stmt_end_fline_index + 1;
			    continue;
			}
		    }
		}
		
		//Keep the stmt
		//First make sure the next line is a print line, and skip it
		if (i+1 < size) {
		    String code_fline_next = code_flines_old.get(i+1).trim();
		    if (code_fline_next.startsWith("writeStringToFile") && code_fline_next.contains("<STMTID>")) {
			if (keep_instru_print) { //Keep the print stmt
			    code_flines_new.add(code_flines_old.get(i+1));
			}
			i += 2; //Skip the following print line
			continue;
		    }
		}
		
		i += 1; //The following line is not a print line, this shouldn't happen, but we keep the line
		continue;
	    }

	    if (code_fline.startsWith("//") && code_fline.endsWith("-END")) {
		//Ignore this line
	    }
	    else if (code_fline.startsWith("writeStringToFile") &&
		     (code_fline.contains("<FUNC-BEGIN>") || code_fline.contains("<FUNC-END>"))) {
		if (keep_instru_print) { //Keep the print stmt
		    code_flines_new.add(code_flines_old.get(i));
		}
	    }
	    else {
		code_flines_new.add(code_flines_old.get(i)); //To not add the trimmed line
	    }

	    i += 1;
	}

	StringBuilder sb = null;
	for (String code_fline_new : code_flines_new) {
	    if (sb == null) { sb = new StringBuilder(); }
	    else { sb.append("\n"); }
	    sb.append(code_fline_new);
	}
	return sb.toString();
    }
    
}
