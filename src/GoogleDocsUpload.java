/* Copyright (c) 2009 Anton Beloglazov, http://beloglazov.info
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

/**
 * Google Docs Upload
 * 
 * A tool for batch upload of documents to a Google Docs account with recursive traversing of directories.
 * Supported file formats are: csv, doc, html, ods, odt, pdf, png, ppt, rtf, swf, tsv, txt, xls, zip.
 * 
 * Usage: java -jar google-docs-upload.jar
 * Usage: java -jar google-docs-upload.jar <path> --recursive
 * Usage: java -jar google-docs-upload.jar <path> --username <user> --password <pass>
 * Usage: java -jar google-docs-upload.jar <path> --authSub <token>
 *     [--recursive]                 Recursively traverse directories.
 *     [--username <username>]       Username for a Google account.
 *     [--password <password>]       Password for a Google account.
 *     [--authSub <token>]           AuthSub token.
 *     [--auth_protocol <protocol>]  The protocol to use with authentication.
 *     [--auth_host <host:port>]     The host of the auth server to use.
 *     [--protocol <protocol>]       The protocol to use with the HTTP requests.
 *     [--host <host:port>]          Where is the feed (default = docs.google.com) 
 *     
 * @author Anton Beloglazov
 * @since 09/2009
 * @version 1.0
 * 
 */
public class GoogleDocsUpload {

	/** The document list. */
	private DocumentList documentList;
	
	/** Supported file formats **/
	public static String[] SUPPORTED_FORMATS = { "csv", "doc", "html", "ods", "odt", "pdf", "png", "ppt", "rtf", "swf", "tsv", "txt", "xls", "zip" };
	
	/** Welcome message, introducing the program. */
	private static final String[] WELCOME_MESSAGE = { "",
		"Using this tool, you can batch upload your documents to a Google Docs account with recursive traversing of directories.",
		"Supported file formats are: csv, doc, html, ods, odt, pdf, png, ppt, rtf, swf, tsv, txt, xls, zip.",
		"Type 'help' for a list of parameters.", "" 
	};	
	
	/** The message for displaying the usage parameters. */
	private static final String[] USAGE_MESSAGE = { "",
		"Usage: java -jar google-docs-upload.jar",
		"Usage: java -jar google-docs-upload.jar <path> --recursive",
		"Usage: java -jar google-docs-upload.jar <path> --username <username> --password <pass>",
		"Usage: java -jar google-docs-upload.jar <path> --authSub <token>",
		"    [--recursive]                 Recursively traverse directories.",
		"    [--username <username>]       Username for a Google account.",
		"    [--password <password>]       Password for a Google account.",
		"    [--authSub <token>]           AuthSub token.",
		"    [--auth_protocol <protocol>]  The protocol to use with authentication.",
		"    [--auth_host <host:port>]     The host of the auth server to use.",
		"    [--protocol <protocol>]       The protocol to use with the HTTP requests.",
		"    [--host <host:port>]          Where is the feed (default = docs.google.com)", "" 
	};

	/**
	 * Constructor.
	 * 
	 * @param appName the app name
	 * @param authProtocol the auth protocol
	 * @param authHost the auth host
	 * @param protocol the protocol
	 * @param host the host
	 * 
	 * @throws DocumentListException the document list exception
	 */
	public GoogleDocsUpload(String appName, String authProtocol, String authHost, String protocol, String host) throws DocumentListException {
		setDocumentList(new DocumentList(appName, authProtocol, authHost, protocol, host));
	}
	
	/**
	 * Runs the application.
	 * 
	 * @param args the command-line arguments
	 * 
	 * @throws DocumentListException the document list exception
	 * @throws ServiceException the service exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void main(String[] args) throws DocumentListException, IOException, ServiceException {
		SimpleCommandLineParser parser = new SimpleCommandLineParser(args);
		String authProtocol = parser.getValue("auth_protocol");
		String authHost = parser.getValue("auth_host");
		String authSub = parser.getValue("authSub", "auth", "a");
		String username = parser.getValue("username", "user", "u");
		String password = parser.getValue("password", "pass", "p");
		String protocol = parser.getValue("protocol");
		String host = parser.getValue("host", "s");
		boolean help = parser.containsKey("help", "h");
		boolean recursive = parser.containsKey("recursive", "r");
		String path = null;
		
		if (help) {
			printMessages(USAGE_MESSAGE);
			System.out.print("Supported file formats are: ");
			boolean notFirst = false;
			for (String format : SUPPORTED_FORMATS) {
				if (notFirst) {
					System.out.print(", ");	
				}
				System.out.print(format);				
				notFirst = true;
			}
			printMessage("");
			System.exit(1);
		}
		
		printMessages(WELCOME_MESSAGE);

		if (args.length > 0 && args[0] != "help" && !args[0].substring(0, 1).equals("-") && !args[0].substring(0, 2).equals("--")) {
			path = args[0]; 			
		}

		if (authProtocol == null) {
			authProtocol = DocumentList.DEFAULT_AUTH_PROTOCOL;
		}

		if (authHost == null) {
			authHost = DocumentList.DEFAULT_AUTH_HOST;
		}

		if (protocol == null) {
			protocol = DocumentList.DEFAULT_PROTOCOL;
		}

		if (host == null) {
			host = DocumentList.DEFAULT_HOST;
		}
		
		Scanner scanner = null;
		
		if (path == null || ((username == null || password == null) && authSub == null)) {
			scanner = new Scanner(System.in);
			
			if (username == null && authSub == null) {
				System.out.print("Username: ");
				username = scanner.nextLine();						
			}
			
			if (password == null && authSub == null) {
				System.out.print("Password: ");
				password = String.copyValueOf(System.console().readPassword());
			}
		}
		
		GoogleDocsUpload app = new GoogleDocsUpload("google-docs-upload", authProtocol, authHost, protocol, host);

		if (password != null) {
			try {
				app.login(username, password);
			} catch (AuthenticationException e) {
				printMessage("Authentification error");
				System.exit(1);
			}
		} else {
			try {
				app.login(authSub);
			} catch (AuthenticationException e) {
				printMessage("Authentification error");
				System.exit(1);
			}
		}
		
		if (path == null) {
			if (scanner == null) {
				scanner = new Scanner(System.in);
			}
			
			System.out.print("Path: ");
			path = scanner.nextLine();						
		}

		app.upload(path, recursive);
	}

	/**
	 * Authenticates the client using ClientLogin.
	 * 
	 * @param username User's username
	 * @param password User's password
	 * 
	 * @throws DocumentListException the document list exception
	 * @throws AuthenticationException the authentication exception
	 */
	public void login(String username, String password) throws AuthenticationException, DocumentListException {
		getDocumentList().login(username, password);
	}
	
	/**
	 * Authenticates the client using AuthSub.
	 * 
	 * @param authSubToken the auth sub token
	 * 
	 * @throws DocumentListException the document list exception
	 * @throws AuthenticationException the authentication exception
	 */
	public void login(String authSubToken) throws AuthenticationException, DocumentListException {
		getDocumentList().loginWithAuthSubToken(authSubToken);
	}	
		
	/**
	 * Uploads specified folder.
	 * 
	 * @param path the path to upload
	 * @param recursive the flag for recursive upload
	 * 
	 * @throws ServiceException the service exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void upload(String path, boolean recursive) throws IOException, ServiceException {
		File folder = new File(path);
		if (!folder.exists()) {
			printMessage("Specified folder " + path + " doesn't exist");
			System.exit(1);			
		}
		
		printMessage("\nUploading" + (recursive ? " recursively" : "") + " the folder " + path + "\n");
		
		List<File> list = new ArrayList<File>();
		getFiles(folder, list, recursive);
		
		int uploaded = 0;
		
		ArrayList<String> formats = new ArrayList<String>(Arrays.asList(SUPPORTED_FORMATS));
		
		for (File file : list) {
			printMessage(file.getAbsolutePath());
			
			if (!formats.contains(file.getName().substring(file.getName().lastIndexOf(".") + 1))) {
				printMessage(" - Skipped: the file format is not supported");
				continue;
			}
				
			for (int i = 0; i < 3; i++) {				
				try {
					getDocumentList().uploadFile(file.getAbsolutePath(), file.getName().substring(0, file.getName().lastIndexOf(".")));
					uploaded++;
					break;
				} catch (DocumentListException e) {
					printMessage(" - Upload error: " + e.getMessage());
					if (i < 2) {
						printMessage(" - Another try...");
					}
				}
			}
		}
		
		printMessage("\nTotal uploaded: " + uploaded);
	}

	/**
	 * Gets the list of files.
	 * 
	 * @param folder the folder
	 * @param list the file list to be populated
	 * @param recursive the flag for recursive directory traversing
	 * 
	 * @return the list of files
	 */
	protected static void getFiles(File folder, List<File> list, boolean recursive) {
		folder.setReadOnly();
		File[] files = folder.listFiles();
		for (File file : files) {
			if (!file.isDirectory()) {
				list.add(file);
			} else if (recursive) {
				getFiles(file, list, recursive);
			}
		}
	}
	
	/**
	 * Prints out a message.
	 * 
	 * @param msg the message to be printed.
	 */
	protected static void printMessage(String msg) {
		System.out.println(msg);
	}
	
	/**
	 * Prints out a list of messages.
	 * 
	 * @param msg the message to be printed.
	 */
	protected static void printMessages(String[] msg) {
		for (String s : msg) {
			printMessage(s);
		}
	}

	/**
	 * Gets the document list.
	 * 
	 * @return the document list
	 */
	protected DocumentList getDocumentList() {
		return documentList;
	}

	/**
	 * Sets the document list.
	 * 
	 * @param documentList the new document list
	 */
	protected void setDocumentList(DocumentList documentList) {
		this.documentList = documentList;
	}

}
