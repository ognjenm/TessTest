###Windows Dependency:
The Tesseract Binaries shipped with Tess4J depend on the Visual C++ Redistributable for VS2013.
This dependency needs to be installed on a Windows machine before running Tess4J.

https://www.dropbox.com/s/1ttb8lwn2qfbz0m/Screenshot%202016-10-24%2017.02.34.png?dl=1
Screenshot of settings


mvn clean package

###USAGE:
place application.properties file in same folder as pdfdemo-0.0.1-SNAPSHOT.ja

java -jar pdfdemo-0.0.1-SNAPSHOT.jar C:\\Inputfile.pdf C:\\outfile


- Tested on Win 10 64bit - no need to install anything except java
- On Linux system Tesseract must be installed separately
