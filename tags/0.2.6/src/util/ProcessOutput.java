package util;

/**
 * Stdout and stderr of a process.
 */
public class ProcessOutput {
	private int returnCode;
	private String stdout;
  private String stderr;

  public ProcessOutput(int returnCode, String stdout, String stderr) {
	  this.returnCode = returnCode;
	  this.stdout = stdout;
    this.stderr = stderr;
  }

	public int getReturnCode() {
		return returnCode;
	}

	public String getStdout() {
    return stdout;
  }

  public String getStderr() {
    return stderr;
  }
}
