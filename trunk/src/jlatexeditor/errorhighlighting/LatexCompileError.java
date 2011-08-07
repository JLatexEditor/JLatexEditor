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

	private int outputLine;
  private File file = null;
  private String fileName = null;
  private int lineStart = -1;
  private int lineEnd = -1;

  private String message = null;
  private String cause = null;
  private String textBefore = null;
  private String textAfter = null;
	private boolean followUpError;

  // Getter and Setter methods
  public int getType() {
    return type;
  }

	public String getTypeString() {
		switch (type) {
			case TYPE_ERROR: return "error";
			case TYPE_WARNING: return "warning";
			case TYPE_OVERFULL_HBOX: return "overfull hbox";
		}
		return null;
	}

  public void setType(int type) {
    this.type = type;
  }

	public int getOutputLine() {
		return outputLine;
	}

	public void setOutputLine(int outputLine) {
		this.outputLine = outputLine;
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

	public void setFollowUpError(boolean followUpError) {
		this.followUpError = followUpError;
	}

	public boolean isFollowUpError() {
		return followUpError;
	}

	private String normalFont() {
		return "<font color=\"#000000\">";
	}

	private String messageFont() {
		return "<font color=\"#9a6634\">";
	}

  public String toString() {
	  StringBuilder message = new StringBuilder(normalFont()).append(getFileName()).append("</font><font color=\"gray\">: ");
    if (getLineStart() != -1) {
      message.append(getLineStart());
      if (getLineStart() != getLineEnd()) message.append("--").append(getLineEnd());
    }
    message.append("</font>\n");
    message.append("  ").append(messageFont()).append(getMessage()).append("</font>\n");
    if (getTextBefore() != null) {
      message.append("  ").append(getTextBefore()).append("<font color=\"red\">[ERROR]</font>").append(getTextAfter()).append("\n");
    }
    return message.toString();
  }

	@Override
	public LatexCompileError clone() {
		LatexCompileError clone = new LatexCompileError();
		clone.type = type;

		clone.outputLine = outputLine;
		clone.file = file;
		clone.fileName = fileName;
		clone.lineStart = lineStart;
		clone.lineEnd = lineEnd;

		clone.message = message;
		clone.cause = cause;
		clone.textBefore = textBefore;
		clone.textAfter = textAfter;

		return clone;
	}
}
