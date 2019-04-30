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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.IOUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import javax.imageio.ImageIO;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

public class RunnableLinkChecker implements Runnable {

	private String strThreadID;
	private String strURL;
	private String uid;
	private String password;
	private boolean boolRunAsBFSSearch;
	
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
								, boolean __boolRunAsBFSSearch) throws FileNotFoundException {
		
		this.strThreadID	= __strThreadID;
		this.strURL			= __url;
		this.uid			= __uid;
		this.password		= __password;
		this.boolRunAsBFSSearch			= __boolRunAsBFSSearch;
		
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
											String exp_msg = "Exception in initialValue() of f_out_external_links : " + e.getMessage();
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
			
			boolean isCopySuccess = false;
			
			int waitMin = 1000; // 1 sec
			int waitMax = 5000; // 5 sec

			FileInputStream f_from = new FileInputStream(strFname_from);  
		    ReadWriteLock lock = new ReentrantReadWriteLock(true); // fair=true
		     
		    while ( !lock.writeLock().tryLock() ) {
		    	// wait randomly between waitMin sec to waitMax sec.
		    	int waitMillSec = ThreadLocalRandom.current().nextInt(waitMin, waitMax + 1);
		    	Thread.sleep(waitMillSec);
		    }
		    
		    try {
		    	BufferedInputStream input = new BufferedInputStream(f_from);
				IOUtils.copy(input, f_to);
				input.close();
				isCopySuccess = true;
		    }
		    catch (Exception e) {
		    	e.printStackTrace();
				String exp_msg = "Exception when copying in appendAndDeleteTmpFile() : " + e.getMessage() + " in thread " + Thread.currentThread().getId() + ", for a file " + strFname_from;
		    	System.out.println(exp_msg);
		    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
		    	Integer prevCount = (Integer) numExceptions.get();
		    	numExceptions.set( new Integer(prevCount.intValue() + 1) );
		    }
		    finally {
		    	
		    	if (isCopySuccess) {
			    	File f = new File(strFname_from);
			    	f.delete();
		    	}
		    	
		    	lock.writeLock().unlock();
		    }
	    }
		catch (IOException e) {
			e.printStackTrace();
			String exp_msg = "IOException in appendAndDeleteTmpFile() : " + e.getMessage() + " in thread " + Thread.currentThread().getId() + ", for a file " + strFname_from;
	    	System.out.println(exp_msg);
	    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
	    	Integer prevCount = (Integer) numExceptions.get();
	    	numExceptions.set( new Integer(prevCount.intValue() + 1) );
		}
		catch (Exception e) {
			e.printStackTrace();
			String exp_msg = "Exception in appendAndDeleteTmpFile() : " + e.getMessage() + " in thread " + Thread.currentThread().getId() + ", for a file " + strFname_from;
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
		strFname_externalLinks.set("results" + File.separator + "__tmp_" + Long.toString(numThreadId) + "_"  + strThreadID + "__external_links.txt");
		strFname_exceptions.set("results" + File.separator + "__tmp_" + Long.toString(numThreadId) + "_" + strThreadID + "__exceptions.txt");

		// shared variables from BrokenLinkChecker class.
		String strRootURL	= LinkValidator.getRootURL();
		boolean boolOptAny	= LinkValidator.getOptAny();
		boolean boolOptVerbose	= LinkValidator.getOptVerboseFlg();
		boolean boolOptCapture = LinkValidator.getOptScreenCaptureFlg();
		boolean boolOptSkipElement = LinkValidator.getOptSkipElementFlg();
		boolean boolOptSitemapMode = LinkValidator.getSitemapModeFlg();

		ConcurrentHashMap<String, Integer> visitedLinkMap = LinkValidator.getVisitedLinkMap();
		//ConcurrentLinkedDeque<String> stack = LinkValidator.getStack();
		ConcurrentLinkedDeque<String> deque = LinkValidator.getDeque();
		ConcurrentLinkedDeque<FirefoxDriver> dqBrowserDrivers = LinkValidator.getDQBrowserDrivers();
		
		FirefoxDriver browserDriver = dqBrowserDrivers.pop();
		
		try {
			
			f_out_ok.set(new FileOutputStream(strFname_ok.get()));
			f_out_error.set(new FileOutputStream(strFname_error.get()));
		    f_out_externalLinks.set(new FileOutputStream(strFname_externalLinks.get()));
		    f_out_exceptions.set(new FileOutputStream(strFname_exceptions.get()));
		    
		    if (boolOptVerbose) {
		    	System.out.println("[Current Target] : " + this.strURL);
		    }
			new PrintStream(f_out_ok.get()).println( "[Current Target] : " + this.strURL );
			
			visitedLinkMap.putIfAbsent(this.strURL, 1);
			
			String url_get = "";
			
			if ( this.uid != "" || this.password != "") {
				url_get = this.strURL.replaceFirst( "(" + strPtnProtocol + ")", "$1"+ this.uid +":"+ this.password +"@" );  // add id and pass to URL (e.g. https{0,1}:// -> https{0,1}://uid:password@ )
			}
			else {
				url_get = this.strURL;
			}

			browserDriver.get(url_get);
			
			if (boolOptCapture) {
				// take the screenshot of the browsing page.
				
				//
				// replace some characters in url to use it as the capture images filename(png).
				//
				//String url_httpTrimed_01 = this.strURL.replaceFirst("^https{0,1}://[^/]+/", "");
				String url_httpTrimed_01 = this.strURL.replaceFirst("^" + strPtnProtocol + "[^/]+/?", "");  //trim protocol and hostname from URL. (e.g. http(s)://hostname/path -> path)
				String url_httpTrimed_02 = url_httpTrimed_01.replaceAll("[/?\"<>|]", "_");
				
				Screenshot fpScreenshot = new AShot().shootingStrategy(ShootingStrategies.viewportPasting(1000)).takeScreenshot(browserDriver);
				File f_ScreenShot = new File("results" + File.separator + "screenshot" + File.separator + URLDecoder.decode(url_httpTrimed_02, "UTF-8") + ".png");
				File parentDir = f_ScreenShot.getParentFile();
				if (parentDir != null && ! parentDir.exists() ) {
					if(!parentDir.mkdirs()){
				        throw new IOException("error creating results/screenshot directory");
				    }
				}
				if (!f_ScreenShot.exists()) {
					ImageIO.write(fpScreenshot.getImage(),"PNG", f_ScreenShot);
				}
				
			}
			
			if (!boolOptSkipElement) {
			
			    ArrayList<WebElement> allLinks = findAllLinks(browserDriver, boolOptAny);   
			    
			    if (boolOptVerbose) {
			    	System.out.println("Total number of elements found " + allLinks.size());
			    }
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
				    	else if (!boolOptSitemapMode && strTagName.equalsIgnoreCase("img")) {
				    		strTgtURL  = element.getAttribute("src");
				    		linkType = "<img>";
				    	}
				    	else if (!boolOptSitemapMode && strTagName.equalsIgnoreCase("link")) {
				    		strTgtURL  = element.getAttribute("href");
				    		linkType = "<link>";
				    	}
				    	
				    	ResponseDataObj respData;
				    	
				    	if ( strTgtURL != null )
				    	{
				    		String msg = null;
				    		//String noUidPwdURL = strTgtURL.replaceFirst( "(https{0,1}://)" + this.uid + ":" + this.password + "@", "$1" );
				    		String noUidPwdURL = strTgtURL.replaceFirst( "(" + strPtnProtocol + ")" + this.uid + ":" + this.password + "@", "$1" ); // trim uid and password (e.g. https{0,1}://uid:password@ -> https{0,1}://)
				    		String noUidPwdURL_decoded = java.net.URLDecoder.decode(noUidPwdURL, StandardCharsets.UTF_8.name());
				    		
				    		if(visitedLinkMap.containsKey(noUidPwdURL_decoded))
				    		{

				    			msg = this.strURL + "\t" + linkType + "\t" + noUidPwdURL_decoded + "\t" + "(visited)"; 
				    			new PrintStream(f_out_ok.get()).println( msg );
				    			
				    		}
				    		else if (strTagName.equalsIgnoreCase("a") && isExternalSite(strRootURL, noUidPwdURL_decoded)) {
				    			// external link
	
				    			msg = this.strURL + "\t" + linkType + "\t" + noUidPwdURL_decoded + "\t" + "(external link)";
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
					    		visitedLinkMap.put(noUidPwdURL_decoded, 1);
		    			
				    			if( (this.boolRunAsBFSSearch == true)
				    					&& !( strTgtURL.contains("mailto:") || strTgtURL.contains("tel:") )
				    					&& strTagName.equalsIgnoreCase("a")
				    					&& !deque.contains(noUidPwdURL_decoded)
				    					&& !( strTgtURL.lastIndexOf("#") > strTgtURL.lastIndexOf("/") )
				    					&& !( strTgtURL.endsWith(".png") || strTgtURL.endsWith(".jpg") || strTgtURL.endsWith(".gif") )
				    					) { // Do not access to not-A-tag URL via Firefox driver.
				    				//stack.push(noUidPwdURL);  // stack
				    				deque.addLast(noUidPwdURL_decoded);  // queue
				    			}
					    		
					    		msg = this.strURL 
					    				+ "\t" + linkType 
					    				+ "\t" + noUidPwdURL_decoded
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
	
				    		if (boolOptVerbose) {
				    			System.out.println(msg);
				    		}
		
				    	}
				 
				    }
				    catch (UnsupportedEncodingException e) {
		    		    // not going to happen - value came from JDK's own StandardCharsets
				    	// just for noUidPwdURL_decoded in case something wrong happens
				    	exp_msg = this.strURL + "\t" + "At attribute : \"" + element.getAttribute("innerHTML") + "\".\t" + "[UnsupportedEncodingException] Message  :  " + e.getMessage();
				    	System.out.println(exp_msg);
				    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
				    	Integer prevCount = (Integer) numExceptions.get();
				    	numExceptions.set( new Integer(prevCount.intValue() + 1) );
				    	
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
			
			// push back browserdriver to drivers-dequeue to reuse
			dqBrowserDrivers.addLast(browserDriver);
			
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
		    	LinkValidator.addAndGetNumExceptions(numExceptions.get());
			}
					
			
			appendAndDeleteTmpFile(LinkValidator.getFStreamOutOk(), strFname_ok.get());
			appendAndDeleteTmpFile(LinkValidator.getFStreamOutError(), strFname_error.get());
			appendAndDeleteTmpFile(LinkValidator.getFStreamOutExternalSites(), strFname_externalLinks.get());
			appendAndDeleteTmpFile(LinkValidator.getFStreamOutExceptions(), strFname_exceptions.get());
			

		}
		
	}

}
