package jlatexeditor.tools;

import jlatexeditor.errorhighlighting.LatexCompileError;
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

  public synchronized ArrayList<Result> update(File dir) throws Exception {
    ArrayList<Result> results = new ArrayList<Result>();

    Process svn;
    try{
      svn = ProcessUtil.exec("svn --non-interactive update", dir);
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
          case('A') : results.add(new Result(file, Result.TYPE_ADD)); break;
          case('D') : results.add(new Result(file, Result.TYPE_DELETE)); break;
          case('U') : results.add(new Result(file, Result.TYPE_UPDATE));break;
          case('G') : results.add(new Result(file, Result.TYPE_MERGED)); break;
          case('C') : results.add(new Result(file, Result.TYPE_CONFLICT)); break;
          default: throw new Exception("Parsing SVN output failed: " + line + ".");
        }
      }
    }

    return results;
  }

  public synchronized boolean commit(File dir, String message) {
    message = message.replace('"', ' ');
    message = message.replace('\\', ' ');

    Process svn;
    try{
      svn = ProcessUtil.exec("svn --non-interactive commit -m \"" + message + "\"", dir);
    } catch(Exception e){
      e.printStackTrace();
      return false;
    }

    boolean success = true;

    BufferedReader in = new BufferedReader(new InputStreamReader(svn.getInputStream()), 100000);
    try{
      String line, lastLine = null;
      while((line = in.readLine()) != null){
        System.out.println(line);
        if(line.startsWith("svn: Commit failed")) success = false;
        lastLine = line;
      }
      if(lastLine != null && !lastLine.startsWith("Committed revision")) success = false;
    } catch(IOException ignored){
    }

    return success;
  }

  public synchronized void resolved(File file) {
    Process svn = null;
    try{
      svn = ProcessUtil.exec("svn resolved " + file.getName(), file.getParentFile());
      svn.waitFor();
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  public static class Result {
    public static final int TYPE_UPDATE   = 0;
    public static final int TYPE_MERGED   = 1;
    public static final int TYPE_ADD      = 2;
    public static final int TYPE_DELETE   = 3;
    public static final int TYPE_CONFLICT = 4;

    private File file;
    private int type;

    public Result(File file, int type) {
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
}
