package util;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test cases for ResourceManager.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class ResourceManagerTest extends TestCase {
	public void testResourceManager() throws IOException {
		ResourceManager rm = new ResourceManager();
		rm.addResourceZip(new File("test/resources/data.zip"));
		rm.addResourceDir(new File("test/resources/"));

		String workingDir = System.getProperty("user.dir");

		compareLists(
				rm.getFile("."),
				"zip:file:" + workingDir + "/test/resources/data.zip!/",
				"test/resources");

		compareLists(
				rm.getFile("da"),
				"test/resources/da");

		compareLists(
				rm.getFile("data"),
				"zip:file:" + workingDir + "/test/resources/data.zip!/data/",
				"test/resources/data");

		compareLists(
				rm.getFile("data/a"),
				"zip:file:" + workingDir + "/test/resources/data.zip!/data/a/",
				"test/resources/data/a");

		assertTrue(rm.getFile("data/a").get(0).exists());
		assertTrue(rm.getFile("data/a").get(0).isDirectory());
		assertTrue(rm.getFile("data/a").get(1).exists());
		assertTrue(rm.getFile("data/a").get(1).isDirectory());

		List<List<VFS.VFile>> list = rm.listFiles("data/a");
		assertEquals(2, list.size());
		{
			List<VFS.VFile> vFiles = list.get(0);
			assertEquals("a.txt", vFiles.get(0).getName());
			assertEquals("a1", vFiles.get(1).getName());
			assertEquals("a2", vFiles.get(2).getName());
		}
		{
			List<VFS.VFile> vFiles = list.get(1);
//			assertEquals(".svn", vFiles.get(0).getName());
			assertEquals("a.txt", vFiles.get(1).getName());
			assertEquals("a1", vFiles.get(2).getName());
			assertEquals("a2", vFiles.get(3).getName());
		}

		compareLists(
				rm.getFile("data/aa"),
				"test/resources/data/aa");

		assertFalse(rm.getFile("data/aa").get(0).exists());

		compareLists(
				rm.getFile("data/a/a.txt"),
				"zip:file:" + workingDir + "/test/resources/data.zip!/data/a/a.txt",
				"test/resources/data/a/a.txt");

		assertTrue(rm.getFile("data/a/a.txt").get(0).exists());
		assertFalse(rm.getFile("data/a/a.txt").get(0).isDirectory());
		assertEquals("a zip", new String(rm.getFile("data/a/a.txt").get(0).getContent()));
		assertTrue(rm.getFile("data/a/a.txt").get(1).exists());
		assertFalse(rm.getFile("data/a/a.txt").get(1).isDirectory());
		assertEquals("a local", new String(rm.getFile("data/a/a.txt").get(1).getContent()));
	}

	private void compareLists(List act, String... exp) {
		int i;
		int length = Math.min(act.size(), exp.length);

		for (i = 0; i < length; i++) {
			assertEquals(i + "th list element", exp[i], act.get(i).toString());
		}

		if (act.size() < exp.length) {
			fail(i + "th list element (" + exp[i] + ") missing in act");
		}
		if (act.size() > exp.length) {
			fail(i + "th list element (" + act.get(i).toString() + ") missing in exp");
		}
	}
}
