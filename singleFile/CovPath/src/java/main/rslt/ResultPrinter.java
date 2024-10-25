package moss.covpath;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import org.apache.commons.io.FileUtils;
import java.text.DecimalFormat;

public class ResultPrinter
{
    public static void main(String[] args) {
	String progname = args[0];
	File logf = new File(args[1]);
	List<String> logflines = null;
	try { logflines = FileUtils.readLines(logf, (String) null); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (logflines == null) { return; }

	DecimalFormat df = new DecimalFormat("0.00");

	float red = -1f, gen = -1f, oscore = -1f;
	for (int i=logflines.size()-1; i>=0; i--) {
	    String logfline = logflines.get(i).trim();
	    if (logfline.startsWith("Best Reduction")) {
		red = Float.parseFloat(logfline.substring(logfline.indexOf(":")+1).trim());
	    }
	    else if (logfline.startsWith("Best Generality")) {
		gen = Float.parseFloat(logfline.substring(logfline.indexOf(":")+1).trim());
	    }
	    else if (logfline.startsWith("Best OScore")) {
		oscore = Float.parseFloat(logfline.substring(logfline.indexOf(":")+1).trim());
	    }

	    if (red != -1 && gen != -1 && oscore != -1) {
		break;
	    }
	}

	System.out.println(progname + "," + df.format(red)
			   + "," + df.format(gen) + "," + df.format(oscore));
    }
}
