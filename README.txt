o Program Name
  SLinkValidator

o Basic behavior
  * This program,
    - browses the given URL by Firefox and validate link or image location in the page.
    — follows below 2 types of contents by default.
        "href" in <a> tag
        "src" in <img> tag
    - also checks "href" content in <link> tag if “-a” or “—all” option was specified in command line.
    - validate given URL page and links in the page.
    - continue browsing/checking if the found link path was included in the given URL.

o Verified Platform
  * Windows 7, Mac OS X 10.10 (Yosemite) (JRE 1.8 or higher)

o Required software/component
  * Java 8 (JRE 1.8 or higher)
  * Selenium Client WebDriver
  * Firefox (browses using firefox driver)
  * Apache Commons CLI (I have created this software as the CLI application)

o Feature
  * automatically follows links in browsed page.
    - This program automatically browses links found in the given URL page if the link path contains the first URL.
    - You can specify urls by making a url list (a file contains one url per line).
  * automatically takes snapshot.
    - This program takes a snapshot of the browsed page automatically.
      —-  "/" character in the URL path is replaced to "_" (underscore).
      —- Other invalid characters as the windows file name such as ? <>|"(double quote) are
         also replaced to "_".
      —- This software does not support Chrome and InternetExplorer since the driver of those two browser
          cannot take full page screenshot.
  * Multi threading. 
    The thread number, which is equal the number of simultaneously opening browser, can be specified by “-T” option.
    If you give ‘-T auto’ as the option, this program automatically detect the number of available processor and use 
  * BASIC authentication handling.

o How to use.
  Build runnable jars with necessary jar(s) and run it as the command line application
  with specifying necessary options.

  (example)
  $> java -jar SLinkValidator.jar -url http://a.b.com/


o How to specify URL to browse.

  o -url <URL>
    Specify one URL to be checked.
    This software browses the page checks the links in it,
    and follows the link that contains the given URL in its path.

  o -f <filename>
    Checks only the given urls in specified file.
    The url should be listed one per line.

o All available options

 -a,--all                      Also check "link" tag.
 -f,--url-list <FILE>          Specify a text file containing urls to be
                               checked.
 -h,--help                     print this help.
 -id,--user <USERNAME>         user id for the BASIC authentication.
 -p,--password <PASSWORD>      password for the BASIC authentication.
 -o, --timeout <Timeout Sec>   second for timeout.
 -T,--thread <NUM of Thread>   number of thread (must be an integer, less
                               than 16). 'AUTO' for available processor
                               num.
 -url <URL>                    Base URL to be checked.
 -v,--verbose                  verbose output mode.
 -V,--version                  print version number. 

o Output
  * This program generates following directories/files as output.

    - ‘results’ : directory that result files to be saved.
    - ‘results/screenshot’ : screenshots are saved in this directory.

  * Following files will be created under "results" directory.

    - healthy_links-[TIMESTAMP].txt
      log of links with HTTP status code 2xx, 3xx.
  
    - broken_links-[TIMESTAMP].txt
      log of links with HTTP status above 400 (4xx, 5xx).
  
    - exceptions-[TIMESTAMP].txt
      logs of java exception.
  
    - external_links-[TIMESTAMP].txt
      logs of links being judged external, which is included in the given URL.
  
    - __tmp-xxxx.txt
      temp file of each thread, will be deleted after each thread finishes.
      (the meaning of the filename is  __tmp_[thread ID]_[browsed page num]_[timestamp]_xxx.txt)

o Others
  - Browsing timeout is 2 miniutes in case of download pop-up (zip download etc.).
  - This program neither follow links that created dynamically by JavaScript nor submit the form automatically.
    (follows only links in the page source.)
  - As mentioned in above “Feature” section, this program does not support Chrome and InternetExplorer
    since the driver of those two browser cannot take full-page screenshot.
