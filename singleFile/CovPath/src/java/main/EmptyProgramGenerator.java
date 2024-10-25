package moss.covpath;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import org.apache.commons.io.FileUtils;


public class EmptyProgramGenerator
{
    public static void main(String[] args) {
	File codef = new File(args[0]);
	File linef = new File(args[1]);
	File covf = new File(args[2]); //An arbitrary coverage file of the program

	PathCoverage pcov = PathCoverageGenerator.getPathCoverage(covf);
	PathCoverage empty_pcov = PathCoverageGenerator.getPathCoverageWithZeroCounts(pcov);
	String empty_code = GCovBasedCodeRemover.getRemovedString(codef, linef, empty_pcov);
	System.out.println(empty_code);
    }
}
