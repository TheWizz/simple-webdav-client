package pixi;

import io.crowdcode.webdav.JRWebDavClient;
import io.crowdcode.webdav.data.WebDavDirectory;
import io.crowdcode.webdav.data.WebDavFile;
import io.crowdcode.webdav.data.WebDavLsResult;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Program entry point, and synchronization handler.
 */
public class Syncronize {
	private final JRWebDavClient wc;

	public static void main(String[] args) {
		if (args.length == 4) {
			String url = args[0];
			String username = args[1];
			String password = args[2];
			String fileSystemRoot = args[3];
			try {
				new Syncronize(new URI(url), username, password, fileSystemRoot);
			} catch (Exception iox) {
				System.err.println("Error; " + iox.toString());
			}
		} else {
			PrintStream out = System.err;
			out.println("Simple WebDAV file synchronizer, cloning from server to local files.");
			out.println("Parameters:");
			out.println("  url      - Root on WebDAV server.");
			out.println("  user     - User name for accessing the server.");
			out.println("  password - Password for accessing the server.");
			out.println("  path     - Local filesystem path where to write the clone.");
		}
	}

	Syncronize(
		URI uri,
		String username, String password,
		String destPath
	) throws IOException, DavException {
		wc = new JRWebDavClient();
		wc.init(uri, username, password);
		Path dest = Paths.get(destPath);
		copyFiles("", dest);
	}

	void copyFiles(String subPath, Path destDir) throws IOException, DavException {
		WebDavLsResult lr = wc.ls(subPath);

		List<WebDavFile> files = lr.getFiles();
		if (!files.isEmpty()) {
			Files.createDirectories(destDir);
			for (WebDavFile file : files) {
				String fileName = file.getName();
				if (!fileName.startsWith(".")) {	// Skip invisible files (often MacOS residue)
					ZonedDateTime serverLastMod = ZonedDateTime.parse(file.getLastModified(), DateTimeFormatter.RFC_1123_DATE_TIME);
					long serverFileSize = file.getLength();
					Path destFile = destDir.resolve(fileName);
					if (!Files.exists(destFile) ||
						!sameSizeAndModTime(serverLastMod, serverFileSize, destFile)
					) {	// New file or file has different size or last-modified-time
						// System.out.println("COPYING " + file.getName());
						try (InputStream webDavFileData = wc.readFile(file)) {
							Files.copy(webDavFileData, destFile, StandardCopyOption.REPLACE_EXISTING);
							Files.setLastModifiedTime(destFile, FileTime.from(serverLastMod.toInstant()));
						}
					}
				}
			}
		}

		List<WebDavDirectory> dirs = lr.getDirectories();
		for (WebDavDirectory dir : dirs) {
			Object dirNameValue = dir.getPropertiesPresent().get(DavConstants.PROPERTY_DISPLAYNAME).getValue();
			if (dirNameValue != null) {
				String dirName = dirNameValue.toString();
				if (!dirName.startsWith(".")) {    // Skip invisible directories
					// System.out.println("DIR " + dirName);
					Path sub = destDir.resolve(dirName);
					copyFiles(subPath + dirName + '/', sub);
				}
			}
		}
	}

	/**
	 Return true if localFile has the specified size and last-modified-time (accurate to one second).
	 */
	static private boolean sameSizeAndModTime(ZonedDateTime serverLastMod, long serverFileSize, Path localFile) throws IOException {
		ZonedDateTime fileTime = ZonedDateTime.ofInstant(
			Files.getLastModifiedTime(localFile).toInstant(),
			ZoneId.systemDefault()
		);
		return serverLastMod.toEpochSecond() == fileTime.toEpochSecond() &&
			serverFileSize == Files.size(localFile);
	}
}
