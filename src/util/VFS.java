package util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

/**
 * Virtual file.  This can be a real file (VFS.File) or a file in a zip file (VFS.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public interface VFS {
	public static interface VFile<F extends VFile> {
		public boolean exists();
		public boolean isDirectory();
		public String getName();
		public String getLocalPath();
		public URI getUri();

		public InputStream getInputStream() throws IOException;
		public byte[] getContent() throws IOException;

		public F getFile(String path);
		public List<String> listFileNames();
		public List<F> listFiles();
	}



	public static class File implements VFile<File> {
		private java.io.File file;

		public File(java.io.File file) {
			this.file = file;
		}

		public java.io.File getFile() {
			return file;
		}

		@Override
		public boolean exists() {
			return file.exists();
		}

		@Override
		public boolean isDirectory() {
			return file.isDirectory();
		}

		@Override
		public InputStream getInputStream() throws FileNotFoundException {
			return new FileInputStream(file);
		}

		@Override
		public byte[] getContent() throws IOException {
			return StreamUtils.readBytesFromInputStream(getInputStream());
		}

		@Override
		public File getFile(String path) {
			return new File(new java.io.File(file, path));
		}

		@Override
		public List<String> listFileNames() {
			return Arrays.asList(file.list());
		}

		@Override
		public List<File> listFiles() {
			java.io.File[] fileArray = file.listFiles();
			Arrays.sort(fileArray);

			ArrayList<File> files = new ArrayList<File>();
			for (java.io.File file1 : fileArray) {
				files.add(new File(file1));
			}

			return files;
		}

		@Override
		public String getName() {
			return file.getName();
		}

		@Override
		public String getLocalPath() {
			return file.getPath();
		}

		@Override
		public URI getUri() {
			return file.toURI();
		}

		@Override
		public String toString() {
			return file.toString();
		}
	}

	public static class ZipFile {
		private java.io.File file;
		private java.util.zip.ZipFile zipFile;
		private ZipEntry rootEntry;
		private String uriPrefix;

		public ZipFile(java.io.File file) throws IOException {
			this.file = file;
			zipFile = new java.util.zip.ZipFile(file);
			uriPrefix = "zip:" + file.toURI().toString() + "!/";
			rootEntry = new RootZipEntry(this);

			// build index
			Enumeration<? extends java.util.zip.ZipEntry> en = zipFile.entries();
			while(en.hasMoreElements()) {
				java.util.zip.ZipEntry zipEntry = en.nextElement();

				// split path
				String[] path = zipEntry.getName().split("/");
				// walk through path

				ZipEntry currentZipEntry = rootEntry;
				for (int i = 0; i < path.length-1; i++) {
					String dirName = path[i];
					currentZipEntry = currentZipEntry.lookup(dirName);
				}
				currentZipEntry.addEntry(path[path.length-1], new RealZipEntry(this, zipEntry));
			}
		}

		public String getUriPrefix() {
			return uriPrefix;
		}

		public java.io.File getFile() {
			return file;
		}

		public java.util.zip.ZipFile getZipFile() {
			return zipFile;
		}

		public ZipEntry getRootEntry() {
			return rootEntry;
		}
	}

	public static abstract class ZipEntry implements VFile<ZipEntry> {
		protected ZipFile zipFile;
		protected URI uri;
		protected String path;
		protected SortedMap<String,ZipEntry> children = new TreeMap<String, ZipEntry>();

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public URI getUri() {
			return uri;
		}

		public ZipFile getZipFile() {
			return zipFile;
		}
		
		@Override
		public ZipEntry getFile(String path) {
			ZipEntry curr = this;
			for (String name: path.split("/")) {
				curr = curr.lookup(name);
				if (curr == null) {
					return null;
				}
			}
			return curr;
		}

		@Override
		public List<String> listFileNames() {
			ArrayList<String> files = new ArrayList<String>();

			for (String name : children.keySet()) {
				files.add(name);
			}

			return files;
		}

		@Override
		public List<ZipEntry> listFiles() {
			List<ZipEntry> files = new ArrayList<ZipEntry>();

			for (ZipEntry entry : children.values()) {
				files.add(entry);
			}

			return files;
		}

		public ZipEntry lookup(String name) {
			return children.get(name);
		}
		
		public void addEntry(String name, ZipEntry zipEntry) {
			children.put(name, zipEntry);
		}

		@Override
		public String toString() {
			return uri.toASCIIString();
		}
	}

	public static class RootZipEntry extends ZipEntry {
		public RootZipEntry(ZipFile zipFile) {
			this.zipFile = zipFile;
			uri = URI.create(zipFile.getUriPrefix());
		}

		@Override
		public boolean isDirectory() {
			return true;
		}

		@Override
		public String getName() {
			return ".";
		}

		@Override
		public String getLocalPath() {
			return ".";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			throw new IOException("Zip entry " + uri + " is not a file but a directory");
		}

		@Override
		public byte[] getContent() throws IOException {
			throw new IOException("Zip entry " + uri + " is not a file but a directory");
		}
	}

	public static class RealZipEntry extends ZipEntry implements Comparable {
		private java.util.zip.ZipEntry zipEntry;
		private String name;

		public RealZipEntry(ZipFile zipFile, java.util.zip.ZipEntry zipEntry) throws IOException {
			this.zipFile = zipFile;
			this.zipEntry = zipEntry;
			this.path = zipEntry.getName();
			String[] parts = path.split("/");
			this.name = parts[parts.length-1];

			uri = URI.create(zipFile.getUriPrefix() + path);
		}

		@Override
		public boolean isDirectory() {
			return zipEntry.isDirectory();
		}

		public InputStream getInputStream() throws IOException {
			return zipFile.getZipFile().getInputStream(zipEntry);
		}

		@Override
		public byte[] getContent() throws IOException {
			return StreamUtils.readBytesFromInputStream(getInputStream());
		}

		public String getName() {
			return name;
		}

		@Override
		public String getLocalPath() {
			return zipEntry.getName();
		}

		public java.util.zip.ZipEntry getZipEntry() {
			return zipEntry;
		}

		@Override
		public int compareTo(Object o) {
			if (o instanceof ZipEntry) {
				ZipEntry that = (ZipEntry) o;
				return this.getName().compareTo(that.getName());
			}
			return 0;
		}
	}
}
