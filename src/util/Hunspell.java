package util;

import de.endrullis.utils.StringUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java API for the command line tool hunspell.
 *
 * @author Stefan Endrullis
 */
public final class Hunspell implements SpellChecker {
  public static String HUNSPELL_EXECUTABLE = "hunspell";

  private static final Matcher masterDictMatcher = Pattern.compile("/([^/\\.]+)\\.multi").matcher("");

  private static boolean instanceFailed = false;
  private static HashMap<String, Hunspell> instances = new HashMap<String, Hunspell>();

  private PrintStream hunspellIn;
  private BufferedReader hunspellOut;
  private BufferedReader hunspellErr;
  private InputStream out;

  /** Words in the personal dictionary. */
  private HashSet<String> personalWords = new HashSet<String>();


  public static void main(String[] args) throws IOException {
    // print all available dictionaries
    System.out.println("available dictionaries");
    for (String dict : availableDicts()) {
      System.out.println(dict);
    }
    // start hunspell with language "en" / "en_GB"
    Hunspell hunspell = new Hunspell("en_GB");
    // test some words
    System.out.println("\ntest some words");
    System.out.println(hunspell.check("the"));
    System.out.println(hunspell.check("bla"));
    System.out.println(hunspell.check("teh"));
    System.out.println(hunspell.check("linebreak"));
    hunspell.shutdown();

    hunspell = new Hunspell("de_DE");
    // test some words
    System.out.println("\ntest some words");
    System.out.println(hunspell.check("Eingabemaske"));
    System.out.println(hunspell.check("Eingabemenge"));
    System.out.println(hunspell.check("Bierbauch"));
    hunspell.shutdown();
  }

  /**
   * Starts hunspell with language "en" and language tag "en_GB".
   *
   * @throws java.io.IOException if hunspell could not be started
   */
  public Hunspell() throws IOException {
    this("en_GB");
  }

  /**
   * Creates the hunspell wrapper that runs hunspell in background.
   *
   * @param lang language, e.g. "en" or "en_GB"
   * @throws java.io.IOException if hunspell could not be started
   */
  public Hunspell(String lang) throws IOException {
    startAspell(lang, lang);
  }

  /**
   * Creates the hunspell wrapper that runs hunspell in background.
   *
   * @param lang    language, e.g. "en"
   * @param langTag language tag, e.g. "en_GB"
   * @throws java.io.IOException if hunspell could not be started
   */
  public Hunspell(String lang, String langTag) throws IOException {
    startAspell(lang, langTag);
  }

  private void startAspell(String lang, String langTag) throws IOException {
    String[] hunspellCommand = new String[]{
		    HUNSPELL_EXECUTABLE,
				"-a",
				"-d",
				lang,
    };

    Process hunspellProcess = Runtime.getRuntime().exec(hunspellCommand);

    // see if hunspell died
    try {
      int exitValue = hunspellProcess.exitValue();
      throw new IOException("Aspell failed to start / aborted with error code " + exitValue);
    } catch (IllegalThreadStateException ignored) {
    }

    hunspellIn = new PrintStream(new BufferedOutputStream(hunspellProcess.getOutputStream()), true);
    out = hunspellProcess.getInputStream();
    hunspellOut = new BufferedReader(new InputStreamReader(out));
    hunspellErr = new BufferedReader(new InputStreamReader(hunspellProcess.getErrorStream()));

    // read version line
    String version = hunspellOut.readLine();
    if (version == null) throw new IOException("Aspell failed to start: " + hunspellErr.readLine());

    personalWords.clear();
    personalWords.addAll(Arrays.asList(getPersonalWordList()));
  }

  /**
   * Check spelling of the word using hunspell.
   *
   * @param word word to check
   * @return hunspell result
   * @throws java.io.IOException thrown if execution of hunspell failed
   */
  public synchronized Result check(String word) throws IOException {
    flushOut();
    hunspellIn.println(word);
    hunspellIn.flush();

    String line = hunspellOut.readLine();

    if (line.equals("*") || line.startsWith("+") || line.startsWith("-")) {
      hunspellOut.readLine();
      return new Result();
    } else if (line.startsWith("#")) {
      hunspellOut.readLine();
      return new Result(new ArrayList<String>(0));
    } else if (line.startsWith("&")) {
      hunspellOut.readLine();
      return new Result(Arrays.asList(line.split(": ")[1].split(", ")));
    } else {
      throw new RuntimeException("unknown hunspell answer: " + line);
    }
  }

  /**
   * Adds the word to the hunspell user dictionary.
   *
   * @param word word to add
   */
  public synchronized void addToPersonalDict(String word) {
    hunspellIn.println("*" + word);
    hunspellIn.println("#");
    hunspellIn.flush();

    personalWords.add(word);
  }

  public synchronized void removeFromPersonalDict(String word) throws IOException {
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
   *
   * @param option option name
   * @return value
   * @throws java.io.IOException
   */
  public synchronized String getOption(String option) throws IOException {
    return call("$$cr " + option);
  }

  /**
   * Sets the value of the given option.
   *
   * @param option option name
   * @param value  value
   */
  public synchronized void setOption(String option, String value) {
    hunspellIn.println("$$cs " + option + "," + value);
    hunspellIn.flush();
  }

  public void setLang(String lang) {
    setOption("lang", lang);
  }

  public String getLang() throws IOException {
    return getOption("lang");
  }

  public String getMasterLang() throws IOException {
    masterDictMatcher.reset(getOption("master"));
    masterDictMatcher.find();
    return masterDictMatcher.group(1);
  }

  public void addReplacement(String misspelling, String correction) {
    hunspellIn.println("$$ra " + misspelling + "," + correction);
    hunspellIn.flush();
  }

  public String[] getPersonalWordList() throws IOException {
	  // TODO
	  return new String[0];
	  /*
    String listString = call("$$pp");
    listString = listString.substring(listString.indexOf(':') + 1).trim();
    return listString.split(", ");
    */
  }

  public String[] getSessionWordList() throws IOException {
    String listString = call("$$ps");
    listString = listString.substring(listString.indexOf(':') + 1).trim();
    return listString.split(", ");
  }

  private String call(String input) throws IOException {
    flushOut();
    hunspellIn.println(input);
    hunspellIn.flush();
    return hunspellOut.readLine();
  }

  private void flushOut() throws IOException {
    while (out.available() > 0) hunspellOut.readLine();
  }

  public HashSet<String> getPersonalWords() {
    return personalWords;
  }

  /**
   * Shutdown hunspell.
   */
  public void shutdown() {
    try {
      hunspellIn.close();
      hunspellOut.close();
      hunspellErr.close();
    } catch (Exception ignored) {
    }
  }

  public static Hunspell getInstance(String lang) {
    if (instanceFailed) return null;

    Hunspell instance = instances.get(lang);
    if (instance == null) {
      try {
        instance = new Hunspell(lang);
        instances.put(lang, instance);
      } catch (IOException e) {
        instanceFailed = true;
        System.err.println("Warning: Failed to initialize spell checker 'hunspell':");
        System.err.println("  " + e.getMessage());
      }
    }

    return instance;
  }

  /**
   * Returns the available dictionaries provided by hunspell.
   *
   * @return list of dictionaries provided by hunspell
   * @throws java.io.IOException thrown if execution of hunspell failed
   */
  public static List<String> availableDicts() throws IOException {
    Process process = Runtime.getRuntime().exec(new String[]{
		    HUNSPELL_EXECUTABLE,
				"-D",
				"-l"
    });
	  process.getOutputStream().close();
    BufferedReader r = new BufferedReader(new InputStreamReader(process.getErrorStream()));

	  // full list
    List<String> fullDicts = new ArrayList<String>();
    String line;
	  while (!r.readLine().startsWith("AVAILABLE DICTIONARIES"));

    while ((line = r.readLine()) != null) {
	    if (line.equals("LOADED DICTIONARY:")) break;
	    fullDicts.add(line);
    }

	  // abbreviate to language names
	  Set<String> dicts = new HashSet<String>();
	  for (String fullDict : fullDicts) {
		  String abbrName = StringUtils.stringAfter(fullDict, "/", 'l');
		  if (abbrName == null) {
			  abbrName = fullDict;
		  }
		  dicts.add(abbrName);
	  }

	  List<String> sortedDicts = new ArrayList<String>(dicts);
	  Collections.sort(sortedDicts);

    return sortedDicts;
  }
}
