package moss.covpath;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import org.apache.commons.io.FileUtils;
import java.text.DecimalFormat;

public class GetResult
{
    public static DecimalFormat int_df = new DecimalFormat("0");
    public static DecimalFormat hour_float_df = new DecimalFormat("0.0");
    public static DecimalFormat score_float_df = new DecimalFormat("0.00");
    public static DecimalFormat pathprob_float_df = new DecimalFormat("0.000");    
    
    public static void main(String[] args) {
	String progname = args[0];
	String type = args[1];
	if ("pathsample".equals(type)) {
	    File logf = new File(args[2]);
	    File path_count_f = new File(args[3]);
	    float[] sampling_info = getDQSamplingResult(logf);
	    System.out.println("PROGNAME,#SAMPLES,MAXK,#PATHS,PATH_IDENTIFICATION_TIME(HOUR),PATH_QUANTIFICATION_TIME(HOUR)");
	    System.out.println(progname+","+
			       int_df.format(sampling_info[0])+","+
			       int_df.format(sampling_info[1])+","+
			       getPathCounts(path_count_f).size()+","+
			       hour_float_df.format(sampling_info[2])+","+
			       hour_float_df.format(sampling_info[3]));
	}

	else if ("pathcount".equals(type)) {
	    File path_count_f = new File(args[2]);
	    List<Integer> pcs = getPathCounts(path_count_f);
	    System.out.println("PATH_ID,COUNT");
	    for (int i=0; i<pcs.size(); i++) {
		System.out.println(i+","+pcs.get(i).intValue());
	    }
	}

	else if ("pathprob".equals(type)) {
	    File path_count_f = new File(args[2]);
	    int quan_sample_num = Integer.parseInt(args[3]);
	    List<Float> pps = getPathProbabilities(path_count_f, quan_sample_num);
	    System.out.println("PATH_ID,PROBABILITY");
	    for (int i=0; i<pps.size(); i++) {
		System.out.println(i+","+pathprob_float_df.format(pps.get(i).floatValue()));
	    }
	}
	
	else if ("mcmcsearch".equals(type)) {
	    File mcmc_logf = new File(args[2]);
	    float[] mcmc_rslt = getMCMCSearchResult(mcmc_logf);
	    System.out.println("PROGNAME,#ITERS,#SAMPLES,BEST_SAMPLE_ID,BEST_SRED,BEST_ARED,BEST_RED,BEST_GEN,BEST_OSCORE");
	    System.out.println(progname+","+
			       int_df.format(mcmc_rslt[0])+","+
			       int_df.format(mcmc_rslt[1])+","+
			       int_df.format(mcmc_rslt[2])+","+
			       score_float_df.format(mcmc_rslt[3])+","+
			       score_float_df.format(mcmc_rslt[4])+","+
			       score_float_df.format(mcmc_rslt[5])+","+
			       score_float_df.format(mcmc_rslt[6])+","+
			       score_float_df.format(mcmc_rslt[7]));
	}

	else if ("debopmcmcsearch".equals(type)) {
	    File mcmc_logf = new File(args[2]);
	    float[] mcmc_rslt = getDebopMCMCSearchResult(mcmc_logf);
	    System.out.println("PROGNAME,#ITERS,#SAMPLES,BEST_SAMPLE_ID,BEST_SRED,BEST_ARED,BEST_RED,BEST_GEN,BEST_OSCORE");
	    System.out.println(progname+","+
			       int_df.format(mcmc_rslt[0])+","+
			       int_df.format(mcmc_rslt[1])+","+
			       int_df.format(mcmc_rslt[2])+","+
			       score_float_df.format(mcmc_rslt[3])+","+
			       score_float_df.format(mcmc_rslt[4])+","+
			       score_float_df.format(mcmc_rslt[5])+","+
			       score_float_df.format(mcmc_rslt[6])+","+
			       score_float_df.format(mcmc_rslt[7]));
	}

	else if ("debopinputs".equals(type)) {
	    File path_count_f = new File(args[2]);
	    Set<String> input_ids = getInputIdsForAllPaths(path_count_f);
	    StringBuilder sb = null;
	    for (String input_id : input_ids) {
		if (sb == null) { sb = new StringBuilder(); }
		else { sb.append(","); }
		sb.append(input_id);
	    }
	    if (sb != null) {
		System.out.println(sb.toString());
	    }
	}
	else if ("chiselinputs".equals(type)) {
	    File path_count_f = new File(args[2]);
	    File debdq_mcmc_log_f = new File(args[3]);
	    String ip_cover_dpath = args[4];

	    //Get the coverage of every selected path
	    Map<String, String> pid_fid_map = getPathIdFileIdMap(path_count_f);
	    List<String> best_path_ids = getBestPathIds(debdq_mcmc_log_f);
	    List<PathCoverage> selected_pcovs = new ArrayList<PathCoverage>();
	    for (String best_path_id : best_path_ids) {
		String ip_cover_fid = pid_fid_map.get(best_path_id);
		File ip_cover_f = new File(ip_cover_dpath+"/"+ip_cover_fid);
		selected_pcovs.add(PathCoverageGenerator.getPathCoverage(ip_cover_f));
	    }
	    if (selected_pcovs.isEmpty()) { return; }

	    //Get all preserved path ids
	    List<String> rslt_pids = new ArrayList<String>(); //Preserved path ids
	    PathCoverage merged_pcov = PathCoverageGenerator.getMergedPathCoverage(selected_pcovs, 0); //binary merge
	    for (String pid : pid_fid_map.keySet()) {
		if (best_path_ids.contains(pid)) { rslt_pids.add(pid); }
		else {
		    File ip_cover_f = new File(ip_cover_dpath+"/"+pid_fid_map.get(pid));
		    PathCoverage pcov = PathCoverageGenerator.getPathCoverage(ip_cover_f);
		    if (merged_pcov.coversByLines(pcov)) {
			rslt_pids.add(pid);
		    }
		}
	    }

	    //Get all the corresponding input ids
	    Set<String> input_ids = getInputIdsForGivenPaths(path_count_f, rslt_pids);
	    StringBuilder sb = null;
	    for (String input_id : input_ids) {
		if (sb == null) { sb = new StringBuilder(); }
		else { sb.append(","); }
		sb.append(input_id);
	    }
	    if (sb != null) {
		System.out.println(sb.toString());
	    }
	}
	else if ("chiselprogred".equals(type)) {
	    File size_f = new File(args[2]);
	    float kr = Float.parseFloat(args[3]);
	    List<String> size_flines = null;
	    try { size_flines = FileUtils.readLines(size_f, (String) null); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    if (size_flines == null) { return; }

	    int origin_stmts = Integer.parseInt(size_flines.get(4).trim());
	    int reduced_stmts = Integer.parseInt(size_flines.get(5).trim());
	    int origin_gadgets = Integer.parseInt(size_flines.get(2).trim());
	    int reduced_gadgets = Integer.parseInt(size_flines.get(3).trim());
	    float size_red = (float) (origin_stmts - reduced_stmts) / (float) origin_stmts;
	    float attksurf_red = (float) (origin_gadgets - reduced_gadgets) / (float) origin_gadgets;
	    float red = (1-kr) * size_red + kr * attksurf_red;
	    System.out.println("PROGNAME,SizeRed,AttkSurfRed,Red");
	    System.out.println(progname + "," +
			       score_float_df.format(size_red) + "," +
			       score_float_df.format(attksurf_red) + "," +
			       score_float_df.format(red));
	}

	else if ("get_preserved_pids_for_selected_pids".equals(type)) {
	    File path_count_f = new File(args[2]);
	    File selected_pid_f = new File(args[3]);
	    String ip_cover_dpath = args[4];

	    //Get the coverage of every selected path
	    Map<String, String> pid_fid_map = getPathIdFileIdMap(path_count_f);
	    String pid_str = null;
	    try { pid_str = FileUtils.readFileToString(selected_pid_f, (String) null); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    if (pid_str == null) { return; }
	    List<String> selected_pids = Arrays.asList(pid_str.trim().split(","));

	    //Get preserved pids
	    List<String> rslt_pids = getPreservedPids(ip_cover_dpath, pid_fid_map, selected_pids);
	    StringBuilder sb = new StringBuilder();
	    for (int i=0; i<rslt_pids.size(); i++) {
		if (i == 0) { sb.append(rslt_pids.get(i)); }
		else { sb.append("," + rslt_pids.get(i)); }
	    }
	    System.out.println(sb.toString());
	}

	else if ("get_generality_of_selected_pids".equals(type)) {
	    File path_count_f = new File(args[2]);
	    File selected_pid_f = new File(args[3]);
	    String ip_cover_dpath = args[4];
	    int quan_sample_num = Integer.parseInt(args[5]);

	    //Get the coverage of every selected path
	    Map<String, String> pid_fid_map = getPathIdFileIdMap(path_count_f);
	    String pid_str = null;
	    try { pid_str = FileUtils.readFileToString(selected_pid_f, (String) null); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    if (pid_str == null) { return; }
	    List<String> selected_pids = Arrays.asList(pid_str.trim().split(","));

	    //Get preserved pids
	    List<String> rslt_pids = getPreservedPids(ip_cover_dpath, pid_fid_map, selected_pids);
	    
	    //Get sum of counts for preserved path ids
	    int counts = 0;
	    List<Integer> path_counts = getPathCounts(path_count_f); //indexed by pids
	    for (String rslt_pid : rslt_pids) {
		counts += path_counts.get(Integer.parseInt(rslt_pid)).intValue();
	    }
	    System.out.println((float) counts / (float) quan_sample_num);
	}
	
    }

    private static List<String> getPreservedPids(String ip_cover_dpath, Map<String, String> pid_fid_map, List<String> selected_pids) {
	List<PathCoverage> selected_pcovs = new ArrayList<PathCoverage>();
	for (String selected_pid : selected_pids) {
	    String ip_cover_fid = pid_fid_map.get(selected_pid);
	    File ip_cover_f = new File(ip_cover_dpath+"/"+ip_cover_fid);
	    selected_pcovs.add(PathCoverageGenerator.getPathCoverage(ip_cover_f));
	}
	if (selected_pcovs.isEmpty()) { return new ArrayList<String>(); }
	
	//Get all preserved path ids
	List<String> rslt_pids = new ArrayList<String>(); //Preserved path ids
	PathCoverage merged_pcov = PathCoverageGenerator.getMergedPathCoverage(selected_pcovs, 0); //binary merge
	for (String pid : pid_fid_map.keySet()) {
	    if (selected_pids.contains(pid)) { rslt_pids.add(pid); }
	    else {
		File ip_cover_f = new File(ip_cover_dpath+"/"+pid_fid_map.get(pid));
		PathCoverage pcov = PathCoverageGenerator.getPathCoverage(ip_cover_f);
		if (merged_pcov.coversByLines(pcov)) {
		    rslt_pids.add(pid);
		}
	    }
	}
	return rslt_pids;
    }
    
    public static float[] getDQSamplingResult(File logf) {
	List<String> logf_lines = null;
	try { logf_lines = FileUtils.readLines(logf, (String) null); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (logf_lines == null) { return null; }

	float path_identify_time_in_hour = -1;
	float path_quantify_time_in_hour = -1;
	float max_iters = -1;
	float max_k = -1;

	int logf_lines_size = logf_lines.size();
	for (int i=logf_lines_size-1; i>=0; i--) {
	    String logf_line = logf_lines.get(i).trim();

	    if (path_identify_time_in_hour == -1 &&
		logf_line.startsWith("Path identification time:")) {
		path_identify_time_in_hour = Float.parseFloat(logf_line.substring(logf_line.indexOf(":")+1, logf_line.length()-1).trim()) / ((float) 3600);
	    }

	    else if (path_quantify_time_in_hour == -1 &&
		     logf_line.startsWith("Path quantification time:")) {
		path_quantify_time_in_hour = Float.parseFloat(logf_line.substring(logf_line.indexOf(":")+1, logf_line.length()-1).trim()) / ((float) 3600);
	    }

	    else if (max_iters == -1 &&
		     logf_line.startsWith("Current Iteration Id:")) {
		max_iters = Float.parseFloat(logf_line.substring(logf_line.indexOf(":")+1, logf_line.indexOf(";")).trim());
		max_iters += 1; //Id starting with 0
	    }

	    if (logf_line.startsWith("Current k:")) {
		float currk = Float.parseFloat(logf_line.substring(logf_line.indexOf(":")+1).trim());
		if (currk > max_k) {
		    max_k = currk;
		}
	    }
	}

	return new float[] { max_iters, max_k, path_identify_time_in_hour, path_quantify_time_in_hour };
    }

    public static List<Integer> getPathCounts(File path_count_f) {
	List<String> path_count_flines = null;
        try { path_count_flines = FileUtils.readLines(path_count_f, (String) null); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (path_count_flines == null) { return null; }

	List<Integer> path_counts = new ArrayList<Integer>();
	for (String path_count_fline : path_count_flines) {
	    String[] elems = path_count_fline.split(",");
	    path_counts.add(Integer.parseInt(elems[2]));
	}
	return path_counts;
    }

    public static List<Float> getPathProbabilities(File path_count_f, int quan_sample_num) {
	List<Integer> path_counts = getPathCounts(path_count_f);
	List<Float> path_probs = new ArrayList<Float>();
	for (Integer path_count : path_counts) {
	    float path_prob = (float) path_count.intValue() / (float) quan_sample_num;
	    path_probs.add(path_prob);
	}
	return path_probs;
    }
    
    public static float[] getMCMCSearchResult(File mcmc_logf) {
	List<String> mcmc_logf_lines = null;
        try { mcmc_logf_lines = FileUtils.readLines(mcmc_logf, (String) null); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        if (mcmc_logf_lines == null) { return null; }

	float iters = -1;
	float samples = -1;
	float best_sample_id = -1;
	float best_sr = -1;
	float best_ar = -1;
	float best_red = -1;
	float best_gen = -1;
	float best_oscore = -1;
	
	int mcmc_logf_lines_size = mcmc_logf_lines.size();
	for (int i=mcmc_logf_lines_size-1; i>=0; i--) {
	    String mcmc_logf_line = mcmc_logf_lines.get(i).trim();
	    if (iters == -1 && mcmc_logf_line.startsWith("Iteration:")) {
		iters = Float.parseFloat(mcmc_logf_line.substring(mcmc_logf_line.indexOf(":")+1).trim());
	    }
	    
	    if (samples == -1 && mcmc_logf_line.startsWith("Generate sample id:")) {
		samples = Float.parseFloat(mcmc_logf_line.substring(mcmc_logf_line.indexOf(":")+1).trim());
	    }

	    if (best_sample_id == -1 && mcmc_logf_line.startsWith("Best Sample Id:")) {
		best_sample_id = Float.parseFloat(mcmc_logf_line.substring(mcmc_logf_line.indexOf(":")+1).trim());
	    }
	    
	    if (best_sr == -1 && mcmc_logf_line.startsWith("Best Size Reduction:")) {
		best_sr = Float.parseFloat(mcmc_logf_line.substring(mcmc_logf_line.indexOf(":")+1).trim());
	    }

	    if (best_ar == -1 && mcmc_logf_line.startsWith("Best AttkSurf Reduction:")) {
		best_ar = Float.parseFloat(mcmc_logf_line.substring(mcmc_logf_line.indexOf(":")+1).trim());
	    }
	    
	    if (best_red == -1 && mcmc_logf_line.startsWith("Best Reduction:")) {
		best_red = Float.parseFloat(mcmc_logf_line.substring(mcmc_logf_line.indexOf(":")+1).trim());
	    }

	    if (best_gen == -1 && mcmc_logf_line.startsWith("Best Generality:")) {
		best_gen = Float.parseFloat(mcmc_logf_line.substring(mcmc_logf_line.indexOf(":")+1).trim());
	    }

	    if (best_oscore == -1 && mcmc_logf_line.startsWith("Best OScore:")) {
		best_oscore = Float.parseFloat(mcmc_logf_line.substring(mcmc_logf_line.indexOf(":")+1).trim());
	    }

	    if (iters != -1 && samples != -1 && best_sample_id != -1 &&
		best_sr != -1 && best_ar != -1 &&
		best_red != -1 && best_gen != -1 && best_oscore != -1) {
		break;
	    }
	}

	return new float[] { iters, samples, best_sample_id, best_sr, best_ar, best_red, best_gen, best_oscore };
    }

    public static float[] getDebopMCMCSearchResult(File mcmc_logf) {
	List<String> mcmc_logf_lines = null;
        try { mcmc_logf_lines = FileUtils.readLines(mcmc_logf, (String) null); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        if (mcmc_logf_lines == null) { return null; }

	float iters = -1;
	float samples = -1;
	float best_sample_id = -1;
	float best_sr = -1;
	float best_ar = -1;
	float best_red = -1;
	float best_gen = -1;
	float best_oscore = -1;
	
	int mcmc_logf_lines_size = mcmc_logf_lines.size();
	for (int i=mcmc_logf_lines_size-1; i>=0; i--) {
	    String mcmc_logf_line = mcmc_logf_lines.get(i).trim();
	    if (iters == -1 && mcmc_logf_line.startsWith("Current Iteration:")) {
		String[] elems = mcmc_logf_line.split(";");
		iters = Float.parseFloat(elems[0].substring(elems[0].indexOf(":")+1).trim());
		samples = Float.parseFloat(elems[1].substring(elems[1].indexOf(":")+1).trim());
	    }

	    if (best_sample_id == -1 && mcmc_logf_line.startsWith("Current Best Sample Id:")) {
		best_sample_id = Float.parseFloat(mcmc_logf_line.substring(mcmc_logf_line.indexOf(":")+1).trim());
	    }

	    if (best_sr == -1 && mcmc_logf_line.startsWith("Current Best SR-Score:")) {
		String[] elems = mcmc_logf_line.split(";");
		best_sr = Float.parseFloat(elems[0].substring(elems[0].indexOf(":")+1).trim());
		best_ar = Float.parseFloat(elems[1].substring(elems[1].indexOf(":")+1).trim());
		best_red = Float.parseFloat(elems[2].substring(elems[2].indexOf(":")+1).trim());
		best_gen = Float.parseFloat(elems[3].substring(elems[3].indexOf(":")+1).trim());
		best_oscore = Float.parseFloat(elems[4].substring(elems[4].indexOf(":")+1).trim());
	    }

	    if (iters != -1 && samples != -1 && best_sample_id != -1 &&
		best_sr != -1 && best_ar != -1 &&
		best_red != -1 && best_gen != -1 && best_oscore != -1) {
		break;
	    }
	}

	return new float[] { iters, samples, best_sample_id, best_sr, best_ar, best_red, best_gen, best_oscore };
    }

    /* These are all the inputs Debop needs. */
    public static Set<String> getInputIdsForAllPaths(File path_counted_f) {
	Set<String> rslt_ids = new HashSet<String>();
        List<String> path_counted_flines = null;
        try { path_counted_flines = FileUtils.readLines(path_counted_f, (String) null); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        if (path_counted_flines == null) { return rslt_ids; }

	for (String path_counted_fline : path_counted_flines) {
	    path_counted_fline = path_counted_fline.trim();
	    if (path_counted_fline.endsWith("0,")) { //Nothing to add
		continue;
	    }
	    else {
		String[] elems = path_counted_fline.split(",");
		for (int i=3; i<elems.length; i++) { rslt_ids.add(elems[i]); }
	    }
        }
	return rslt_ids;
    }

    //This is used to generate Chisel's inputs.
    //Pass path_ids as the best ids found by Debdq.
    public static Set<String> getInputIdsForGivenPaths(File path_counted_f, List<String> path_ids) {
        List<String> path_counted_flines = null;
        try { path_counted_flines = FileUtils.readLines(path_counted_f, (String) null); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        if (path_counted_flines == null) { return new HashSet<String>(); }

        Set<String> input_ids = new HashSet<String>();
        for (String path_counted_fline : path_counted_flines) {
	    path_counted_fline = path_counted_fline.trim();	    
            String[] elems = path_counted_fline.split(",");
            if (path_ids.contains(elems[0])) { //Target path
		if (path_counted_fline.endsWith("0,")) { continue; } //Nothing to add
		else {
		    for (int i=3; i<elems.length; i++) { //Add all covered input ids
			input_ids.add(elems[i]);
		    }
		}
            }
        }

        return input_ids;
    }

    public static List<String> getBestPathIds(File mcmclog_f) {
        List<String> mcmclog_flines = null;
        try { mcmclog_flines = FileUtils.readLines(mcmclog_f, (String) null); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        if (mcmclog_flines == null) { return new ArrayList<String>(); }

        for (int i=mcmclog_flines.size()-1; i>=0; i--) {
            String mcmclog_fline = mcmclog_flines.get(i).trim();
            if (mcmclog_fline.startsWith("Best Trace Ids Covered:")) {
		String[] ids = mcmclog_fline.substring(mcmclog_fline.indexOf(":")+1).trim().split(",");
                return Arrays.asList(ids);
            }
	}

	return null;
    }

    private static Map<String,String> getPathIdFileIdMap(File path_count_f) {
	Map<String,String> pid_file_map = new HashMap<String,String>();
	List<String> lines = null;
	try { lines = FileUtils.readLines(path_count_f); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (lines == null) { return pid_file_map; }

	for (String line : lines) {
	    String[] elems = line.split(",", 4);
	    pid_file_map.put(elems[0], elems[1]);
	}
	
	return pid_file_map;
    }
}
