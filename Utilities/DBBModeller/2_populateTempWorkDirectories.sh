#################################
# Script to process YAML files and setup the work directory
# and scans application contexts
#################################

# script variables
rc=0
application=""

# path for storing all files
SCRIPT_HOME="$(
    cd -- "$(dirname "$0")" >/dev/null 2>&1
    pwd -P
)"
PARENT_DIR="$(dirname "$SCRIPT_HOME")"
codebaseDir="${PARENT_DIR}/work_codebase"
applicationDir="${PARENT_DIR}/work_applications"

if [ ! -d "$applicationDir" ]; then
    mkdir $applicationDir
fi

###########
# functions
###########

# method assessing the application config file
processApplicationConfig() {

    applicationName=$(echo ${application%.*})
    # Evaluate the YAML scripts
    if [ $rc -eq 0 ]; then
        echo "*******************************************************************"
        echo "Process Application Descriptor ${PARENT_DIR}/applicationConfigurations/$application"
        echo "*******************************************************************"
        groovyz $SCRIPT_HOME/processApplicationConfigurations.groovy ${codebaseDir} ${applicationDir} ${PARENT_DIR}/applicationConfigurations/$application
        rc=$?
    fi

    if [ $rc -eq 0 ]; then
        echo "*******************************************************************"
        echo "Scan application directory ${applicationDir}/${applicationName}"
        echo "*******************************************************************"
        groovyz $SCRIPT_HOME/scanApplication.groovy -w ${applicationDir} -a ${applicationName}
        rc=$?
    fi

}

printUnownedModules() {

    #
    # Print remaining COBOL code and print remaining COBYBOOKs
    if [ $rc -eq 0 ]; then
        echo "*******************************************************************"
        echo "* Print files that were left behind in the work codebase directory"
        echo "*******************************************************************"

        ls -Rlisa ${codebaseDir}
        rc=$?
    fi

}

copyUnclassifiedCopybooks() {

    #
    # Copy remaining unclassified copybooks to Unclassified-Copybooks directory.
    if [ $rc -eq 0 ]; then
        echo "*******************************************************************"
        echo "* Copy unassigned copybooks to copybook folder"
        echo "*******************************************************************"

        cp -Rf ${codebaseDir}/copy ${applicationDir}/Unclassified-Copybooks

    fi

    if [ $rc -eq 0 ]; then
        echo "*******************************************************************"
        echo "Scan unclassified copybooks ${applicationDir}/${application}"
        echo "*******************************************************************"
        groovyz $SCRIPT_HOME/scanApplication.groovy -w ${applicationDir} -a Unclassified-Copybooks
        rc=$?
    fi

}

################
# functions end
################

###########
# Mainline
###########

# Assess all provided application configurations
for f in $(ls ${PARENT_DIR}/applicationConfigurations); do
    application=$f
    processApplicationConfig
done

# Print remaining COBOL code and print remaining COBYBOOKs
printUnownedModules

# Copy remaining unclassified copybooks to Unclassified-Copybooks directory
copyUnclassifiedCopybooks
