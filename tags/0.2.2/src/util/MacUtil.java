package util;

public class MacUtil {
  /**
  * Activates the application (bring it to front).
  */
  public static void activateApplication() {
    byte processId[] = new byte[8];
    GetCurrentProcess( processId );
    SetFrontProcess( processId );
  }

  // ----- JDirect ------
  private static native int GetCurrentProcess( byte[] processId );
  private static native int SetFrontProcess( byte[] processId );

  // The call to new Linker() installs the native methods
  static {
    if(OSUtil.getOS() == OSUtil.OS_MAC) {
      try {
        jnidirect.Linker.link(MacUtil.class, new String[] { "Carbon" });
      } catch(Throwable e) {}
    }
  }
}
