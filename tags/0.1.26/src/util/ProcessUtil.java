package util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class ProcessUtil {
  public static Process exec(String command, File dir) throws IOException {
    Process process;

    ArrayList<String> env = new ArrayList<String>();
    for(Map.Entry<String, String> entry : System.getenv().entrySet()) {
      if(entry.getKey().equalsIgnoreCase("PWD")) continue;
      env.add(entry.getKey() + "=" + entry.getValue());
    }
    String[] envArray = new String[env.size()];
    env.toArray(envArray);

    process = Runtime.getRuntime().exec(command, envArray, dir);

    return process;
  }
}
