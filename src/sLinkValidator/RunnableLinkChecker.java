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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
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
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import javax.imageio.ImageIO;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class RunnableLinkChecker implements Runnable {

	private String strThreadID;
	private String strURL;
	private String uid;
	private String password;
	private String resultsdir;
	private boolean boolRunAsBFSSearch;
	
	static Pattern ptn_http		= Pattern.compile("^https{0,1}://");
	static Pattern ptn_no_http	= Pattern.compile("^((?!https{0,1}://).)+$");
	static Pattern ptn_root_url		= Pattern.compile("^https{0,1}://[^/]+");
	
	private ThreadLocal<Integer> numHealthyLink;
	private ThreadLocal<Integer> numInvalidLink;
	private ThreadLocal<Integer> numExternalLinks;
	private ThreadLocal<Integer> numExceptions;
	private ThreadLocal<Integer> numConsoleSevere;
	private ThreadLocal<Integer> numConsoleWarn;
	
	private static ThreadLocal<FileOutputStream> f_out_ok;
	private static ThreadLocal<FileOutputStream> f_out_error;
	private static ThreadLocal<FileOutputStream> f_out_externalLinks;
	private static ThreadLocal<FileOutputStream> f_out_exceptions;
	private static ThreadLocal<FileOutputStream> f_out_consolelog;
	
	private static ThreadLocal<String> strFname_ok;
	private static ThreadLocal<String> strFname_error;
	private static ThreadLocal<String> strFname_externalLinks;
	private static ThreadLocal<String> strFname_exceptions;
	private static ThreadLocal<String> strFname_consoleLog;
	
	private static final Pattern TITLE_TAG =
	        Pattern.compile("\\<title>(.*)\\</title>", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
	
	
	public RunnableLinkChecker(String __strThreadID
								, String __url
								, String __uid
								, String __password
								, String __resultsdir
								, boolean __boolRunAsBFSSearch) throws FileNotFoundException {
		
		this.strThreadID	= __strThreadID;
		this.strURL			= __url;
		this.uid			= __uid;
		this.password		= __password;
		this.resultsdir     = __resultsdir;
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
		numConsoleSevere	= new ThreadLocal<Integer>() {
							@Override protected Integer initialValue() {
								Integer zero = new Integer(0);
								return zero;
							}
						};
		numConsoleWarn	= new ThreadLocal<Integer>() {
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
		strFname_consoleLog	= new ThreadLocal<String>() {
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
		f_out_consolelog	= new ThreadLocal<FileOutputStream>() {
									@Override protected FileOutputStream initialValue() {
										FileOutputStream fos = null;
										try {
											fos = new FileOutputStream(strFname_consoleLog.get());
										} catch (FileNotFoundException e) {
											e.printStackTrace();
											String exp_msg = "Exception in initialValue() of f_out_consolelog : " + e.getMessage();
									    	System.out.println(exp_msg);
										}
										return fos;
									}
								};

	}
	
	/******************************
	 * isRedirect(int statusCode)
	 * 				: Check status code for redirects.	
	 ******************************
	 * @param statusCode
	 * @return boolean (true => redirect, false => not redirect)
	 *****/
	protected static boolean isRedirect(int statusCode) {
	    if (statusCode != HttpURLConnection.HTTP_OK) {
	        if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP
	            || statusCode == HttpURLConnection.HTTP_MOVED_PERM
	                || statusCode == HttpURLConnection.HTTP_SEE_OTHER
	                	|| statusCode == 307 /* HTTP_TEMP_REDIRECT */
	                		||statusCode == 308 /* HTTP_PERM_REDIRECT  */ ) {
	            return true;
	        }
	    }
	    return false;
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
		  elementList.addAll(driver.findElements(By.tagName("script")));
		  
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
	
	
	// fetched from https://gist.github.com/joseporiol/9409883
	/**
     * Loops through response headers until Content-Type is found.
     * @param conn
     * @return ContentType object representing the value of
     * the Content-Type header
     */
    private static ContentType getContentTypeHeader(URLConnection conn) {
        int i = 0;
        boolean moreHeaders = true;
        do {
            String headerName = conn.getHeaderFieldKey(i);
            String headerValue = conn.getHeaderField(i);
            if (headerName != null && headerName.equals("Content-Type"))
                return new ContentType(headerValue);
 
            i++;
            moreHeaders = headerName != null || headerValue != null;
        }
        while (moreHeaders);
 
        return null;
    }
 
    private static Charset getCharset(ContentType contentType) {
        if (contentType != null && contentType.charsetName != null && Charset.isSupported(contentType.charsetName))
            return Charset.forName(contentType.charsetName);
        else
            return null;
    }
 
    /**
     * Class holds the content type and charset (if present)
     */
    private static final class ContentType {
        private static final Pattern CHARSET_HEADER = Pattern.compile("charset=([-_a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
 
        private String contentType;
        private String charsetName;
        private ContentType(String headerValue) {
            if (headerValue == null)
                throw new IllegalArgumentException("ContentType must be constructed with a not-null headerValue");
            int n = headerValue.indexOf(";");
            if (n != -1) {
                contentType = headerValue.substring(0, n);
                Matcher matcher = CHARSET_HEADER.matcher(headerValue);
                if (matcher.find())
                    charsetName = matcher.group(1);
            }
            else
                contentType = headerValue;
        }
    }
    // end of from https://gist.github.com/joseporiol/9409883
    
	/******************************
	 * isLinkBroken(URL url, String uid, String password)
	 * 				: check if given url is broken or not (access to the url and return the status code).
	 ******************************
	 * 
	 * @param url  : URL to be checked 
	 * @param uid  : User ID for the Basic authentication.
	 * @param password : password for the Basic authentication.
	 * @param boolOptInstanceFollowRedirects : boolean (true => follow redirect, false => not follow redirect (default))
	 * @return ResponseDataObj(response, statusCode)
	 * @throws Exception
	 */
	public static ResponseDataObj isLinkBroken(URL url, String uid, String password, boolean boolOptInstanceFollowRedirects) 
	{
		
		String strResponse = "";
		String strRedirectUrl = "";
		String strPageTitle = "";
		int intStatusCode = 0;
		HttpURLConnection connection;
		ContentType contentType = null;
		
		String strResponseRedirectTo = "";
		int intStatusCodeRedirectTo = 0;
		HttpURLConnection connRedirect;
		
		try
		{
			//
			// Disable Certificate Validation
			// thanks to : https://nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
			//
			// Create a trust manager that does not validate certificate chains
	        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
	                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                    return null;
	                }
	                public void checkClientTrusted(X509Certificate[] certs, String authType) {
	                }
	                public void checkServerTrusted(X509Certificate[] certs, String authType) {
	                }
	            }
	        };
	 
	        // Install the all-trusting trust manager
	        SSLContext sc = SSLContext.getInstance("SSL");
	        sc.init(null, trustAllCerts, new java.security.SecureRandom());
	        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	 
	        // Create all-trusting host name verifier
	        HostnameVerifier allHostsValid = new HostnameVerifier() {
	            public boolean verify(String hostname, SSLSession session) {
	                return true;
	            }
	        };
	 
	        // Install the all-trusting host verifier
	        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	        //
	        // End of Disable Certificate Validation
	        //
			
			connection = (HttpURLConnection) url.openConnection();
			if (!boolOptInstanceFollowRedirects) {
				//do not follow redirects to fetch http status code 30x with basic authentication
				connection.setInstanceFollowRedirects(false);
			}
			
			// in case of basic auth.
			if (uid != "" || password != "") {
				String userpass = uid+":"+password;
				new Base64();
				String basicAuth = "Basic " + new String(Base64.encode(userpass.getBytes()));
				connection.setRequestProperty("Authorization", basicAuth);
			}
			
			// make a connection
		    connection.connect();
		    strResponse	= connection.getResponseMessage();
		    intStatusCode	= connection.getResponseCode();
		    
		    if (intStatusCode == HttpURLConnection.HTTP_OK)
		    {
		    	/* */
			    contentType = getContentTypeHeader(connection);
			    
			    
			    /*
			     // from https://stackoverflow.com/questions/40099397/how-can-i-get-the-page-title-information-from-a-url-in-java/40099983
			    
			    // get page title of url
			    response = url.openStream();
			    Scanner scanner = new Scanner(response);
			    String responseBody = scanner.useDelimiter("\\A").next();
			    strPageTitle = responseBody.substring(responseBody.indexOf("<title>") + 7, responseBody.indexOf("</title>"));
			    scanner.close();
			    */
			    
			    if (contentType.contentType.equals("text/html")) {
			    	// determine the charset, or use the default
		            Charset charset = getCharset(contentType);
		            if (charset == null)
		                charset = Charset.defaultCharset();
		 
		            // read the response body, using BufferedReader for performance
		            InputStream in = connection.getInputStream();
		            BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
		            int n = 0, totalRead = 0;
		            char[] buf = new char[1024];
		            StringBuilder content = new StringBuilder();
		 
		            // read until EOF or first 8192 characters
		            while (totalRead < 8192 && (n = reader.read(buf, 0, buf.length)) != -1) {
		                content.append(buf, 0, n);
		                totalRead += n;
		            }
		            reader.close();
		 
		            // extract the title
		            Matcher matcher = TITLE_TAG.matcher(content);
		            if (matcher.find()) {
		                /* replace any occurrences of whitespace (which may
		                 * include line feeds and other uglies) as well
		                 * as HTML brackets with a space */
		            	strPageTitle = matcher.group(1).replaceAll("[\\s\\<>]+", " ").trim();
		            }
			    }
			    /* */
		    }
		    // in case of redirect
		    else if ( isRedirect(intStatusCode) ) {
		    	
		    	strRedirectUrl = connection.getHeaderField("Location");
		    	
		    	//
		    	// put original url's anchor to redirecturl
		    	// to let output url be the same address as in browser's address bar, since
		    	// "The URL fragment (everything from # on) not even gets sent to the server."
		    	// see : <https://stackoverflow.com/questions/15133023/hash-url-rewrite-in-htaccess>
		    	// 
		    	if (url.getRef() != null && url.getRef().length() != 0) {
		    		strRedirectUrl = strRedirectUrl + "#" + url.getRef();
		    	}
		    	
		    	URL objTgtURL = getOjbTgtURL(LinkValidator.getRootURL(), strRedirectUrl);
		    	
		    	connRedirect = (HttpURLConnection) objTgtURL.openConnection();
		    	strResponseRedirectTo	= connRedirect.getResponseMessage();
			    intStatusCodeRedirectTo	= connRedirect.getResponseCode();
			    
			    connRedirect.disconnect();

		    }
		    
		    connection.disconnect();
		 
		    return new ResponseDataObj(strPageTitle, strResponse, intStatusCode, strRedirectUrl, intStatusCodeRedirectTo, strResponseRedirectTo);
		 
		}
		catch(Exception exp)
		{
			return new ResponseDataObj(exp.getMessage(), "", -1, "", -1, "");
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
		
		//String rooURL_without_protocol = "";
		//String rgtURL_without_protocol = "";
		
		if (mtch_http.find()) {
			// not relative url
			
			// if strict, then compare with protocol. (change code in future)
			//Pattern ptn_root_url = Pattern.compile(rootURL);
			//Matcher mtch_root_url = ptn_root_url.matcher(tgtURL);
			
			//
			// compare url without protocol
			//
			/*
			 // if you use url.getHost(), you cannot compare something like "//www.toyo.ac.jp/nyushi/" (i.e. in case of rootURL is "domain + subdir")
			URL __r_URL = new URL(rootURL);
			URL __t_URL = new URL(tgtURL);
			Pattern ptn_root_url = Pattern.compile(__r_URL.getHost());
			Matcher mtch_root_url = ptn_root_url.matcher(__r_URL.getHost());
			*/
			String rooURL_without_protocol = "^" + rootURL.replaceFirst("http[s]?:", "");  // added heading "^" to avoid to match something like //www.linkedin.com/?session_redirect=https://"rootURL"/.....
			String rgtURL_without_protocol = tgtURL.replaceFirst("http[s]?:", "");

			Pattern ptn_root_url = Pattern.compile(rooURL_without_protocol);
			Matcher mtch_root_url = ptn_root_url.matcher(rgtURL_without_protocol);
			if (!mtch_root_url.find()) {
				// root url was not find in tgtURL
				res = true;
			}
		}
		
		return res;
		
	}
	
	/******************************
	 * getOjbTgtURL (String rootURL, String tgtURL)
	 * 				: return java.net.URL object.
	 ******************************
	 *
	 * @param strRootURL :	rootURL
	 * @param tgtURL  :	target URL to be checked.
	 * @return objTgtURL : java.net.URL object
	 */
	@SuppressWarnings("finally")
	private static URL getOjbTgtURL(String strRootURL, String strTgtURL) throws Exception
	{
		URL objTgtURL = null;
		
		Matcher mtch_no_http = ptn_no_http.matcher(strTgtURL);
		
		try {
		
			if ( mtch_no_http.find() )  // if strTgtURL was relative.
			{
				objTgtURL = new URL(new URL(strRootURL), strTgtURL);
			}
			else {
				objTgtURL = new URL(strTgtURL);
			}
			
		}
		catch(Exception exp)
		{
			// do nothing
			throw exp;
		}
		finally {
			return objTgtURL;
	    }
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
	/******************************
	 * handleDoubleQuoteForCSV(String str_in)
	 * 				take a file lock of "f_to" and append all contents in "strFname_from" to "f_to".
	 ******************************
	 * @param str_in
	 * @return String
	 */
	private String handleDoubleQuoteForCSV(String str_in) {
		
		String str_out = "";
		
		try {
			str_out = str_in.replaceAll("\"", "\"\"");
		}
		catch (Exception e) {
			e.printStackTrace();
			String exp_msg = "Exception in () : " + e.getMessage() + " in thread " + Thread.currentThread().getId() + ", for a string " + str_in;
	    	System.out.println(exp_msg);
	    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
	    	Integer prevCount = (Integer) numExceptions.get();
	    	numExceptions.set( new Integer(prevCount.intValue() + 1) );	
		}
		
		return str_out;
	}
	/******************************
	 * incrementThreadLocalInt(hreadLocal<Integer> tint, int val)
	 * 				increment thread local integer.
	 ******************************
	 * @param tint
	 * @param val
	 * @return void
	 */
	private static void incrementThreadLocalInt(ThreadLocal<Integer> tint, int val) {
		Integer prevCount = (Integer) tint.get();
		tint.set( new Integer(prevCount.intValue() + val) );
	}
	
	public void run()
	{
		
		long numThreadId         = Thread.currentThread().getId();
		String exp_msg           = null;
		String console_msg       = null;
		String strPtnProtocol    = "https{0,1}://";
		
		strFname_ok.set(resultsdir + File.separator + "__tmp_" + Long.toString(numThreadId) + "_" + strThreadID + "__healthy_links.csv");
		strFname_error.set(resultsdir + File.separator + "__tmp_" + Long.toString(numThreadId) + "_" + strThreadID + "__broken_links.csv");
		strFname_externalLinks.set(resultsdir + File.separator + "__tmp_" + Long.toString(numThreadId) + "_"  + strThreadID + "__external_links.csv");
		strFname_exceptions.set(resultsdir + File.separator + "__tmp_" + Long.toString(numThreadId) + "_" + strThreadID + "__exceptions.txt");
		strFname_consoleLog.set(resultsdir + File.separator + "__tmp_" + Long.toString(numThreadId) + "_" + strThreadID + "__console_logs.csv");

		// shared variables from BrokenLinkChecker class.
		String strRootURL	= LinkValidator.getRootURL();
		boolean boolOptAny	= LinkValidator.getOptAny();
		boolean boolUrlList = LinkValidator.getBoolUrlList();
		boolean boolOptVerbose	= LinkValidator.getOptVerboseFlg();
		boolean boolOptCapture = LinkValidator.getOptScreenCaptureFlg();
		boolean boolOptSkipElement = LinkValidator.getOptSkipElementFlg();
		boolean boolOptSitemapMode = LinkValidator.getSitemapModeFlg();
		boolean boolOptInstanceFollowRedirects = LinkValidator.getOptInstanceFollowRedirects();

		ConcurrentHashMap<String, Integer> visitedLinkMap = LinkValidator.getVisitedLinkMap();
		//ConcurrentLinkedDeque<String> stack = LinkValidator.getStack();
		ConcurrentLinkedDeque<String> deque = LinkValidator.getDeque();
		//ConcurrentLinkedDeque<FirefoxDriver> dqBrowserDrivers = LinkValidator.getDQBrowserDrivers();
		ConcurrentLinkedDeque<WebDriver> dqBrowserDrivers = LinkValidator.getDQBrowserDrivers();
		
		//FirefoxDriver browserDriver = dqBrowserDrivers.pop();
		WebDriver browserDriver = dqBrowserDrivers.pop();
		
		try {
			
			f_out_ok.set(new FileOutputStream(strFname_ok.get()));
			f_out_error.set(new FileOutputStream(strFname_error.get()));
		    f_out_externalLinks.set(new FileOutputStream(strFname_externalLinks.get()));
		    f_out_exceptions.set(new FileOutputStream(strFname_exceptions.get()));
		    f_out_consolelog.set(new FileOutputStream(strFname_consoleLog.get()));
		    
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
			
			if (boolUrlList == true) {
				Matcher m_rootUrl = ptn_root_url.matcher(url_get);
				if (m_rootUrl.find()) {
					strRootURL = m_rootUrl.group(0)  + "/";
				}
			}

			try {
				browserDriver.get(url_get);
			}
			catch(WebDriverException exp) {
				//System.out.println("WebDriverException occured");
				exp_msg = String.format("[In Main Loop] An Exception occured at page %s. Message : %s", this.strURL, exp.getMessage());
	  	    	System.out.println(exp_msg);
	  	    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
	  	    	incrementThreadLocalInt(numExceptions, 1);
	  	    }
			/*
			 * below code was from https://stackoverflow.com/questions/51176912/capture-console-error-using-javascriptexecutor-class-in-selenium
			 * > if you want to capture both console.error and console.log, you may override them, 
			 * > and then push the message in an array. You have to inject code to override them immediately after navigate to the page. 
			 * > In order to do this, you have to set PageLoadStrategy = none. See my code below.
			 * 
			 * but somehow, this does not work...
			 * 
			 */
			/*
			String script = 
				    "(function() {" + 
				        "var oldLog = console.error;" +
				        "window.myError = [];" +
				        "console.error = function (message) {" + 
				            "window.myError.push(message);" + 
				            "oldLog.apply(console, arguments);" +
				        "};" +
				    "})();" ;
			((JavascriptExecutor) browserDriver).executeScript(script);
			TimeUnit.SECONDS.sleep(5);
			String err = (String)((JavascriptExecutor) browserDriver).executeScript("return JSON.stringify(window.myError);");
			System.out.println("----->>>>> err is " + err);
			*/
			
			if (boolOptCapture) {
				// take the screenshot of the browsing page.
				
				//
				// replace some characters in url to use it as the capture images filename(png).
				//
				//String url_httpTrimed_01 = this.strURL.replaceFirst("^https{0,1}://[^/]+/", "");
				String url_httpTrimed_01 = this.strURL.replaceFirst("^" + strPtnProtocol + "[^/]+/?", "");  //trim protocol and hostname from URL. (e.g. http(s)://hostname/path -> path)
				String url_httpTrimed_02 = url_httpTrimed_01.replaceAll("[/?\"<>|:*]", "_");
				
				Screenshot fpScreenshot = new AShot().shootingStrategy(ShootingStrategies.viewportPasting(1000)).takeScreenshot(browserDriver);
				File f_ScreenShot = new File(resultsdir + File.separator + "screenshot" + File.separator + URLDecoder.decode(url_httpTrimed_02, "UTF-8") + ".png");
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
				    	String linkText = "";
				    	String altText = "";
				    	
				    	if (strTagName.equalsIgnoreCase("a")) {
				    		strTgtURL = element.getAttribute("href");
				    		linkType = "<a>";
				    		linkText = element.getText();
				    	}
				    	else if (!boolOptSitemapMode) {
				    		if (strTagName.equalsIgnoreCase("img")) {
					    		strTgtURL  = element.getAttribute("src");
					    		linkType = "<img>";
					    		altText = element.getAttribute("alt");
					    		if (element.getAttribute("alt") != null) {
					    			altText = element.getAttribute("alt");
					    		}
					    	}
					    	else if (strTagName.equalsIgnoreCase("script")) {
					    		strTgtURL  = element.getAttribute("src");
					    		linkType = "<script>";
					    		if (element.getAttribute("alt") != null) {
					    			altText = element.getAttribute("alt");
					    		}
					    	}
					    	else if (strTagName.equalsIgnoreCase("link")) {
					    		strTgtURL  = element.getAttribute("href");
					    		linkType = "<link>";
					    		if (element.getText() != null) {
					    			linkText = element.getText();
					    		}
					    	}
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

				    			msg = String.format("%s,%s,%s,(visited),,,",
							    					"\"" + handleDoubleQuoteForCSV(this.strURL) + "\""
							    					, linkType 
							    					, "\"" + handleDoubleQuoteForCSV(noUidPwdURL_decoded)  + "\"");
				    			new PrintStream(f_out_ok.get()).println( msg );
				    			
				    		}
				    		// else if (strTagName.equalsIgnoreCase("a") && isExternalSite(strRootURL, noUidPwdURL_decoded)) {
				    		else if (isExternalSite(strRootURL, noUidPwdURL_decoded)) {
				    			// external link
				    			msg = String.format("%s,%s,%s,(external link),,,",
								    				"\"" + this.strURL + "\""
								    				, linkType
								    				, "\"" + handleDoubleQuoteForCSV(noUidPwdURL_decoded) + "\"");
				    			new PrintStream(f_out_externalLinks.get()).println( msg );
				    			Integer prevCount = (Integer) numExternalLinks.get();
				    			numExternalLinks.set( new Integer(prevCount.intValue() + 1) );
				    		}
				    		else {
				    			
				    			// (Note)
				    			// at this moment, objTgtURL is always null because of finally() part.			    			
				    			objTgtURL = getOjbTgtURL(strRootURL, strTgtURL);
				    			
				    			//assert objTgtURL ;
				    			if (objTgtURL == null) {
				    				throw new IllegalArgumentException("getOjbTgtURL(" + strRootURL + "," + strTgtURL + ") returned null!");
				    			}
				    			
					    		respData = isLinkBroken(objTgtURL, uid, password, boolOptInstanceFollowRedirects);
					    		visitedLinkMap.put(noUidPwdURL_decoded, 1);
		    			
				    			if( (this.boolRunAsBFSSearch == true)
				    					&& ( respData.getRespCode() >= 0 && respData.getRespCode() < 400 )  // exclude 404 etc.
				    					&& !( strTgtURL.contains("mailto:") || strTgtURL.contains("tel:") )
				    					&& strTagName.equalsIgnoreCase("a")
				    					&& !deque.contains(noUidPwdURL_decoded)
				    					&& !( strTgtURL.lastIndexOf("#") > strTgtURL.lastIndexOf("/") )
				    					&& !( strTgtURL.endsWith(".png") || strTgtURL.endsWith(".jpg") || strTgtURL.endsWith(".gif") )
				    					) { // Do not access to not-A-tag URL via Firefox driver.
				    				//stack.push(noUidPwdURL);  // stack
				    				deque.addLast(noUidPwdURL_decoded);  // queue
				    			}
					    		
					    		
					    		msg = String.format("%s,%s,%s,%s,%s,%s,%s",
								    				"\"" + handleDoubleQuoteForCSV(this.strURL) + "\""
								    				, linkType
								    				, "\"" + handleDoubleQuoteForCSV(noUidPwdURL_decoded) + "\""
								    				, respData.getRespMsg()
								    				, respData.getRespCode()
								    				, "\"" + altText.replaceAll("\r", "").replaceAll("\n", "").replaceAll("\"", "\"\"") + "\""
								    				, "\"" + linkText.replaceAll("\r", "").replaceAll("\n", "").replaceAll("\"", "\"\"") + "\"");
					    		 
				    			if ( respData.getRespCode() >= 400 )
				    			{
			    					new PrintStream(f_out_error.get()).println(msg);
			    					Integer prevCount = (Integer) numInvalidLink.get();
				    				numInvalidLink.set(new Integer(prevCount.intValue() + 1) ); 
				    			}
				    			else if (respData.getRespCode() < 0 )
				    			{
				    				new PrintStream(f_out_exceptions.get()).println(msg);
				    				Integer prevCount = (Integer) numExceptions.get();
				    				numExceptions.set(new Integer(prevCount.intValue() + 1) ); 
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
				    	exp_msg = String.format("%s,"
						    					+ "Tag : \"%s\","
								    			+ "At attribute : \"%s\","
								    			+ "[UnsupportedEncodingException] @class : %s,@method : %s,Message : %s",
							    			this.strURL
							    			, element.getTagName()
							    			, element.getAttribute("innerHTML")
							    			, e.getStackTrace()[0].getClassName()
							    			, e.getStackTrace()[0].getMethodName()
							    			, e.getMessage());
				    	System.out.println(exp_msg);
				    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
				    	incrementThreadLocalInt(numExceptions, 1);
				    	
		    		}
				    catch(Exception exp)
				    {
				    	exp_msg = String.format("%s,"
					    						+ "Tag:\"%s\","
								    			+ "At attribute : \"%s\","
								    			+ "[Exception] @class : %s, @method : %s, Message : %s",
							    			this.strURL
							    			, element.getTagName()
							    			, element.getAttribute("innerHTML")
							    			, exp.getStackTrace()[0].getClassName()
							    			, exp.getStackTrace()[0].getMethodName()
							    			, exp.getMessage());
				    	System.out.println(exp_msg);
				    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
				    }
				    finally {
				    	objTgtURL = null;
				    }
			     
		    	}
			}
			
			/*//2020/12/27 * /
			// take javascript console's information (error, warning)
			//LogEntries logEntries = browserDriver.manage().logs().get(LogType.BROWSER);
			String script = 
				    "(function() {" + 
				        "var oldLog = console.error;" +
				        "window.myError = [];" +
				        "console.error = function (message) {" + 
				            "window.myError.push(message);" + 
				            "oldLog.apply(console, arguments);" +
				        "};" +
				    "})();" ;
			((JavascriptExecutor) browserDriver).executeScript(script);
			//((JavascriptExecutor) browserDriver).executeScript("console.error('Test Error')");
			//Thread.sleep(3000);
			Thread.currentThread();
			Thread.sleep(10000);
			String err = (String)((JavascriptExecutor) browserDriver).executeScript("return JSON.stringify(window.myError);");
			System.out.println("----->>>>> err is " + err);
			System.out.println("----->>>>> URL is " + this.strURL);
			//for (LogEntry entry : logEntries) {
			 //   System.out.println( ">>>>>>>>>>" + entry.getTimestamp() + " " + this.strURL + " " + entry.getLevel() + " " + entry.getMessage());
			//}
			/ * //end of 2020/12/27 */
			
			/*//2020/12/28 */
			// take javascript console's information (error, warning)
			/* */
			LogEntries logEntries = browserDriver.manage().logs().get(LogType.BROWSER);
			for (LogEntry entry : logEntries) {
				console_msg = String.format("%s,%s,%s",
						                    entry.getLevel()
						                    , "\"" + handleDoubleQuoteForCSV(entry.getMessage()) + "\""
						                    , this.strURL
						                    );
		    	new PrintStream(f_out_consolelog.get()).println(console_msg);
		    	
				if (entry.getLevel().equals(Level.SEVERE))
				{
					incrementThreadLocalInt(numConsoleSevere, 1);
				}
				else if (entry.getLevel().equals(Level.WARNING))
				{
					incrementThreadLocalInt(numConsoleWarn, 1);
				}
			    //System.out.println( ">>>>>>>>>>" + entry.getTimestamp() + " " + this.strURL + "*****" + entry.getLevel() + "*****" + entry.getMessage());
			}
			/* */
			/* //end of 2020/12/28 */
		}
		catch(Exception exp)
		{
	    	exp_msg = String.format("[In Main Loop] An Exception occured at page %s. Message : %s", this.strURL, exp.getMessage());
	    	System.out.println(exp_msg);
	    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
	    	incrementThreadLocalInt(numExceptions, 1);
		}
		finally {
			
			// push back browserdriver to drivers-dequeue to reuse
			dqBrowserDrivers.addLast(browserDriver);
			
			// add obtained numbers to values in main class.
		    LinkValidator.addAndGetNumHealthyLink(numHealthyLink.get());
			LinkValidator.addAndGetNumInvalidLink(numInvalidLink.get());
			LinkValidator.addAndGetNumExternalLinks(numExternalLinks.get());
			LinkValidator.addAndGetNumExceptions(numExceptions.get());
			LinkValidator.addAndGetNumConsoleSevere(numConsoleSevere.get());
			LinkValidator.addAndGetNumConsoleWarn(numConsoleWarn.get());
			
			try {
				f_out_ok.get().close();
				f_out_error.get().close();
			    f_out_externalLinks.get().close();
			    f_out_exceptions.get().close();
			    f_out_consolelog.get().close();
			} catch (IOException exp) {
				exp.printStackTrace();
				exp_msg = String.format("[finally part in Main run()] An Exception occured at page %s. Message : %s", this.strURL, exp.getMessage());
		    	System.out.println(exp_msg);
		    	new PrintStream(f_out_exceptions.get()).println(exp_msg);
		    	incrementThreadLocalInt(numExceptions, 1);
		    	LinkValidator.addAndGetNumExceptions(numExceptions.get());
			}
					
			
			appendAndDeleteTmpFile(LinkValidator.getFStreamOutOk(), strFname_ok.get());
			appendAndDeleteTmpFile(LinkValidator.getFStreamOutError(), strFname_error.get());
			appendAndDeleteTmpFile(LinkValidator.getFStreamOutExternalSites(), strFname_externalLinks.get());
			appendAndDeleteTmpFile(LinkValidator.getFStreamOutExceptions(), strFname_exceptions.get());
			appendAndDeleteTmpFile(LinkValidator.getFStreamOutConsoleLog(), strFname_consoleLog.get());
			

		}
		
	}

}
