#!/bin/bash
# Bash Script Example

function buildTools {
	cd DELTA.Desktop
	echo -e "\n\nBuilding DELTA Tools.\nIf this is your first build, please be patient as a copy of the Gradle bundle will be downloaded..."
	./gradlew deltabuild
	cd ..
}

function buildDeltaCore {
	cd DELTA.Android
	./gradlew :delta.core:assemble &&
	echo "Building complete. The APK files are located under DELTA.Android/delta.core/build/outputs/apk/"
	cd ..
}

function launchExperimentMaker {
	cd bin
	java -jar delta.desktoptools.experimentmaker.jar
	cd ..
}

function launchLogTool {
	cd bin
	java -jar delta.desktoptools.logtool.jar
	cd ..
}

function launchWebService {
	cd bin
	echo "Note: Delta Web Server settings can be changed in bin/delta_settings.ini"
	java -jar delta.webserver.jar
	cd..
}

clear
while :
do
    cat<<EOF


====================================
DELTA Tools helper utility. Welcome!
------------------------------------

This script will help you build and launch the DELTA Desktop tools.
IMPORTANT: Before attempting to build the DELTA Tools, please ensure that you have the Java 7 JDK installed and configured!

Please enter your choice:

(1) Build DELTA Tools
(2) Build DELTA Core App
(3) Launch Experiment Maker
(4) Launch Log Tool
(5) Start the DELTA Web Service
(6) Quit
------------------------------
EOF
    read -n1 -s
    case "$REPLY" in
    "1")  buildTools ;;
    "2")  buildDeltaCore ;;
    "3")  launchExperimentMaker ;;
    "4")  launchLogTool ;;
    "5")  launchWebService ;;
    "6")  exit                      ;;
     * )  echo "Invalid option, please try again"     ;;
    esac
    sleep 1
done





