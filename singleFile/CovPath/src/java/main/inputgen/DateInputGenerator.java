package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class DateInputGenerator
{
    //t0='08:17:48'
    //d0='1997-01-19'
    //d1="$d0 $t0 +0"
    //fmt="+%Y-%m-%d %T"
    //n_seconds=72057594037927935
    
    public static void main(String[] args) {
	String id = args[0];
	String output_arg_dpath = args[1];
        String output_file_dpath = args[2];

	String argstr = null;
	String filestr = null; //For --file, -f, & -r cases
	String ctgstr = null;

	int category = getRandom(0,68);
	ctgstr = ""+category;

	if (category == 0) {
	    argstr = "";
	}

	else if (category == 1) {
	    //--date '02/29/1996 1 year' +%Y-%m-%d
	    argstr = "--date '" + getRandomDay("slash");
	    int y = getRandom(1, 10);
	    argstr += " " + y + " years'";
	    argstr += " +%Y-%m-%d";
	}

	else if (category >= 2 && category <= 7) {
	    //--date '1995-1-1' +%U
	    argstr = "--date '" + getRandomDay("dash") + "'";
	    argstr += " +%U";
	}

	else if (category >= 8 && category <= 10) {
	    //--date '1995-1-1' +%V
	    argstr = "--date '" + getRandomDay("dash") + "'";
	    argstr += " +%V";
	}

	else if (category >= 11 && category <= 13) {
	    //--date '1995-1-1' +%W
	    argstr = "--date '" + getRandomDay("dash") + "'";
	    argstr += " +%W";
	}

	else if (category == 14) {
	    //--date '1998-1-1 3 years' +%Y
	    argstr = "--date '" + getRandomDay("dash");
	    int y = getRandom(1, 10);
	    argstr += " " + y + " years'";
	    argstr += " +%Y";
	}

	else if (category == 15) {
	    //-d 'TZ="America/Los_Angeles" 09:00 next Fri'
	    argstr = "-d 'TZ=\"America/Los_Angeles\"";
	    argstr += " " + getRandomTime(false);
	    argstr += " next " + getRandomWeekDay();
	    argstr += "'";
	}

	else if (category >= 16 && category <= 21) {
	    //-d "1997-01-19 08:17:48 +0 now" "+%Y-%m-%d %T"
	    argstr = "-d '";
	    argstr += getRandomTimeInD1Format();
	    
	    int rani = getRandom(0,5);
	    if (rani == 0) { argstr += " now'"; }
	    else if (rani == 1) { argstr += " yesterday'"; }
	    else if (rani == 2) { argstr += " tomorrow'"; }
	    else if (rani == 3) {
		int y = getRandom(1,10);
		if (y == 1) { argstr += " " + y + " year ago'"; }
		else { argstr += " " + y + " years ago'"; }
	    }
	    else if (rani == 4) {
		int m = getRandom(1,10);
		if (m == 1) { argstr += " " + m + " month ago'"; }
		else { argstr += " " + m + " months ago'"; }
	    }
	    else if (rani == 5) {
		int w = getRandom(1,10);
		if (w == 1) { argstr += " " + w + " week ago'"; }
		else { argstr += " " + w + " weeks ago'"; }
	    }
	    argstr += " '+%Y-%m-%d %T'";
	}

	else if	(category == 22) {
	    //--rfc-3339=ns -d'1970-01-01 00:00:00.2234567 UTC +961062237.987654321 sec'
	    argstr = "--rfc-3339=ns -d";
	    argstr += "'" + getRandomDay("dash");
	    argstr += " " + getRandomTime(true, true); //With decimal for sec
	    argstr += " UTC +";
	    for (int i=0; i<9; i++) { argstr += getRandom(0,9); }
	    argstr += ".";
	    for (int i=0; i<9; i++) { argstr += getRandom(0,9); }
	    argstr += " sec'";
	}

	else if	(category == 23) {
	    //-d '2005-03-27 +1 day' '+%Y'
	    argstr = "-d '" + getRandomDay("dash");
	    argstr += " +" + getRandom(1,10) + " day' '+%Y'";
	}

	else if	(category == 24) {
	    //-d @-22 +%05s
	    argstr = "-d @-";
	    argstr += getRandom(1,99);
	    argstr += " +%0" + getRandom(1,9) + "s";
	}

	else if	(category == 25) {
	    //-d @-22 +%_5s
	    argstr = "-d @-";
	    argstr += getRandom(1,99);
	    argstr += " +%_" + getRandom(1,9) + "s";
	}

	else if (category == 26) { //Special oracle needed!
	    //-d "72057594037927935" (seconds)
	    argstr = "-d ";
	    for (int i=0; i<17; i++) { argstr += getRandom(0,9); }
	}

	else if (category == 27) {
	    //-d 1999-12-08 +%_3d
	    argstr = "-d";
	    argstr += " " + getRandomDay("dash");
	    argstr += " +%_" + getRandom(1,9) + "d";
	}

	else if (category == 28) {
	    //-d 1999-12-08 +%03d
	    argstr = "-d";
	    argstr += " " + getRandomDay("dash");
	    argstr += " +%0" + getRandom(1,9) + "d";
	}

	else if (category == 29) {
	    //-d "1999-12-08 7:30" "+%^c"
	    argstr = "-d";
	    argstr += " '" + getRandomDay("dash");
	    argstr += " " + getRandomTime(false);
	    argstr += "' '+%^c'";
	}

	else if (category == 30) {
	    //--rfc-3339=ns -d "2038-01-19 03:14:07.999999999"
	    argstr = "--rfc-3339=";
	    argstr += "ns -d '";
	    argstr += getRandomDay("dash") + " " + getRandomTime(true,true) + "'";
	}
	
	else if (category == 31) {
	    //--rfc-3339=sec -d @31536000
	    argstr = "--rfc-3339=";
	    argstr += "sec -d @";
	    for (int i=0; i<8; i++) { argstr += getRandom(1,9); }
	}

	else if (category == 32) {
	    //--rfc-3339=date -d May-23-2003
	    argstr = "--rfc-3339=";
	    argstr += "date -d ";
	    argstr += getRandomDay("rdash");
	}

	else if (category == 33) {
	    //-d '1999-06-01' '+%3004Y' (a large padding for year)
	    argstr = "-d '" + getRandomDay("dash") + "'";
	    argstr += " '+%"+getRandom(0,5000)+"Y'";
	}

	else if (category == 34) {
	    //--utc -d '1970-01-01 UTC +961062237 sec'
	    argstr = "--utc -d '"+getRandomDay("dash");
	    argstr += " UTC +";
	    for (int i=0; i<9; i++) {
		argstr += getRandom(0,9);
	    }
	    argstr += " sec'";
	}

	else if (category == 35) {
	    //--utc -d '1970-01-01 00:00:00 UTC +961062237 sec'
	    argstr = "--utc -d '"+getRandomDay("dash");
	    argstr += " " + getRandomTime(true, false); //With sec but no decimal for sec
	    argstr += " UTC +";
	    for (int i=0; i<9; i++) {
		argstr += getRandom(0,9);
	    }
	    argstr += " sec'";
	}
	
	else if (category == 36) {
	    //-R -d '1997-01-19 08:17:48 +0'
	    argstr = "-R -d '";
	    argstr += getRandomTimeInD1Format() + "'";
	}

	else if (category == 37) {
	    //-d 000909 "$fmt"
	    argstr = "-d ";
	    for (int i=0; i<6; i++) {
                argstr += getRandom(0,9);
            }
	    argstr += " '+%Y-%m-%d %T'";
	}

	else if (category == 38 || category == 39) {
	    //-u -d '1996-11-10 0:00:00 +0' "$fmt"
	    argstr = "-u -d '";
	    argstr += getRandomTimeInD1Format();
	    argstr += "' '+%Y-%m-%d %T'";
	}

	else if (category == 40 || category == 41) {
	    //-d "$d1 4 seconds ago" "$fmt"
	    argstr = "-d";
	    argstr += " '" + getRandomTimeInD1Format();
	    argstr += " " + getRandom(0, 9);
	    argstr += " seconds ago'"; //Doesn't matter using plural format for all
	    argstr += " '" + getFMTString() + "'";
	}

	else if (category == 42) {
	    //-d '20050101  1 day' +%F
	    argstr = "-d '";
	    argstr += getRandomDay("compact") + " ";
	    argstr += getRandom(0,9) + "days'";
	    argstr += " +%F";
	}

	else if (category == 43) {
	    //-d '20050101 +1 day' +%F
	    argstr = "-d '";
	    argstr += getRandomDay("compact") + " ";
	    if (getRandom(0,1) == 1) { argstr += "+"; }
	    argstr += getRandom(0,9) + "days'";
	    argstr += " +%F";
	}

	else if (category >= 44 && category <= 50) {
	    //-d "$d1 next second" '+%Y-%m-%d %T'
	    //-d "$d1 next minute" '+%Y-%m-%d %T'
	    //-d "$d1 next hour" '+%Y-%m-%d %T'
	    //-d "$d1 next day" '+%Y-%m-%d %T'
	    //-d "$d1 next week" '+%Y-%m-%d %T'
	    //-d "$d1 next month" '+%Y-%m-%d %T'
	    //-d "$d1 next year" '+%Y-%m-%d %T'
	    argstr = "-d";
	    argstr += " '" + getRandomTimeInD1Format() + " next ";
	    int argi = getRandom(0,6);
	    if (argi == 0) { argstr += "second"; }
	    else if (argi == 1) { argstr += "minute"; }
	    else if (argi == 2) { argstr += "hour"; }
	    else if (argi == 3) { argstr += "day"; }
	    else if (argi == 4) { argstr += "week"; }
	    else if (argi == 5) { argstr += "month"; }
	    else if (argi == 6) { argstr += "year"; }
	    argstr += "' '+%Y-%m-%d %T'";
	}

	else if (category == 51) {
	    //-u -d '08/01/97 6:00' '+%D,%H:%M'
	    argstr = "-u -d";
	    argstr += " '" + getRandomDay("compact");
	    argstr += " " + getRandomTime(false);
	    argstr += "'";
	    argstr += " '+%D,%H:%M'";
	}

	else if (category == 52) {
	    //-u -d '08/01/97 6:00 UTC +4 hours' '+%D,%H:%M'
	    argstr = "-u -d";
	    argstr += " '" + getRandomDay("compact");
	    argstr += " " + getRandomTime(false);
	    argstr += " UTC +" + getRandom(0,9) + " hours'";
	    argstr += " '+%D,%H:%M'";
	}

	else if (category == 53 || category == 54) {
	    //-u --file=f '+%Y-%m-%d %T'
	    filestr = null;
	    int ln = getRandom(1,5); //1-5 lines of random time to be saved in file
	    for (int i=0; i<ln; i++) {
		if (filestr == null) { filestr = ""; }
		else { filestr += "\n"; }
		filestr += getRandomTimeInD1Format();
	    }
	    argstr = "-u --file= '+%Y-%m-%d %T'"; //Further step should replace "--file=" with "--file=FILE" where FILE is the file path
	}

	else if (category == 55) {
	    //-d '1970-01-01 00:00:01' +%s
	    argstr = "-d '";
	    argstr += getRandomDay("dash");
	    argstr += " " + getRandomTime(true, false);
	    argstr += "'";
	    argstr += " +%s";
	}

	else if (category == 56) {
	    //-d '1970-01-01 00:00:01 UTC +2 hours' +%s
	    argstr = "-d '";
	    argstr += getRandomDay("dash");
	    argstr += " " + getRandomTime(true, false);
	    argstr += " UTC +" + getRandom(0,9) + " hours'";
	    argstr += " +%s";
	}

	else if (category == 57) {
	    //-d 2000-01-01 +%s
	    argstr = "-d ";
	    argstr += getRandomDay("dash");
	    argstr += " +%s";
	}

	else if	(category == 58) {
	    //-d '1970-01-01 UTC 946684800 sec' +'%Y-%m-%d %T %z'
	    argstr = "-d '";
	    argstr += getRandomDay("dash");
	    argstr += " UTC ";
	    for (int i=0; i<9; i++) { argstr += getRandom(0,9); }
	    argstr += " sec'";
	    argstr += " '+%Y-%m-%d %T %z'";
	}

	else if (category >= 59 && category <= 63) {
	    //-d "$d0 $t0 this minute" "$fmt"
	    //-d "$d0 $t0 this hour" "$fmt"
	    //-d "$d0 $t0 this week" "$fmt"
	    //-d "$d0 $t0 this month" "$fmt"
	    //-d "$d0 $t0 this year" "$fmt"
	    argstr = "-d '";
	    argstr += getRandomDay("dash");
	    argstr += " " + getRandomTime(true, false);
	    argstr += " this ";
	    int argi = getRandom(0,4);
	    if (argi == 0) { argstr += "minute"; }
	    else if (argi == 1) { argstr += "hour"; }
	    else if (argi == 2) { argstr += "week"; }
	    else if (argi == 3) { argstr += "month"; }
	    else if (argi == 4) { argstr += "year"; }
	    argstr += "' '" + getFMTString() + "'";
	}

	else if (category >= 64 && category <= 66) {
	    //-d "$d1 1 day ago" "$fmt"
	    //-d "$d1 2 hours ago" "$fmt"
	    //-d "$d1 3 minutes ago" "$fmt"
	    argstr = "-d '";
	    argstr += getRandomTimeInD1Format();
	    argstr += " " + getRandom(0,9) + " ";
	    int argi = getRandom(0,2);
	    if (argi == 0) { argstr += "days"; }
	    else if (argi == 1) { argstr += "hours"; }
	    else if (argi == 2) { argstr += "minutes"; }
	    argstr += " ago'";
	    argstr += " '"+getFMTString()+"'";
	}

	else if (category == 67 || category == 68) {
	    //-f datefile
	    //-r datefile (display the last modification time)
	    filestr = null;
	    int ln = getRandom(1,5); //1-5 lines of random time to be saved in file
	    for (int i=0; i<ln; i++) {
		if (filestr == null) { filestr = ""; }
		else { filestr += "\n"; }
		filestr += getRandomTimeInD1Format();
	    }
	    if (category == 67) { argstr = "-f"; }
	    else if (category == 68) { argstr = "-r"; }

	    //Write to runarg${id}.txt
	    try { FileUtils.writeStringToFile(new File(output_file_dpath+"/runarg"+id+".txt"), id); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }

	}
	
	//Write to arg file
	if (argstr != null) {
	    try { FileUtils.writeStringToFile(new File(output_arg_dpath+"/"+id), argstr); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}	

	//Write to input content file
	if (filestr != null) {
	    try { FileUtils.writeStringToFile(new File(output_file_dpath+"/"+id), filestr); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}

	//Write to ctg${id}.txt
	if (ctgstr != null) {
	    try { FileUtils.writeStringToFile(new File(output_file_dpath+"/ctg"+id+".txt"), ctgstr); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	}
    }

    //Generate a random integer from [l,h]
    public static int getRandom(int l, int h) {
	return l + (int) ((h-l+1) * Math.random());
    }

    public static String getRandomDay(String format) {
	int y = getRandom(1970, 2050);
	int m = getRandom(1, 12);
	int d = getRandom(1, 31);

	String ystr = "" + y;
	String mstr = (m < 10) ? ("0" + m) : "" + m;
	String dstr = (d < 10) ? ("0" + d) : "" + d;
	if ("dash".equals(format)) {
	    return ystr + "-" + mstr + "-" + dstr;
	}
	else if ("compact".equals(format)) {
	    //E.g., 19970101
	    return "" + ystr + mstr + dstr;
	}
	else if ("slash".equals(format)) {
	    //E.g., 08/01/97
	    return mstr + "/" + dstr + "/" + ystr.substring(2);
	}
	else if ("rdash".equals(format)) {
	    //E.g., May-23-1995
	    if (m == 1) { mstr = "Jan"; }
	    else if (m == 2) { mstr = "Feb"; }
	    else if (m == 3) { mstr = "Mar"; }
	    else if (m == 4) { mstr = "Apr"; }
	    else if (m == 5) { mstr = "May"; }
	    else if (m == 6) { mstr = "Jun"; }
	    else if (m == 7) { mstr = "July"; } //Four-letter is fine
	    else if (m == 8) { mstr = "Aug"; }
	    else if (m == 9) { mstr = "Sep"; }
	    else if (m == 10) { mstr = "Oct"; }
	    else if (m == 11) { mstr = "Nov"; }
	    else if (m == 12) { mstr = "Dec"; }
	    return mstr + "-" + dstr + "-" + ystr;
	}
	else {
	    return ystr + "-" + mstr + "-" + dstr;
	}
    }

    public static String getRandomTime(boolean with_sec) {
	return getRandomTime(with_sec, false);
    }
    
    public static String getRandomTime(boolean with_sec, boolean with_decimal_for_sec) {
	String h = getTimeNumberString(getRandom(0, 23));
	String m = getTimeNumberString(getRandom(0, 59));

	if (with_sec) {
	    String s = getTimeNumberString(getRandom(0, 59));
	    String rslt = h + ":" + m + ":" + s;
	    if (with_decimal_for_sec) {
		for (int i=0; i<9; i++) {
		    if (i == 0) { rslt += "."; }
		    rslt += getRandom(0,9);
		}
	    }
	    return rslt;
	}
	else {
	    return h + ":" + m;
	}
    }

    public static String getTimeNumberString(int n) {
	if (n < 10) { return "0" + n; }
	else { return "" + n; }
    }

    public static String getRandomWeekDay() {
	int rani = getRandom(0,6);
	if (rani == 0) { return "Mon"; }
	else if (rani == 1) { return "Tue"; }
	else if (rani == 2) { return "Wed"; }
	else if (rani == 3) { return "Thu"; }
	else if (rani == 4) { return "Fri"; }
	else if (rani == 5) { return "Sat"; }
	else if (rani == 6) { return "Sun"; }
	return "Mon";
    }

    //$d1: 1997-01-19 08:17:48 +0
    public static String getRandomTimeInD1Format() {
	String rslt = getRandomDay("dash");
	rslt += " " + getRandomTime(true, false);
	rslt += " +" + getRandom(0, 9);
	return rslt;
    }

    public static String getFMTString() {
	return "+%Y-%m-%d %T";
    }
}
