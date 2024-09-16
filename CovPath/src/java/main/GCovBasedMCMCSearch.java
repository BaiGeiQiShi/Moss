package moss.covpath;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;

import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;

import moss.covpath.LogUtil;

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;

public class GCovBasedMCMCSearch {
	private static int errid = 0;
	private static int sred_type = 2; // 0: covered lines; 1: executable bytes; 2: covered stmts (see progcounter's
										// stmt visitor for a stmt's definition).
	// private static int startmod = 0; //0: empty path; 1: all paths
	private static int cov_merge_type = 0; // 0: binary; 1: real

	public static void main(String[] args) throws ParseException {
		// #region Manage Option
		org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
		options.addOption("t", "tracecount", true, "path to trace count file");
		options.addOption("i", "identifypathfolder", true, "path to identify path folder");
		options.addOption("s", "sampleoutput", true, "path to store samples");
		options.addOption("m", "maxsamples", true, "max samples number");
		options.addOption("I", "maxiters", true, "max iterations number");
		options.addOption(
				"f", "codefile", true, "one code file to be reduced. \n Can not work with -L together");
		options.addOption("F", "codelist", true,
				"a file that contains a list of codes to be reduced. \nCan not work with -f together");
		options.addOption("l", "line", true, "line information text");
		options.addOption("L", "linelist", true, "a file contains the projects corresponding lines information");
		options.addOption("p", "workingdir", true, "working directory of the reduction process");
		options.addOption("n", "programname", true, "compiled program name");
		options.addOption(
				"r", "wsa", true, "Weight balancing size & attack surface reds");
		options.addOption("w", "wrg", true, "Weight balancing red & gen");
		options.addOption("k", "densitycoef", true, "Constant for computing density score");
		options.addOption("q", "quantifypathnumber", true, "Total inputs used for path quantification");
		options.addOption("S", "sizereductionmethod", true,
				"Set the size reduction size\n{0: covered lines; \n1: executable bytes; \n2: covered stmts");
		options.addOption(
				"B", "baseinputfile", true, "base inputs file. Testcases in it will not be removed");
		options.addOption("h", "help", false, "Display this help message");

		org.apache.commons.cli.CommandLineParser parser = new org.apache.commons.cli.DefaultParser();
		org.apache.commons.cli.CommandLine cmd = null;
		File trace_count_f = null;
		String ip_cover_dpath = null;
		String code_output_dpath = null;
		int max_samples = 0;
		int max_iters = 0;
		List<File> codefs = new ArrayList<>();
		Map<File, File> linefs = new HashMap<>();
		String prog_dpath = null;
		String prog_name = null;
		float kr = 0.0f;
		float w = 0.0f;
		double kvalue = 0.0;
		int quan_num = 0;
		File base_inputs_file = null;
		Set<Integer> base_inputs = new HashSet<>();
		try {
			cmd = parser.parse(options, args);

			if (cmd.hasOption("help")) {
				new org.apache.commons.cli.HelpFormatter().printHelp("CovPath", options);
				return;
			}

			trace_count_f = new File(cmd.getOptionValue("tracecount")); // tmp/path_counted.txt
			ip_cover_dpath = cmd.getOptionValue("identifypathfolder"); // identify_path
			code_output_dpath = cmd.getOptionValue("sampleoutput"); // tmp/sample_output
			if (!(new File(code_output_dpath)).mkdir()) {
				System.err.println(code_output_dpath + " didn't created successfully. Exit.");
				System.exit(1);
			}
			LogUtil.configureLogger(code_output_dpath + "/log.txt", Level.FINE);

			max_samples = Integer.parseInt(cmd.getOptionValue("maxsamples")); // domgad_samplenum
			max_iters = Integer.parseInt(cmd.getOptionValue("maxiters")); // domgad_samplenum

			if (cmd.hasOption("codefile") && !cmd.hasOption("codelist")) {
				codefs.add(new File(cmd.getOptionValue("codefile")));
			} else if (cmd.hasOption("codelist")) { // code list takes precedence over code file
				try (BufferedReader br = new BufferedReader(new FileReader(cmd.getOptionValue("codelist")))) {
					String line;
					LogUtil.logDebug("--Collecting code file list from " + cmd.getOptionValue("codelist"));
					while ((line = br.readLine()) != null) {
						if (line.startsWith("#") || line.startsWith(" ")) {
							LogUtil.logTrace("---Pass line `" + line + "`");
							continue;
						}
						codefs.add(new File(line.trim()));
						LogUtil.logTrace("---Add codefile:" + line.trim());
					}
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			} else if (!cmd.hasOption("codefile") && !cmd.hasOption("codelist")) {
				LogUtil.logWarning("No specified file to reduce. Use '-f file' or '-F file-list'");
				System.exit(1);
			}
			LogUtil.logDebug("--Check codefs");
			for (File temp : codefs) {
				LogUtil.logTrace("---" + temp.getAbsolutePath());
			}

			if (cmd.hasOption("line") && !cmd.hasOption("linelist")) {
				linefs.put(codefs.get(0), new File(cmd.getOptionValue("line")));
			} else if (cmd.hasOption("linelist")) { // line list takes precedence over code file
				try (BufferedReader br = new BufferedReader(new FileReader(cmd.getOptionValue("linelist")))) {
					String line;
					while ((line = br.readLine()) != null) {
						if (line.startsWith("#")) {
							continue;
						}
						String[] parts = line.split(":");
						if (parts.length < 2) {
							continue;
						}

						File file = new File(parts[0].trim());
						File lineinfo = new File(parts[1].trim());
						linefs.put(file, lineinfo);
					}
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			} else if (!cmd.hasOption("line") && !cmd.hasOption("linelist")) {
				System.err.println("No specified line information file. Use '-l line' or '-L linelist'");
				System.exit(0);
			}
			LogUtil.logDebug("--Check linefs");
			for (Map.Entry<File, File> entry : linefs.entrySet()) {
				String key = entry.getKey().getAbsolutePath();
				String value = entry.getValue().getAbsolutePath();
				LogUtil.logTrace("---" + key + ":" + value);
			}

			prog_dpath = cmd.getOptionValue("workingdir"); // tmp
			prog_name = cmd.getOptionValue("programname"); // {PROGRAM}
			kr = Float.parseFloat(cmd.getOptionValue("wsa")); // Weight balancing size & attack surface reds
			w = Float.parseFloat(cmd.getOptionValue("wrg")); // Weight balancing red & gen
			kvalue = Float.parseFloat(cmd.getOptionValue("densitycoef")); // Constant for computing density score
			quan_num = Integer.parseInt(cmd.getOptionValue("quantifypathnumber")); // #Total inputs used for path
																					// quantification
			sred_type = Integer.parseInt(cmd.getOptionValue("sizereductionmethod")); // Set the size reduction size{0:
																						// covered lines; 1: executable
																						// bytes; 2: covered stmts (see
																						// progcounter's stmt visitor
																						// for a stmt's definition).}
			if (cmd.hasOption("baseinputfile")) {
				base_inputs_file = new File(cmd.getOptionValue("baseinputfile"));
				try (BufferedReader br = new BufferedReader(new FileReader(base_inputs_file))) {
					String line;
					while ((line = br.readLine()) != null) {
						if (line.startsWith("#")) {
							continue;
						}
						String[] parts = line.split("@");
						if (parts.length < 2) {
							continue;
						}

						String key = parts[0].trim();
						String[] testcases = parts[1].split(",");

						for (String testcase : testcases) {
							base_inputs.add(Integer.parseInt(testcase));
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		} catch (Exception e) {
			System.err.println("Error parsing command line options: " + e.getMessage());
			e.printStackTrace();
			for (String arg : args) {
				System.err.println(arg);
			}
			System.exit(1);
		}
		// #endregion

		try {
			// Get Start timestamp
			// long start_t = System.currentTimeMillis();

			LogUtil.logInfo("Program name: " + prog_name);
			LogUtil.logInfo("Max Number of Samples: " + max_samples);
			LogUtil.logInfo("Max Iteration of Samples: " + max_iters);
			LogUtil.logInfo("Weight kr: " + kr);
			LogUtil.logInfo("Weight w: " + w);
			LogUtil.logInfo("K-value: " + kvalue);
			LogUtil.logInfo("Total Number of Inputs used for Path Quantification: " + quan_num);

			// Build the trace-count map (showing how many inputs covered by each trace)
			// TODO: REMOVE 计算每个Trace(path)的count 执行次数？
			Map<Integer, Integer> trace_count_map = getTraceIdCountMap(trace_count_f);

			int tnum = trace_count_map.keySet().size();
			int total_count = 0;
			for (int tidx = 0; tidx < tnum; tidx++) {
				total_count += trace_count_map.get(tidx).intValue();
			}
			LogUtil.logDebug(String.format("Max Generality = %f/%f = %f", (float) total_count, (float) quan_num,
					(float) total_count / (float) quan_num));

			File codef = null, linef = null;

			// Bloated path
			// TODO: 这是什么
			List<Integer> bloated = new ArrayList<Integer>();

			// Build the trace-file map (showing which file-id, from ip_cover_dir, contains
			// the trace)
			Map<Integer, String> trace_file_map = getTraceIdFileIdMap(trace_count_f);

			// Build the path coverage list indexed by trace idx
			List<PathCoverage> pcovs = new ArrayList<PathCoverage>();
			for (int tidx = 0; tidx < tnum; tidx++) {
				pcovs.add(PathCoverageGenerator
						.getPathCoverage(new File(ip_cover_dpath + "/" + trace_file_map.get(tidx))));
			}

			if (pcovs.size() == 0) {
				throw new UnsupportedOperationException(
						"No path coverage file. Please double check `" + ip_cover_dpath + "`\n");
			}

			int[] bitvec = new int[tnum];

			// #region generate empty(base) file
			{
				LogUtil.logInfo("-Generating empty(base) file");
				PathCoverage empty_merged_pcov = null;
				empty_merged_pcov = PathCoverageGenerator.getPathCoverageWithZeroCounts(pcovs.get(0)); // an arbitrary pcov

				for (int idx = 0; idx != codefs.size(); idx++) {
					codef = codefs.get(idx);
					linef = linefs.get(codef);
					LogUtil.logTrace("codef: " + codef);
					LogUtil.logTrace("linef: " + linef);
					if(codef==null || linef==null){
						continue;
					}
					// Get reduced code
					String empty_code = GCovBasedCodeRemover.getRemovedString(codef, linef, empty_merged_pcov);

					File empty_reduced_codef = new File(
							codef.getAbsolutePath().replace(".c.cov.origin.c",".c").replace(".c", ".c.base.origin.c"));
					LogUtil.logTrace(
							"Base file:" + codef.getAbsolutePath().replace(".c.cov.origin.c",".c").replace(".c", ".c.base.origin.c"));
					try {
						FileUtils.writeStringToFile(empty_reduced_codef, empty_code);
					} catch (Throwable t) {
						System.err.println(t);
						t.printStackTrace();
					}
				}
			}
			// #endregion
			

			// MCMC range initialization
			List<Integer> path_index = new ArrayList<Integer>(); // select path from path_index
			for (int i = 0; i < tnum; i++) {
				path_index.add(i);
			}

			// MCMC Sampling
			int samples = 0;

			float curr_sred = -1;
			float curr_ared = -1;
			float curr_red = -1;
			float curr_gen = -1;
			float curr_oscore = -1;
			float curr_dscore = -1;

			// #region variables for initial
			// Have to compare full-coverage with zero-coverage
			float zero_sred = -1;
			float zero_ared = -1;
			float zero_red = -1;
			float zero_gen = -1;
			float zero_oscore = -1;
			float zero_dscore = -1;
			String zero_gen_code = " ";

			float all_sred = -1;
			float all_ared = -1;
			float all_red = -1;
			float all_gen = -1;
			float all_oscore = -1;
			float all_dscore = -1;
			String full_gen_code = " ";
			// #endregion

			// Accept_pro
			double curr_accept_pro = -1;
			double next_accept_pro = -1;

			// Start from a coverage that just fit in with base-inputs
			if (base_inputs.size() != 0) {
				// Remove base inputs from search range
				path_index.removeAll(base_inputs);

				for (int i = 0; i < tnum; i++) {
					bitvec[i] = 0;
				}
				for (int base_input : base_inputs) {
					bitvec[base_input] = 1;
				}

				// compute initial scores for a path that just covers base-inputs
				{
					List<PathCoverage> selected_pcovs = new ArrayList<PathCoverage>();
					for (int i = 0; i < tnum; i++) {
						if (bitvec[i] == 1) {
							selected_pcovs.add(pcovs.get(i));
						}
					}

					PathCoverage merged_pcov = null;
					if (selected_pcovs.isEmpty()) {
						merged_pcov = PathCoverageGenerator.getPathCoverageWithZeroCounts(pcovs.get(0)); 
					} else {
						merged_pcov = PathCoverageGenerator.getMergedPathCoverage(selected_pcovs, cov_merge_type);
					}

					// Get reduced code
					for (int idx = 0; idx != codefs.size(); idx++) {
						codef = codefs.get(idx);
						linef = linefs.get(codef);
						if(codef==null || linef==null){
							continue;
						}
						LogUtil.logTrace("---Base input: " + codef);
						String reduced_code = GCovBasedCodeRemover.getRemovedString(codef, linef, merged_pcov);

						File reduced_codef = new File(
								codef.getAbsolutePath().replace(".c.cov.origin.c", ".c"));
						File init_codef = new File(code_output_dpath,
								codef.getName().replace(".c", ".sample-1.c"));
						try {
							FileUtils.writeStringToFile(reduced_codef, reduced_code);
							FileUtils.writeStringToFile(init_codef, reduced_code);
						} catch (Throwable t) {
							System.err.println(t);
							t.printStackTrace();
						}
					}

					// Get size
					int[] size_arr = getSize(prog_dpath, codefs);
					if (size_arr == null) {
						return;
					}

					if (sred_type == 0) {
						int[] lnum_arr = getTotalAndCoveredLineNumbers(merged_pcov);
						curr_sred = (float) (lnum_arr[0] - lnum_arr[1]) / (float) lnum_arr[0];
					} else if (sred_type == 1) {
						curr_sred = (float) (size_arr[0] - size_arr[1]) / (float) size_arr[0];
					} else if (sred_type == 2) {
						curr_sred = (float) (size_arr[4] - size_arr[5]) / (float) size_arr[4];
					}

					// Compute scores
					curr_ared = (float) (size_arr[2] - size_arr[3]) / (float) size_arr[2];
					if (curr_ared < (float) 0.0) {
						curr_ared = (float) 0.0;
					}

					curr_red = (float) ((1 - kr) * curr_sred + kr * curr_ared);
					curr_gen = 0;
					for (int i = 0; i < tnum; i++) {
						if (bitvec[i] == 1 || merged_pcov.coversByLines(pcovs.get(i))) {
							curr_gen += trace_count_map.get(i).intValue();

							// get bloated pathes
							if (bitvec[i] != 1) {
								bloated.add(i);
							}

						}
					}
					curr_gen /= quan_num;
					curr_oscore = (float) ((1 - w) * curr_red + w * curr_gen);

					curr_dscore = (float) Math.exp(kvalue * curr_oscore);
				}

			}
			// Didn't use base inputs, then choose to start on full or zero generality
			else {
				LogUtil.logInfo("-Generating full file");
				// Compute initial scores for full generality
				{
					// Get coverage
					List<PathCoverage> all_pcovs = new ArrayList<PathCoverage>();
					for (int i = 0; i < tnum; i++) {
						all_pcovs.add(pcovs.get(i));
					}
					PathCoverage all_merged_pcov = null;
					if (all_pcovs.isEmpty()) {
						all_merged_pcov = PathCoverageGenerator.getPathCoverageWithZeroCounts(pcovs.get(0)); 
					} else {
						all_merged_pcov = PathCoverageGenerator.getMergedPathCoverage(all_pcovs, cov_merge_type);
					}

					// Get reduced code
					for (int idx = 0; idx != codefs.size(); ++idx) {
						codef = codefs.get(idx);
						linef = linefs.get(codef);
						if(codef==null || linef==null){
							continue;
						}
						full_gen_code = GCovBasedCodeRemover.getRemovedString(codef, linef, all_merged_pcov);
						File full_gen_codef = new File(
								codef.getAbsolutePath().replace(".c.cov.origin.c", ".c"));
						File full_gen_codef_origin = new File(
								codef.getAbsolutePath().replace(".c.cov.origin.c", ".c").replace(".c", ".c.origin.c"));
						try {
							FileUtils.writeStringToFile(full_gen_codef, full_gen_code);
							FileUtils.writeStringToFile(full_gen_codef_origin, full_gen_code);
						} catch (Throwable t) {
							System.err.println(t);
							t.printStackTrace();
						}
					}

					// #region compute score
					LogUtil.logTrace("Calculating full-generality's scores");
					int[] all_size_arr = getSize(prog_dpath, codefs);
					if (all_size_arr == null) {
						return;
					}

					if (sred_type == 0) {
						int[] lnum_arr = getTotalAndCoveredLineNumbers(all_merged_pcov);
						all_sred = (float) (lnum_arr[0] - lnum_arr[1]) / (float) lnum_arr[0];
					} else if (sred_type == 1) {
						all_sred = (float) (all_size_arr[0] - all_size_arr[1]) / (float) all_size_arr[0];
					} else if (sred_type == 2) {
						all_sred = (float) (all_size_arr[4] - all_size_arr[5]) / (float) all_size_arr[4];
					}

					// Compute scores
					all_ared = (float) (all_size_arr[2] - all_size_arr[3]) / (float) all_size_arr[2];
					if (all_ared < (float) 0.0) {
						all_ared = (float) 0.0;
					}
					all_red = (float) ((1 - kr) * all_sred + kr * all_ared);
					all_gen = (float) 1.0;
					all_oscore = (float) ((1 - w) * all_red + w * all_gen);
					LogUtil.logDebug(String.format("Full File's O-score %f = %f + %f", all_oscore, (1 - w) * all_red, w * all_gen));

					all_dscore = (float) Math.exp(kvalue * all_oscore);
					// #endregion
				}
				// Compute initial scores for empty generality
				{
					// Get coverage
					List<PathCoverage> zero_pcovs = new ArrayList<PathCoverage>();

					PathCoverage zero_merged_pcov = PathCoverageGenerator.getPathCoverageWithZeroCounts(pcovs.get(0));

					// Get reduced code
					for (int idx = 0; idx != codefs.size(); ++idx) {
						codef = codefs.get(idx);
						linef = linefs.get(codef);
						if(codef==null || linef==null){
							continue;
						}
						zero_gen_code = GCovBasedCodeRemover.getRemovedString(codef, linef, zero_merged_pcov);
						File zero_gen_codef = new File(
								codef.getAbsolutePath().replace(".c.cov.origin.c", ".c"));
						try {
							FileUtils.writeStringToFile(zero_gen_codef, zero_gen_code);
						} catch (Throwable t) {
							System.err.println(t);
							t.printStackTrace();
						}
					}

					// #region Calculate Scores for zero-covered path
					LogUtil.logTrace("Calculating zero-generality's scores");
					int[] zero_size_arr = getSize(prog_dpath, codefs);
					if (zero_size_arr == null) {
						return;
					}

					if (sred_type == 0) {
						int[] lnum_arr = getTotalAndCoveredLineNumbers(zero_merged_pcov);
						zero_sred = (float) (lnum_arr[0] - lnum_arr[1]) / (float) lnum_arr[0];
					} else if (sred_type == 1) {
						zero_sred = (float) (zero_size_arr[0] - zero_size_arr[1]) / (float) zero_size_arr[0];
					} else if (sred_type == 2) {
						zero_sred = (float) (zero_size_arr[4] - zero_size_arr[5]) / (float) zero_size_arr[4];
					}

					// Compute scores
					zero_ared = (float) (zero_size_arr[2] - zero_size_arr[3]) / (float) zero_size_arr[2];
					if (zero_ared < (float) 0.0) {
						zero_ared = (float) 0.0;
					}
					zero_red = (float) ((1 - kr) * zero_sred + kr * zero_ared);
					zero_gen = (float) 0.0;
					zero_oscore = (float) ((1 - w) * zero_red + w * zero_gen);
					LogUtil.logDebug(String.format("Empty File's O-score %f = %f + %f", zero_oscore, (1 - w) * zero_red, w * zero_gen));
					zero_dscore = (float) Math.exp(kvalue * zero_oscore);
					// #endregion
				}

				// choose zero or full generality
				if (zero_oscore >= all_oscore) {
					LogUtil.logInfo("Empty file O-Score: " + zero_oscore + " >= Full file O-Score: " + all_oscore);
					LogUtil.logInfo("Start at Empty file");

					curr_sred = zero_sred;
					curr_ared = zero_ared;
					curr_red = zero_red;
					curr_gen = zero_gen;
					curr_oscore = zero_oscore;
					curr_dscore = zero_dscore;

					for (int i = 0; i < tnum; i++) {
						bitvec[i] = 0;
					}

					// Generate sample -1
					// zero-generality files are just generated and didn't get overriden. Copy these
					// as sample -1
					LogUtil.logDebug("--Copying reduced file as sample -1");
					for (int idx = 0; idx != codefs.size(); ++idx) {
						codef = codefs.get(idx);
						linef = linefs.get(codef);
						if(codef==null || linef==null){
							continue;
						}
						File reducedcodef = new File(
								codef.getAbsolutePath().replace(".c.cov.origin.c", ".c"));
						File init_codef_tosave = new File(code_output_dpath,
								codef.getName().replace(".c.cov.origin.c",".c").replace(".c", ".sample-1.c"));
						File init_codef = new File(
								codef.getAbsolutePath().replace(".c.cov.origin.c",".c").replace(".c", ".sample-1.c"));
						LogUtil.logTrace("---Copy " + reducedcodef.getAbsolutePath() + " to " + init_codef);
						LogUtil.logTrace("---Copy " + reducedcodef.getAbsolutePath() + " to " + init_codef_tosave);
						try {
							FileUtils.copyFile(reducedcodef, init_codef_tosave);
							FileUtils.copyFile(reducedcodef, init_codef);
						} catch (Throwable t) {
							System.err.println(t);
							t.printStackTrace();
						}
					}
				} else { //zero < full
					LogUtil.logInfo("Full file O-Score: " + all_oscore + " > Empty file O-Score: " + zero_oscore);
					LogUtil.logInfo("Start at Full file");
					curr_sred = all_sred;
					curr_ared = all_ared;
					curr_red = all_red;
					curr_gen = all_gen;
					curr_oscore = all_oscore;
					curr_dscore = all_dscore;

					for (int i = 0; i < tnum; i++) {
						bitvec[i] = 1;
					}

					// Generate sample -1.c
					// full-generality files are exactly files in codefs. Copy them as sample -1
					LogUtil.logDebug("--Copying cov.origin.c as sample -1");
					for (int idx = 0; idx != codefs.size(); ++idx) {
						codef = codefs.get(idx);
						linef = linefs.get(codef);
						if(codef==null || linef==null){
							continue;
						}
						
						File origin_codef = new File(codef.getAbsolutePath().replace(".c.cov.origin.c", ".c").replace(".c", ".c.origin.c"));
						File init_codef_tosave = new File(
								codef.getAbsolutePath().replace(".c.cov.origin.c", ".c").replace(".c", ".sample-1.c"));
						File init_codef = new File(code_output_dpath,
								codef.getName().replace(".c.cov.origin.c", ".c").replace(".c", ".sample-1.c"));
						LogUtil.logTrace("---Copy " + codef.getAbsolutePath().replace(".c.cov.origin.c", ".c").replace(".c", ".c.origin.c") + " to " + init_codef);
						LogUtil.logTrace("---Copy " + codef.getAbsolutePath().replace(".c.cov.origin.c", ".c").replace(".c", ".c.origin.c") + " to " + init_codef_tosave);
						try {
							FileUtils.copyFile(origin_codef, init_codef_tosave);
							FileUtils.copyFile(origin_codef, init_codef);
						} catch (Throwable t) {
							System.err.println(t);
							t.printStackTrace();
						}
					}
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
			for (int i = 0; i < tnum; i++) {
				best_bitvec[i] = bitvec[i];
			}

			path_index.clear();
			for (int i = 0; i < tnum; i++) {
				path_index.add(i);
			}

			// Dynamically adjust search range
			if (base_inputs.size() != 0) {
				path_index.removeAll(base_inputs);
			}
			if (!bloated.isEmpty()) {
				path_index.removeAll(bloated);
			}

			// Accept_pro
			curr_accept_pro = 1.0 / (path_index.size());

			// #region Iteration loop
			for (int iter = 0; iter < max_iters && samples < max_samples; iter++) {
				LogUtil.logInfo("<<<<<<<<<<<<<<<<<<<<<<<<<<");
				LogUtil.logInfo("Best Sample Id: " + best_sample);
				LogUtil.logInfo("Best Size Reduction: " + best_sred);
				LogUtil.logInfo("Best AttkSurf Reduction: " + best_ared);
				LogUtil.logInfo("Best Reduction: " + best_red);
				LogUtil.logInfo("Best Generality: " + best_gen);
				LogUtil.logInfo("Best OScore: " + best_oscore);
				LogUtil.logInfo("Best DScore: " + best_dscore);
				LogUtil.logInfo("Best Trace Ids Covered: " +IntStream.range(0, best_bitvec.length)
						.filter(i -> best_bitvec[i] == 1)
						.mapToObj(String::valueOf)
						.collect(Collectors.joining(",")));
				LogUtil.logInfo(">>>>>>>>>>>>>>>>>>>>>>>>>>");

				LogUtil.logInfo("Iteration: " + iter + " Generate sample id: " + samples);

				// Select an index to flip
				int selected_index = (int) (Math.random() * path_index.size());
				selected_index = path_index.get(selected_index);
				LogUtil.logDebug(String.format("Selected_index: %d", selected_index));

				// Print info
				LogUtil.logDebug("MCMC range is:" +
						path_index.stream()
								.map(Object::toString)
								.collect(Collectors.joining(" ")));

				bitvec[selected_index] = 1 - bitvec[selected_index];  //flip the selected trace
				if (bitvec[selected_index] == 0) {
					LogUtil.logInfo("Remove trace " + selected_index);
				} else {
					LogUtil.logInfo("Add trace " + selected_index);
				}

				// Get coverage
				List<PathCoverage> selected_pcovs = new ArrayList<PathCoverage>();
				for (int i = 0; i < tnum; i++) {
					if (bitvec[i] == 1) {
						selected_pcovs.add(pcovs.get(i));
					}
				}

				PathCoverage merged_pcov = null;
				if (selected_pcovs.isEmpty()) {
					merged_pcov = PathCoverageGenerator.getPathCoverageWithZeroCounts(pcovs.get(0));
				} else {
					merged_pcov = PathCoverageGenerator.getMergedPathCoverage(selected_pcovs, cov_merge_type);
				}
				System.err.println(merged_pcov.toString());

				// Get reduced code
				for (int idx = 0; idx != codefs.size(); idx++) {
					codef = codefs.get(idx);
					linef = linefs.get(codef);
						if(codef==null || linef==null){
							continue;
						}
					// LogUtil.logTrace("---Iteration input: " + codef);

					// cov_codef: cov_codef ends with `.c.cov.origin.c` no matter about filename is `file.c` or `file.c.cov.origin.c`
					File cov_codef = new File(codef.getAbsolutePath().replace(".c.cov.origin.c", ".c").replace(".c",".c.cov.origin.c"));
					File reduced_codef = new File(codef.getAbsolutePath().replace(".c.cov.origin.c", ".c"));
					FileUtils.copyFile(cov_codef, codef);
					String reduced_code = GCovBasedCodeRemover.getRemovedString(codef, linef, merged_pcov);
					try {
						FileUtils.writeStringToFile(reduced_codef, reduced_code);
					} catch (Throwable t) {
						System.err.println(t);
						t.printStackTrace();
					}
				}
				// #region Get size
				int[] size_arr = getSize(prog_dpath, codefs);
				if (size_arr == null) { // cannot execute the getsize
					LogUtil.logInfo("Revert. Error when getting reduced program's size");
					bitvec[selected_index] = 1 - bitvec[selected_index];
					continue;
				}

				// Compute reduction
				float sred = -1;
				int[] lnum_arr = null;
				if (sred_type == 0) {
					lnum_arr = getTotalAndCoveredLineNumbers(merged_pcov); // Total and Covered lines
					sred = (float) (lnum_arr[0] - lnum_arr[1]) / (float) lnum_arr[0];
				} else if (sred_type == 1) {
					sred = (float) (size_arr[0] - size_arr[1]) / (float) size_arr[0];
				} else if (sred_type == 2) {
					sred = (float) (size_arr[4] - size_arr[5]) / (float) size_arr[4];
				}

				float ared = (float) (size_arr[2] - size_arr[3]) / (float) size_arr[2];
				if (ared < (float) 0.0) {
					ared = (float) 0.0;
				}

				float red = (float) ((1 - kr) * sred + kr * ared);

				// Compute generality
				bloated.clear();
				float gen = 0;
				for (int i = 0; i < tnum; i++) {
					if (bitvec[i] == 1 || merged_pcov.coversByLines(pcovs.get(i))) {
						gen += trace_count_map.get(i).intValue();

						// get bloated pathes
						if (bitvec[i] != 1) {
							bloated.add(i);
						}
					}
				}
				gen /= quan_num;

				// Path_tmp for count q(i+1,i)
				List<Integer> path_tmp = new ArrayList<Integer>();
				for (int i = 0; i < tnum; i++) {
					path_tmp.add(i);
				}

				if (base_inputs.size() != 0) {
					path_tmp.removeAll(base_inputs);
				}

				if (!bloated.isEmpty()) {
					path_tmp.removeAll(bloated);
				}
				next_accept_pro = 1.0 / path_tmp.size();

				// Compute oscore
				float oscore = (float) ((1 - w) * red + w * gen);

				// Compute dscore
				float dscore = (float) Math.exp(kvalue * oscore);

				// Compute dscore ratio
				double ratio = (dscore / curr_dscore) * (curr_accept_pro / next_accept_pro);
				
				StringBuilder score_sb = new StringBuilder();
				if (sred_type == 0) {
					score_sb.append("Origin #LOC: " + lnum_arr[0]);
					score_sb.append("\nReduced #LOC: " + lnum_arr[1]);
				} else if (sred_type == 1) {
					score_sb.append("Origin #Bytes: " + size_arr[0]);
					score_sb.append("\nReduced #Bytes: " + size_arr[1]);
				} else if (sred_type == 2) {
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
				// System.out.println(score_sb.toString());
				
				StringBuilder curr_rslt_sb = new StringBuilder();
				curr_rslt_sb.append("Trace Ids Covered: ");
				StringBuilder curr_id_sb = null;
				for (int i = 0; i < tnum; i++) {
					if (bitvec[i] == 1) {
						if (curr_id_sb == null) {
							curr_id_sb = new StringBuilder();
						} else {
							curr_id_sb.append(",");
						}
						curr_id_sb.append(i);
					}
				}
				if (curr_id_sb != null) {
					curr_rslt_sb.append(curr_id_sb.toString());
				}
				// System.out.println(curr_rslt_sb.toString());

				LogUtil.logInfo("Origin #Gadgets: " + size_arr[2]);
				LogUtil.logInfo("Reduced #Gadgets: " + size_arr[3]);
				LogUtil.logInfo("Size Reduction: " + sred);
				LogUtil.logInfo("AttkSurf Reduction: " + ared);
				LogUtil.logInfo("Reduction: " + red);
				LogUtil.logInfo("Generality: " + gen);
				LogUtil.logInfo("OScore: " + oscore);
				LogUtil.logInfo("DScore: " + dscore);
				LogUtil.logInfo("Trace Ids Covered: "+IntStream.range(0, bitvec.length)
						.filter(i -> bitvec[i] == 1)
						.mapToObj(String::valueOf)
						.collect(Collectors.joining(",")));
				// #endregion
				
				//#region Accept or not?
				// if (Math.random() < ratio && gen >0.0) {
				if (Math.random() < ratio) {
					LogUtil.logInfo("Accept.");
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

						for (int j = 0; j < tnum; j++) {
							best_bitvec[j] = bitvec[j];
						}
					}

					// Copy code file to output dir
					try {
						for (int idx = 0; idx != codefs.size(); idx++) {
							codef = codefs.get(idx);
							linef = linefs.get(codef);
						if(codef==null || linef==null){
							continue;
						}
							File reduced_codef = new File(
									codef.getAbsolutePath().replace(".c.cov.origin.c", ".c"));
							File output_codef_tosave = new File(code_output_dpath,
									codef.getName().replace(".cov.origin.c", ".c").replace(".c",String.format(".sample%d.c", samples)));
							File output_codef = new File(
									codef.getAbsolutePath().replace(".cov.origin.c", ".c").replace(".c", String.format(".sample%d.c", samples)));
							FileUtils.copyFile(reduced_codef, output_codef_tosave);
							FileUtils.copyFile(reduced_codef, output_codef);
						}

					} catch (Throwable t) {
						System.err.println(t);
						t.printStackTrace();
					}

					samples += 1;

					// Dynamic change search range
					path_index.clear();
					for (int i = 0; i < tnum; i++) {
						path_index.add(i);
					}

					if (base_inputs.size() != 0) {
						path_index.removeAll(base_inputs);
					}

					if (!bloated.isEmpty()) {
						path_index.removeAll(bloated);
					}

					// Accept_pro
					curr_accept_pro = next_accept_pro;
				
				} else {
					LogUtil.logInfo("Reject. Revert.");
					bitvec[selected_index] = 1 - bitvec[selected_index];
				}
				//#endregion
			}
			// #endregion

			// #region Report
			LogUtil.logInfo("**************************");
			LogUtil.logInfo("Best Sample Id: " + best_sample);
			LogUtil.logInfo("Best Size Reduction: " + best_sred);
			LogUtil.logInfo("Best AttkSurf Reduction: " + best_ared);
			LogUtil.logInfo("Best Reduction: " + best_red);
			LogUtil.logInfo("Best Generality: " + best_gen);
			LogUtil.logInfo("Best OScore: " + best_oscore);
			LogUtil.logInfo("Best DScore: " + best_dscore);
			LogUtil.logInfo("Best Trace Ids Covered: " +IntStream.range(0, best_bitvec.length)
					.filter(i -> best_bitvec[i] == 1)
					.mapToObj(String::valueOf)
					.collect(Collectors.joining(",")));
			LogUtil.logInfo("**************************");
			// #endregion Report

		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			LogUtil.closeLogger();
		}
	}

	public static Map<Integer, Integer> getTraceIdCountMap(File trace_count_f) {
		Map<Integer, Integer> trace_count_map = new HashMap<Integer, Integer>();
		List<String> lines = null;
		try {
			lines = FileUtils.readLines(trace_count_f);
			for (String line : lines) {
				String[] items = line.split(",", 4);
				trace_count_map.put(Integer.parseInt(items[0]), Integer.parseInt(items[2]));
			}

		} catch (Throwable t) {
			System.err.println(t);
			t.printStackTrace();
			System.exit(1);
		}
		return trace_count_map;
	}

	public static Map<Integer, String> getTraceIdFileIdMap(File trace_count_f) {
		Map<Integer, String> trace_file_map = new HashMap<Integer, String>();
		List<String> lines = null;
		try {
			lines = FileUtils.readLines(trace_count_f);
			for (String line : lines) {
				String[] item = line.split(",", 4);

				trace_file_map.put(Integer.parseInt(item[0]), item[0]);
			}

		} catch (Throwable t) {
			System.err.println(t);
			t.printStackTrace();
			System.exit(1);
		}

		return trace_file_map;
	}

	// Covf should be in gcov json format (generated by gcov -i)
	public static int getLineCount(File covf, String type) {
		String covf_ctnt = null;
		try {
			covf_ctnt = FileUtils.readFileToString(covf, (String) null);
		} catch (Throwable t) {
			System.err.println(t);
			t.printStackTrace();
		}
		if (covf_ctnt == null) {
			return -1;
		} else {
			return getLineCount(covf_ctnt, type);
		}
	}

	public static int getLineCount(String cov_str, String type) {
		if (cov_str == null) {
			return -1;
		}

		int n = 0;
		String[] cov_lines = cov_str.split("\n");
		for (String cov_line : cov_lines) {
			cov_line = cov_line.trim();
			if (cov_line.startsWith("lcount:")) {
				if ("full".equals(type)) {
					n += 1;
				} else if ("cov".equals(type)) {
					if (!cov_line.endsWith(",0")) {
						n += 1;
					}
				}
			}
		}
		return n;
	}

	// This should be called only AFTER .c is generated.
	private static int[] getSize(String prog_dpath, List<File> codefs) {
		DefaultExecutor executor = new DefaultExecutor();
		executor.setWorkingDirectory(new File(prog_dpath));
		ExecuteWatchdog watchdog = new ExecuteWatchdog(600000); // Timeout in 10min
		executor.setWatchdog(watchdog);

		// Run getsize.sh
		int exitValue = -1;
		try {
			exitValue = executor.execute(CommandLine.parse("./getsize.sh"));
		} catch (Throwable t) {
			System.err.println(t);
			t.printStackTrace();
		}
		if (exitValue != 0) {
			LogUtil.logWarning("Invalid Program");
			for (File codef : codefs) {
				File err_codef = new File(prog_dpath + "/errcode/" + codef.getName() + ".err" + errid + ".c"); // Make
																												// copy
																												// of
																												// err
																												// code
				try {
					FileUtils.copyFile(new File(codef.getAbsolutePath().replace(".cov.origin.c", ".c")),
							err_codef);
				} catch (Throwable t) {
					System.err.println(t);
					t.printStackTrace();
				}
			}
			errid += 1;
			return null;
		}

		// #region Parse size result
		File red_rsltf = new File(prog_dpath + "/size_rslt.txt");
		List<String> red_rsltf_lines = null;
		try {
			red_rsltf_lines = FileUtils.readLines(red_rsltf, (String) null);
		} catch (Throwable t) {
			System.err.println(t);
			t.printStackTrace();
		}
		if (red_rsltf_lines == null) {
			System.out.println("Unable to get size info for reduced program.");
			return null;
		}
		int origin_bytes = -1, red_bytes = -1,
				origin_gdt = -1, red_gdt = -1,
				origin_stmts = -1, red_stmts = -1;
		try {
			origin_bytes = Integer.parseInt(red_rsltf_lines.get(0).trim());
			red_bytes = Integer.parseInt(red_rsltf_lines.get(1).trim());
			origin_gdt = Integer.parseInt(red_rsltf_lines.get(2).trim());
			red_gdt = Integer.parseInt(red_rsltf_lines.get(3).trim());
			origin_stmts = Integer.parseInt(red_rsltf_lines.get(4).trim());
			red_stmts = Integer.parseInt(red_rsltf_lines.get(5).trim());
		} catch (Throwable t) {
			System.err.println(t);
			t.printStackTrace();
		}

		if (origin_bytes == -1 || red_bytes == -1 || origin_gdt == -1 || red_gdt == -1 ||
				origin_stmts == -1 || red_stmts == -1) {
			LogUtil.logWarning("Error parsing sizes for reduced program. There are -1 in size_rslt.txt");
			return null;
		}
		// #endregion

		// Return four ints
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
		// Map<Integer, Integer> lcmap = pcov.getLineCountMap();
		// Map<Integer, Integer> lcmap = new HashMap<>();
		int total = 0, cover = 0;
		// for (Integer l : lcmap.keySet()) {
		//	total += 1;
		//	if (lcmap.get(l).intValue() != 0) {
		//		cover += 1;
		//	}
		// }
		for (Map.Entry<String, FilePathCoverage> fileEntry : pcov.pcMap.entrySet()) {
			String filename = fileEntry.getKey();
			FilePathCoverage fileLcount = fileEntry.getValue();
			cover += fileLcount.lcountCovered.length;
			total += fileLcount.lcountAll.length;
		}
		return new int[] { total, cover };
	}

}
