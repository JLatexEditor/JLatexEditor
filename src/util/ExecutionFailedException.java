package util;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Thrown by SystemUtils.exec(Srting cmd) if the execution of the command failed.
 *
 * @see util.SystemUtils#exec(String)
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class ExecutionFailedException extends IOException {
  private int returnCode = 0;
  private ArrayList<String> errLines;

  public ExecutionFailedException() {
    super();
  }

  public ExecutionFailedException(String s) {
    super(s);
  }

  public ExecutionFailedException(String s, ArrayList<String> errLines) {
    super(s);
    this.errLines = errLines;
  }

  public ExecutionFailedException(int returnCode, String s, ArrayList<String> errLines) {
    super(s);
    this.returnCode = returnCode;
    this.errLines = errLines;
  }

  public int getReturnCode() {
    return returnCode;
  }

  public ArrayList<String> getErrLines() {
    return errLines;
  }
}
