// Copyright 2015 Koji Nobumoto
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package sLinkValidator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openqa.selenium.firefox.FirefoxDriver;
 

public class LinkValidator {
	
	private static String strVersionNum = "0.15";
	private static String strProgramName = "SLinkValidator";
	private static String OS = null;

	static Pattern ptn_http		= Pattern.compile("http://");
	static Pattern ptn_no_http	= Pattern.compile("^((?!http://).)+$");
	
	private static String strPathToGeckoDriver		= "";
	private static String strRootURL		= "";
	private static boolean boolOptAny		= false;
	private static boolean boolOptVerbose	= false;
	private static boolean boolOptScreencapture	= false;
	private static boolean boolOptSkipElement	= false;
	private static boolean boolOptSitemapMode	= false;
	private static boolean boolOptInstanceFollowRedirects	= false;
	private static int numTimeoutSec = 60; // actually it will be *2 (e.g. if you set 60, the timeout will be 120 sec).
	private static int numImplicitlyWait = 30;
	private static int numThread	= 1;
	// (a note about numMaxThread)
	// Since the default initial capacity of ConcurrentHashMap() ("concurrencyLevel") 
	// is 16, I set the max thread number to be 16.
	private static int numMaxThread = 16;

	private static FileOutputStream f_out_ok;
	private static FileOutputStream f_out_error;
	private static FileOutputStream f_out_externalLinks;
	private static FileOutputStream f_out_exceptions;
	private static FileOutputStream f_out_summary;
	
	private static String strFnameOk = "";
	private static String strFnameError = "";
	private static String sttFNnameExternalLink = "";
	private static String strFnameExceptions = "";
	private static String strFnameSummary = "";

	private final static ConcurrentHashMap<String, Integer> visitedLinkMap = new ConcurrentHashMap<String, Integer>();
	
	private static AtomicInteger numHealthyLink		= new AtomicInteger(0);
	private static AtomicInteger numInvalidLink		= new AtomicInteger(0);
	private static AtomicInteger numExternalLinks	= new AtomicInteger(0);
	private static AtomicInteger numExceptions		= new AtomicInteger(0);
	private static int numBrowsedPages	= 0;
	
	// stack for BFS search (ConcurrentLinkedDeque class's deque).
	private static boolean boolRunAsBFSSearch = false;
	//private static ConcurrentLinkedDeque<String> stack = new ConcurrentLinkedDeque<String>();
	private static ConcurrentLinkedDeque<String> deque = new ConcurrentLinkedDeque<String>();
	private static ConcurrentLinkedDeque<FirefoxDriver> dqBrowserDrivers = new ConcurrentLinkedDeque<FirefoxDriver>();
	
	//////////////////////////////
	//
	// Beginning of getter definitions.
	//
	public final static FileOutputStream getFStreamOutOk() {
		return f_out_ok;
	}
	public final static FileOutputStream getFStreamOutError() {
		return f_out_error;
	}
	public final static FileOutputStream getFStreamOutExternalSites() {
		return f_out_externalLinks;
	}
	public final static FileOutputStream getFStreamOutExceptions() {
		return f_out_exceptions;
	}
	public final static ConcurrentHashMap<String, Integer> getVisitedLinkMap() {
		return visitedLinkMap;
	}
	/*
	public final static String getPathToGeckoDriver() {
		return strPathToGeckoDriver;
	}
	*/
	public final static String getFnameOK() {
		return strFnameOk;
	}
	public final static String getFnameError() {
		return strFnameError;
	}
	public final static String getFnameExternalLink() {
		return sttFNnameExternalLink;
	}
	public final static String getFnameExceptions() {
		return strFnameExceptions;
	}
	public final static String getRootURL() {
		return strRootURL;
	}
	public final static boolean getOptAny() {
		return boolOptAny;
	}
	public final static boolean getOptVerboseFlg() {
		return boolOptVerbose;
	}
	public final static boolean getOptScreenCaptureFlg() {
		return boolOptScreencapture;
	}
	public final static boolean getOptSkipElementFlg() {
		return boolOptSkipElement;
	}
	public final static boolean getSitemapModeFlg() {
		return boolOptSitemapMode;
	}
	public final static boolean getOptInstanceFollowRedirects() {
		return boolOptInstanceFollowRedirects;
	}
	/*
	public final static ConcurrentLinkedDeque<String> getStack() {
		return stack;
	}
	*/
	public final static ConcurrentLinkedDeque<String> getDeque() {
		return deque;
	}
	public final static ConcurrentLinkedDeque<FirefoxDriver> getDQBrowserDrivers() {
		return dqBrowserDrivers;
	}
	public final static int getNumTimeoutSec() {
		return numTimeoutSec;
	}
	public final static int getnumImplicitlyWaitSec() {
		return numImplicitlyWait;
	}
	//
	// End of getter definitions
	

	//////////////////////////////
	//
	// Beginning of Atomic operations
	//
	public static void addAndGetNumHealthyLink(int delta) {
		numHealthyLink.addAndGet(delta);
	}
	public static void addAndGetNumInvalidLink(int delta) {
		numInvalidLink.addAndGet(delta);
	}
	public static void addAndGetNumExternalLinks(int delta) {
		numExternalLinks.addAndGet(delta);
	}
	public static void addAndGetNumExceptions(int delta) {
		numExceptions.addAndGet(delta);
	}

	// End of Atomic operations
	
	
 
	// main
	public static void main(String[] args) throws Exception {
		
		
		// create the command line parser
		CommandLineParser parser = new DefaultParser();
		
		// create the options object.
		Options options = new Options();

		Option optPathToGeckoDriver = Option.builder("gecko")
				.longOpt("path-to-gecko")
				.desc("[Mandatory] full path to geckodriver.exe")
				.required(true)
				.hasArg()
				.build();
		Option optAny	= Option.builder( "a" )
				.longOpt("all")
				.desc("Also check \"link\" tag.")
				.required(false)
				.build();
		Option optListFile = Option.builder( "f" )
				.longOpt("url-list")
				.desc("Specify a text file containing urls to be checked.")
				.required(false)
				.hasArg()
				.argName("FILE")
				.build();
		Option optHelp	= Option.builder("h")
				.longOpt("help")
				.desc("print this help.")
				.required(false)
				.build();	
		Option optUid	= Option.builder("id")
				.longOpt("user")
				.desc("user id for the BASIC authentication.")
				.required(false)
				.hasArg()
				.argName("USERNAME")
				.build();
		Option optCapture	= Option.builder("capture")
				.longOpt("screenshot")
				.desc("take the page capture.")
				.required(false)
				.build();
		Option optSkipElement	= Option.builder("skipelement")
				.longOpt("no-element-check")
				.desc("checks given url only, no element in the page is checked.")
				.required(false)
				.build();
		Option optTimeOut	= Option.builder("o")
				.longOpt("timeout")
				.desc("timeout second.")
				.required(false)
				.hasArg()
				.argName("TIMEOUT")
				.build();
		Option optImplicitlyWait	= Option.builder("implicitlywait")
				.longOpt("implicitlywait")
				.desc("implicitlywait second.")
				.required(false)
				.hasArg()
				.argName("IMPLICITLYWAIT")
				.build();
		Option optPasswd	= Option.builder("p")
				.longOpt("password")
				.desc("password for the BASIC authentication.")
				.required(false)
				.hasArg()
				.argName("PASSWORD")
				.build();
		Option optNumThread	= Option.builder("T")
				.longOpt("thread")
				.desc("number of thread (must be an integer, less than " + numMaxThread + "). 'AUTO' for available processer num. ")
				.required(false)
				.hasArg()
				.argName("NUM of Thread")
				.build();
		Option optUrl	= Option.builder("url")
				.desc("Base URL to be checked.")
				.required(false)
				.hasArg()
				.argName("URL")
				.build();
		Option optSitemapMode	= Option.builder("s")
				.longOpt("sitemap")
				.desc("Sitemap mode. Follows only <a> tag.")
				.required(false)
				.argName("SITEMAP")
				.build();
		Option optInstanceFollowRedirects	= Option.builder("instancefollowredirects")
				.longOpt("instancefollowredirects")
				.desc("if set, HttpURLConnection follows redirects.")
				.required(false)
				.argName("INSTANCEFOLLOWREDIRECTS")
				.build();
		Option optVerbose	= Option.builder("v")
				.longOpt("verbose")
				.desc("verbose output mode. (outputs all result on colsole)")
				.required(false)
				.build();
		Option optVersionNum	= Option.builder("V")
				.longOpt("version")
				.desc("print version number.")
				.required(false)
				.build();


		options.addOption(optPathToGeckoDriver);	// -gecko, --path-to-gecko
		options.addOption(optAny);					// -a, -all
		options.addOption(optListFile);				// -f, -url-list
		options.addOption(optHelp);					// -h, -help
		options.addOption(optUid);					// -id, -user
		options.addOption(optCapture);			    // -capture, -screenshot
		options.addOption(optSkipElement);			// -skipelement, -no-element-check
		options.addOption(optPasswd);				// -p, -password
		options.addOption(optNumThread);			// -T, -thread
		options.addOption(optTimeOut);				// -o, -timeout
		options.addOption(optImplicitlyWait);		// -implicitlywait
		options.addOption(optUrl);					// -url
		options.addOption(optSitemapMode);			// -s, -sitemap
		options.addOption(optInstanceFollowRedirects);			// -instancefollowredirects
		options.addOption(optVerbose);				// -v, -verbose
		options.addOption(optVersionNum);			// -V, -version
		
		
		try {
			
			String strUid	= "";
			String strPasswd	= "";
			
			long startTime = System.currentTimeMillis();

			String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
			
			// Parse the command line arguments.
			CommandLine cmdline = parser.parse(options, args);
			

			// -gecko (mandatory) : full path to the geckodriver binary file (e.g. [win] c:\path\to\geckodriver.exe, [mac] /path/to/geckodriver)
			if (cmdline.hasOption("gecko")) {
				strPathToGeckoDriver = cmdline.getOptionValue("gecko");	
				if (strPathToGeckoDriver == null) {
					System.out.println("Specified path to geckodriver.exe was null. Please check again.");
					System.exit(0);
				}
			}
			else {
				System.out.println("You Must Specify full path to geckodriver.exe. e.g. '-gecko C:\\Program Files (x86)\\geckodriver\\geckodriver.exe'");
				System.exit(0);
			}
			
			// -a : any flag. (checks <link> tag's href)
			if (cmdline.hasOption("a")) {
				boolOptAny = true;
			}
			// -h : show help.  or no option specified
			if ( cmdline.hasOption("h") || args.length == 0) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(strProgramName, options, true);
				System.exit(0);
			}
			// -id : User ID for BASIC auth.
			if (cmdline.hasOption("id")) {
				strUid = cmdline.getOptionValue("id");
			}
			// -capture : take capture.
			if (cmdline.hasOption("capture")) {
				boolOptScreencapture = true;
			}
			// -skipelement : no element link check within the page.
			if (cmdline.hasOption("skipelement")) {
				boolOptSkipElement = true;
			}
			// -p : password for BASIC auth.
			if (cmdline.hasOption("p")) {
				strPasswd = cmdline.getOptionValue("p");
			}
			// -o : timeout second.
			if (cmdline.hasOption("o")) {
				numTimeoutSec = Integer.parseInt(cmdline.getOptionValue("o"))/2;
			}
			// -implicitlywait : implicitlywait second.
			if (cmdline.hasOption("implicitlywait")) {
				numImplicitlyWait = Integer.parseInt(cmdline.getOptionValue("implicitlywait"))/2;
			}
			// -s : sitemap Mode (follows only <a> tag).
			if (cmdline.hasOption("s")) {
				boolOptSitemapMode = true;
			}
			// -instancefollowredirects (if set HttpURLConnection follows redirects.).
			if (cmdline.hasOption("instancefollowredirects")) {
				boolOptInstanceFollowRedirects = true;
			}
			// -T : num of thread.
			if (cmdline.hasOption("T")) {
				if ( cmdline.getOptionValue("T").equalsIgnoreCase("auto") ) {
					
					numThread = Runtime.getRuntime().availableProcessors();
					if (numThread > numMaxThread ) {
						numThread = numMaxThread;
					}
					
				}
				else {
					try {
						numThread = Integer.parseInt(cmdline.getOptionValue("T"));
						if (numThread > numMaxThread ) {
							System.err.println( "Please specify the number less than " + numMaxThread + "." );
							HelpFormatter formatter = new HelpFormatter();
							formatter.printHelp(strProgramName, options, true);
							System.exit(0);
						}
					}
					catch (NumberFormatException e) {
						System.err.println(e.getMessage());
						System.err.println( "Thread number must be an integer (less than " + numMaxThread + ")." );
						HelpFormatter formatter = new HelpFormatter();
						formatter.printHelp(strProgramName, options, true);
						System.exit(0);
					}
				}
			}
			// -V : show version
			if ( cmdline.hasOption("V") ) {
				System.out.println(strProgramName + " : Version " + strVersionNum + "." );
				System.exit(0);
			}
			// -v : verbose mode flag
			if ( cmdline.hasOption("v") ) {
				boolOptVerbose = true;
			}
			
			// illegal combination of options.
			if ( cmdline.hasOption("f") && cmdline.hasOption("url") ) {
				System.err.println( "Cannot specify \"-url\" and \"-f\" option at the same time." );
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(strProgramName, options, true);
				System.exit(0);
			}
			if ( cmdline.hasOption("a") && cmdline.hasOption("skipelement") ) {
				System.err.println( "Cannot specify \"-a\" and \"-skipelement\" option at the same time." );
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(strProgramName, options, true);
				System.exit(0);
			}
			if ( cmdline.hasOption("a") && cmdline.hasOption("s") ) {
				System.err.println( "Cannot specify \"-a\" (any) and \"-s\" (sitemapmode) option at the same time." );
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(strProgramName, options, true);
				System.exit(0);
			}
			if ( cmdline.hasOption("skipelement") && cmdline.hasOption("s") ) {
				System.err.println( "Cannot specify \"-skipelement\" and \"-s\" (sitemapmode) option at the same time." );
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(strProgramName, options, true);
				System.exit(0);
			}
			if ( !cmdline.hasOption("f") && !cmdline.hasOption("url") ) {
				System.err.println( "Either \"-f\" or \"-url\" must be specified." );
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(strProgramName, options, true);
				System.exit(0);
			}

			
			// in case given file of URL lists or given root url
			if (cmdline.hasOption("f") || cmdline.hasOption("url")) {
				
				// (attention)
				// This software does not support Chrome and InternetExplorer
				// since they cannot take full page screenshot.
				
				// Chrome
				//System.setProperty("webdriver.chrome.driver", "some/path/to/chromedriver");
				//WebDriver browserDriver = new ChromeDriver();
				
				// InternetExplorer
				//System.setProperty("webdriver.ie.driver", "some/path/to/IEDriverServer");
				//DesiredCapabilities ieCapabilities = DesiredCapabilities.internetExplorer();
				//ieCapabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
				//WebDriver browserDriver = new InternetExplorerDriver(ieCapabilities);
				//WebDriver browserDriver = new InternetExplorerDriver();
				
				
				// (note)
				// java.io.File doesn't represent an open file, it represents a path in the filesystem. 
				// Therefore having close method on it doesn't make sense.
				File f_ResultDir = new File("." + File.separator + "results");
				if (!f_ResultDir.exists()) {
					f_ResultDir.mkdir();
				}

				strFnameOk            = "healthy_links-" + timeStamp + ".csv";
				strFnameError         = "broken_links-" + timeStamp + ".csv";
				sttFNnameExternalLink = "external_links-" + timeStamp + ".csv";
				strFnameExceptions    = "exceptions-" + timeStamp + ".txt";
				strFnameSummary       = "summary-" + timeStamp + ".txt";
				
				f_out_ok	= new FileOutputStream ("results" + File.separator + strFnameOk, true);
			    f_out_error	= new FileOutputStream ("results" + File.separator + strFnameError, true);
			    f_out_externalLinks	= new FileOutputStream ("results" + File.separator + sttFNnameExternalLink, true);
			    f_out_exceptions	= new FileOutputStream ("results" + File.separator + strFnameExceptions, true);
			    f_out_summary	    = new FileOutputStream ("results" + File.separator + strFnameSummary, true);

			    String strCsvHeaders = "Source"
			    					+ "," + "Type"
			    					+ "," + "Destination"
			    					+ "," + "Status"
			    					+ "," + "\"Status Code\""
			    					+ "," + "\"Alt text\""
			    					+ "," + "Anchor";
			    
			    new PrintStream(f_out_ok).println(strCsvHeaders);
			    new PrintStream(f_out_error).println(strCsvHeaders);
			    new PrintStream(f_out_externalLinks).println(strCsvHeaders);
			    
			    FileOutputStream f_out_dequecontents = null;
				ExecutorService executorService = Executors.newFixedThreadPool(numThread);
				
				String url = "";
				
				try {
					
					f_out_dequecontents = new FileOutputStream ("results" + File.separator + "browsed_pages-" + timeStamp + ".csv", true);
					new PrintStream(f_out_dequecontents).println("URL,\"Response Code\",\"Response Message\",\"Redirect To\"");
					
					if (cmdline.hasOption("f")) {
						// given file of url lists
						
						boolRunAsBFSSearch = false;
						
						String urlListFile = cmdline.getOptionValue("f");
						
						File f = new File(urlListFile);
						if(!f.exists() || f.isDirectory()) {
							System.err.println("The specified file \"" + urlListFile + "\" does not exist.");
							System.exit(0);
						}
						
					    BufferedReader f_in = new BufferedReader(new FileReader(urlListFile));
						
						while( (url = f_in.readLine()) != null ) {		
							deque.addLast(url);
						}
						
						f_in.close();
						
					}
					else if (cmdline.hasOption("url")) {
						// root url was given
						
						//BFS
						boolRunAsBFSSearch = true;
						
						strRootURL = cmdline.getOptionValue("url");
						if (!strRootURL.matches(".*[.](htm(l)?|pdf)$") 
								&& strRootURL.matches(".*[A-Za-z0-9]$"))
						{
							strRootURL = strRootURL + "/";
						}
						
						//stack.push(strRootURL);
						deque.add(strRootURL);
						new PrintStream(f_out_dequecontents).println("[Root URL] is : " + strRootURL + "\n");
						
					}
					
					int bdCnd = numThread;
					while(bdCnd > 0) {
						System.setProperty("webdriver.gecko.driver", strPathToGeckoDriver); // for Selenium 3 and FF 50+
						FirefoxDriver browserDriver_tmp = new FirefoxDriver();
						browserDriver_tmp.manage().timeouts().pageLoadTimeout(numTimeoutSec, TimeUnit.SECONDS);
						//browserDriver_tmp.manage().timeouts().implicitlyWait(numTimeoutSec, TimeUnit.SECONDS);  // (note) want to set to 120 second but somehow, it waits (second * 2) second. Bug?
						browserDriver_tmp.manage().timeouts().implicitlyWait(numImplicitlyWait, TimeUnit.SECONDS);
						browserDriver_tmp.manage().timeouts().setScriptTimeout(numTimeoutSec, TimeUnit.SECONDS);
						
						dqBrowserDrivers.addLast(browserDriver_tmp);
						
						bdCnd--;
					}
						
					
					// run thread(s).
					//while(!stack.isEmpty()) {
					while(!deque.isEmpty()) {
						
						if (numThread == 1) {
							// In case numThread is 1, perform the check by the safest way.
							
							url = deque.pop();
							
							//new PrintStream(f_out_dequecontents).println(url);

							// obtain http response code
							ResponseDataObj respData = RunnableLinkChecker.isLinkBroken(new URL(url), strUid, strPasswd, boolOptInstanceFollowRedirects);
							new PrintStream(f_out_dequecontents).println("\"" + url.replaceAll("\"", "\"\"") + "\""
																		+ "," + respData.getRespCode()
																		+ "," + "\"" + respData.getRespMsg().replaceAll("\"", "\"\"") + "\""
																		+ "," + "\"" + respData.getRedirectUrl().replaceAll("\"", "\"\"") + "\"");
							
							RunnableLinkChecker runnable = new RunnableLinkChecker(Integer.toString(numBrowsedPages) + "_" + timeStamp
																					, url
																					, strUid
																					, strPasswd
																					, boolRunAsBFSSearch);
							
							
							Thread thread_1 = new Thread( runnable, Integer.toString(numBrowsedPages) );
							thread_1.start();
							thread_1.join();
							
							numBrowsedPages++;
							
							
						}
						else {
							
							int numThreadCnt = numThread;
							int numArrSize = ( numThread <= deque.size()) ? numThread : deque.size();
							List<Callable<Object>> todo = new ArrayList<Callable<Object>>(numArrSize);
							
							while(!deque.isEmpty() || (numThreadCnt > 0 && deque.size() >= numThreadCnt ) ) {
								
								url = deque.pop();
								
								// new PrintStream(f_out_dequecontents).println(url);
								
								// obtain http response code
								ResponseDataObj respData = RunnableLinkChecker.isLinkBroken(new URL(url), strUid, strPasswd, boolOptInstanceFollowRedirects);
								new PrintStream(f_out_dequecontents).println("\"" + url.replaceAll("\"", "\"\"") + "\""
																				+ "," + respData.getRespCode()
																				+ "," + "\"" + respData.getRespMsg().replaceAll("\"", "\"\"") + "\""
																				+ "," + "\"" + respData.getRedirectUrl().replaceAll("\"", "\"\"") + "\"");

								
								RunnableLinkChecker runnable = new RunnableLinkChecker(Integer.toString(numBrowsedPages) + "_" + timeStamp
																						, url
																						, strUid
																						, strPasswd
																						, boolRunAsBFSSearch);
								//executorService.execute(runnable);
								todo.add(Executors.callable(runnable));
								
								numBrowsedPages++;
								numThreadCnt--;
							}
							
							List<Future<Object>> futures = executorService.invokeAll(todo);
							if (boolOptVerbose) {
								for(Future<Object> future : futures) {
									if (future.get() == null) {
										System.out.println("future.get() is null. The futre object is " + future.toString() + " .");
										new PrintStream(f_out_exceptions).println("future.get() is null. The futre object is " + future.toString() + " .");
									}
									else {
										new PrintStream(f_out_ok).println("future.get = " + future.get().toString());
									}
								}
							}
							numThreadCnt = numThread;
	
						}
							
					}
					if (!executorService.isShutdown()) {
						executorService.shutdown();
						while (!executorService.awaitTermination(600, TimeUnit.SECONDS)) {
							;
						}
					}
					
					//new PrintStream(f_out_dequecontents).println("Total Browsed Pages = " + numBrowsedPages);
					PrintStream printStream = new PrintStream(f_out_dequecontents);
					printStream.println("Total Browsed Pages = " + numBrowsedPages);
					printStream.close();
					
					new PrintStream(f_out_dequecontents).println(" ");
								
				}
				finally {
					try { f_out_dequecontents.close(); } catch (Exception e) {}
				}
				
				long endTime = System.currentTimeMillis();
				long differenceTime = endTime - startTime;
			    
				System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(differenceTime) + " seconds.");
				System.out.println("Total healthy Links = "	+	numHealthyLink);
			    System.out.println("Total broken Links = "	+	numInvalidLink);
			    System.out.println("Total Exceptions = "	+	numExceptions);
			    System.out.println("Total External Links = "	+	numExternalLinks);
			    System.out.println("Total Browsed Pages = "	+	numBrowsedPages);
			    
			    new PrintStream(f_out_summary).println("It took " + TimeUnit.MILLISECONDS.toSeconds(differenceTime) + " seconds.");
			    new PrintStream(f_out_summary).println("Total healthy Links = "	+	numHealthyLink);
			    new PrintStream(f_out_summary).println("Total broken Links = "	+	numInvalidLink);
			    new PrintStream(f_out_summary).println("Total Exceptions = "	+	numExceptions);
			    new PrintStream(f_out_summary).println("Total External Links = "	+	numExternalLinks);
			    new PrintStream(f_out_summary).println("Total Browsed Pages = "	+	numBrowsedPages);
			    f_out_summary.close();
			    
			    new PrintStream(f_out_error).println("Total broken Links = " + numInvalidLink);
			    new PrintStream(f_out_error).println(" ");
			    f_out_error.close();
			    
			    new PrintStream(f_out_ok).println("It took " + TimeUnit.MILLISECONDS.toSeconds(differenceTime) + " seconds.");
			    new PrintStream(f_out_ok).println("Total healthy Links = " + numHealthyLink);
			    new PrintStream(f_out_ok).println(" ");
			    f_out_ok.close();
			    
			    new PrintStream(f_out_externalLinks).println("Total External Linkis = " + numExternalLinks);
			    new PrintStream(f_out_externalLinks).println(" ");
			    f_out_externalLinks.close();
			    
			    new PrintStream(f_out_exceptions).println("Total Exceptions = " + numExceptions);
			    new PrintStream(f_out_exceptions).println(" ");
			    f_out_exceptions.close();
				
			}
			
		}
		catch( ParseException exp) {
			// oops, something went wrong
	        System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
	        HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("BrokenLinkChecker", options);
			System.exit(0);
		}
		finally {
			
			while( !dqBrowserDrivers.isEmpty() ) {
				FirefoxDriver browserDriver = dqBrowserDrivers.pop();
				browserDriver.close();
			}
			
			OS = System.getProperty("os.name");
			// cleanup the geckodriver in case Windows
			if ( OS.startsWith("Windows") ) {
				Runtime.getRuntime().exec("taskkill /F /IM geckodriver.exe /T");
			}

		}
		   
	}

}
