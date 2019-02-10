REM free some space

POWERSHELL "[Net.ServicePointManager]::SecurityProtocol = 'tls12, tls11, tls'; (New-Object System.Net.WebClient).DownloadFile('https://github.com/ojdkbuild/ojdkbuild/releases/download/1.8.0.191-1/java-1.8.0-openjdk-1.8.0.191-1.b12.ojdkbuild.windows.x86_64.zip','java.zip')"
POWERSHELL "Expand-Archive java.zip -DestinationPath c:\java8"
DIR c:\java8
DIR c:\java8\java-1.8.0-openjdk-1.8.0.191-1.b12.ojdkbuild.windows.x86_64
SET JAVA_HOME=c:\java8\java-1.8.0-openjdk-1.8.0.191-1.b12.ojdkbuild.windows.x86_64

REM CALL refreshenv
REM SET GRADLE_ERROR_LEVEL=%errorlevel%
REM CALL gradlew.bat --stop
REM exit /b %GRADLE_ERROR_LEVEL%

CALL gradlew.bat --no-daemon -s -i mingwX64Test
