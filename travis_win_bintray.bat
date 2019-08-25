REM free some space

POWERSHELL "[Net.ServicePointManager]::SecurityProtocol = 'tls12, tls11, tls'; (New-Object System.Net.WebClient).DownloadFile('https://github.com/ojdkbuild/ojdkbuild/releases/download/1.8.0.191-1/java-1.8.0-openjdk-1.8.0.191-1.b12.ojdkbuild.windows.x86_64.zip','java.zip')"
POWERSHELL "Expand-Archive java.zip -DestinationPath c:\java8"
DIR c:\java8
DIR c:\java8\java-1.8.0-openjdk-1.8.0.191-1.b12.ojdkbuild.windows.x86_64
SET JAVA_HOME=c:\java8\java-1.8.0-openjdk-1.8.0.191-1.b12.ojdkbuild.windows.x86_64

SET GRADLE_OPTS=-Dorg.gradle.daemon=false
CALL gradlew.bat --no-daemon -s -i localPublishToBintrayIfRequired
SET GRADLE_ERROR_LEVEL=%errorlevel%
CALL gradlew.bat --stop
taskkill /f /im java.exe
exit /b %GRADLE_ERROR_LEVEL%
