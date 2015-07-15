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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

public class RunnableLinkChecker implements Runnable {

	private String strThreadID;
	private String strURL;
	private String uid;
	private String password;
	private int numTimeoutSec;
	private boolean boolRunAsDFSSearch;
	
	static Pattern ptn_http		= Pattern.compile("^https{0,1}://");
	static Pattern ptn_no_http	= Pattern.compile("^((?!https{0,1}://).)+$");
	
	private ThreadLocal<Integer> numHealthyLink;
	private ThreadLocal<Integer> numInvalidLink;
	private ThreadLocal<Integer> numExternalLinks;
	private ThreadLocal<Integer> numExceptions;
	
	private static ThreadLocal<FileOutputStream> f_out_ok;
	private static ThreadLocal<FileOutputStream> f_out_error;
	private static ThreadLocal<FileOutputStream> f_out_externalLinks;
	private static ThreadLocal<FileOutputStream> f_out_exceptions;
	
	private static ThreadLocal<String> strFname_ok;
	private static ThreadLocal<String> strFname_error;
	private static ThreadLocal<String> strFname_externalLinks;
	private static ThreadLocal<String> strFname_exceptions;
	
	public RunnableLinkChecker(String __strThreadID
								, String __url
								, String __uid
								, String __password
								, int __timeout
								, boolean __boolRunAsDFSSearch) throws FileNotFoundException {
		
		this.strThreadID	= __strThreadID;
		this.strURL			= __url;
		this.uid			= __uid;
		this.password		= __password;
		this.numTimeoutSec	= __timeout;
		this.boolRunAsDFSSearch			= __boolRunAsDFSSearch;
		
		numHealthyLink	= new ThreadLocal<Integer>() {
							@Override protected Integer initialValue() {
								Integer zero = new Integer(0);
								return zero;
							}
						};
		numInvalidLink	= new ThreadLocal<Integer>() {
							@Override protected Integer initialValue() {
								Integer zero = new Integer(0);
								return zero;
							}
						};
		numExternalLinks	= new ThreadLocal<Integer>() {
							@Override protected Integer initialValue() {
								Integer zero = new Integer(0);
								return zero;
							}
						};
		numExceptions	= new ThreadLocal<Integer>() {
							@Override protected Integer initialValue() {
								Integer zero = new Integer(0);
								return zero;
							}
						};

		
		strFname_ok	= new ThreadLocal<String>() {
							@Override protected String initialValue() {
									return "";
							}
						};
		strFname_error	= new ThreadLocal<String>() {
							@Override protected String initialValue() {
									return "";
							}
						};
		strFname_externalLinks	= new ThreadLocal<String>() {
							@Override protected String initialValue() {
								return "";
							}
						};
		strFname_exceptions	= new ThreadLocal<String>() {
								@Override protected String initialValue() {
									return "";
								}
							};
							
		
		f_out_ok	= new ThreadLocal<FileOutputStream>() {
							@Override protected FileOutputStream initialValue() {
								FileOutputStream fos = null;
								try {
									fos = new FileOutputStream(strFname_ok.get());
								} catch (FileNotFoundException e) {
									e.printStackTrace();
									String exp_msg = "Exception in initialValue() of f_out_ok : " + e.getMessage();
							    	System.out.println(exp_msg);
								}
								return fos;
							}
						};
		f_out_error	= new ThreadLocal<FileOutputStream>() {
							@Override protected FileOutputStream initialValue() {
								FileOutputStream fos = null;
								try {
									fos = new FileOutputStream(strFname_error.get());
								} catch (FileNotFoundException e) {
									e.printStackTrace();
									String exp_msg = "Exception in initialValue() of f_out_error : " + e.getMessage();
							    	System.out.println(exp_msg);
								}
								return fos;
							}
						};
		f_out_externalLinks	= new ThreadLocal<FileOutputStream>() {
									@Override protected FileOutputStream initialValue() {
										FileOutputStream fos = null;
										try {
											fos = new FileOutputStream(strFname_externalLinks.get());
										} catch (FileNotFoundException e) {
											e.printStackTrace();
											String exp_msg = "Exception in initialValue() of f_out_externalLinks : " + e.getMessage();
									    	System.out.println(exp_msg);
										}
										return fos;
									}
								};
		f_out_exceptions	= new ThreadLocal<FileOutputStream>() {
									@Override protected FileOutputStream initialValue() {
										FileOutputStream fos = null;
										try {
											fos = new FileOutputStream(strFname_exceptions.get());
										} catch (FileNotFoundException e) {
											e.printStackTrace();
											String exp_msg = "Exception in initialValue() of f_out_exceptions : " + e.getMessage();
									    	System.out.println(exp_msg);
										}
										return fos;
									}
								};
		

	}
	
	
	/******************************
	 * findAllLinks(WebDriver driver, boolean boolOptAny)
	 * 				: find links to be checked in the web page (seek the links in the page source).	
	 ******************************
	 * @param driver : WebDriver (IOW, browser driver)
	 * @param boolOptAny : if true, include <link href="xxxx">'s href part.
	 * @return : ArrayList<WebElement>
	 *****/
	public static ArrayList<WebElement>  findAllLinks(WebDriver driver, boolean boolOptAny)
	  {
	 
		  ArrayList<WebElement> elementList = new ArrayList<WebElement>();
		 
		  elementList = (ArrayList<WebElement>) driver.findElements(By.tagName("a"));
		  elementList.addAll(driver.findElements(By.tagName("img")));
		  
		  if (boolOptAny) {
			  elementList.addAll(driver.findElements(By.tagName("link")));
		  }
		 
		  ArrayList<WebElement>  finalList = new ArrayList<WebElement>();
		 
		  for (WebElement element : elementList)
		  {
			  if(element.getAttribute("href") != null || element.getAttribute("src") != null)
			  {
				  finalList.add(element);
			  }	  
		 
		  }	
		 
		  return finalList;
	 
	  }
	
	/******************************
	 * isLinkBroken(URL url, String uid, String password)
	 * 				: check if given url is broken or not (access to the url and return the status code).
	 ******************************
	 * 
	 * @param url  : URL to be checked 
	 * @param uid  : User ID for the Basic authentication.
	 * @param password : password for the Basic authentication.
	 * @return ResponseDataObj(response, statusCode)
	 * @throws Exception
	 */
	public static ResponseDataObj isLinkBroken(URL url, String uid, String password) 
	{
		
		String response = "";
		int statusCode = 0;
		HttpURLConnection connection;
	 
		try
		{
			
			connection = (HttpURLConnection) url.openConnection();
			
			// in case of basic auth.
			if (uid != "" || password != "") {
				String userpass = uid+":"+password;
				new Base64();
				String basicAuth = "Basic " + new String(Base64.encode(userpass.getBytes()));
				connection.setRequestProperty("Authorization", basicAuth);
			}
			
		    connection.connect();
		    response	= connection.getResponseMessage();
		    statusCode	= connection.getResponseCode();	        
		    connection.disconnect();
		 
		    return new ResponseDataObj(response, statusCode);
		 
		}
		catch(Exception exp)
		{
			return new ResponseDataObj(exp.getMessage(), -1);
		}  
	}
	
	/******************************
	 * isExternalSite(String rootURL, String tgtURL)
	 * 				: check if the given URL(tgtURL) is within rootURL or not.
	 ******************************
	 *
	 * @param rootURL :	rootURL (check should be done only if within this URL)
	 * @param tgtURL  :	target URL to be checked.
	 * @return boolean (true => External Site, false => Internal Site)
	 */
	public static boolean isExternalSite(String rootURL, String tgtURL)
	{
		boolean res = false;
		Matcher mtch_http = ptn_http.matcher(tgtURL); // if relative url, then Internal Site (=> false).
		
		if (mtch_http.find()) {
			// not relative url
			Pattern ptn_root_url = Pattern.compile(rootURL);
			Matcher mtch_root_url = ptn_root_url.matcher(tgtURL);
			if (!mtch_root_url.find()) {
				// root url was not find in tgtURL
				res = true;
			}
		}
		
		return res;
		
	}
	
	/******************************
	 * appendAndDeleteTmpFile(FileOutputStream f_to, String strFname_from)
	 * 				take a file lock of "f_to" and append all contents in "strFname_from" to "f_to".
	 ******************************
	 * @param f_to
	 * @param strFname_from
	 * @return void
	 */
	private void appendAndDeleteTmpFile(FileOutputStream f_to, String strFname_from) {
		try {
			
			FileInputStream f_from = new FileInputStream(strFname_from);
			
		    // obtain Lock of the output file
		    FileLock lock = f_to.getChannel().tryLock();
		    
		    while (lock == null) {
		    	lock = f_to.getChannel().tryLock();
				Thread.sleep(1000);
		    }
		    
		    try {
		    	BufferedInputStream input = new BufferedInputStream(f_from);
				IOUtils.copy(input, f_to);
		    }
		    finally {
		    	lock.release();
		    	f_from.close();

		    	// at the end of this function, delete temp file.
		    	File f = new File(strFname_from);
		    	f.delete();
		    	
		    }
	    
	    }
		catch (IOException e) {
			e.printStackTrace();
			String exp_msg = "IOException in appendAndDeleteTmpFile() : " + e.getMessage();
	    	System.out.println(exp_msg);
	    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
	    	Integer prevCount = (Integer) numExceptions.get();
	    	numExceptions.set( new Integer(prevCount.intValue() + 1) );
		}
		catch (Exception e) {
			e.printStackTrace();
			String exp_msg = "Exception in appendFile() : " + e.getMessage();
	    	System.out.println(exp_msg);
	    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
	    	Integer prevCount = (Integer) numExceptions.get();
	    	numExceptions.set( new Integer(prevCount.intValue() + 1) );
		} 
	 
	}
	
	public void run()
	{
		
		long numThreadId = Thread.currentThread().getId();
		String exp_msg = null;
		String strPtnProtocol = "https{0,1}://";
		
		strFname_ok.set("results" + File.separator + "__tmp_" + Long.toString(numThreadId) + "_" + strThreadID + "__healthy_links.txt");
		strFname_error.set("results" + File.separator + "__tmp_" + Long.toString(numThreadId) + "_" + strThreadID + "__broken_links.txt");
		strFname_externalLinks.set("results" + File.separator + "__tmp_" + Long.toString(numThreadId) + "_"  + strThreadID + "__externalLinks.txt");
		strFname_exceptions.set("results" + File.separator + "__tmp_" + Long.toString(numThreadId) + "_" + strThreadID + "__exceptions.txt");

		// shared variables from BrokenLinkChecker class.
		String strRootURL	= LinkValidator.getRootURL();
		boolean boolOptAny	= LinkValidator.getOptAny();
		boolean boolOptVerbose	= LinkValidator.getOptVerboseFlg();

		ConcurrentHashMap<String, Integer> visitedLinkMap = LinkValidator.getVisitedLinkMap();
		ConcurrentLinkedDeque<String> stack = LinkValidator.getStack();
		
		// FireFox
		FirefoxDriver browserDriver = new FirefoxDriver();
		browserDriver.manage().timeouts().pageLoadTimeout(numTimeoutSec, TimeUnit.SECONDS);
		browserDriver.manage().timeouts().implicitlyWait(numTimeoutSec, TimeUnit.SECONDS);  // (note) want to set to 120 second but somehow, it waits (second * 2) second. Bug?
		browserDriver.manage().timeouts().setScriptTimeout(numTimeoutSec, TimeUnit.SECONDS);
		
		try {
			
			f_out_ok.set(new FileOutputStream(strFname_ok.get()));
			f_out_error.set(new FileOutputStream(strFname_error.get()));
		    f_out_externalLinks.set(new FileOutputStream(strFname_externalLinks.get()));
		    f_out_exceptions.set(new FileOutputStream(strFname_exceptions.get()));
			
			System.out.println("[Current Target] : " + this.strURL);
			new PrintStream(f_out_ok.get()).println( "[Current Target] : " + this.strURL );
			
			visitedLinkMap.putIfAbsent(this.strURL, 1);
			
			String url_get = "";
			
			if ( this.uid != "" || this.password != "") {
				//url_get = this.strURL.replaceFirst( "(https{0,1}://)", "$1"+ this.uid +":"+ this.password +"@" );
				url_get = this.strURL.replaceFirst( "(" + strPtnProtocol + ")", "$1"+ this.uid +":"+ this.password +"@" );  // add id and pass to URL (e.g. https{0,1}:// -> https{0,1}://uid:password@ )
			}
			else {
				url_get = this.strURL;
			}

			browserDriver.get(url_get);
			
			//
			// replace some characters in url to use it as the capture images filename(png).
			//
			//String url_httpTrimed_01 = this.strURL.replaceFirst("^https{0,1}://[^/]+/", "");
			String url_httpTrimed_01 = this.strURL.replaceFirst("^" + strPtnProtocol + "[^/]+/", "");  //trim protocol and hostname from URL. (e.g. http(s)://hostname/path -> path)
			String url_httpTrimed_02 = url_httpTrimed_01.replaceAll("[/?\"<>|]", "_");
			
			// taking a screenshot.
			File scrFile = ((TakesScreenshot)browserDriver).getScreenshotAs(OutputType.FILE);
			File f_ScreenShot = new File("results" + File.separator + "screenshot" + File.separator + URLDecoder.decode(url_httpTrimed_02, "UTF-8") + ".png");
			if (!f_ScreenShot.exists()) {
				FileUtils.copyFile(scrFile, f_ScreenShot);
			}
			
		    ArrayList<WebElement> allLinks = findAllLinks(browserDriver, boolOptAny);   
		    
		    System.out.println("Total number of elements found " + allLinks.size());
		    new PrintStream(f_out_ok.get()).println( "Total number of elements found " + allLinks.size() );
	    
		    URL objTgtURL = null;

		    for( WebElement element : allLinks ){
		 
			    try
			    {
			    	String strTagName =  element.getTagName();
			    	String strTgtURL = null;
			    	String linkType = "";
			    	
			    	if (strTagName.equalsIgnoreCase("a")) {
			    		strTgtURL = element.getAttribute("href");
			    		linkType = "<a>";
			    	}
			    	else if (strTagName.equalsIgnoreCase("img")) {
			    		strTgtURL  = element.getAttribute("src");
			    		linkType = "<img>";
			    	}
			    	else if (strTagName.equalsIgnoreCase("link")) {
			    		strTgtURL  = element.getAttribute("href");
			    		linkType = "<link>";
			    	}
			    	
			    	ResponseDataObj respData;
			    	
			    	if ( strTgtURL != null )
			    	{
			    		String msg = null;
			    		//String noUidPwdURL = strTgtURL.replaceFirst( "(https{0,1}://)" + this.uid + ":" + this.password + "@", "$1" );
			    		String noUidPwdURL = strTgtURL.replaceFirst( "(" + strPtnProtocol + ")" + this.uid + ":" + this.password + "@", "$1" ); // trim uid and password (e.g. https{0,1}://uid:password@ -> https{0,1}://)  
			    		
			    		if(visitedLinkMap.containsKey(noUidPwdURL))
			    		{
			    			msg = (boolOptVerbose) ? 
			    					this.strURL + "\t" + linkType + "\t" + noUidPwdURL + "\t" + "(visited)" 
			    					: this.strURL + "\t" + linkType + "\t" + noUidPwdURL;
			    			
			    			new PrintStream(f_out_ok.get()).println( msg );
			    			
			    		}
			    		else if (strTagName.equalsIgnoreCase("a") && isExternalSite(strRootURL, noUidPwdURL)) {
			    			// external link
			    			
			    			msg = (boolOptVerbose) ?
			    					this.strURL + "\t" + linkType + "\t" + noUidPwdURL + "\t" + "(external link)"
			    					: this.strURL + "\t" + linkType + "\t" + noUidPwdURL;
			    			
			    			new PrintStream(f_out_externalLinks.get()).println( msg );
			    			Integer prevCount = (Integer) numExternalLinks.get();
			    			numExternalLinks.set( new Integer(prevCount.intValue() + 1) );
			    		}
			    		else {
			    			
			    			// (Note)
			    			// at this moment, objTgtURL is always null because of finally() part.
			    			
			    			Matcher mtch_no_http = ptn_no_http.matcher(strTgtURL);
			    			if ( mtch_no_http.find() )  // if strTgtURL was relative.
				    		{
			    				objTgtURL = new URL(new URL(strRootURL), strTgtURL);
				    		}
			    			else {
			    				objTgtURL = new URL(strTgtURL);
			    			}
			    			
				    		respData = isLinkBroken(objTgtURL, uid, password);
			    			visitedLinkMap.put(strTgtURL, 1);
	    			
			    			if( (this.boolRunAsDFSSearch == true)
			    					&& !( strTgtURL.contains("mailto:") || strTgtURL.contains("tel:") )
			    					&& strTagName.equalsIgnoreCase("a")
			    					&& !stack.contains(noUidPwdURL)
			    					&& !(strTgtURL.lastIndexOf("#") > strTgtURL.lastIndexOf("/"))
			    					) { // Do not access to not-A-tag URL via Firefox driver.
			    				stack.push(noUidPwdURL);
			    			}
				    		
				    		msg = this.strURL 
				    				+ "\t" + linkType 
				    				+ "\t" + noUidPwdURL
				    				+ "\t" + respData.getRespMsg() 
				    				+ "\t" + respData.getRespCode();
				    		 
			    			if ( respData.getRespCode() >= 400 )
			    			{
		    					new PrintStream(f_out_error.get()).println(msg);
		    					Integer prevCount = (Integer) numInvalidLink.get();
			    				numInvalidLink.set(new Integer(prevCount.intValue() + 1) ); 
			    			}
			    			else
			    			{
		    					new PrintStream(f_out_ok.get()).println(msg);
		    					Integer prevCount = (Integer) numHealthyLink.get();
			    				numHealthyLink.set( new Integer(prevCount.intValue() + 1) );
			    			}
			    		}

		    			System.out.println(msg);
	
			    	}
			 
			    }
			    catch(Exception exp)
			    {
			    	exp_msg = this.strURL + "\t" + "At attribute : \"" + element.getAttribute("innerHTML") + "\".\t" + "Message  :  " + exp.getMessage();
			    	System.out.println(exp_msg);
			    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
			    	Integer prevCount = (Integer) numExceptions.get();
			    	numExceptions.set( new Integer(prevCount.intValue() + 1) );
			    }
			    finally {
			    	objTgtURL = null;
			    }
		     
	    	}
		    
		}
		catch(Exception exp)
		{
	    	exp_msg = "[In Main Loop] An Exception occured at page " + this.strURL + " .\tMessage  :  " + exp.getMessage();
	    	System.out.println(exp_msg);
	    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
	    	Integer prevCount = (Integer) numExceptions.get();
	    	numExceptions.set( new Integer(prevCount.intValue() + 1) );
		}
		finally {
			browserDriver.close();
			
			// add obtained numbers to values in main class.
		    LinkValidator.addAndGetNumHealthyLink(numHealthyLink.get());
			LinkValidator.addAndGetNumInvalidLink(numInvalidLink.get());
			LinkValidator.addAndGetNumExternalLinks(numExternalLinks.get());
			LinkValidator.addAndGetNumExceptions(numExceptions.get());
			
			try {
				f_out_ok.get().close();
				f_out_error.get().close();
			    f_out_externalLinks.get().close();
			    f_out_exceptions.get().close();
			} catch (IOException exp) {
				exp.printStackTrace();
				exp_msg = "[finally part in Main run()] An Exception occured at page " + this.strURL + " .\tMessage  :  " + exp.getMessage();
		    	System.out.println(exp_msg);
		    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
		    	Integer prevCount = (Integer) numExceptions.get();
		    	numExceptions.set( new Integer(prevCount.intValue() + 1) );
			}
			
		    appendAndDeleteTmpFile(LinkValidator.getFStreamOutOk(), strFname_ok.get());
		    appendAndDeleteTmpFile(LinkValidator.getFStreamOutError(), strFname_error.get());
		    appendAndDeleteTmpFile(LinkValidator.getFStreamOutExternalSites(), strFname_externalLinks.get());
		    appendAndDeleteTmpFile(LinkValidator.getFStreamOutExceptions(), strFname_exceptions.get());

		}
		
	}

}
