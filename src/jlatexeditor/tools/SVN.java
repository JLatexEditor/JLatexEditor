package jlatexeditor.tools;

import util.ProcessUtil;
import util.StreamUtils;
import util.Tuple;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * SVN utilities.
 */
public class SVN {
  private static SVN instance = null;

  public static SVN getInstance() {
    if (instance == null) instance = new SVN();
    return instance;
  }

  public synchronized ArrayList<UpdateResult> update(File dir) throws Exception {
    ArrayList<UpdateResult> results = new ArrayList<UpdateResult>();

    Process svn;
		svn = ProcessUtil.exec(new String[]{"svn", "--non-interactive", "update"}, dir);

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

    Process svn = ProcessUtil.exec(new String[]{"svn", "--non-interactive", "commit", "-m", message}, dir);

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
              ProcessUtil.exec(new String[]{"svn", "--non-interactive", "--show-updates", "status"}, dir) :
              ProcessUtil.exec(new String[]{"svn", "--non-interactive", "status"}, dir);
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
        int serverStatus =
                line.substring(0,cutColumn).indexOf('*') >= 0
                        || line.substring(0,cutColumn).indexOf('!') >= 0 ?
                        StatusResult.SERVER_OUTDATED : StatusResult.SERVER_UP_TO_DATE;

        String revisionAndFile = line.substring(cutColumn).trim();
        int spaceIndex = revisionAndFile.indexOf(' ');
        String fileName = spaceIndex == -1 ? revisionAndFile : revisionAndFile.substring(spaceIndex).trim();
        File file = new File(dir, fileName);
        switch (c) {
          case ('A'):
            results.add(new StatusResult(file, fileName, StatusResult.LOCAL_ADD, serverStatus));
            break;
          case ('D'):
            results.add(new StatusResult(file, fileName, StatusResult.LOCAL_DELETE, serverStatus));
            break;
          case ('M'):
            results.add(new StatusResult(file, fileName, StatusResult.LOCAL_MODIFIED, serverStatus));
            break;
          case ('C'):
            results.add(new StatusResult(file, fileName, StatusResult.LOCAL_CONFLICT, serverStatus));
            break;
          case ('?'):
            results.add(new StatusResult(file, fileName, StatusResult.LOCAL_NOT_SVN, serverStatus));
            break;
          case (' '):
            results.add(new StatusResult(file, fileName, StatusResult.LOCAL_UNCHANGED, serverStatus));
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

  public synchronized void resolved(File file) {
    Process svn = null;
    try {
      svn = ProcessUtil.exec(new String[]{"svn", "--non-interactive", "resolved", file.getName()}, file.getParentFile());
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
    public static final int LOCAL_ADD = 0;
    public static final int LOCAL_DELETE = 1;
    public static final int LOCAL_MODIFIED = 2;
    public static final int LOCAL_CONFLICT = 3;
    public static final int LOCAL_UNCHANGED = 4;
    public static final int LOCAL_NOT_SVN = 5;

    public static final int SERVER_UP_TO_DATE = 0;
    public static final int SERVER_OUTDATED = 1;

    private File file;
    private String relativePath;
    private int localStatus;
    private int serverStatus;

    public StatusResult(File file, String relativePath, int localStatus, int serverStatus) {
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

    public int getLocalStatus() {
      return localStatus;
    }

    public int getServerStatus() {
      return serverStatus;
    }
  }
}
