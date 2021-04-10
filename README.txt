o Program Name
  SLinkValidator (Simple Link Validator)

o Basic behavior
  * This program,
    - browses the given URL by Chrome and validate link or image location in the page.
    — follows below 2 types of contents by default.
        "href" in <a> tag
        "src" in <img> tag
    - also checks "href" content in <link> tag if “-a” or “—all” option was specified in command line.
      (* "-skipelement" option skips all above link check and just browse given url(s))
    - validate given URL page and links in the page.
    - continue browsing/checking if the found link path was under the given URL (domain or initial URL).
    - this program depends on the Chromedriver to take browser's console.log.


o Verified Platform
  * Windows 10

o Required software/component
  * Java 8 (JRE 1.8 or higher)
  * Selenium Client WebDriver
  * ChromeDriver
  * Apache Commons CLI (I have created this software as the CLI application)

o Feature
  * automatically follows links in browsed page.
    - This program automatically browses links found in the given URL page if the link path contains the initial URL (it's a domain in most cases).
      -- "-skipelement" option skips all above link check and just browse given url(s)
    - You can specify urls by making a url list (a file contains one url per line).
  * Multi threading. 
    The thread number, which is equal to the number of simultaneously opening browser, can be specified by “-T” option.
    If you give ‘-T auto’ as the option, this program automatically detect the number of available processor and use it.
  * BASIC authentication handling.
  * saves chrome browser's console.log.
  * takes page capture ("-capture" option)
    - with the "-capture" option, this program takes a screen capture of the browsed page.
      --  "/" character in the URL path is replaced to "_" (underscore).
      -- Other invalid characters as the windows file name such as ? <>|"(double quote) are
         also replaced to "_".

o How to use.
  Build runnable jars with necessary jar(s) and run it as the command line application
  with specifying necessary options.

  (example)
  $> java -jar SLinkValidator.jar -webdriver "C:\Program Files (x86)\chromedriver\chromedriver.exe" -url http://a.b.com/


o How to specify URL to browse.

  o -url <URL>
    Specify one URL to be checked.
    This software browses the page checks the links in it,
    and follows the link that contains the given URL in its path.

  o -f <filename>
    Checks only the given urls in specified file.
    The url should be listed one per line.

o All available options

 -webdriver, --path-to-webdriver (mandatory)
                               Specify the full path to the chromedriver binary. 

 -a,--all                      Also check "link" tag.
 -capture,--screenshot         takes the page capture.
 -f,--url-list <FILE>          Specify a text file containing urls to be
                               checked.
 -h,--help                     print this help.
 -id,--user <USERNAME>         user id for the BASIC authentication.
 -p,--password <PASSWORD>      password for the BASIC authentication.
 -o, --timeout <Timeout Sec>   second for timeout.
 --implicitlywait <ImplicitlyWait sec>
                               second for implicitly wait.
 -skipelement, --no-element-check
                               checks given url only, no element in the page is checked.
 -instancefollowredirects      if set, HttpURLConnection follows redirects.
 -T,--thread <NUM of Thread>   number of thread (must be an integer, less
                               than 16). 'AUTO' for available processor
                               num.
 -url <URL>                    Base URL to be checked.
 -v,--verbose                  verbose output mode (outputs all result on colsole).
 -s,--sitemap                  sitemap mode (follows only <a> tag）
 -V,--version                  print version number. 

o Output
  * This program generates following directories/files as output.

    - ‘results’ : directory that result files to be saved.
    - ‘results/screenshot’ : screenshots are saved in this directory.

  * Following files will be created under "results" directory.

    - 01.summary-[TIMESTAMP].txt
      summary of the output

    - 02.browsed_pages-[TIMESTAMP].csv
      browsed pages and it's status.

    - 03.healthy_links-[TIMESTAMP].csv
      log of links with HTTP status code 2xx, 3xx.
  
    - 04.broken_links-[TIMESTAMP].csv
      log of links with HTTP status above 400 (4xx, 5xx).
  
    - 05.external_links-[TIMESTAMP].txt
      logs of links being judged external, which is included in the given URL.
  
    - 06.exceptions-[TIMESTAMP].txt
      logs of java exception.
  
    - 07.console_logs-[TIMESTAMP].csv
      console.logs of browsed pages.
      
      - 07-02.formatted_console_logs-[TIMESTAMP].csv
        TSV formatted file of 07.console_logs-XXXX.csv
  
    - __tmp-xxxx.csv
      temp file of each thread, will be deleted after each thread finishes.
      (the meaning of the filename is  __tmp_[thread ID]_[browsed page num]_[timestamp]_xxx.csv)

o Others
  - This program neither follow links that created dynamically by JavaScript nor submit the form automatically.
    (follows only links in the page source.)
