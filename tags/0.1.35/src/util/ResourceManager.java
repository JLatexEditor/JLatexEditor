package util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manage Resources instead of files.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class ResourceManager {
	/** Virtual resource dirs. */
	private ArrayList<VFS.VFile> dirs = new ArrayList<VFS.VFile>();

	/**
	 * Adds a virtual directory to the resource manager.
	 *
	 * @param dir virtual directory
	 */
	public void addResourceDir(VFS.VFile dir) {
		dirs.add(dir);
	}

	/**
	 * Adds a local directory to the resource manager.
	 *
	 * @param dir local directory
	 */
	public void addResourceDir(File dir) {
		dirs.add(new VFS.File(dir));
	}

	/**
	 * Adds a resource zip archive to the resource manager.
	 *
	 * @param zip zip archive
	 * @throws IOException thrown if reading the zip file fails
	 */
	public void addResourceZip(File zip) throws IOException {
		dirs.add(new VFS.ZipFile(zip).getRootEntry());
	}

	/**
	 * Returns a list of resources with the given path.
	 *
	 * @param path path to the resources
	 * @return list of resources with the given path
	 */
	public List<VFS.VFile> getFile(String path) {
		// return dirs.map{_.getFile(path)}
		ArrayList<VFS.VFile> list = new ArrayList<VFS.VFile>();

		boolean root = path.equals(".");

		for (VFS.VFile dir : dirs) {
			VFS.VFile thatFile = root ? dir : dir.getFile(path);
			if (thatFile != null) {
				list.add(thatFile);
			}
		}

		return list;
	}

	/**
	 * Returns a list of file lists that represent the files located in the directories you are looking for.
	 *
	 * @param path path to the directory the listing shall be applied to
	 * @return list of file lists that represent the files located in the directories you are looking for
	 */
	public List<List<VFS.VFile>> listFiles(String path) {
		// return dirs.map{_.getFile(path)}
		ArrayList<List<VFS.VFile>> list = new ArrayList<List<VFS.VFile>>();

		boolean root = path.equals(".");

		for (VFS.VFile dir : dirs) {
			VFS.VFile thatDir = root ? dir : dir.getFile(path);
			if (thatDir != null && thatDir.exists() && thatDir.isDirectory()) {
				list.add(thatDir.listFiles());
			}
		}

		return list;
	}
}
