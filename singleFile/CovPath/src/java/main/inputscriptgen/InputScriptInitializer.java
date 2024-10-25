package moss.covpath;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import org.apache.commons.io.FileUtils;

class InputScriptInitializer
{
    public static String getInputString() {
	return "#!/bin/bash\n\nBIN=$1\nOUTDIR=$2\nTIMEOUT=$3\nINDIR=$4\n\n";
    }
}
