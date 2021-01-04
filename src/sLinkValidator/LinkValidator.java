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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
 

public class LinkValidator {
	
	private static String strVersionNum = "0.17";
	private static String strProgramName = "SLinkValidator";
	private static String OS = null;

	static Pattern ptn_http		= Pattern.compile("http://");
	static Pattern ptn_no_http	= Pattern.compile("^((?!http://).)+$");
	
	//private static String strPathToGeckoDriver		= "";
	private static String strPathToWebDriver		= "";
	private static String strRootURL		= "";
	private static boolean boolOptAny		= false;
	private static boolean boolUrlList		= false;
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
	private static FileOutputStream f_out_consolelog;
	
	private static String strFnameOk               = "";
	private static String strFnameError            = "";
	private static String sttFNnameExternalLink    = "";
	private static String strFnameExceptions       = "";
	private static String strFnameSummary          = "";
	private static String strFnameConsoleLog       = "";

	private final static ConcurrentHashMap<String, Integer> visitedLinkMap = new ConcurrentHashMap<String, Integer>();
	
	private static AtomicInteger numHealthyLink		= new AtomicInteger(0);
	private static AtomicInteger numInvalidLink		= new AtomicInteger(0);
	private static AtomicInteger numExternalLinks	= new AtomicInteger(0);
	private static AtomicInteger numExceptions		= new AtomicInteger(0);
	private static AtomicInteger numConsoleSevere	= new AtomicInteger(0);
	private static AtomicInteger numConsoleWarn		= new AtomicInteger(0);
	private static int numBrowsedPages	= 0;
	
	// stack for BFS search (ConcurrentLinkedDeque class's deque).
	private static boolean boolRunAsBFSSearch = false;
	//private static ConcurrentLinkedDeque<String> stack = new ConcurrentLinkedDeque<String>();
	private static ConcurrentLinkedDeque<String> deque = new ConcurrentLinkedDeque<String>();
	//private static ConcurrentLinkedDeque<FirefoxDriver> dqBrowserDrivers = new ConcurrentLinkedDeque<FirefoxDriver>();
	private static ConcurrentLinkedDeque<WebDriver> dqBrowserDrivers = new ConcurrentLinkedDeque<WebDriver>();
	
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
	public final static FileOutputStream getFStreamOutConsoleLog() {
		return f_out_consolelog;
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
	public final static String getFnameConsoleLog() {
		return strFnameConsoleLog;
	}
	public final static String getRootURL() {
		return strRootURL;
	}
	public final static boolean getOptAny() {
		return boolOptAny;
	}
	public final static boolean getBoolUrlList() {
		return boolUrlList;
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
	/*
	public final static ConcurrentLinkedDeque<FirefoxDriver> getDQBrowserDrivers() {
		return dqBrowserDrivers;
	}
	*/
	public final static ConcurrentLinkedDeque<WebDriver> getDQBrowserDrivers() {
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
	public static void addAndGetNumConsoleSevere(int delta) {
		numConsoleSevere.addAndGet(delta);
	}
	public static void addAndGetNumConsoleWarn(int delta) {
		numConsoleWarn.addAndGet(delta);
	}
	// End of Atomic operations
	
	
 
	// main
	public static void main(String[] args) throws Exception {
		
		
		// create the command line parser
		CommandLineParser parser = new DefaultParser();
		
		// create the options object.
		Options options = new Options();

		/*
		Option optPathToGeckoDriver = Option.builder("gecko")
				.longOpt("path-to-gecko")
				.desc("[Mandatory] full path to geckodriver.exe")
				.required(true)
				.hasArg()
				.build();
				*/
		Option optPathToWebDriver = Option.builder("webdriver")
				.longOpt("path-to-webdriver")
				.desc("[Mandatory] full path to chromedriver.exe")
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


		//options.addOption(optPathToGeckoDriver);	// -gecko, --path-to-gecko
		options.addOption(optPathToWebDriver);	    // -webdriver, --path-to-webdrive
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
			String strResultsDir = "";
			
			long startTime = System.currentTimeMillis();

			String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
			
			// Parse the command line arguments.
			CommandLine cmdline = parser.parse(options, args);
			

			/*
			// -gecko (mandatory) : full path to the geckodriver binary file (e.g. [win] c:\path\to\geckodriver.exe, [mac] /path/to/geckodriver)
			if (cmdline.hasOption("gecko")) {
				strPathToGeckoDriver = cmdline.getOptionValue("gecko");	
				if (strPathToGeckoDriver == null) {
					System.out.println("Specified path to geckodriver.exe was null. Please check again.");
					System.exit(0);
				}
			}
			*/
			// -webdriver (mandatory) : full path to the geckodriver binary file (e.g. [win] c:\path\to\chromedriver.exe, [mac] /path/to/chromedriver)
			if (cmdline.hasOption("webdriver")) {
				strPathToWebDriver = cmdline.getOptionValue("webdriver");	
				if (strPathToWebDriver == null) {
					System.out.println("Specified path to webdriver.exe was null. Please check again.");
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
			// -f, -url-list (read targets from file)
			if (cmdline.hasOption("f")) {
				boolUrlList = true;
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
				
				
				strResultsDir = "results" + "-" + timeStamp;
				
				// (note)
				// java.io.File doesn't represent an open file, it represents a path in the filesystem. 
				// Therefore having close method on it doesn't make sense.
				File f_ResultDir = new File("." + File.separator + strResultsDir);
				if (!f_ResultDir.exists()) {
					f_ResultDir.mkdir();
				}

				strFnameOk            = "03.healthy_links-" + timeStamp + ".csv";
				strFnameError         = "04.broken_links-" + timeStamp + ".csv";
				sttFNnameExternalLink = "05.external_links-" + timeStamp + ".csv";
				strFnameExceptions    = "06.exceptions-" + timeStamp + ".txt";
				strFnameSummary       = "01.summary-" + timeStamp + ".txt";
				strFnameConsoleLog    = "07.console_logs-" + timeStamp + ".csv";
				
				f_out_ok	        = new FileOutputStream (strResultsDir + File.separator + strFnameOk, true);
			    f_out_error	        = new FileOutputStream (strResultsDir + File.separator + strFnameError, true);
			    f_out_externalLinks	= new FileOutputStream (strResultsDir + File.separator + sttFNnameExternalLink, true);
			    f_out_exceptions	= new FileOutputStream (strResultsDir + File.separator + strFnameExceptions, true);
			    f_out_summary	    = new FileOutputStream (strResultsDir + File.separator + strFnameSummary, true);
			    f_out_consolelog    = new FileOutputStream (strResultsDir + File.separator + strFnameConsoleLog, true);

			    String strCsvHeaders = String.format("%s,%s,%s,%s,%s,%s,%s",
										    		"Source"
							    					, "Type"
							    					, "Destination"
							    					, "Status"
							    					, "\"Status Code\""
							    					, "\"Alt text\""
							    					, "Anchor");
			    
			    new PrintStream(f_out_ok).println(strCsvHeaders);
			    new PrintStream(f_out_error).println(strCsvHeaders);
			    new PrintStream(f_out_externalLinks).println(strCsvHeaders);
			    
			    new PrintStream(f_out_consolelog).println( String.format("%s,%s,%s",
			    		                                   "LogLevel"
			    		                                   , "Message"
			    		                                   , "URL")
			    		                                 );
			    
			    FileOutputStream f_out_dequecontents = null;
				ExecutorService executorService = Executors.newFixedThreadPool(numThread);
				
				String url = "";
				String strRespCodeRedirectTo = "";
				
				try {
					
					f_out_dequecontents = new FileOutputStream (strResultsDir + File.separator + "02.browsed_pages-" + timeStamp + ".csv", true);
					new PrintStream(f_out_dequecontents).println(String.format("%s,%s,%s,%s,%s,%s,%s",
																				"URL"
																				, "Title"
																				, "\"Response Code\""
																				,"\"Response Message\""
																				,"\"Redirect To\""
																				,"\"Response Code(RedirectTo)\""
																				,"\"Response Message(RedirectTo)\"")
																);
					
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
						
						/*
						 * giving up to use geckodriver since I want to take JavaScript's console.error.
						 * changed to chromedriver instead. (2020/12/28)
						 * 
						 *  https://github.com/mozilla/geckodriver/issues/330
						 *  > andreastt commented on 11 Nov 2016
						 *  > geckodriver is an implementation of W3C WebDriver which doesn’t specify a log interface at the moment, so this is expected behaviour.
						 *  -----
						 *  > whimboo commented on 11 Feb 2019
						 *  > Starting with Firefox 65 you are able to at least route any logging through the Console API to the geckodriver log. 
						 *  > Here the entry from our geckodriver 0.24.0 release notes:
						 *  > > When using the preference devtools.console.stdout.content set to
						 *  > > true logging of console API calls like info(), warn(), and
						 *  > > error() can be routed to stdout.
						 */
						
						/*
						System.setProperty("webdriver.gecko.driver", strPathToGeckoDriver); // for Selenium 3 and FF 50+
						FirefoxDriver browserDriver_tmp = new FirefoxDriver();
						*/
						
						/*//2020/12/27 * /
						// see https://stackoverflow.com/questions/52464598/how-can-i-set-a-default-profile-for-the-firefox-driver-in-selenium-webdriver-3
						//FirefoxProfile profile = new FirefoxProfile();
						//profile.setPreference("devtools.console.stdout.content", true);
						ProfilesIni profileIni = new ProfilesIni();
						FirefoxProfile ff_profile = profileIni.getProfile("default");
						ff_profile.setPreference("devtools.console.stdout.content", true);
						FirefoxOptions ff_options = new FirefoxOptions();
						ff_options.setProfile(ff_profile);
						
						FirefoxDriver browserDriver_tmp = new FirefoxDriver(ff_options);
						/ * //end of 2020/12/27 */
						
						// 2020/12/28
						/*
						ChromeOptions chrome_opt = new ChromeOptions();
						chrome_opt.setPageLoadStrategy(PageLoadStrategy.NONE);
						
						System.setProperty("webdriver.chrome.driver", strPathToWebDriver);
						WebDriver browserDriver_tmp = new ChromeDriver(chrome_opt);
						*/
						/* //end of 2020/12/28 */
						
						System.setProperty("webdriver.chrome.driver", strPathToWebDriver);
						WebDriver browserDriver_tmp = new ChromeDriver();
						
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

							// obtain http response code
							ResponseDataObj respData = RunnableLinkChecker.isLinkBroken(new URL(url), strUid, strPasswd, boolOptInstanceFollowRedirects);
							strRespCodeRedirectTo = "";
							if (respData.getRespCodeRedirectTo() != 0) {
								strRespCodeRedirectTo = String.valueOf(respData.getRespCodeRedirectTo());
							}
							new PrintStream(f_out_dequecontents).println(String.format("%s,%s,%s,%s,%s,%s,%s",
																						"\"" + url.replaceAll("\"", "\"\"") + "\""
																						, "\"" + respData.getPageTitle().replaceAll("\"", "\"\"") + "\""
																						, respData.getRespCode()
																						, "\"" + respData.getRespMsg().replaceAll("\"", "\"\"") + "\""
																						, "\"" + respData.getRedirectUrl().replaceAll("\"", "\"\"") + "\""
																						, strRespCodeRedirectTo
																						, "\"" + respData.getRespMsgRedirectTo().replaceAll("\"", "\"\"") + "\"")
																		);
							
							RunnableLinkChecker runnable = new RunnableLinkChecker(Integer.toString(numBrowsedPages) + "_" + timeStamp
																					, url
																					, strUid
																					, strPasswd
																					, strResultsDir
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

								// obtain http response code
								ResponseDataObj respData = RunnableLinkChecker.isLinkBroken(new URL(url), strUid, strPasswd, boolOptInstanceFollowRedirects);
								strRespCodeRedirectTo = "";
								if (respData.getRespCodeRedirectTo() != 0) {
									strRespCodeRedirectTo = String.valueOf(respData.getRespCodeRedirectTo());
								}
								new PrintStream(f_out_dequecontents).println(String.format("%s,%s,%s,%s,%s,%s,%s",
																							"\"" + url.replaceAll("\"", "\"\"") + "\""
																							, "\"" + respData.getPageTitle().replaceAll("\"", "\"\"") + "\""
																							, respData.getRespCode()
																							, "\"" + respData.getRespMsg().replaceAll("\"", "\"\"") + "\""
																							, "\"" + respData.getRedirectUrl().replaceAll("\"", "\"\"") + "\""
																							, strRespCodeRedirectTo
																							, "\"" + respData.getRespMsgRedirectTo().replaceAll("\"", "\"\"") + "\"")
																			);;

								
								RunnableLinkChecker runnable = new RunnableLinkChecker(Integer.toString(numBrowsedPages) + "_" + timeStamp
																						, url
																						, strUid
																						, strPasswd
																						, strResultsDir
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
				String stNoteInCaseSkipelement = "";
				
				if ( boolOptSkipElement == true)
				{
					stNoteInCaseSkipelement = " (element check was skipped)";
				}
			    
				System.out.println("It took " + TimeUnit.MILLISECONDS.toSeconds(differenceTime) + " seconds.");
				System.out.println("Total healthy Links = "		+	numHealthyLink		+ stNoteInCaseSkipelement);
			    System.out.println("Total broken Links = "		+	numInvalidLink		+ stNoteInCaseSkipelement);
			    System.out.println("Total Exceptions = "		+	numExceptions		+ stNoteInCaseSkipelement);
			    System.out.println("Total External Links = "	+	numExternalLinks	+ stNoteInCaseSkipelement);
			    System.out.println("Total Browsed Pages = "		+	numBrowsedPages);
			    System.out.println("Total SEVERE in browser console = "		+	numConsoleSevere);
			    System.out.println("Total WARNING in browser console = "	+	numConsoleWarn);
			    
			    new PrintStream(f_out_summary).println("It took " + TimeUnit.MILLISECONDS.toSeconds(differenceTime) + " seconds.");
			    new PrintStream(f_out_summary).println("Total healthy Links = "		+	numHealthyLink		+ stNoteInCaseSkipelement);
			    new PrintStream(f_out_summary).println("Total broken Links = "		+	numInvalidLink		+ stNoteInCaseSkipelement);
			    new PrintStream(f_out_summary).println("Total Exceptions = "		+	numExceptions		+ stNoteInCaseSkipelement);
			    new PrintStream(f_out_summary).println("Total External Links = "	+	numExternalLinks	+ stNoteInCaseSkipelement);
			    new PrintStream(f_out_summary).println("Total Browsed Pages = "		+	numBrowsedPages);
			    new PrintStream(f_out_summary).println("Total SEVERE in browser console = "		+	numConsoleSevere);
			    new PrintStream(f_out_summary).println("Total WARNING in browser console = "	+	numConsoleWarn);
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
			    
			    new PrintStream(f_out_consolelog).println("Total SEVERE  = " + numConsoleSevere);
			    new PrintStream(f_out_consolelog).println("Total WARNING = " + numConsoleWarn);
			    new PrintStream(f_out_consolelog).println(" ");
			    f_out_consolelog.close();
			    
			    // re-format the output of console.log.
			    new ReformatConsoleLogOutputFile(timeStamp);
				
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
				//FirefoxDriver browserDriver = dqBrowserDrivers.pop();
				WebDriver browserDriver = dqBrowserDrivers.pop();
				browserDriver.close();
			}
			
			OS = System.getProperty("os.name");
			// cleanup the geckodriver in case Windows
			if ( OS.startsWith("Windows") ) {
				//Runtime.getRuntime().exec("taskkill /F /IM geckodriver.exe /T");
				Runtime.getRuntime().exec("taskkill /F /IM chromedriver.exe /T");
			}

		}
		   
	}

}
