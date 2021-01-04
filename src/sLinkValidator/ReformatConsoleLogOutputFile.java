// Copyright 2020 Koji Nobumoto
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
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReformatConsoleLogOutputFile
{
	private final static ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> hashtable = 
			new ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>>();

	// output the data of hashtable into TSV (Tab Separated Varlues) format.
	public static void printOutHashTable(ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> ht
										, String strResultDir
										, String strFname) {
		
		String exp_msg           = null;
		
		try {
			FileOutputStream f_out_formattedhashtable;
			f_out_formattedhashtable = new FileOutputStream (strResultDir + File.separator + strFname, true);
			
			new PrintStream(f_out_formattedhashtable).println( String.format("%s\t%s\t%s",
															                    "LogLevel"
															                    , "Message"
															                    , "URL")
															                  );
			
			boolean boolFirstLogLevel = true;
			boolean boolFirstMessage = true;
			//boolean boolFirstUrl = true;
	
			for (String loglevel : ht.keySet()) {
				ConcurrentHashMap<String, HashSet<String>> messages = ht.get(loglevel);
				
				for (String msg : messages.keySet()) {
					for (String url : messages.get(msg)) {
						if (boolFirstLogLevel && boolFirstMessage)
						{
							new PrintStream(f_out_formattedhashtable).println(String.format("%s\t%s\t%s", loglevel, msg, url));
							boolFirstLogLevel = false;
							boolFirstMessage = false;
						}
						else if (boolFirstMessage)
						{
							//new PrintStream(f_out_formattedhashtable).println(String.format("\t%s\t%s", msg, url));
							// for the better visualization
							new PrintStream(f_out_formattedhashtable).println(String.format("%s\t%s\t%s", loglevel, msg, url));
							boolFirstMessage = false;
						}
						else
						{
							new PrintStream(f_out_formattedhashtable).println(String.format("\t\t%s", url));
						}
					}
					boolFirstMessage = true;
					
				}
				boolFirstLogLevel = true;
				
			}
			
			f_out_formattedhashtable.close();
		}
		catch(Exception exp) {
			exp.printStackTrace();
    		exp_msg = String.format("Exception in printOutHashTable(). Message : %s", exp.getMessage());
	    	System.out.println(exp_msg);
		}
	}
	
	@SuppressWarnings("serial")
	public ReformatConsoleLogOutputFile(String timeStamp) throws Exception {
    	
    	String exp_msg           = null;
    	
    	String strResultsDir = "results" + "-" + timeStamp;
    	String strSourceFile = LinkValidator.getFnameConsoleLog();
    	String strDestFile = "07-02.formatted_console_logs-" + timeStamp + ".csv";
    	
    	BufferedReader reader;
    	    	
    	try {
    		reader = new BufferedReader(
    					new FileReader(strResultsDir + File.separator + strSourceFile)
    					);
    		String line = reader.readLine();
    		
    		while (line != null) {
    			
    			// initialize variables
    			String strPageUrl		= "";
    			String strLogLevel		= "";
    			String strLogMsg		= "";
    			
    			Pattern pattern = Pattern.compile("^([^,]+),\\s*\"(.+)\",\\s*([^,]+)$");
    			Matcher match = pattern.matcher(line);
    			
    			if (match.find())
    			{
    				strLogLevel	= match.group(1).trim();
    				strLogMsg	= match.group(2).trim();
    				strPageUrl	= match.group(3).trim();
    				
    				final String __strLogMsg = new String(strLogMsg);
    				hashtable.putIfAbsent(strLogLevel, new ConcurrentHashMap<String, HashSet<String>>(){{
    															put(__strLogMsg, new HashSet<String>()); 
    														}}
    									);
    				hashtable.get(strLogLevel).putIfAbsent(strLogMsg, new HashSet<String>());
    				hashtable.get(strLogLevel).get(strLogMsg).add(strPageUrl);
    				
    			}
    			
    			line = reader.readLine();
    			
    		}
    		reader.close();
    		
    		printOutHashTable(hashtable, strResultsDir, strDestFile);
    	}
    	catch (IOException exp) {
    		exp.printStackTrace();
    		exp_msg = String.format("Exception in ReformatOutputFile. Message : %s", exp.getMessage());
	    	System.out.println(exp_msg);
    	}
        

    }

}