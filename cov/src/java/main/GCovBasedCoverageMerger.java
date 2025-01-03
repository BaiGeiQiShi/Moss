package cov;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class GCovBasedCoverageMerger {
	public static void main(String[] args) {
		String type = args[0];
		List<File> cover_fs = new ArrayList<File>();
		for (int i = 1; i < args.length; i++) {
			File cover_df = new File(args[i]);
			if (cover_df.isFile()) {
				cover_fs.add(cover_df);
			} else if (cover_df.isDirectory()) {
				File[] cfs = cover_df.listFiles();
				for (File cf : cfs) {
					cover_fs.add(cf);
				}
			}
		}
		String cover_str = getCoverageString(cover_fs, type);
		System.out.println(cover_str);
	}

	// cover_f is an arbitrary coverage string (for the target program)
	public static String getCoverageStringWithZeroCounts(File cover_f) {
		StringBuilder rslt_sb = null;
		List<String> cover_flines = null;
		try {
			cover_flines = FileUtils.readLines(cover_f, (String) null);
		} catch (Throwable t) {
			System.err.println(t);
			t.printStackTrace();
		}
		if (cover_flines == null) {
			return null;
		}

		for (String cover_fline : cover_flines) {
			if (rslt_sb == null) {
				rslt_sb = new StringBuilder();
			} else {
				rslt_sb.append("\n");
			}

			if (cover_fline.startsWith("file:")) {
				rslt_sb.append(cover_fline);
			} else {
				String[] cover_fline_elems = cover_fline.split(",");
				for (int i = 0; i < cover_fline_elems.length; i++) {
					if (i != 0) {
						rslt_sb.append(",");
					}
					if (i == 1) {
						rslt_sb.append("0");
					} // Make it 0 count.
					else {
						rslt_sb.append(cover_fline_elems[i]);
					}
				}
			}
		}

		return (rslt_sb == null) ? null : rslt_sb.toString();
	}

	// cover_fs should only contain gcov files
	public static String getCoverageString(List<File> cover_fs, String type) {
		// Key is line# of the .gcov file, and value is the line content.
		Map<Integer, String> cmap = new HashMap<Integer, String>();
		for (File cover_f : cover_fs) {
			List<String> cover_flines = null;
			try {
				cover_flines = FileUtils.readLines(cover_f, (String) null);
			} catch (Throwable t) {
				System.err.println(t);
				t.printStackTrace();
			}
			if (cover_flines == null) {
				continue;
			}

			// Omit empty files
			boolean is_empty = true;
			for (String cover_fline : cover_flines) {
				if (!cover_fline.trim().equals("")) {
					is_empty = false;
					break;
				}
			}
			if (is_empty) {
				System.err.println("Ignored an empty gcov file: " + cover_f.getPath());
				continue;
			}

			int cover_flines_size = cover_flines.size();
			for (int i = 0; i < cover_flines_size; i++) {
				String curr_str = cmap.get(i);
				if (curr_str == null) {
					cmap.put(i, cover_flines.get(i).trim());
				} else {
					String cover_fline = cover_flines.get(i).trim();
					if (cover_fline.startsWith("file:") && curr_str.startsWith("file:")) {
						if (!cover_fline.equals(curr_str)) {
							System.err.println("Inconsistent file names:");
							System.err.println(cover_fline);
							System.err.println(curr_str);
							return null;
						}
					} else {
						String[] cover_fline_elems = cover_fline.split(",");
						String[] curr_str_elems = curr_str.split(",");
						if (!cover_fline_elems[0].equals(curr_str_elems[0])) {
							System.err.println("Inconsistent elems:");
							System.err.println(cover_fline_elems[0]);
							System.err.println(curr_str_elems[0]);
							return null;
						}
						else if(cover_fline_elems[0].equals("")){
							continue;
						}
						else {
							int cover_fline_count = Integer.parseInt(cover_fline_elems[1]);
							int curr_str_count = Integer.parseInt(curr_str_elems[1]);
							int new_count = -1;
							if ("binary".equals(type)) {
								new_count = ((cover_fline_count > 0) || (curr_str_count > 0)) ? 1 : 0;
							} else if ("real".equals(type)) {
								new_count = cover_fline_count + curr_str_count;
							} else {
								System.err.println("Unknown type: " + type);
								return null;
							}

							if (new_count != curr_str_count) { // Update the count
								String new_str = curr_str_elems[0] + "," + new_count;
								for (int j = 2; j < curr_str_elems.length; j++) {
									new_str += "," + curr_str_elems[j];
								}
								cmap.put(i, new_str);
							}

							// As a special case, when the line to be merged (i.e., cover_fline)
							// contains the label "inconsistent" in the end,
							// but the current merged line (i.e., curr_str) does not,
							// add this label to the line.
							if (cover_fline_elems.length == 4 &&
									curr_str_elems.length == 3 &&
									cover_fline_elems[3].equals("inconsistent")) { // Update to add mark
								cmap.put(i, cmap.get(i) + ",inconsistent");
							}
						}
					}
				}
			}
		}

		StringBuilder sb = null;
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			if (cmap.get(i) == null) {
				break;
			}

			if (i == 0) {
				sb = new StringBuilder();
			} else {
				sb.append("\n");
			}

			sb.append(cmap.get(i));
		}

		return (sb == null) ? "" : sb.toString();
	}
}
