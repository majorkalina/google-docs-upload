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
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.data.docs.DocumentListFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.InvalidEntryException;
import com.google.gdata.util.ServiceException;

/**
 * Google Docs Upload
 * 
 * A tool for batch upload of documents to a Google Docs account preserving folder structure.
 * Supported file formats are: csv, doc, docx, html, htm, ods, odt, pdf, ppt, pps, rtf, sxw, tsv, tab, txt, xls, xlsx.
 * 
 * Usage: java -jar google-docs-upload.jar
 * Usage: java -jar google-docs-upload.jar <path> --recursive
 * Usage: java -jar google-docs-upload.jar <path> --username <username> --password <password>
 * Usage: java -jar google-docs-upload.jar <path> --auth-sub <token>
 *     [--username <username>]       Username for a Google account.
 *     [--password <password>]       Password for a Google account.
 *     [--recursive]                 Recursively upload all subfolders.
 *     [--remote-folder]             The remote folder path to upload the documents separated by '/'.
 *     [--without-folders]           Do not recreate folder structure in Google Docs.
 *     [--add-all]                   Upload all documents even if there are already documents with the same names.		
 *     [--skip-all]                  Skip all documents if there there are already documents with the same names.	
 *     [--replace-all]               Replace all documents in Google Docs, which have the same names as the uploaded.	
 *     [--disable-retries]           Disable auto-retries in the cases of failed upload.	
 *     [--auth-sub <token>]          AuthSub token.
 *     [--auth-protocol <protocol>]  The protocol to use with authentication.
 *     [--auth-host <host:port>]     The host of the auth server to use.
 *     [--protocol <protocol>]       The protocol to use with the HTTP requests.
 *     [--host <host:port>]          Where is the feed (default = docs.google.com)
 *     
 * You can also use short versions of the options, such as -u (--username), -p (--password), -rf (--remote-folder), etc.
 * 
 * @author Anton Beloglazov
 * @since 03/09/2009
 * @version 1.3.1 02/01/2010
 */
public class GoogleDocsUpload {

	/** The document list. */
	private DocumentList documentList;
	
	/** The output stream *. */
	private static PrintWriter out;
	
	/** The file filter. */
	private static FileFilter fileFilter = new FileFilter() {		
		@Override
		public boolean accept(File file) {
			return file.isFile();
		}
	};
	
	/** The folder filter. */
	private static FileFilter folderFilter = new FileFilter() {		
		@Override
		public boolean accept(File file) {
			return file.isDirectory();
		}
	};
	
	/** Supported file formats http://code.google.com/intl/ru/apis/documents/faq.html#WhatKindOfFilesCanIUpload * */
	public static String[] SUPPORTED_FORMATS = { "csv", "doc", "docx", "html", "htm", "ods", "odt", "pdf", "ppt", "pps", "rtf", "sxw", "tsv", "tab", "txt", "xls", "xlsx" };
	
	/** File formats -> Google Docs formats *. */
	public static Map<String, String> FORMATS_MAP; {
		FORMATS_MAP = new HashMap<String, String>();
		
		FORMATS_MAP.put("doc", "document");
		FORMATS_MAP.put("docx", "document");
		FORMATS_MAP.put("htm", "document");
		FORMATS_MAP.put("html", "document");
		FORMATS_MAP.put("rtf", "document");
		FORMATS_MAP.put("sxw", "document");
		FORMATS_MAP.put("txt", "document");
		FORMATS_MAP.put("odt", "document");
		
		FORMATS_MAP.put("csv", "spreadsheet");
		FORMATS_MAP.put("ods", "spreadsheet");
		FORMATS_MAP.put("tab", "spreadsheet");
		FORMATS_MAP.put("tsb", "spreadsheet");
		FORMATS_MAP.put("tsv", "spreadsheet");
		FORMATS_MAP.put("xls", "spreadsheet");
		FORMATS_MAP.put("xlsx", "spreadsheet");
		
		FORMATS_MAP.put("pps", "presentation");		
		FORMATS_MAP.put("ppt", "presentation");
		
		FORMATS_MAP.put("pdf", "pdf");
	}	
	
	/** Google Docs formats -> size limits *. */
	public static Map<String, Long> SIZE_LIMITS; {
		SIZE_LIMITS = new HashMap<String, Long>();		
		SIZE_LIMITS.put("document", 500000L);
		SIZE_LIMITS.put("spreadsheet", 1000000L);
		SIZE_LIMITS.put("presentation", 10000000L);
		SIZE_LIMITS.put("pdf", 10000000L);
	}	
	
	/** Welcome message, introducing the program. */
	protected static final String[] WELCOME_MESSAGE = { "",
		"Google Docs Upload 1.3.1",
		"Using this tool, you can batch upload your documents to a Google Docs account preserving folder structure.",
		"Supported file formats are: csv, doc, docx, html, htm, ods, odt, pdf, ppt, pps, rtf, sxw, tsv, tab, txt, xls, xlsx.",
		"Type 'help' for a list of parameters.", "" 
	};	
	
	/** The message for displaying the usage parameters. */
	protected static final String[] USAGE_MESSAGE = { "",
		"Usage: java -jar google-docs-upload.jar",
		"Usage: java -jar google-docs-upload.jar <path> --recursive",
		"Usage: java -jar google-docs-upload.jar <path> --username <username> --password <password>",
		"Usage: java -jar google-docs-upload.jar <path> --auth-sub <token>",
		"    [--username <username>]       Username for a Google account.",
		"    [--password <password>]       Password for a Google account.",
		"    [--recursive]                 Recursively upload all subfolders.",
		"    [--remote-folder]             The remote folder path to upload the documents separated by '/'.",
		"    [--without-folders]           Do not recreate folder structure in Google Docs.",		
		"    [--add-all]                   Upload all documents even if there are already documents with the same names.",		
		"    [--skip-all]                  Skip all documents if there there are already documents with the same names.",		
		"    [--replace-all]               Replace all documents in Google Docs, which have the same names as the uploaded.",		
		"    [--disable-retries]           Disable auto-retries in the cases of failed upload.",		
		"    [--auth-sub <token>]          AuthSub token.",
		"    [--auth-protocol <protocol>]  The protocol to use with authentication.",
		"    [--auth-host <host:port>]     The host of the auth server to use.",
		"    [--protocol <protocol>]       The protocol to use with the HTTP requests.",
		"    [--host <host:port>]          Where is the feed (default = docs.google.com)", "",
		"You can also use short versions of the options, such as -u (--username), -p (--password), -rf (--remote-folder), etc."
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
		String authProtocol = parser.getValue("auth-protocol", "ap");
		String authHost = parser.getValue("auth-host", "ah");
		String authSub = parser.getValue("auth-sub", "auth", "as");
		String username = parser.getValue("username", "user", "u");
		String password = parser.getValue("password", "pass", "p");
		String remoteFolder = parser.getValue("remote-folder", "rf");
		String protocol = parser.getValue("protocol");
		String host = parser.getValue("host", "s");
		boolean help = parser.containsKey("help", "h");
		boolean recursive = parser.containsKey("recursive", "r");
		boolean withoutFolders = parser.containsKey("without-folders", "wf");
		boolean addAll = parser.containsKey("add-all", "aa");
		boolean skipAll = parser.containsKey("skip-all", "sa");
		boolean replaceAll = parser.containsKey("replace-all", "ra");
		boolean disableRetries = parser.containsKey("disable-retries", "dr");
		String path = null;
		
		if (help) {
			printMessages(USAGE_MESSAGE);
			print("Supported file formats are: ");
			boolean notFirst = false;
			for (String format : SUPPORTED_FORMATS) {
				if (notFirst) {
					print(", ");	
				}
				print(format);				
				notFirst = true;
			}
			printLine("");
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
				print("Username: ");
				username = scanner.nextLine();						
			}
			
			if (password == null && authSub == null) {
				print("Password: ");
				password = String.copyValueOf(System.console().readPassword());
			}
		}
		
		GoogleDocsUpload app = new GoogleDocsUpload("google-docs-upload", authProtocol, authHost, protocol, host);

		if (password != null) {
			try {
				app.login(username, password);
			} catch (AuthenticationException e) {
				printLine("Authentification error");
				System.exit(1);
			}
		} else {
			try {
				app.login(authSub);
			} catch (AuthenticationException e) {
				printLine("Authentification error");
				System.exit(1);
			}
		}
		
		if (path == null) {
			if (scanner == null) {
				scanner = new Scanner(System.in);
			}
			
			print("Path: ");
			path = scanner.nextLine();						
		}

		app.upload(path, recursive, remoteFolder, withoutFolders, addAll, skipAll, replaceAll, disableRetries);
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
	 * @param remoteFolder the remote folder
	 * @param replaceAll the replace all
	 * @param skipAll the skip all
	 * @param addAll the add all
	 * @param withoutFolders the without folders
	 * @param disableRetries the disable retries
	 */
	public void upload(String path, boolean recursive, String remoteFolder, boolean withoutFolders, boolean addAll, boolean skipAll, boolean replaceAll, boolean disableRetries) {		
		File file = new File(path);
		if (!file.exists()) {
			printLine("Specified path " + path + " doesn't exist");
			System.exit(1);			
		}
		
		if (file.isDirectory()) {
			String message = "\nUploading" + (recursive ? " recursively" : "") + " the folder " + path;
			if (remoteFolder != null && remoteFolder.length() > 0) {
				 message += " to " + remoteFolder;
			}
			printLine(message + "\n");					
			int uploaded = uploadFolder(file, getRemoteFolderByPath(remoteFolder), 0, Integer.valueOf(getFileCount(file, recursive)), recursive, withoutFolders, Boolean.valueOf(addAll), Boolean.valueOf(skipAll), Boolean.valueOf(replaceAll), disableRetries);
			printLine("\nFiles uploaded: " + uploaded);		
		} else {
			printLine("\n" + file.getAbsolutePath());
			DocumentListEntry remoteFolderEntry = getRemoteFolderByPath(remoteFolder);
			uploadFile(file, remoteFolderEntry, getDocsFromFolder(remoteFolderEntry), Boolean.valueOf(addAll), Boolean.valueOf(skipAll), Boolean.valueOf(replaceAll), disableRetries);
			printLine("\nThe file has been uploaded");
		}		
	}
	

	/**
	 * Internal method for uploading a folder.
	 * 
	 * @param folder the folder
	 * @param remoteFolder the remote folder
	 * @param totalFileCount the total file count
	 * @param fileCount the file count
	 * @param recursive the recursive
	 * @param withoutFolders the without folders
	 * @param addAll the add all duplicates
	 * @param skipAll the skip all duplicates
	 * @param replaceAll the replace all duplicates
	 * @param disableRetries the disable retries
	 * 
	 * @return the number of uploaded documents
	 */
	protected int uploadFolder(File folder, DocumentListEntry remoteFolder, Integer fileCount, Integer totalFileCount, boolean recursive, boolean withoutFolders, Boolean addAll, Boolean skipAll, Boolean replaceAll, boolean disableRetries) {
		folder.setReadOnly();
		DocumentListFeed remoteSubFolders = getSubFolders(remoteFolder);
		DocumentListFeed remoteDocs = getDocsFromFolder(remoteFolder);
		int uploaded = 0;
		for (File file : folder.listFiles()) {
			if (!file.isDirectory()) {	
				fileCount++;
				printLine("[" + fileCount + "/" + totalFileCount + "] " + file.getAbsolutePath());
				if (uploadFile(file, remoteFolder, remoteDocs, addAll, skipAll, replaceAll, disableRetries)) {
					uploaded++;
				}
			}
		}
		
		for (File file : folder.listFiles()) {
			if (recursive && file.isDirectory()) {
				DocumentListEntry currentRemoteFolder = null;
				if (!withoutFolders) {
					currentRemoteFolder = documentListFindByTitle(getFolderName(file), remoteSubFolders);
					if (currentRemoteFolder == null) {
						try {
							if (remoteFolder == null) {
								currentRemoteFolder = getDocumentList().createNew(getFolderName(file), "folder");
							} else {
								currentRemoteFolder = getDocumentList().createNewSubFolder(getFolderName(file), remoteFolder.getResourceId());
							}						
						} catch (Exception e) {
							printLine(" - Skipped: failed to create the folder, files will be uploaded to the upper-level folder");
							e.printStackTrace();
						}			
						if (currentRemoteFolder == null) {
							printLine(" - Skipped: failed to create the folder, files will be uploaded to the upper-level folder");
						}
					}
				}
				uploaded += uploadFolder(file, currentRemoteFolder, fileCount, totalFileCount, recursive, withoutFolders, addAll, skipAll, replaceAll, disableRetries);
			} 
		}
		
		return uploaded;	
	}
	
	/**
	 * Upload file.
	 * 
	 * @param file the file
	 * @param remoteFolder the remote folder
	 * @param remoteDocs the remote docs
	 * @param addAll the add all
	 * @param skipAll the skip all
	 * @param replaceAll the replace all
	 * @param disableRetries the disable retries
	 * 
	 * @return true, if successful
	 */
	protected boolean uploadFile(File file, DocumentListEntry remoteFolder, DocumentListFeed remoteDocs, Boolean addAll, Boolean skipAll, Boolean replaceAll, boolean disableRetries) {	
		if (!isAllowedFormat(file)) {
			printLine(" - Skipped: the file format is not supported");
			return false;
		}
		if (!isAllowedSize(file)) {
			printLine(" - Skipped: the file size exceeds the limit");
			return false;
		}

		DocumentListEntry currentRemoteDoc = documentListFindByTitle(getFileName(file), remoteDocs);
		boolean skip = false;
		if (currentRemoteDoc != null && !addAll && currentRemoteDoc.getType().equals(getFileType(file))) {
			boolean replace = false;
			
			if (!skipAll && !replaceAll) {
				String choice = null;
				printLine(" - A document with the same name and type found in Google Docs");
				
				while (true) {
					print(" - add (a) / skip (s) / replace (r) / add all (aa) / skip all (sa) / replace all (ra): ");
					Scanner scanner = new Scanner(System.in);
					choice = scanner.nextLine();
					if (choice.equals("a") || choice.equals("s") || choice.equals("r") || choice.equals("aa") || choice.equals("sa") || choice.equals("ra")) {
						break;
					}
				}
				
				if (choice.equals("s")) {
					skip = true;							
				} else if (choice.equals("r")) {
					replace = true;							
				} else if (choice.equals("aa")) {
					addAll = true;							
				} else if (choice.equals("sa")) {
					skipAll = true;							
				} else if (choice.equals("ra")) {
					replaceAll = true;							
				}
			}
			
			if (replaceAll || replace) {
				try {
					getDocumentList().trashObject(currentRemoteDoc.getResourceId(), true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}										
		}
			
		if (currentRemoteDoc == null || !skipAll && !skip) {
			int cnt = 3;
			if (disableRetries) {
				cnt = 1;
			}
			for (int i = 0; i < cnt; i++) {				
				try {
					if (remoteFolder == null) {
						getDocumentList().uploadFile(file.getAbsolutePath(), getFileName(file));
					} else {
						getDocumentList().uploadFileToFolder(file.getAbsolutePath(), getFileName(file), remoteFolder.getResourceId());
					}
					break;
				} catch (InvalidEntryException e) {
					printLine(" - Skipped: " + e.getMessage());
					return false;		
				} catch (Exception e) {
					printLine(" - Upload error: " + e.getMessage());
					if (i < 2 && !disableRetries) {
						printLine(" - Another try...");
					} else {
						printLine(" - Skipped");
						return false;
					}
				}
			}
		} else {
			printLine(" - Skipped");
			return false;
		}
		
		return true;		
	}
	
	/**
	 * Gets the root folders.
	 * 
	 * @return the root folders
	 */
	public DocumentListFeed getRootFolders() {		
		DocumentListFeed results = new DocumentListFeed();
		try {
			DocumentListFeed docs = getDocumentList().getDocsListFeed("folders");
			List<DocumentListEntry> list = new ArrayList<DocumentListEntry>();
			for (DocumentListEntry doc : docs.getEntries()) {
				if (doc.getParentLinks().isEmpty()) {
					list.add(doc);
				}			
			}
			results.setEntries(list);			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}
	
	/**
	 * Gets the sub folders.
	 * 
	 * @param folder the folder
	 * 
	 * @return the sub folders
	 */
	public DocumentListFeed getSubFolders(DocumentListEntry folder) {
		DocumentListFeed results = null;
		if (folder == null) {
			return getRootFolders();
		}
		try {
			results = getDocumentList().getSubFolders(folder.getResourceId());
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return results;
	}
	
	/**
	 * Gets the docs.
	 * 
	 * @param folder the folder
	 * 
	 * @return the docs
	 */
	public DocumentListFeed getDocsFromFolder(DocumentListEntry folder) {
		DocumentListFeed results = null;
		if (folder == null) {
			results = new DocumentListFeed();
			DocumentListFeed docs = null;
			try {
				docs = getDocumentList().getDocsListFeed("all");
			} catch (Exception e) {
				e.printStackTrace();
			}
			List<DocumentListEntry> list = new ArrayList<DocumentListEntry>();
			for (DocumentListEntry doc : docs.getEntries()) {
				if (doc.getParentLinks().isEmpty() && !doc.getType().equals("folder")) {
					list.add(doc);
				}			
			}
			results.setEntries(list);
		} else {
			results = new DocumentListFeed();
			DocumentListFeed docs = null;
			try {
				docs = getDocumentList().getFolderDocsListFeed(folder.getResourceId());
			} catch (Exception e) {
				e.printStackTrace();
			}
			List<DocumentListEntry> list = new ArrayList<DocumentListEntry>();
			for (DocumentListEntry doc : docs.getEntries()) {
				if (!doc.getType().equals("folder")) {
					list.add(doc);
				}			
			}
			results.setEntries(list);					
		}
		return results;
	}
	
	/**
	 * Gets the remote folder by path.
	 * 
	 * @param path the path
	 * 
	 * @return the remote folder by path
	 */
	public DocumentListEntry getRemoteFolderByPath(String path) {
		if (path == null || path.length() < 1) {
			return null;
		}
		String[] pathArray = path.split("/");
		DocumentListFeed remoteSubFolders = getRootFolders();
		DocumentListEntry parentRemoteFolder = null;
		DocumentListEntry currentRemoteFolder = null;
		for (String folder : pathArray) {
			if (folder.length() == 0) {
				continue;
			}
			currentRemoteFolder = documentListFindByTitle(folder, remoteSubFolders);
			if (currentRemoteFolder == null || !currentRemoteFolder.getType().equals("folder")) {
				try {
					if (parentRemoteFolder == null) {
						currentRemoteFolder = getDocumentList().createNew(folder, "folder");
					} else {
						currentRemoteFolder = getDocumentList().createNewSubFolder(folder, parentRemoteFolder.getResourceId());
					}						
				} catch (Exception e) {
					e.printStackTrace();
				}			
			}
			remoteSubFolders = getSubFolders(currentRemoteFolder);
			parentRemoteFolder = currentRemoteFolder;
		}		
		return currentRemoteFolder;		
	}
		
	/**
	 * Gets the file count.
	 * 
	 * @param folder the folder
	 * @param recursive the recursive
	 * 
	 * @return the file count
	 */
	public int getFileCount(File folder, boolean recursive) {
		int count = folder.listFiles(fileFilter).length;
		for (File subfolder : folder.listFiles(folderFilter)) {
			count += getFileCount(subfolder, recursive);
		};
		return count;
	}

	/**
	 * Checks if is allowed format.
	 * 
	 * @param file the file
	 * 
	 * @return true, if is allowed format
	 */
	protected boolean isAllowedFormat(File file) {
		List<String> formats = new ArrayList<String>(Arrays.asList(SUPPORTED_FORMATS));
		return formats.contains(getFileExtension(file));
	}
	
	/**
	 * Checks if is allowed size.
	 * 
	 * @param file the file
	 * 
	 * @return true, if is allowed size
	 */
	protected boolean isAllowedSize(File file) {
		Long size = SIZE_LIMITS.get(getFileType(file));
		if (size != null && file.length() <= size) {
			return true;			
		}
		return false;
	}
	
	/**
	 * Gets the file name.
	 * 
	 * @param file the file
	 * 
	 * @return the file name
	 */
	protected static String getFileName(File file) {
		return file.getName().substring(0, file.getName().lastIndexOf("."));
	}
	
	/**
	 * Gets the folder name.
	 * 
	 * @param folder the folder
	 * 
	 * @return the folder name
	 */
	protected static String getFolderName(File folder) {
		return folder.getName();
	}
	
	/**
	 * Gets the file extension.
	 * 
	 * @param file the file
	 * 
	 * @return the file extension
	 */
	protected static String getFileExtension(File file) {
		return file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
	}
	
	/**
	 * Gets the file type.
	 * 
	 * @param file the file
	 * 
	 * @return the file type
	 */
	protected static String getFileType(File file) {
		return FORMATS_MAP.get(getFileExtension(file));
	}

	/**
	 * Document list find by title.
	 * 
	 * @param title the title
	 * @param documentListFeed the document list feed
	 * 
	 * @return the document list entry
	 */
	protected DocumentListEntry documentListFindByTitle(String title, DocumentListFeed documentListFeed) {
		for (DocumentListEntry doc : documentListFeed.getEntries()) {
			if (doc.getTitle().getPlainText().equals(title)) {
				return doc;
			}			
		}
		return null;
	}
	
	/**
	 * Prints out a message.
	 * 
	 * @param msg the message to be printed.
	 */
	protected static void print(String msg) {
		getOut().print(msg);
		getOut().flush();
	}
	
	/**
	 * Prints out a message.
	 * 
	 * @param msg the message to be printed.
	 */
	protected static void printLine(String msg) {
		print(msg + "\n");
	}

	/**
	 * Prints out a list of messages.
	 * 
	 * @param msg the message to be printed.
	 */
	protected static void printMessages(String[] msg) {
		for (String s : msg) {
			printLine(s);
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
	
	/**
	 * Gets the out.
	 * 
	 * @return the out
	 */
	protected static PrintWriter getOut() {
		if (out == null) {
			try {
				out = new PrintWriter(new OutputStreamWriter(System.out, "Cp866"), true);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return out;
	}
	
}
