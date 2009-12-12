package util;

import java.io.*;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java API for the command line tool aspell.
 *
 * A documentation about aspell can be found here:
 * http://aspell.net/man-html/Through-A-Pipe.html#Through-A-Pipe
 *
 * @author Stefan Endrullis
 */
public final class Aspell {
	private static final Matcher masterDictMatcher = Pattern.compile("/([^/\\.]+)\\.multi").matcher("");
	private static Aspell instance = null;

	private PrintStream aspellIn;
	private BufferedReader aspellOut;
	private BufferedReader aspellErr;
	private InputStream out;
	/** Words in the personal dictionary. */
	private HashSet<String> personalWords = new HashSet<String>();

	public static void main(String[] args) throws IOException {
		// print all available dictionaries
		System.out.println("available dictionaries");
		for (String dict: availableDicts()) {
			System.out.println(dict);
		}

		// start aspell with language "en" / "en_GB"
		Aspell aspell = new Aspell();
		// test some words
		System.out.println("\ntest come words");
		System.out.println(aspell.check("the"));
		System.out.println(aspell.check("bla"));
		System.out.println(aspell.check("teh"));
		System.out.println(aspell.check("linebreak"));
		aspell.shutdown();
	}

	/**
	 * Starts aspell with language "en" and language tag "en_GB".
	 *
	 * @throws IOException if aspell could not be started
	 */
	public Aspell() throws IOException {
		this ("en_GB");
	}

	/**
	 * Creates the aspell wrapper that runs aspell in background.
	 *
	 * @param lang language, e.g. "en" or "en_GB"
	 * @throws IOException if aspell could not be started
	 */
	public Aspell(String lang) throws IOException {
		startAspell(lang, lang);
	}

	/**
	 * Creates the aspell wrapper that runs aspell in background.
	 *
	 * @param lang language, e.g. "en"
	 * @param langTag language tag, e.g. "en_GB"
	 * @throws IOException if aspell could not be started
	 */
	public Aspell(String lang, String langTag) throws IOException {
		startAspell(lang, langTag);
	}

	private void startAspell(String lang, String langTag) throws IOException {
		String[] aspellCommand = new String[]{
			"aspell",
			"-a",
			"--lang=" + lang,
			"--language-tag=" + langTag
		};

		Process aspellProcess = Runtime.getRuntime().exec(aspellCommand);

		// see if aspell died
		try {
			int exitValue = aspellProcess.exitValue();
			throw new IOException("Aspell failed to start / aborted with error code " + exitValue);
		} catch (IllegalThreadStateException ignored) {}

		aspellIn  = new PrintStream(new BufferedOutputStream(aspellProcess.getOutputStream()), true);
		out = aspellProcess.getInputStream();
		aspellOut = new BufferedReader(new InputStreamReader(out));
		aspellErr = new BufferedReader(new InputStreamReader(aspellProcess.getErrorStream()));

		// read version line
		aspellOut.readLine();

		personalWords.clear();
		personalWords.addAll(Arrays.asList(getPersonalWordList()));
	}

	/**
	 * Check spelling of the word using aspell.
	 *
	 * @param word word to check
	 * @return aspell result
	 * @throws IOException thrown if execution of aspell failed
	 */
	public synchronized Result check(String word) throws IOException {
		flushOut();
		aspellIn.println(word);
		aspellIn.flush();

    String line = aspellOut.readLine();

		if (line.equals("*")) {
			aspellOut.readLine();
			return new Result();
		} else
		if (line.startsWith("#")) {
			aspellOut.readLine();
			return new Result(new ArrayList<String>(0));
		} else
		if (line.startsWith("&")) {
			aspellOut.readLine();
			return new Result(Arrays.asList(line.split(": ")[1].split(", ")));
		} else {
			throw new RuntimeException("unknown aspell answer: " + line);
		}
	}

	/**
	 * Adds the word to the aspell user dictionary.
	 *
	 * @param word word to add
	 */
	public synchronized void addToPersonalDict(String word) {
		aspellIn.println("*" + word);
		aspellIn.println("#");
		aspellIn.flush();

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
	 * @param value value
	 */
	public synchronized void setOption(String option, String value) {
		aspellIn.println("$$cs " + option + "," + value);
		aspellIn.flush();
	}

	public void   setLang(String lang) { setOption("lang", lang);	}
	public String getLang() throws IOException { return getOption("lang"); }
	
	public void   setLanguageTag(String languageTag) { setOption("language-tag", languageTag); }
	public String getLanguageTag() throws IOException { return getOption("language-tag"); }

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
		String listString = call("$$pp");
		listString = listString.substring(listString.indexOf(':')+1).trim();
		return listString.split(", ");
	}

	public String[] getSessionWordList() throws IOException {
		String listString = call("$$ps");
		listString = listString.substring(listString.indexOf(':')+1).trim();
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
		} catch (Exception ignored) {}
	}

	public static Aspell getInstance() throws IOException {
		if (instance == null) {
			instance = new Aspell();
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
		Process process = Runtime.getRuntime().exec(new String[]{
			"aspell",
			"dump",
			"dicts"
		});
		BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));

		List<String> dicts = new ArrayList<String>();
		String line;
		while ((line = r.readLine()) != null) dicts.add(line);

		return dicts;
	}

	/**
	 * Aspell result.
	 */
	public static class Result {
		private boolean correct;
		private List<String> suggestions;

		/** Creates a correct result. */
		public Result() {
			correct = true;
		}

		/** Creates a result with suggestions.
		 * @param suggestions list of suggestions
		 */
		public Result(List<String> suggestions) {
			this.suggestions = suggestions;
			correct = false;
		}

		public boolean isCorrect() {
			return correct;
		}

		public List<String> getSuggestions() {
			return suggestions;
		}

		@Override
		public String toString() {
			return correct ?
				"correct" :
				"misspelled; suggestions: " + suggestions;
		}
	}
}
