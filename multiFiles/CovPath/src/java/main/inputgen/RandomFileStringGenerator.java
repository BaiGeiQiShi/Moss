package moss.covpath;

import org.apache.commons.lang3.RandomStringUtils;

public class RandomFileStringGenerator
{
    public static String getRandomString() {
	//Generate a file string
	int lnum = RandomGenerator.getUniformInt(1, 100);
	StringBuilder sb = null;
	for (int i=0; i<lnum; i++) {
	    if (sb == null) { sb = new StringBuilder(); }
	    else { sb.append("\n"); }
	    int cnum = RandomGenerator.getUniformInt(1, 100);
	    sb.append(RandomStringUtils.randomAscii(cnum));
	}
	return sb.toString();
    }
}
