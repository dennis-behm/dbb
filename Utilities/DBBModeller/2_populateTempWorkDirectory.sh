#################################
# Script to process YAML files and setup the work directory
#################################

# script variables
rc=0
application=""

# path for storing all files
SCRIPT_HOME="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
PARENT_DIR="$(dirname "$SCRIPT_HOME")"
codebaseDir="${PARENT_DIR}/work_codebase"
applicationDir="${PARENT_DIR}/work_applications"

if [ ! -d "$applicationDir" ]; then
    mkdir $applicationDir
fi

# method assessing the application config file
processApplicationConfig() {
    # Evaluate the YAML scripts
    if [ $rc -eq 0 ]; then
        groovyz $SCRIPT_HOME/processApplicationConfigurations.groovy ${codebaseDir} ${applicationDir} ${PARENT_DIR}/applicationConfigurations/$application
        rc=$?
    fi

}

# 
for f in $(ls ${PARENT_DIR}/applicationConfigurations); do
    echo "** Found applicationConfiguration $f "
    application=$f
    processApplicationConfig
done

#
# Print remaining COBOL code and print remaining COBYBOOKs
if [ $rc -eq 0 ]; then
    echo "* Print files that were left behind in the work codebase directory"
    ls -Rlisa ${codebaseDir}
    rc=$?
fi

#
# Copy remaining unclassified copybooks to Unclassified-Copybooks directory.
if [ $rc -eq 0 ]; then
    echo "* Copy unassigned copybooks to copybook folder"
    cp -Rf ${codebaseDir}/copy ${applicationDir}/Unclassified-Copybooks
fi
