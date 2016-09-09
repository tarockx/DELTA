ECHO OFF
CLS
:MENU
ECHO.
ECHO ====================================
ECHO DELTA Tools helper utility. Welcome!
ECHO ------------------------------------
ECHO.
ECHO This script will help you build and launch the DELTA Desktop tools.
ECHO IMPORTANT: Before attempting to build the DELTA Tools, please ensure that you have the Java 7 JDK installed and configured!
ECHO.
ECHO 1) Build DELTA Tools
ECHO 2) Launch Experiment Maker
ECHO 3) Launch Log Tool
ECHO 4) Start the DELTA Web Service
ECHO 5) Quit
ECHO.
SET /P M=Make your choice:
IF %M%==1 GOTO BUILD
IF %M%==2 GOTO LAUNCHMAKER
IF %M%==3 GOTO LAUNCHLOGTOOL
IF %M%==4 GOTO LAUNCHWEBSERVICE
IF %M%==5 GOTO QUIT
ECHO Invalid option, please try again

:BUILD
cd DELTA.Desktop
ECHO.
ECHO.
ECHO Building DELTA Tools.
ECHO If this is your first build, please be patient as a copy of the Gradle bundle will be downloaded...
call gradlew deltabuild
cd..
GOTO MENU

:LAUNCHMAKER
cd bin
start java -jar delta.desktoptools.experimentmaker.jar
cd..
GOTO MENU

:LAUNCHLOGTOOL
cd bin
start java -jar delta.desktoptools.logtool.jar
cd..
GOTO MENU

:LAUNCHWEBSERVICE
cd bin
start java -jar delta.webserver.jar
cd..
GOTO MENU

:QUIT
ECHO Bye...