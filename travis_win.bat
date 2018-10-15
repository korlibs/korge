REM free some space

dir

REM dir c:\
REM dir "c:\Program Files"
REM dir "c:\Program Files (x86)"

REM dir /s C:\ProgramData\chocolatey
REM choco uninstall all
REM choco uninstall -y -f cmake cmake.install DotNet4.5 DotNet4.6 windows-sdk-10.0 winscp winscp.install ruby microsoft-build-tools visualstudio2017-workload-netcorebuildtools visualstudio2017-workload-vctools visualstudio2017-workload-webbuildtools visualstudio2017buildtools

RD /s /q "c:\Program Files\IIS"
RD /s /q "c:\Program Files\Java"
RD /s /q "c:\Program Files\Microsoft"
RD /s /q "c:\Program Files\Microsoft Visual Studio"
RD /s /q "c:\Program Files\Microsoft Visual Studio 14.0"
RD /s /q "c:\Program Files\cmake"
RD /s /q "c:\Program Files\Microsoft SDKs"
RD /s /q "c:\Program Files (x86)\IIS"
RD /s /q "c:\Program Files (x86)\Java"
RD /s /q "c:\Program Files (x86)\Microsoft"
RD /s /q "c:\Program Files (x86)\Microsoft Visual Studio"
RD /s /q "c:\Program Files (x86)\Microsoft Visual Studio 14.0"
RD /s /q "c:\Program Files (x86)\cmake"
RD /s /q "c:\Program Files (x86)\Microsoft SDKs"
REM RD /s /q C:\ProgramData\chocolatey
REM RD /s /q C:\ProgramData

dir

choco list --local-only
choco install jdk8 -y -params "installdir=c:\\java8"

del c:\java8\src.zip
del c:\java8\javafx-src.zip

dir c:\java8
dir c:\java8\lib

CALL refreshenv

SET JAVA_HOME=c:\java8
CALL gradlew.bat --no-daemon -s -i mingwX64Test
CALL gradlew.bat --stop
