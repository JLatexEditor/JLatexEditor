package util;

import de.endrullis.utils.StringUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java API for the command line tool aspell.
 * <p/>
 * A documentation about aspell can be found here:
 * http://aspell.net/man-html/Through-A-Pipe.html#Through-A-Pipe
 *
 * @author Stefan Endrullis
 */
public final class Aspell implements SpellChecker {
  public static String ASPELL_EXECUTABLE = "aspell";

  private static final Matcher masterDictMatcher = Pattern.compile("/([^/\\.]+)\\.multi").matcher("");

  private static boolean instanceFailed = false;
  private static HashMap<String, Aspell> instances = new HashMap<String, Aspell>();

  private String lang;
  private String langTag;

  private String lastWord = null;
  private HashSet<String> badWords = new HashSet<String>();

  private Process aspellProcess = null;
  private PrintStream aspellIn;
  private BufferedReader aspellOut;
  private BufferedReader aspellErr;
  private InputStream out;
	
  /** Words in the personal dictionary. */
  private HashSet<String> personalWords = new HashSet<String>();

  /** Windows is fucking shit! Step over to a real operating system. */
  private boolean fuckWindows = false;

  public static void main(String[] args) throws IOException {
    // print all available dictionaries
    System.out.println("available dictionaries");
    for (String dict : availableDicts()) {
      System.out.println(dict);
    }

    // start aspell with language "en" / "en_GB"
    Aspell aspell = new Aspell("en_GB");
    // test some words
    System.out.println("\ntest some words");
    System.out.println(aspell.check("the"));
    System.out.println(aspell.check("bla"));
    System.out.println(aspell.check("teh"));
    System.out.println(aspell.check("linebreak"));
    aspell.shutdown();

    aspell = new Aspell("de_DE");
    // test some words
    System.out.println("\ntest some words");
    System.out.println(aspell.check("Eingabemaske"));
    System.out.println(aspell.check("Eingabemenge"));
	  System.out.println(aspell.check("Bierbauch"));
    aspell.shutdown();
  }

  /**
   * Starts aspell with language "en" and language tag "en_GB".
   *
   * @throws IOException if aspell could not be started
   */
  public Aspell() throws IOException {
    this("en_GB");
  }

  /**
   * Creates the aspell wrapper that runs aspell in background.
   *
   * @param lang language, e.g. "en" or "en_GB"
   * @throws IOException if aspell could not be started
   */
  public Aspell(String lang) throws IOException {
    this(lang, lang);
  }

  /**
   * Creates the aspell wrapper that runs aspell in background.
   *
   * @param lang    language, e.g. "en"
   * @param langTag language tag, e.g. "en_GB"
   * @throws IOException if aspell could not be started
   */
  public Aspell(String lang, String langTag) throws IOException {
    this.lang = lang;
    this.langTag = langTag;
    startAspell(lang, langTag);
  }

  private void startAspell(String lang, String langTag) throws IOException {
    String[] aspellCommand = new String[]{
            ASPELL_EXECUTABLE,
            "-a",
            "--lang=" + lang,
            "--language-tag=" + langTag
    };

    aspellProcess = Runtime.getRuntime().exec(aspellCommand);

    // see if aspell died
    try {
      int exitValue = aspellProcess.exitValue();
      throw new IOException("Aspell failed to start / aborted with error code " + exitValue);
    } catch (IllegalThreadStateException ignored) {
    }

    aspellIn = new PrintStream(new BufferedOutputStream(aspellProcess.getOutputStream()), true);
    out = aspellProcess.getInputStream();
    aspellOut = new BufferedReader(new InputStreamReader(out));
    aspellErr = new BufferedReader(new InputStreamReader(aspellProcess.getErrorStream()));

    // read version line
    String version = aspellOut.readLine();
    if (version == null) throw new IOException("Aspell failed to start: " + aspellErr.readLine());

    if (version.contains("Aspell 0.5")) fuckWindows = true;

    personalWords.clear();
    personalWords.addAll(Arrays.asList(getPersonalWordList()));
  }

  private synchronized void restart() throws IOException {
    if(aspellProcess != null) aspellProcess.destroy();

    startAspell(lang, langTag);
  }

  /**
   * Check spelling of the word using aspell.
   *
   * @param word word to check
   * @return aspell result
   * @throws IOException thrown if execution of aspell failed
   */
  public synchronized Result check(String word) throws IOException {
    return check(word, true);
  }

  private synchronized Result check(String word, boolean guessBadWords) throws IOException {
	  if(badWords.contains(word)) return new Result();

    word = StringUtils.truncate(word);
    flushOut();
    aspellIn.println(word);
    aspellIn.flush();

    String line = aspellOut.readLine();

    if (line.equals("*") || line.startsWith("+") || line.startsWith("-")) {
      aspellOut.readLine();
      return new Result();
    } else if (line.startsWith("#")) {
      aspellOut.readLine();
      return new Result(new ArrayList<String>(0));
    } else if (line.startsWith("&")) {
      aspellOut.readLine();
      return new Result(Arrays.asList(line.split(": ")[1].split(", ")));
    } else {
      restart();

      // try to find out which word caused the problem
      if(guessBadWords) {
        String problemWord = null;

        for(String s : new String[] { lastWord, word }) {
          if(s == null) continue;

          try {
            // try the word multiple times
            for(int checkNr = 0; checkNr < 2; checkNr++) check(s, false);
          } catch (IOException e) {
            if(badWords.add(s)) problemWord = s;
          }
        }

        if(problemWord != null) {
          System.err.println("Warning: aspell has troubles with the word \"" + problemWord + "\".");
          return check(word, true);
        }
      }

      throw new IOException("unknown aspell answer: " + line);
    }
  }

  /**
   * Adds the word to the aspell user dictionary.
   *
   * @param word word to add
   */
  public synchronized void addToPersonalDict(String word) {
	  word = StringUtils.truncate(word);
    aspellIn.println("*" + word);
    aspellIn.println("#");
    aspellIn.flush();

    personalWords.add(word);
  }

  public synchronized void removeFromPersonalDict(String word) throws IOException {
	  word = StringUtils.truncate(word);
    // remove word from personal dict
    File homeDir = new File(System.getProperty("user.home"));
    String personalDictName = getOption("personal");
    File personalDict = new File(homeDir, personalDictName);
    File newPersonalDict = new File(homeDir, personalDictName + "_new");
    BufferedReader r = new BufferedReader(new FileReader(personalDict));
    PrintStream w = new PrintStream(new FileOutputStream(newPersonalDict));

    String line;
    while ((line = r.readLine()) != null) {
      if (!line.equals(word)) {
        w.println(line);
      }
    }

    newPersonalDict.renameTo(personalDict);

    String masterLang = getMasterLang();
    shutdown();
    startAspell(masterLang, masterLang);

    personalWords.remove(word);
  }

  /**
   * Returns the value of the given option.
   * For a list of aspell options see
   * <a href="http://aspell.net/man-html/The-Options.html#The-Options">http://aspell.net/man-html/The-Options.html#The-Options</a>.
   *
   * @param option option name
   * @return value
   * @throws IOException
   */
  public synchronized String getOption(String option) throws IOException {
    return call("$$cr " + option);
  }

  /**
   * Sets the value of the given option.
   * For a list of aspell options see
   * <a href="http://aspell.net/man-html/The-Options.html#The-Options">http://aspell.net/man-html/The-Options.html#The-Options</a>.
   *
   * @param option option name
   * @param value  value
   */
  public synchronized void setOption(String option, String value) {
    aspellIn.println("$$cs " + option + "," + value);
    aspellIn.flush();
  }

  public void setLang(String lang) {
    setOption("lang", lang);
  }

  public String getLang() throws IOException {
    return getOption("lang");
  }

  public void setLanguageTag(String languageTag) {
    setOption("language-tag", languageTag);
  }

  public String getLanguageTag() throws IOException {
    return getOption("language-tag");
  }

  public String getMasterLang() throws IOException {
    masterDictMatcher.reset(getOption("master"));
    masterDictMatcher.find();
    return masterDictMatcher.group(1);
  }

  public void addReplacement(String misspelling, String correction) {
    aspellIn.println("$$ra " + misspelling + "," + correction);
    aspellIn.flush();
  }

  public String[] getPersonalWordList() throws IOException {
    if (fuckWindows) {
      return new String[]{};
    }
    String listString = call("$$pp");
    listString = listString.substring(listString.indexOf(':') + 1).trim();
    return listString.split(", ");
  }

  public String[] getSessionWordList() throws IOException {
    String listString = call("$$ps");
    listString = listString.substring(listString.indexOf(':') + 1).trim();
    return listString.split(", ");
  }

  private String call(String input) throws IOException {
    flushOut();
    aspellIn.println(input);
    aspellIn.flush();
    return aspellOut.readLine();
  }

  private void flushOut() throws IOException {
    while (out.available() > 0) aspellOut.readLine();
  }

  public HashSet<String> getPersonalWords() {
    return personalWords;
  }

  /**
   * Shutdown aspell.
   */
  public void shutdown() {
    try {
      aspellIn.close();
      aspellOut.close();
      aspellErr.close();
    } catch (Exception ignored) {
    }
  }

  public static Aspell getInstance(String lang) {
    if (instanceFailed) return null;

    Aspell instance = instances.get(lang);
    if (instance == null) {
      try {
        instance = new Aspell(lang);
        instances.put(lang, instance);
      } catch (IOException e) {
        instanceFailed = true;
        System.err.println("Warning: Failed to initialize spell checker 'aspell':");
        System.err.println("  " + e.getMessage());
      }
    }

    return instance;
  }

  /**
   * Returns the available dictionaries provided by aspell.
   *
   * @return list of dictionaries provided by aspell
   * @throws IOException thrown if execution of aspell failed
   */
  public static List<String> availableDicts() throws IOException {
    Process process = ProcessUtil.exec(new String[]{
            ASPELL_EXECUTABLE,
            "dump",
            "dicts"
    }, new File(System.getProperty("user.dir")));
    BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));

    List<String> dicts = new ArrayList<String>();
    String line;
    while ((line = r.readLine()) != null) dicts.add(line);

    return dicts;
  }
}
