#!/bin/env bash
# Scan all work directories

# internal script variables
application=""
rc=0

# Locate script home
SCRIPT_HOME="$(
    cd -- "$(dirname "$0")" >/dev/null 2>&1
    pwd -P
)"
PARENT_DIR="$(dirname "$SCRIPT_HOME")"
applicationDir="${PARENT_DIR}/work_applications"

scanApplication() {
    if [ $rc -eq 0 ]; then
        groovyz $SCRIPT_HOME/scanApplications.groovy -w $applicationDir -a $application
        rc=$?
    fi
}

for f in $(ls ${applicationDir}); do
    if [ $rc -eq 0 ]; then
        echo "** Scan application directory $f "
        application=$f
        scanApplication
    fi
done
