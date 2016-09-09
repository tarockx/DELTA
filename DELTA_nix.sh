#!/bin/bash
# Bash Script Example

function buildTools {
	cd DELTA.Desktop
	echo -e "\n\nBuilding DELTA Tools.\nIf this is your first build, please be patient as a copy of the Gradle bundle will be downloaded..."
	chmod +x ./gradlew
	./gradlew deltabuild
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
(2) Launch Experiment Maker
(3) Launch Log Tool
(4) Quit
------------------------------
EOF
    read -n1 -s
    case "$REPLY" in
    "1")  buildTools ;;
    "2")  launchExperimentMaker ;;
    "3")  launchLogTool ;;
    "4")  exit                      ;;
     * )  echo "Invalid option, please try again"     ;;
    esac
    sleep 1
done





