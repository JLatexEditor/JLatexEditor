package util;

import java.util.*;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Java API for the command line tool aspell.
 *
 * @author Stefan Endrullis
 */
public final class Aspell {
	private PrintStream aspellIn;
	private BufferedReader aspellOut;
	private BufferedReader aspellErr;
	private static Aspell instance = null;

	public static void main(String[] args) throws IOException {
		Aspell aspell = new Aspell();
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
		this ("en", "en_GB");
	}

	/**
	 * Creates the aspell wrapper that runs aspell in background.
	 *
	 * @param lang language, e.g. "en"
	 * @param langTag language tag, e.g. "en_GB"
	 * @throws IOException if aspell could not be started
	 */
	public Aspell(String lang, String langTag) throws IOException {
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
		aspellOut = new BufferedReader(new InputStreamReader(aspellProcess.getInputStream()));
		aspellErr = new BufferedReader(new InputStreamReader(aspellProcess.getErrorStream()));
	}

	/**
	 * Check spelling of the word using aspell.
	 *
	 * @param word word to check
	 * @return aspell result
	 */
	public synchronized Result check(String word) throws IOException {
		aspellIn.println(word);
		aspellOut.readLine();

    String line = aspellOut.readLine();
    if(line.equals("")) line = aspellOut.readLine();

		if (line.equals("*")) {
			return new Result();
		} else
		if (line.startsWith("#")) {
			return new Result(new ArrayList<String>(0));
		} else
		if (line.startsWith("&")) {
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
	public synchronized void addToDict(String word) {
		aspellIn.println("*" + word);
		aspellIn.println("#");
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
