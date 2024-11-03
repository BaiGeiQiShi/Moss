package moss.covpath;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import java.util.concurrent.*;



public class GCovBasedMCMCSearch
{
    private static int errid = 0;
    private static int sred_type = 2; //0: covered lines; 1: executable bytes; 2: covered stmts (see progcounter's stmt visitor for a stmt's definition).
//    private static int startmod = 0; //0: empty path; 1: all paths
    private static int cov_merge_type = 0; //0: binary; 1: real

    public static void main(String[] args) {
        File trace_count_f = new File(args[0]);
        String ip_cover_dpath = args[1];
        String code_output_dpath = args[2];
        int max_samples = Integer.parseInt(args[3]);
        File codef = new File(args[4]);
        File linef = new File(args[5]);
        String prog_dpath = args[6];
        String prog_name = args[7];
        float kr = Float.parseFloat(args[8]); //Weight balancing size & attack surface reds
        float w = Float.parseFloat(args[9]); //Weight balancing red & gen
        double kvalue = Float.parseFloat(args[10]); //Constant for computing density score
        int quan_num = Integer.parseInt(args[11]); //#Total inputs used for path quantification
	sred_type = Integer.parseInt(args[12]); //Set the size reduction size{0: covered lines; 1: executable bytes; 2: covered stmts (see progcounter's stmt visitor for a stmt's definition).}
	File base_inputs_file = new File(args[13]);
	if(args.length == 15){
	    int timeout = Integer.parseInt(args[14]);
	    long timeoutDuration = TimeUnit.MINUTES.toMillis(timeout);
	}	

	//Get Start timestamp
	long start_t = System.currentTimeMillis();
	

	//Get base inputs id
	boolean use_base_inputs = true;
	List<Integer> base_inputs = new ArrayList<Integer>();
	try {
        Scanner scanner = new Scanner(base_inputs_file);
        while (scanner.hasNextLine()) {
		String[] temp = scanner.nextLine().split("@");
		if(temp.length==2){
            String prog_file = prog_name + ".c";
			if(temp[0].equals(prog_file)){
		    		String[] testCases = temp[1].split(",");
		    		for (int i = 0; i < testCases.length; i++) {
		    			base_inputs.add(Integer.parseInt(testCases[i]));
		    		}
		    	break;
			}
                //int testCaseId = Integer.parseInt(StringUtils.substringAfterLast(scanner.nextLine(),"o"));
        }else{
            use_base_inputs= false;
        }
	    }
            scanner.close();
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    }

	

    StringBuilder info_sb = new StringBuilder();
    info_sb.append("Program name: " + prog_name);
    info_sb.append("\nMax Number of Samples: " + max_samples);
    info_sb.append("\nWeight kr: " + kr);
    info_sb.append("\nWeight w: " + w);
    info_sb.append("\nK-value: " + kvalue);
    info_sb.append("\nTotal Number of Inputs used for Path Quantification: " + quan_num);

    //Build the trace-count map (showing how many inputs covered by each trace)
    Map<Integer,Integer> tid_count_map = getTraceIdCountMap(trace_count_f);

    int tnum = tid_count_map.keySet().size();
    int total_count = 0;
    for (int i=0; i<tnum; i++) { total_count += tid_count_map.get(i).intValue(); }
    info_sb.append("\nMax Generality: " + ((float) total_count / (float) quan_num));
    System.out.println(info_sb.toString());

    //Bloated path
	List<Integer> bloated = new ArrayList<Integer>();

    //Build the trace-file map (showing which file-id, from ip_cover_dir, contains the trace)
    Map<Integer,String> tid_file_map = getTraceIdFileIdMap(trace_count_f);

    //Build the path coverage list indexed by trace ids
    List<PathCoverage> pcovs = new ArrayList<PathCoverage>();
    for (int tid=0; tid<tnum; tid++) {
        pcovs.add(PathCoverageGenerator.getPathCoverage(new File(ip_cover_dpath+"/"+tid_file_map.get(tid))));
    }

    int[] bitvec = new int[tnum];
	
    //Output empty file
    {
        PathCoverage empty_merged_pcov = null;
        empty_merged_pcov = PathCoverageGenerator.getPathCoverageWithZeroCounts(pcovs.get(0)); //Good to pass an arbitrary coverage file

        //Get reduced code
        String empty_code = GCovBasedCodeRemover.getRemovedString(codef, linef, empty_merged_pcov);
        File empty_reduced_codef = new File(prog_dpath.substring(0,prog_dpath.length()-4)+"/"+prog_name+".c.base.origin.c");
        try { FileUtils.writeStringToFile(empty_reduced_codef, empty_code); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        File tmp_empty_reduced_codef = new File(prog_dpath+"/"+prog_name+".c.base.origin.c");
        try { FileUtils.writeStringToFile(tmp_empty_reduced_codef, empty_code); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
    }

    //Initial MCMC range
    List<Integer> path_index = new ArrayList<Integer>();
    for (int i=0; i<tnum; i++) {
        path_index.add(i);
    }

    //MCMC Sampling
    int max_iters = 10000000;
    int samples = 0;

    float curr_sred = -1;
    float curr_ared = -1;
    float curr_red = -1;
    float curr_gen = -1;
    float curr_oscore = -1;
    float curr_dscore = -1;

        //Just for initial
        float zero_sred = -1;
        float zero_ared = -1;
        float zero_red = -1;
        float zero_gen = -1;
        float zero_oscore = -1;
        float zero_dscore = -1;
        float all_sred = -1;
        float all_ared = -1;
        float all_red = -1;
        float all_gen = -1;
        float all_oscore = -1;
        float all_dscore = -1;
        String zero_reduced_code = " ";
        String all_reduced_code = " ";



    //Accept_pro
    double curr_accept_pro = -1;
    double next_accept_pro = -1;

	    
	//Based inputs must be covered
	if(use_base_inputs){
        //Remove base inputs from search range
        path_index.removeAll(base_inputs);

        for (int i=0; i<tnum; i++) {
            bitvec[i] = 0;
        }
		for(int base_input : base_inputs){
	   		bitvec[base_input] = 1;
		}

        //Compute initial scores (based on base inputs)
        {
            //Get coverage
            List<PathCoverage> selected_pcovs = new ArrayList<PathCoverage>();
            for (int i=0; i<tnum; i++) {
                if (bitvec[i] == 1) {
                    selected_pcovs.add(pcovs.get(i));
                }
            }

            PathCoverage merged_pcov = null;
            if (selected_pcovs.isEmpty()) {
                merged_pcov = PathCoverageGenerator.getPathCoverageWithZeroCounts(pcovs.get(0)); //Good to pass an arbitrary coverage file
            }
            else {
                merged_pcov = PathCoverageGenerator.getMergedPathCoverage(selected_pcovs, cov_merge_type);
            }

            //Get reduced code
            String reduced_code = GCovBasedCodeRemover.getRemovedString(codef, linef, merged_pcov);
            File reduced_codef = new File(prog_dpath+"/"+prog_name+".c.reduced.c");
            try { FileUtils.writeStringToFile(reduced_codef, reduced_code); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

            //Generate sample -1.c
            File init_codef = new File(prog_dpath+"/sample_output/-1.c");
            try { FileUtils.writeStringToFile(init_codef, reduced_code); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }


            //Get size
            int[] size_arr = getSize(prog_dpath, reduced_codef);
            if (size_arr == null) { return; }

            if (sred_type == 0) {
                int[] lnum_arr = getTotalAndCoveredLineNumbers(merged_pcov);
                curr_sred = (float) (lnum_arr[0] - lnum_arr[1]) / (float) lnum_arr[0];
            }
            else if (sred_type == 1) {
                curr_sred = (float) (size_arr[0] - size_arr[1]) / (float) size_arr[0];
            }
            else if (sred_type == 2) {
                curr_sred = (float) (size_arr[4] - size_arr[5]) / (float) size_arr[4];
            }

            //Compute scores
            curr_ared = (float) (size_arr[2] - size_arr[3]) / (float) size_arr[2];
            if(curr_ared < (float)0.0){
                curr_ared = (float)0.0;
            }

            curr_red = (float) ((1-kr) * curr_sred + kr * curr_ared);
            curr_gen = 0;
            for (int i=0; i<tnum; i++) {
                if (bitvec[i] == 1 || merged_pcov.coversByLines(pcovs.get(i))) {
                    curr_gen += tid_count_map.get(i).intValue();

                    //get bloated pathes
                    if(bitvec[i] != 1){
                        bloated.add(i);
                    }

                }
            }
            curr_gen /= quan_num;
            curr_oscore = (float) ((1-w) * curr_red + w * curr_gen);

            //Get Curr timestamp
            //long curr_t = System.currentTimeMillis();
            //double tmp_kvalue = (curr_t - start_t) / 3600000 * kvalue;
            //System.out.println("kvalue: " + tmp_kvalue);

            curr_dscore = (float) Math.exp(kvalue * curr_oscore);
        }

	}
    //Not use base inputs, then choose to start on full or zero generality
    else{
        //Compute initial scores (based on full generality)
        {
            //Get coverage
            List<PathCoverage> all_pcovs = new ArrayList<PathCoverage>();
            for (int i=0; i<tnum; i++) {
                all_pcovs.add(pcovs.get(i));
            }

            PathCoverage all_merged_pcov = null;
            if (all_pcovs.isEmpty()) {
                all_merged_pcov = PathCoverageGenerator.getPathCoverageWithZeroCounts(pcovs.get(0)); //Good to pass an arbitrary coverage file
            }
            else {
                all_merged_pcov = PathCoverageGenerator.getMergedPathCoverage(all_pcovs, cov_merge_type);
            }

            //Get reduced code
            all_reduced_code = GCovBasedCodeRemover.getRemovedString(codef, linef, all_merged_pcov);
            File all_reduced_codef = new File(prog_dpath+"/"+prog_name+".c.reduced.c");
            try { FileUtils.writeStringToFile(all_reduced_codef, all_reduced_code); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }


            //Get size
            int[] all_size_arr = getSize(prog_dpath, all_reduced_codef);
            if (all_size_arr == null) { return; }

            if (sred_type == 0) {
                int[] lnum_arr = getTotalAndCoveredLineNumbers(all_merged_pcov);
                all_sred = (float) (lnum_arr[0] - lnum_arr[1]) / (float) lnum_arr[0];
            }
            else if (sred_type == 1) {
                all_sred = (float) (all_size_arr[0] - all_size_arr[1]) / (float) all_size_arr[0];
            }
            else if (sred_type == 2) {
                all_sred = (float) (all_size_arr[4] - all_size_arr[5]) / (float) all_size_arr[4];
            }

            //Compute scores
            all_ared = (float) (all_size_arr[2] - all_size_arr[3]) / (float) all_size_arr[2];
            if(all_ared < (float)0.0){
                all_ared = (float)0.0;
            }
            all_red = (float) ((1-kr) * all_sred + kr * all_ared);
            all_gen = (float)1.0;
            all_oscore = (float) ((1-w) * all_red + w * all_gen);

            //Get Curr timestamp
            //long curr_t = System.currentTimeMillis();
            //double tmp_kvalue = (curr_t - start_t) / 3600000 * kvalue;
            //System.out.println("kvalue: " + tmp_kvalue);

            all_dscore = (float) Math.exp(kvalue * all_oscore);
        }

        //Compute initial scores (based on empty generality)
        {
            //Get coverage
            List<PathCoverage> zero_pcovs = new ArrayList<PathCoverage>();

            PathCoverage zero_merged_pcov = null;
            if (zero_pcovs.isEmpty()) {
                zero_merged_pcov = PathCoverageGenerator.getPathCoverageWithZeroCounts(pcovs.get(0)); //Good to pass an arbitrary coverage file
            }
            else {
                zero_merged_pcov = PathCoverageGenerator.getMergedPathCoverage(zero_pcovs, cov_merge_type);
            }

            //Get reduced code
            zero_reduced_code = GCovBasedCodeRemover.getRemovedString(codef, linef, zero_merged_pcov);
            File zero_reduced_codef = new File(prog_dpath+"/"+prog_name+".c.reduced.c");
            try { FileUtils.writeStringToFile(zero_reduced_codef, zero_reduced_code); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

            //Get size
            int[] zero_size_arr = getSize(prog_dpath, zero_reduced_codef);
            if (zero_size_arr == null) { return; }

            if (sred_type == 0) {
                int[] lnum_arr = getTotalAndCoveredLineNumbers(zero_merged_pcov);
                zero_sred = (float) (lnum_arr[0] - lnum_arr[1]) / (float) lnum_arr[0];
            }
            else if (sred_type == 1) {
                zero_sred = (float) (zero_size_arr[0] - zero_size_arr[1]) / (float) zero_size_arr[0];
            }
            else if (sred_type == 2) {
                zero_sred = (float) (zero_size_arr[4] - zero_size_arr[5]) / (float) zero_size_arr[4];
            }

            //Compute scores
            zero_ared = (float) (zero_size_arr[2] - zero_size_arr[3]) / (float) zero_size_arr[2];
            if(zero_ared < (float)0.0){
                zero_ared = (float)0.0;
            }
            zero_red = (float) ((1-kr) * zero_sred + kr * zero_ared);
            zero_gen = (float)0.0;
            zero_oscore = (float) ((1-w) * zero_red + w * zero_gen);

            //Get Curr timestamp
            //long curr_t = System.currentTimeMillis();
            //double tmp_kvalue = (curr_t - start_t) / 3600000 * kvalue;
            //System.out.println("kvalue: " + tmp_kvalue);

            zero_dscore = (float) Math.exp(kvalue * zero_oscore);
        }

        //choose zero or full generality
        if(zero_oscore >= all_oscore){
            curr_sred = zero_sred;
            curr_ared = zero_ared;
            curr_red = zero_red;
            curr_gen = zero_gen;
            curr_oscore = zero_oscore;
            curr_dscore = zero_dscore;

            //Generate sample -1.c
            File init_codef = new File(prog_dpath+"/sample_output/-1.c");
            try { FileUtils.writeStringToFile(init_codef, zero_reduced_code); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

            for (int i=0; i<tnum; i++) {
                bitvec[i] = 0;
            }

	    System.out.println("Empty file O-Score: "+ zero_oscore +" >= Full file O-Score: " + all_oscore);
	    System.out.println("Start at Empty file!");

        }else{
            curr_sred = all_sred;
            curr_ared = all_ared;
            curr_red = all_red;
            curr_gen = all_gen;
            curr_oscore = all_oscore;
            curr_dscore = all_dscore;

            //Generate sample -1.c
            File init_codef = new File(prog_dpath+"/sample_output/-1.c");
            try { FileUtils.writeStringToFile(init_codef, all_reduced_code); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

            for (int i=0; i<tnum; i++) {
                bitvec[i] = 1;
            }

	    System.out.println("Full file O-Score: "+ all_oscore +" > Empty file O-Score: " + zero_oscore);
            System.out.println("Start at Full file!");

	}
    }

        int best_sample = -1;
        float best_sred = curr_sred;
        float best_ared = curr_ared;
        float best_red = curr_red;
        float best_gen = curr_gen;
        float best_oscore = curr_oscore;
        float best_dscore = curr_dscore;
        int[] best_bitvec = new int[tnum];
        for (int i=0; i<tnum; i++) { best_bitvec[i] = bitvec[i]; }
        //Dynamic change search range
        path_index.clear();
        for (int i=0; i<tnum; i++) {
            path_index.add(i);
        }
        if(use_base_inputs){
            path_index.removeAll(base_inputs);
        }
        if(!bloated.isEmpty()){
            path_index.removeAll(bloated);
        }


	    //Accept_pro
	    curr_accept_pro = 1.0 / (path_index.size());


        //Iteration loop
        for (int iter=0; iter<max_iters; iter++) {
            if (samples >= max_samples) { break; }

	    if(args.length == 15){
	        //Get Current timestamp
	        long nowTime = System.currentTimeMillis();
                if (nowTime - start_t >= timeoutDuration) {
                    System.out.println("Timeout reached, exiting...");
                    break;
                }
	    }
		


            StringBuilder best_rslt_sb = new StringBuilder();
            best_rslt_sb.append("<<<<<<<<<<<<<<<<<<<<<<<<<<");
            best_rslt_sb.append("\nBest Sample Id: " + best_sample);
            best_rslt_sb.append("\nBest Size Reduction: " + best_sred);
            best_rslt_sb.append("\nBest AttkSurf Reduction: " + best_ared);
            best_rslt_sb.append("\nBest Reduction: " + best_red);
            best_rslt_sb.append("\nBest Generality: " + best_gen);
            best_rslt_sb.append("\nBest OScore: " + best_oscore);
            best_rslt_sb.append("\nBest DScore: " + best_dscore);
            best_rslt_sb.append("\nBest Trace Ids Covered: ");
            StringBuilder best_id_sb = null;
            for (int i=0; i<tnum; i++) {
                if (best_bitvec[i] == 1) {
                    if (best_id_sb == null) { best_id_sb = new StringBuilder(); }
                    else { best_id_sb.append(","); }
                    best_id_sb.append(i);
                }
            }
            if (best_id_sb != null) { best_rslt_sb.append(best_id_sb.toString()); }
            best_rslt_sb.append("\n>>>>>>>>>>>>>>>>>>>>>>>>>>");
            System.out.println(best_rslt_sb.toString());


            System.out.println("Iteration: " + iter + "\nGenerate sample id: " + samples);

            //Select an index to flip
            int selected_index = (int) (Math.random() * path_index.size());
	    //int selected_index = (int) (Math.random() * (path_index.size()-4) + 4);
	    //System.out.println(selected_index);
            selected_index = path_index.get(selected_index);
	    //System.out.println(selected_index);

	    //Print info
            StringBuilder range = new StringBuilder();
            range.append("MCMC range is:");
            for(int i : path_index){
                range.append(i+" ");
            }
            System.out.println(range.toString());


            bitvec[selected_index] = 1 - bitvec[selected_index];
            if (bitvec[selected_index] == 0) {
                System.out.println("Remove trace " + selected_index);
            }
            else {
                System.out.println("Add trace " + selected_index);
            }

            //Get coverage
            List<PathCoverage> selected_pcovs = new ArrayList<PathCoverage>();
            for (int i=0; i<tnum; i++) {
                if (bitvec[i] == 1) {
                    selected_pcovs.add(pcovs.get(i));
                }
            }

            PathCoverage merged_pcov = null;
            if (selected_pcovs.isEmpty()) {
                merged_pcov = PathCoverageGenerator.getPathCoverageWithZeroCounts(pcovs.get(0)); //Good to pass an arbitrary coverage file
            }
            else {
                merged_pcov = PathCoverageGenerator.getMergedPathCoverage(selected_pcovs, cov_merge_type);
            }

            //Get reduced code
            String reduced_code = GCovBasedCodeRemover.getRemovedString(codef, linef, merged_pcov);
            File reduced_codef = new File(prog_dpath+"/"+prog_name+".c.reduced.c");
            try { FileUtils.writeStringToFile(reduced_codef, reduced_code); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

            //Get size
            int[] size_arr = getSize(prog_dpath, reduced_codef);
            if (size_arr == null) {
                System.out.println("Revert.");
                bitvec[selected_index] = 1 - bitvec[selected_index];
                continue;
            }

            //Compute reduction
            float sred = -1;
            int[] lnum_arr = null;
            if (sred_type == 0) {
                lnum_arr = getTotalAndCoveredLineNumbers(merged_pcov); //Total and Covered lines
                sred = (float) (lnum_arr[0] - lnum_arr[1]) / (float) lnum_arr[0];
            }
            else if (sred_type == 1) {
                sred = (float) (size_arr[0] - size_arr[1]) / (float) size_arr[0];
            }
            else if (sred_type == 2) {
                sred = (float) (size_arr[4] - size_arr[5]) / (float) size_arr[4];
            }

            float ared = (float) (size_arr[2] - size_arr[3]) / (float) size_arr[2];
            if(ared < (float)0.0){
                ared = (float)0.0;
            }

            float red = (float) ((1-kr) * sred + kr * ared);

	    //Compute generality
            bloated.clear();
            float gen = 0;
            for (int i=0; i<tnum; i++) {
                if (bitvec[i] == 1 || merged_pcov.coversByLines(pcovs.get(i))) {
                    gen += tid_count_map.get(i).intValue();

                        //get bloated pathes
                        if(bitvec[i] != 1){
                                bloated.add(i);
                        }
                }
            }
            gen /= quan_num;

	    //Path_tmp for count q(i+1,i)
	    List<Integer> path_tmp = new ArrayList<Integer>();
	    for (int i=0; i<tnum; i++) {
                 path_tmp.add(i);
        }

	    if(use_base_inputs){
	    	path_tmp.removeAll(base_inputs);
	    }

	    if(!bloated.isEmpty()){
            path_tmp.removeAll(bloated);
        }
	    next_accept_pro = 1.0 / path_tmp.size();


        //Compute oscore
        float oscore = (float) ((1-w) * red + w * gen);

	    //Get Curr timestamp
            //long curr_t = System.currentTimeMillis();
            //double tmp_kvalue = ((curr_t - start_t) / 3600000.0 ) * kvalue;
	    //System.out.println("kvalue: " + tmp_kvalue);


        //Compute dscore
        float dscore = (float) Math.exp(kvalue * oscore);

        //Compute dscore ratio
        double ratio = (dscore / curr_dscore) * (curr_accept_pro / next_accept_pro);


        StringBuilder score_sb = new StringBuilder();
        if (sred_type == 0) {
            score_sb.append("Origin #LOC: " + lnum_arr[0]);
            score_sb.append("\nReduced #LOC: " + lnum_arr[1]);
        }
        else if (sred_type == 1) {
            score_sb.append("Origin #Bytes: " + size_arr[0]);
            score_sb.append("\nReduced #Bytes: " + size_arr[1]);
        }
        else if (sred_type == 2) {
            score_sb.append("Origin #Stmts: " + size_arr[4]);
            score_sb.append("\nReduced #Stmts: " + size_arr[5]);
        }
        score_sb.append("\nOrigin #Gadgets: " + size_arr[2]);
        score_sb.append("\nReduced #Gadgets: " + size_arr[3]);
        score_sb.append("\nSize Reduction: " + sred);
        score_sb.append("\nAttkSurf Reduction: " + ared);
        score_sb.append("\nReduction: " + red);
        score_sb.append("\nGenerality: " + gen);
        score_sb.append("\nOScore: " + oscore);
        score_sb.append("\nDScore: " + dscore);
        score_sb.append("\nDScore Ratio: " + ratio);
        System.out.println(score_sb.toString());

        StringBuilder curr_rslt_sb = new StringBuilder();
        curr_rslt_sb.append("Trace Ids Covered: ");
        StringBuilder curr_id_sb = null;
        for (int i=0; i<tnum; i++) {
            if (bitvec[i] == 1) {
                if (curr_id_sb == null) { curr_id_sb = new StringBuilder(); }
                else { curr_id_sb.append(","); }
                curr_id_sb.append(i);
            }
        }
        if (curr_id_sb != null) { curr_rslt_sb.append(curr_id_sb.toString()); }
        System.out.println(curr_rslt_sb.toString());

	        //Accept or not?
//          if (Math.random() < ratio && gen >0.0) {
            if (Math.random() < ratio) {
                System.out.println("Accept.");
                curr_sred = sred;
                curr_ared = ared;
                curr_red = red;
                curr_gen = gen;
                curr_oscore = oscore;
                curr_dscore = dscore;



                if (oscore > best_oscore) {
                    best_sample = samples;
                    best_sred = sred;
                    best_ared = ared;
                    best_red = red;
                    best_gen = gen;
                    best_oscore = oscore;
                    best_dscore = dscore;

                    for (int j=0; j<tnum; j++) {
                        best_bitvec[j] = bitvec[j];
                    }
                }

                //Copy code file to output dir
                File output_codef = new File(code_output_dpath+"/"+samples+".c");
                try { FileUtils.copyFile(reduced_codef, output_codef); }
                catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

                samples += 1;

                //Dynamic change search range
                path_index.clear();
                for (int i=0; i<tnum; i++) {
                        path_index.add(i);
                }

                if(use_base_inputs){
                	path_index.removeAll(base_inputs);
		        }

                if(!bloated.isEmpty()){
                        path_index.removeAll(bloated);
                }


		        //Accept_pro
                curr_accept_pro = next_accept_pro;

		

            }else {
                System.out.println("Reject. Revert.");
                bitvec[selected_index] = 1 - bitvec[selected_index];
            }
        }


        StringBuilder best_rslt_sb = new StringBuilder();
        best_rslt_sb.append("**************************");
        best_rslt_sb.append("\nBest Sample Id: " + best_sample);
        best_rslt_sb.append("\nBest Size Reduction: " + best_sred);
        best_rslt_sb.append("\nBest AttkSurf Reduction: " + best_ared);
        best_rslt_sb.append("\nBest Reduction: " + best_red);
        best_rslt_sb.append("\nBest Generality: " + best_gen);
        best_rslt_sb.append("\nBest OScore: " + best_oscore);
        best_rslt_sb.append("\nBest DScore: " + best_dscore);
        best_rslt_sb.append("\nBest Trace Ids Covered: ");
        StringBuilder best_id_sb = null;
        for (int i=0; i<tnum; i++) {
            if (best_bitvec[i] == 1) {
                if (best_id_sb == null) { best_id_sb = new StringBuilder(); }
                else { best_id_sb.append(","); }
                best_id_sb.append(i);
            }
        }
        if (best_id_sb != null) { best_rslt_sb.append(best_id_sb.toString()); }
        best_rslt_sb.append("\n**************************");
        System.out.println(best_rslt_sb.toString());
    }

    public static Map<Integer,Integer> getTraceIdCountMap(File trace_count_f) {
        Map<Integer,Integer> tid_count_map = new HashMap<Integer,Integer>();
        List<String> lines = null;
        try { lines = FileUtils.readLines(trace_count_f); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        if (lines == null) { return tid_count_map; }

        for (String line : lines) {
            String[] elems = line.split(",", 4);
            tid_count_map.put(Integer.parseInt(elems[0]), Integer.parseInt(elems[2]));
        }

        return tid_count_map;
    }

    public static Map<Integer,String> getTraceIdFileIdMap(File trace_count_f) {
        Map<Integer,String> tid_file_map = new HashMap<Integer,String>();
        List<String> lines = null;
        try { lines = FileUtils.readLines(trace_count_f); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        if (lines == null) { return tid_file_map; }

        for (String line : lines) {
            String[] elems = line.split(",", 4);
		
	    //I fixed the bug that tid_count_map[2] does not equals 2, but 10.
            tid_file_map.put(Integer.parseInt(elems[0]), elems[0]);
        }

        return tid_file_map;
    }

    //Covf should be in gcov json format (generated by gcov -i)
    public static int getLineCount(File covf, String type) {
        String covf_ctnt = null;
        try { covf_ctnt = FileUtils.readFileToString(covf, (String) null); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        if (covf_ctnt == null) { return -1; }
        else { return getLineCount(covf_ctnt, type); }
    }

    public static int getLineCount(String cov_str, String type) {
        if (cov_str == null) { return -1; }

        int n = 0;
        String[] cov_lines = cov_str.split("\n");
        for (String cov_line : cov_lines) {
            cov_line = cov_line.trim();
            if (cov_line.startsWith("lcount:")) {
                if ("full".equals(type)) {
                    n += 1;
                }
                else if ("cov".equals(type)) {
                    if (!cov_line.endsWith(",0")) {
                        n += 1;
                    }
                }
            }
        }
        return n;
    }

    //This should be called only AFTER .c.reduced.c is generated.
    private static int[] getSize(String prog_dpath, File reduced_codef) {
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(new File(prog_dpath));
        ExecuteWatchdog watchdog = new ExecuteWatchdog(600000); //Timeout in 30s
        executor.setWatchdog(watchdog);

        //Run getsize.sh
        int exitValue = -1;
        try { exitValue = executor.execute(CommandLine.parse("./getsize.sh")); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        if (exitValue != 0) {
            System.out.println("Invalid program.");
            File err_codef = new File(prog_dpath+"/errcode/"+errid+".c"); //Make copy of err code
            errid += 1;
            try { FileUtils.copyFile(reduced_codef, err_codef); }
            catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
            return null;
        }

        //Parse size result
        File red_rsltf = new File(prog_dpath+"/size_rslt.txt");
        List<String> red_rsltf_lines = null;
        try { red_rsltf_lines = FileUtils.readLines(red_rsltf, (String) null); }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
        if (red_rsltf_lines == null) {
            System.out.println("Unable to get size info for reduced program.");
            return null;
        }
        int origin_bytes=-1, red_bytes=-1,
            origin_gdt=-1, red_gdt=-1,
            origin_stmts=-1, red_stmts=-1;
        try {
            origin_bytes = Integer.parseInt(red_rsltf_lines.get(0).trim());
            red_bytes = Integer.parseInt(red_rsltf_lines.get(1).trim());
            origin_gdt = Integer.parseInt(red_rsltf_lines.get(2).trim());
            red_gdt = Integer.parseInt(red_rsltf_lines.get(3).trim());
            origin_stmts = Integer.parseInt(red_rsltf_lines.get(4).trim());
            red_stmts = Integer.parseInt(red_rsltf_lines.get(5).trim());
        }
        catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

        if (origin_bytes==-1 || red_bytes==-1 || origin_gdt==-1 || red_gdt==-1 ||
            origin_stmts==-1 || red_stmts==-1) {
            System.out.println("Error parsing sizes for reduced program.");
            return null;
        }

        //Return four ints
        return new int[] { origin_bytes, red_bytes, origin_gdt, red_gdt, origin_stmts, red_stmts };
    }

    public static int[] getTotalAndCoveredLineNumbers(String cov_str) {
        String[] lines = cov_str.split("\n");
        int cover = 0;
        int total = 0;
        for (String line : lines) {
            if (line.startsWith("lcount:")) {
                total += 1;
                if (!line.endsWith(",0")) {
                    cover += 1;
                }
            }
        }
        return new int[] { total, cover };
    }

    public static int[] getTotalAndCoveredLineNumbers(PathCoverage pcov) {
        Map<Integer, Integer> lcmap = pcov.getLineCountMap();
        int total = 0, cover = 0;
        for (Integer l : lcmap.keySet()) {
            total += 1;
            if (lcmap.get(l).intValue() != 0) {
                cover += 1;
            }
        }
        return new int[] { total, cover };
    }

}
