#!/bin/bash
# get the BioLockJ build version
function get_version {
	{ read -r v; } <"$BLJ/.version"
	echo $v
}

BLJ_VERSION=$(get_version)

echo version: $BLJ_VERSION