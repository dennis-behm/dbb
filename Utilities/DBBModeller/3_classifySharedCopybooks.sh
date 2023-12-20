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
applicationDirs="${PARENT_DIR}/work_applications"

# classify application Unclassified-Copybooks
groovyz $SCRIPT_HOME/classifyCopybooks.groovy \
 --workspace $applicationDirs \
 --application Unclassified-Copybooks \
 --configurations $PARENT_DIR/applicationConfigurations \
 --copySharedCopybooks \
 --generateUpdatedApplicationConfiguration 