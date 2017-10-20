#!/bin/bash
# Script requires BioLockJ config file passed as a parameter
# Optional 2nd param may be restart flag "r" or "restart"
# Optional 2nd param may be new admin email password, if so it is encoded & stored in the config file

GAP="    "
SPACER="============================================================"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BLJ_JAR=$DIR/dist/BioLockJ.jar
P1=$(echo "$1" | awk '{print tolower($0)}')
P2=$(echo "$2" | awk '{print tolower($0)}')
echo $GAP
echo $GAP $SPACER
echo $GAP $SPACER
echo $GAP
echo "$GAP Executing JAR:  $BLJ_JAR"
echo $GAP
if [ "$#" -eq 1 ]; then
    echo "$GAP Configuration:  $1"
    echo $GAP
	nohup java -jar $BLJ_JAR $1 >/dev/null 2>&1 &
    exitCode=$?
    if [[ $exitCode != "0" ]]; then
        echo "$GAP ERROR - Unable to execute:  $BLJ_JAR"
    else
        echo "$GAP BioLockJ started successfully!"
    fi    
elif [ "$#" -eq 2 ]; then
	if [ "$P1" == "r" ] || [ "$P1" == "restart" ]; then 
    	echo "$GAP RESTART with config:   $2"
    elif [ "$P2" == "r" ] || [ "$P2" == "restart" ]; then
        echo "$GAP RESTART with config:   $1"
    else
        echo "$GAP UPDATE email password..."
	fi
    echo $GAP
    nohup java -jar $BLJ_JAR $1 $2 >/dev/null 2>&1 &
    exitCode=$?
    if [[ $exitCode != "0" ]]; then
        echo "$GAP ERROR - Unable to execute $BLJ_JAR"
    else
        echo "$GAP BioLockJ started successfully!"
    fi  
elif [ "$#" -eq 0 ]; then
        echo "$GAP CANCEL EXECUTION - Must pass script path to BioLockJ config file as parameter" 
elif [ "$#" -gt 2 ]; then
        echo "$GAP CANCEL EXECUTION - Too many script parameters!  BioLockJ accepts a maximum of 2 parameters"
fi
echo $GAP
echo $GAP $SPACER
echo $GAP $SPACER
echo $GAP

