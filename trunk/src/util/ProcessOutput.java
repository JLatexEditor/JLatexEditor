package util;

/**
 * Stdout and stderr of a process.
 */
public class ProcessOutput {
  private String stdout;
  private String stderr;

  public ProcessOutput(String stdout, String stderr) {
    this.stdout = stdout;
    this.stderr = stderr;
  }

  public String getStdout() {
    return stdout;
  }

  public String getStderr() {
    return stderr;
  }
}
