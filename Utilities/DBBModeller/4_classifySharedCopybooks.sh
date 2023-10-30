#!/bin/env bash
###############
# classify and dispatch copybooks
###############

# Locate script home
SCRIPT_HOME="$(
    cd -- "$(dirname "$0")" >/dev/null 2>&1
    pwd -P
)"
PARENT_DIR="$(dirname "$SCRIPT_HOME")"
applicationDir="${PARENT_DIR}/work_applications"

# classify Unclassified-Copybooks
groovyz $SCRIPT_HOME/classifyCopybooks.groovy -w  $applicationDir -a Unclassified-Copybooks -c $PARENT_DIR/applicationConfigurations -cf