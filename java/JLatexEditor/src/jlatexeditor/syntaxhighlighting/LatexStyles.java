
/**
 * @author Jörg Endrullis
 */

package jlatexeditor.syntaxhighlighting;

import sce.component.SCEDocument;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Map;

public class LatexStyles{
  public static final byte TEXT = 0;
  public static final byte COMMAND = 1;
  public static final byte COMMENT = 2;

  public static final byte BRACKET = 3;
  public static final byte PARENTHESES = 4;

  public static final byte IDENTIFIER = 5;
  public static final byte NUMBER = 6;

  public static final byte MATH = 7;

  public static final byte ERROR = Byte.MAX_VALUE;

  public static void addStyles(SCEDocument document){
    Font font = new Font("MonoSpaced", 0, 13);
    Font fontBold = new Font("MonoSpaced", Font.BOLD, 13);
    Font fontItalic = new Font("MonoSpaced", Font.ITALIC, 13);

    // Text
    Map<TextAttribute, Object> styleText = document.addStyle(TEXT, null);
    styleText.put(TextAttribute.FONT, font);
    styleText.put(TextAttribute.FOREGROUND, Color.BLACK);
    // Command
    Map<TextAttribute, Object> styleCommand = document.addStyle(COMMAND, styleText);
    styleCommand.put(TextAttribute.FONT, fontBold);
    styleCommand.put(TextAttribute.FOREGROUND, new Color(0, 0, 128));
    // Comment
    Map<TextAttribute, Object> styleComment = document.addStyle(COMMENT, styleText);
    styleComment.put(TextAttribute.FONT, fontItalic);
    styleComment.put(TextAttribute.FOREGROUND, new Color(128, 128, 128));

    // Bracket
    Map<TextAttribute, Object> styleBracket = document.addStyle(BRACKET, styleText);
    styleBracket.put(TextAttribute.FONT, fontBold);
    styleBracket.put(TextAttribute.FOREGROUND, new Color(102, 14, 122));
    // Parentheses
    Map<TextAttribute, Object> styleParentheses = document.addStyle(PARENTHESES, styleText);

    // Identifier
    Map<TextAttribute, Object> styleIdentifier = document.addStyle(IDENTIFIER, styleText);
    // Number
    Map<TextAttribute, Object> styleNumber = document.addStyle(NUMBER, styleText);
    styleNumber.put(TextAttribute.FOREGROUND, new Color(0, 0, 255));

    // Math
    Map<TextAttribute, Object> styleMath = document.addStyle(MATH, styleText);
    styleMath.put(TextAttribute.FONT, fontBold);
    styleMath.put(TextAttribute.FOREGROUND, new Color(0, 255, 0));

    // Error
    Map<TextAttribute, Object> styleError = document.addStyle(ERROR, styleText);
    styleError.put(TextAttribute.FOREGROUND, new Color(255, 0, 0));
  }
}
