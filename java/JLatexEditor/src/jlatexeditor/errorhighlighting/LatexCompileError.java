/**
 * @author Jörg Endrullis
 */

package jlatexeditor.errorhighlighting;

public class LatexCompileError{
  private int line = -1;
  private String error = null;
  private String help = null;
  private String text_before = null;
  private String text_after = null;

  // Getter and Setter methods
  public int getLine(){
    return line;
  }

  public void setLine(int line){
    this.line = line;
  }

  public String getError(){
    return error;
  }

  public void setError(String error){
    this.error = error;
  }

  public String getHelp(){
    return help;
  }

  public void setHelp(String help){
    this.help = help;
  }

  public String getTextBefore(){
    return text_before;
  }

  public void setTextBefore(String text_before){
    this.text_before = text_before;
  }

  public String getTextAfter(){
    return text_after;
  }

  public void setTextAfter(String text_after){
    this.text_after = text_after;
  }

  public String toString(){
    String message = "Fehler in Zeile " + line + ":\n";
    message += "  " + error + "\n";
    message += "  " + text_before + "[ERROR]" + text_after + "\n";
    message += "Hinweis: \n  " + help.trim();
    return message;
  }
}
