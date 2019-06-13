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
ECHO 2) Build DELTA Core App
ECHO 3) Launch Experiment Maker
ECHO 4) Launch Log Tool
ECHO 5) Start the DELTA Web Service
ECHO 6) Quit
ECHO.
SET /P M=Make your choice:
IF %M%==1 GOTO BUILD
IF %M%==2 GOTO BUILDDELTACORE
IF %M%==3 GOTO LAUNCHMAKER
IF %M%==4 GOTO LAUNCHLOGTOOL
IF %M%==5 GOTO LAUNCHWEBSERVICE
IF %M%==6 GOTO QUIT
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

:BUILDDELTACORE
cd DELTA.Android
call gradlew :delta.core:assemble
ECHO "Building complete. The APK files are located under DELTA.Android/delta.core/build/outputs/apk/"
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
ECHO Note: Delta Web Server settings can be changed in bin/delta_settings.ini
start java -jar delta.webserver.jar
cd..
GOTO MENU

:QUIT
ECHO Bye...