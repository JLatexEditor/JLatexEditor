/**
 * @author Jörg Endrullis
 */

package jlatexeditor.errorhighlighting;

import java.io.File;

public class LatexCompileError {
  public static final int TYPE_ERROR = 0;
  public static final int TYPE_WARNING = 1;
  public static final int TYPE_OVERFULL_HBOX = 2;

  public static int errorType2markerType(int type) {
    return type;
  }

  private int type = TYPE_ERROR;

  private File file = null;
  private String fileName = null;
  private int lineStart = -1;
  private int lineEnd = -1;

  private String message = null;
  private String command = null;
  private String textBefore = null;
  private String textAfter = null;

  // Getter and Setter methods
  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public File getFile() {
    return file;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFile(File file, String fileName) {
    this.file = file;
    this.fileName = fileName;
  }

  public int getLineStart() {
    return lineStart;
  }

  public int getLineEnd() {
    return lineEnd;
  }

  public void setLine(int line) {
    lineStart = line;
    lineEnd = line;
  }

  public void setLineStart(int lineStart) {
    this.lineStart = lineStart;
  }

  public void setLineEnd(int lineEnd) {
    this.lineEnd = lineEnd;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String error) {
    this.message = error;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getTextBefore() {
    return textBefore;
  }

  public void setTextBefore(String textBefore) {
    this.textBefore = textBefore;
  }

  public String getTextAfter() {
    return textAfter;
  }

  public void setTextAfter(String textAfter) {
    this.textAfter = textAfter;
  }

  public String toString() {
    StringBuffer message = new StringBuffer(getFileName()).append(": ");
    if (getLineStart() != -1) {
      message.append(getLineStart());
      if (getLineStart() != getLineEnd()) message.append("--").append(getLineEnd());
    }
    message.append("\n");
    message.append("  ").append(getMessage()).append("\n");
    if (getTextBefore() != null) {
      message.append("  ").append(getTextBefore()).append("[ERROR]").append(getTextAfter()).append("\n");
    }
    return message.toString();
  }
}