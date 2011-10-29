import jlatexeditor.gproperties.GProperties;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class GPropertiesTestApp {
  public static void main(String[] args) {
    GProperties.load();
    GProperties.save();
  }
}
