package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import org.apache.commons.io.FileUtils;

public class PathCounter
{
    public static void main(String[] args) {
	File path_dir = new File(args[0]);
	Map<String,List<String>> path_count_map = count(path_dir);
	Iterator<Map.Entry<String, List<String>>> itr = path_count_map.entrySet().iterator();
	int path_id = 0;
        while(itr.hasNext()) {
	    Map.Entry<String, List<String>> entry = itr.next();
	    List<String> list = entry.getValue();
	    StringBuilder listsb = null;
	    for (String inputid : list) {
		if (listsb == null) { listsb = new StringBuilder(); }
		else { listsb.append(","); }
		listsb.append(inputid);
	    }
	    System.out.println(path_id+","+list.size()+","+listsb.toString());
	    path_id += 1;
	    //for (String s : list) { System.out.print(s+" "); }
	    //System.out.println();
	    //System.out.println(entry.getValue().size());
        }
    }

    public static Map<String,List<String>> count(File path_d) {
	Map<String,List<String>> rslt_map = new HashMap<String,List<String>>();
	File[] path_fs = path_d.listFiles();
	for (File path_f : path_fs) {
	    String path_ctnt = null;
	    try { path_ctnt = FileUtils.readFileToString(path_f, (String) null); }
	    catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	    if (path_ctnt == null) { continue; }

	    String path_id = path_f.getName();
	    path_ctnt = path_ctnt.trim();
	    List<String> path_ids = rslt_map.get(path_ctnt);
	    if (path_ids == null) {
		path_ids = new ArrayList<String>();
		rslt_map.put(path_ctnt, path_ids);
	    }
	    path_ids.add(path_id);
	}
	return rslt_map;
    }
}
