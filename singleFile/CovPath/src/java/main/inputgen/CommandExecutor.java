package edu.gatech.cc.domgad;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;

public class CommandExecutor
{
    public static void executeCmd(String exec_dpath, String cmdstr) {
	//Generate the input tar file
	DefaultExecutor executor = new DefaultExecutor();
	executor.setWorkingDirectory(new File(exec_dpath));
	ExecuteWatchdog watchdog = new ExecuteWatchdog(30000); //Timeout in 30s
	executor.setWatchdog(watchdog);
	int exitValue = -1;
	try { exitValue = executor.execute(CommandLine.parse(cmdstr)); }
	catch (Throwable t) { System.err.println(t); t.printStackTrace(); }
	if (exitValue != 0) { System.err.println("Failed executing "+cmdstr); }
    }
}
