package util;

import de.endrullis.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class ProcessUtil {
  public static Process exec(String command[], File dir, Map<String,String> defaultEnv) throws IOException {
    Process process;

    ArrayList<String> env = new ArrayList<String>(); {
      for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
        if (entry.getKey().equalsIgnoreCase("PWD")) {
          env.add(entry.getKey() + "=" + dir.getAbsolutePath());
        } else {
          env.add(entry.getKey() + "=" + entry.getValue());
        }
      }

      if(defaultEnv != null) {
        for(Map.Entry<String, String> entry : defaultEnv.entrySet()) {
          if(System.getenv().get(entry.getKey()) != null) continue;
          env.add(entry.getKey() + "=" + entry.getValue());
        }
      }
    }
    String[] envArray = new String[env.size()];
    env.toArray(envArray);

    process = dir != null ?
                Runtime.getRuntime().exec(command, envArray, dir) :
                Runtime.getRuntime().exec(command, envArray);

    return process;
  }

  public static Process exec(String command[], File dir) throws IOException {
    return exec(command, dir, null);
  }

  public static Process exec(String command, File dir) throws IOException {
		ArrayList<String> list = StringUtils.tokenize(command);
		String[] array = new String[list.size()];
		list.toArray(array);

	  return exec(array, dir);
	}
}
