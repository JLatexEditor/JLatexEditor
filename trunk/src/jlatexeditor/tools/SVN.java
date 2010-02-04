package jlatexeditor.tools;

import util.Pair;
import util.ProcessUtil;

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
    if(instance == null) instance = new SVN();
    return instance;
  }

  public synchronized ArrayList<UpdateResult> update(File dir) throws Exception {
    ArrayList<UpdateResult> results = new ArrayList<UpdateResult>();

    Process svn;
    try{
      svn = ProcessUtil.exec(new String[] {"svn", "--non-interactive", "update"}, dir);
    } catch(Exception e){
      e.printStackTrace();
      throw new Exception("SVN update failed!");
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(svn.getInputStream()), 100000);
    String line;
    while((line = in.readLine()) != null){
      char c = line.charAt(0);
      if(line.charAt(1) == ' ') {
        String fileName = line.substring(1).trim();
        File file = new File(dir, fileName);
        switch (c) {
          case('A') : results.add(new UpdateResult(file, UpdateResult.TYPE_ADD)); break;
          case('D') : results.add(new UpdateResult(file, UpdateResult.TYPE_DELETE)); break;
          case('U') : results.add(new UpdateResult(file, UpdateResult.TYPE_UPDATE));break;
          case('G') : results.add(new UpdateResult(file, UpdateResult.TYPE_MERGED)); break;
          case('C') : results.add(new UpdateResult(file, UpdateResult.TYPE_CONFLICT)); break;
          default: throw new Exception("Parsing SVN output failed: " + line + ".");
        }
      }
    }

    return results;
  }

  public synchronized Pair<Boolean,String> commit(File dir, String message) {
    message = message.replace('"', ' ');
    message = message.replace('\\', ' ');

    Process svn;
    try{
      svn = ProcessUtil.exec(new String[] {"svn", "--non-interactive", "commit", "-m", message}, dir);
    } catch(Exception e){
      e.printStackTrace();
      return new Pair<Boolean,String>(false, "<font color=red><b>Exception: " + e.getMessage() + "</b></font>");
    }

    boolean success = true;

    StringBuilder builder = new StringBuilder();
    
    BufferedReader in = new BufferedReader(new InputStreamReader(svn.getInputStream()), 100000);
    try{
      String line, lastLine = null;
      while((line = in.readLine()) != null){
        if(line.startsWith("svn:")) {
          builder.append("<font color=red><b>" + line + "</b></font><br>");
        } else {
          builder.append(line + "<br>");
        }
        if(line.startsWith("svn: Commit failed")) success = false;
        lastLine = line;
      }
      if(lastLine != null && !lastLine.startsWith("Committed revision")) success = false;
    } catch(IOException e){
      success = false;
      builder.append("<font color=red><b>Exception: " + e.getMessage() + "</b></font>");
    }

    return new Pair<Boolean,String>(success, builder.toString());
  }

  public synchronized ArrayList<StatusResult> status(File dir) throws Exception {
    ArrayList<StatusResult> results = new ArrayList<StatusResult>();

    Process svn;
    try{
      svn = ProcessUtil.exec(new String[] {"svn", "--show-updates", "status"}, dir);
    } catch(Exception e){
      e.printStackTrace();
      throw new Exception("SVN status failed!");
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(svn.getInputStream()), 100000);
    String line;
    while((line = in.readLine()) != null){
      char c = line.charAt(0);
      if(line.charAt(1) == ' ') {
        int serverStatus = line.charAt(7) == '*' ? StatusResult.SERVER_OUTDATED : StatusResult.SERVER_UP_TO_DATE;

        String revisionAndFile = line.substring(8).trim();
        int spaceIndex = revisionAndFile.indexOf(' ');
        String fileName = spaceIndex == -1 ? revisionAndFile : revisionAndFile.substring(spaceIndex).trim();
        File file = new File(dir, fileName);
        switch (c) {
          case('A') : results.add(new StatusResult(file, StatusResult.LOCAL_ADD, serverStatus)); break;
          case('D') : results.add(new StatusResult(file, StatusResult.LOCAL_DELETE, serverStatus)); break;
          case('M') : results.add(new StatusResult(file, StatusResult.LOCAL_MODIFIED, serverStatus));break;
          case('C') : results.add(new StatusResult(file, StatusResult.LOCAL_CONFLICT, serverStatus)); break;
          case('?') : results.add(new StatusResult(file, StatusResult.LOCAL_NOT_SVN, serverStatus)); break;
          case(' ') : results.add(new StatusResult(file, StatusResult.LOCAL_UNCHANGED, serverStatus)); break;
        }
      }
    }

    return results;
  }

  public synchronized void resolved(File file) {
    Process svn = null;
    try{
      svn = ProcessUtil.exec(new String[] {"svn", "resolved", file.getName()}, file.getParentFile());
      svn.waitFor();
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  public static class UpdateResult {
    public static final int TYPE_UPDATE   = 0;
    public static final int TYPE_MERGED   = 1;
    public static final int TYPE_ADD      = 2;
    public static final int TYPE_DELETE   = 3;
    public static final int TYPE_CONFLICT = 4;

    private File file;
    private int type;

    public UpdateResult(File file, int type) {
      this.file = file;
      this.type = type;
    }

    public File getFile() {
      return file;
    }

    public int getType() {
      return type;
    }
  }

  public static class StatusResult {
    public static final int LOCAL_ADD       = 0;
    public static final int LOCAL_DELETE    = 1;
    public static final int LOCAL_MODIFIED  = 2;
    public static final int LOCAL_CONFLICT  = 3;
    public static final int LOCAL_UNCHANGED = 4;
    public static final int LOCAL_NOT_SVN   = 5;

    public static final int SERVER_UP_TO_DATE = 0;
    public static final int SERVER_OUTDATED   = 1;

    private File file;
    private int localStatus;
    private int serverStatus;

    public StatusResult(File file, int localStatus, int serverStatus) {
      this.file = file;
      this.localStatus = localStatus;
      this.serverStatus = serverStatus;
    }

    public File getFile() {
      return file;
    }

    public int getLocalStatus() {
      return localStatus;
    }

    public int getServerStatus() {
      return serverStatus;
    }
  }
}
