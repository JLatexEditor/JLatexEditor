package jlatexeditor.tools;

import jlatexeditor.gproperties.GProperties;
import util.ProcessUtil;
import util.StreamUtils;
import util.SystemUtils;
import util.Tuple;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * SVN utilities.
 */
public class SVN {
  private static SVN instance = null;
  private static HashMap<String,String> defaultEnv = new HashMap<String, String>() {{
	  if (SystemUtils.isMacOS()) {
      put("LC_CTYPE", "UTF-8");
	  }
  }};

  public static SVN getInstance() {
    if (instance == null) instance = new SVN();
    return instance;
  }
	
	public synchronized boolean isDirUnderVersionControl(File dir) {
		return new File(dir, ".svn").exists();
	}

  public synchronized ArrayList<UpdateResult> update(File dir) throws Exception {
    ArrayList<UpdateResult> results = new ArrayList<UpdateResult>();

    Process svn;
		svn = ProcessUtil.exec(new String[]{GProperties.getString("svn.executable"), "--non-interactive", "update"}, dir, defaultEnv);

    BufferedReader in = new BufferedReader(new InputStreamReader(svn.getInputStream()), 100000);
    String line;
    while ((line = in.readLine()) != null) {
      char c = line.charAt(0);
      if (line.charAt(1) == ' ') {
        String fileName = line.substring(1).trim();
        File file = new File(dir, fileName);
        switch (c) {
          case ('A'):
            results.add(new UpdateResult(file, UpdateResult.Type.add));
            break;
          case ('D'):
            results.add(new UpdateResult(file, UpdateResult.Type.delete));
            break;
          case ('U'):
            results.add(new UpdateResult(file, UpdateResult.Type.update));
            break;
          case ('G'):
            results.add(new UpdateResult(file, UpdateResult.Type.merged));
            break;
          case ('C'):
            results.add(new UpdateResult(file, UpdateResult.Type.conflict));
            break;
          default:
            throw new Exception("Parsing SVN output failed: " + line + ".");
        }
      }
    }

	  checkProcessResult(svn, "SVN update");

    return results;
  }

	public synchronized Tuple<Boolean, String> commit(File dir, String message) throws Exception {
    message = message.replace('"', ' ');
    message = message.replace('\\', ' ');

    Process svn = ProcessUtil.exec(new String[]{GProperties.getString("svn.executable"), "--non-interactive", "commit", "-m", message}, dir, defaultEnv);

		boolean success = true;

    StringBuilder builder = new StringBuilder();

		int linesCount = 0;

    BufferedReader in = new BufferedReader(new InputStreamReader(svn.getInputStream()), 100000);
    try {
      String line, lastLine = null;
      while ((line = in.readLine()) != null) {
				if (line.startsWith("svn:")) {
					builder.append("<font color=red><b>" + line + "</b></font><br>");
				} else {
					linesCount++;
		      // limit the number of content lines shown in the svn popup to 30
					if (linesCount <= 30) {
						builder.append(line + "<br>");
					} else if (linesCount == 31) {
						builder.append("...<br>");
					}
				}
        if (line.startsWith("svn: Commit failed")) success = false;
        lastLine = line;
      }
      if (lastLine != null && !lastLine.startsWith("Committed revision")) success = false;
    } catch (IOException e) {
      success = false;
      builder.append("<font color=red><b>Exception: " + e.getMessage() + "</b></font>");
    }

		checkProcessResult(svn, "SVN update");

    return new Tuple<Boolean, String>(success, builder.toString());
  }

  public synchronized ArrayList<StatusResult> status(File dir, boolean remote) throws Exception {
    ArrayList<StatusResult> results = new ArrayList<StatusResult>();

    Process svn;
    try {
      svn = remote ?
              ProcessUtil.exec(new String[]{GProperties.getString("svn.executable"), "--non-interactive", "--show-updates", "status"}, dir, defaultEnv) :
              ProcessUtil.exec(new String[]{GProperties.getString("svn.executable"), "--non-interactive", "status"}, dir, defaultEnv);
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception("SVN status failed!", e);
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(svn.getInputStream()), 100000);
    String line;
    while ((line = in.readLine()) != null) {
      char c = line.charAt(0);
      if (line.charAt(1) == ' ') {
        int cutColumn = remote ? 10 : 8;
        StatusResult.Server serverStatus = StatusResult.Server.upToDate;
        if(line.substring(0,cutColumn).indexOf('*') >= 0) serverStatus = StatusResult.Server.outdated;
        if(line.substring(0,cutColumn).indexOf('!') >= 0) serverStatus = StatusResult.Server.addOrDelete;

        String revisionAndFile = line.substring(cutColumn).trim();
        int spaceIndex = revisionAndFile.indexOf(' ');
        String fileName = spaceIndex == -1 ? revisionAndFile : revisionAndFile.substring(spaceIndex).trim();
        File file = new File(dir, fileName);
        switch (c) {
          case ('A'):
            results.add(new StatusResult(file, fileName, StatusResult.Local.add, serverStatus));
            break;
          case ('D'):
            results.add(new StatusResult(file, fileName, StatusResult.Local.delete, serverStatus));
            break;
          case ('M'):
            results.add(new StatusResult(file, fileName, StatusResult.Local.modified, serverStatus));
            break;
          case ('C'):
            results.add(new StatusResult(file, fileName, StatusResult.Local.conflict, serverStatus));
            break;
          case ('?'):
            results.add(new StatusResult(file, fileName, StatusResult.Local.notInSvn, serverStatus));
            break;
          case (' '):
            results.add(new StatusResult(file, fileName, StatusResult.Local.unchanged, serverStatus));
            break;
        }
      }
    }

    // in case of errors, try without remove status
    if(remote && results.size() == 0) {
      try {
        BufferedReader errIn = new BufferedReader(new InputStreamReader(svn.getErrorStream()), 100000);
        String err = errIn.readLine();
        errIn.close();

        if(err != null && err.startsWith("svn: ")) {
          return status(dir, false);
        }
      } catch (Throwable e) { /* ignore */ }
    }

    return results;
  }

	/**
	 * Adds a file to svn.
	 *
	 * @param file file to add
	 */
	public synchronized void add(File file) {
	  try {
		  Process svn = ProcessUtil.exec(new String[]{GProperties.getString("svn.executable"), "--non-interactive", "--parents", "add", file.getName()}, file.getParentFile(), defaultEnv);
	    svn.waitFor();
	  } catch (Exception e) {
	    e.printStackTrace();
	  }
	}

  public synchronized void resolved(File file) {
    Process svn;
    try {
      svn = ProcessUtil.exec(new String[]{GProperties.getString("svn.executable"), "--non-interactive", "resolved", file.getName()}, file.getParentFile(), defaultEnv);
      svn.waitFor();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

	/**
	 * Check if the return code of the svn process is OK.
	 *
	 * @param process svn process
	 */
	private void checkProcessResult(Process process, String action) throws Exception {
		String errorString = new String(StreamUtils.readBytesFromInputStream(process.getErrorStream()));
		if (!errorString.equals("")) {
			throw new Exception(action + " failed due to the following error:\n" + errorString);
		}
	}

  public static class UpdateResult {
	  public enum Type { update, merged, add, delete, conflict }

    private File file;
    private Type type;

    public UpdateResult(File file, Type type) {
      this.file = file;
      this.type = type;
    }

    public File getFile() {
      return file;
    }

    public Type getType() {
      return type;
    }
  }

  public static class StatusResult {
	  public enum Local { add, delete, modified, conflict, unchanged, notInSvn }
	  public enum Server { upToDate, outdated, addOrDelete }

    private File file;
    private String relativePath;
    private Local localStatus;
    private Server serverStatus;

    public StatusResult(File file, String relativePath, Local localStatus, Server serverStatus) {
      this.file = file;
      this.relativePath = relativePath;
      this.localStatus = localStatus;
      this.serverStatus = serverStatus;
    }

    public File getFile() {
      return file;
    }

    public String getRelativePath() {
      return relativePath;
    }

    public Local getLocalStatus() {
      return localStatus;
    }

    public Server getServerStatus() {
      return serverStatus;
    }
  }
}
