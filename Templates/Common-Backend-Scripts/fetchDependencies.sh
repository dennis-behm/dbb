#!/bin/env bash
#===================================================================================
# NAME: fetchDependencies.sh
#
# DESCRIPTION: The purpose of this script is to fetch all build dependencies
#
# SYNTAX: See Help() Section Below
#
# OPTIONS: See Help() Section Below
#
# RETURNS:
#
#    rc         - Return Code
#
# RETURN CODES:
#
#    0          - Successful
#    4          - Warning message(s) issued. See Console messages.
#    8          - Error encountered. See Console messages.
#
# NOTE(S):
#
#   None
#
# Maintenance Log
#
# Date       Who Vers  Description
# ---------- --- ----- --------------------------------------------------------------
# 2024/02/23 DB  1.0.0 Initial Release
#===================================================================================

Help() {
    echo $PGM" - Fetch Dependencies ("$PGMVERS")                        "
    echo "                                                              "
    echo "Description:  The purpose of this script is to fetch          "
    echo "all build dependencies                                        "
    echo "                                                              "
    echo "Syntax:                                                       "
    echo "                                                              "
    echo "       "$PGM" [Options]                                       "
    echo "                                                              "
    echo "Options:                                                      "
    echo "                                                              "
    echo "       -h           - Help.                                   "
    echo "                                                              "
    echo "                                                              "
    echo "       -w <workspace>      - Directory Path to a unique       "
    echo "                             working directory                "
    echo "                             Either an absolute path          "
    echo "                             or relative path.                "
    echo "                             If a relative path is provided,  "
    echo "                             buildRootDir and the workspace   "
    echo "                             path are combined                "
    echo "                             Default=None, Required.          "
    echo "                                                              "
    echo "                 Ex: MortgageApplication/main/build-1         "
    echo "                                                              "
    echo "         Ex: /../dbb-logs                                     "
    echo "                                                              "
    echo "       Optional:                                              "
    echo "       -a <Application>    - Folder name to clone the         "
    echo "                             application git repo             "
    echo "                                                              "
    echo "                 Ex: MortgageApplication                      "
    echo "                                                              "
    echo "         Ex: refs/heads/main                                  "
    echo " "
    exit 0
}

#
# Customization
# Configuration file leveraged by the backend scripts
# Either an absolute path or a relative path to the current working directory
SCRIPT_HOME="$(dirname "$0")"
pipelineConfiguration="${SCRIPT_HOME}/pipelineBackend.config"
# Customization - End

#
# Internal Variables
#set -x                  # Uncomment to enable shell script debug
#export BASH_XTRACEFD=1  # Write set -x trace to file descriptor

PGM=$(basename "$0")
PGMVERS="1.0.0"
USER=$(whoami)
SYS=$(uname -Ia)

rc=0
ERRMSG=""
WorkDir=""
application=""
applicationDescriptor=""
askpass=""
pat=""
HELP=$1

if [ "$HELP" = "?" ]; then
    Help
fi

# Validate Shell environment
currentShell=$(ps -p $$ | grep bash)
if [ -z "${currentShell}" ]; then
    rc=8
    ERRMSG=$PGM": [ERROR] The scripts are designed to run in bash. You are running a different shell. rc=${rc}. \n. $(ps -p $$)."
    echo $ERRMSG
fi
#

# Print script title
if [ $rc -eq 0 ]; then
    echo $PGM": [INFO] Fetch Dependencies Wrapper. Version="$PGMVERS
fi

# Read and import pipeline configuration
if [ $rc -eq 0 ]; then
    if [ ! -f "${pipelineConfiguration}" ]; then
        rc=8
        ERRMSG=$PGM": [ERROR] Pipeline Configuration File (${pipelineConfiguration}) was not found. rc="$rc
        echo $ERRMSG
    else
        source $pipelineConfiguration
    fi
fi

#

# Get Options
if [ $rc -eq 0 ]; then
    while getopts "h:a:w:p:" opt; do
        case $opt in
        h)
            Help
            ;;
        w)
            argument="$OPTARG"
            nextchar="$(expr substr $argument 1 1)"
            if [ -z "$argument" ] || [ "$nextchar" = "-" ]; then
                rc=4
                ERRMSG=$PGM": [WARNING] Build Workspace Folder Name is required. rc="$rc
                echo $ERRMSG
                break
            fi
            Workspace="$argument"
            ;;
        a)
            argument="$OPTARG"
            nextchar="$(expr substr $argument 1 1)"
            if [ -z "$argument" ] || [ "$nextchar" = "-" ]; then
                rc=4
                ERRMSG=$PGM": [WARNING] Application Name is required. rc="$rc
                echo $ERRMSG
                break
            fi
            application="$argument"
            ;;
        p)
            argument="$OPTARG"
            nextchar="$(expr substr $argument 1 1)"
            if [ -z "$argument" ] || [ "$nextchar" = "-" ]; then
                rc=4
                ERRMSG=$PGM": [WARNING] Personal Access Token is required. rc="$rc
                echo $ERRMSG
                break
            fi
            pat="$argument"
            ;;
        \?)
            Help
            rc=1
            break
            ;;
        :)
            rc=4
            ERRMSG=$PGM": [WARNING] Option -$OPTARG requires an argument. rc="$rc
            echo $ERRMSG
            break
            ;;
        esac
    done
fi
#

# Validate Options
validateOptions() {

    if [ -z "${application}" ]; then
        rc=8
        ERRMSG=$PGM": [ERROR] Application Name is required. rc="$rc
        echo $ERRMSG
    fi

    if [ -z "${Workspace}" ]; then
        rc=8
        ERRMSG=$PGM": [ERROR] Unique Workspace Path is required. rc="$rc
        echo $ERRMSG
    else
        if [[ ${Workspace:0:1} != "/" ]]; then
            if [ ! -d "${buildRootDir}" ]; then
                rc=8
                ERRMSG=$PGM": [ERROR] Workspace Directory (${buildRootDir}) was not found. rc="$rc
                echo $ERRMSG
            fi
        fi
    fi

    if [ ! -f "${applicationDependencyConfiguration}" ]; then
        rc=8
        ERRMSG=$PGM": [ERROR] Application Dependency Configurations File (${applicationDependencyConfiguration}) was not found. rc="$rc
        echo $ERRMSG
    fi

    # TODO: - externalize to config
    applicationDescriptor="$(getWorkDirectory)/${application}/applicationDescriptor.yaml"

    # Set up to perform the clone of the Repo
    if [ ! -f "${applicationDescriptor}" ]; then
        rc=8
        ERRMSG=$PGM": [ERROR] Application Descriptor File (${applicationDescriptor}) was not found. rc="$rc
        echo $ERRMSG
    fi

}
#

# Call validate Options
if [ $rc -eq 0 ]; then
    validateOptions
fi
#

# Compute variables
askpass=$(getWorkDirectory)/gitAskpass.sh

# Ready to go
if [ $rc -eq 0 ]; then
    echo $PGM": [INFO] **************************************************************"
    echo $PGM": [INFO] ** Start Fetch Dependencies on HOST/USER: ${SYS}/${USER}"
    echo $PGM": [INFO] **                     WorkDir:" $(getWorkDirectory)
    echo $PGM": [INFO] **                 Application:" ${application}
    echo $PGM": [INFO] **     Application Descriptor :" ${applicationDescriptor}
    echo $PGM": [INFO] ** Application Configurations :" ${applicationDependencyConfiguration}
    if [ ! -z "${pat}" ]; then
        echo $PGM": [INFO] **      Personal Access Token : provided"
    fi
    echo $PGM": [INFO] **************************************************************"
    echo ""
fi
#

if [ $rc -eq 0 ]; then
    echo $PGM": [INFO] ** Generate GIT_ASKPASS to $askpass"
    echo "#!/bin/sh " >$askpass
    echo "case \$1 in " >>$askpass
    echo " Username*) echo 'UserName';; " >>$askpass
    echo " Password*) echo '"${pat}"' ;; " >>$askpass
    echo "esac " >>$askpass
    chmod 700 $askpass
fi

# Git only
if [ $rc -eq 0 ]; then
    fetchCmds=$(getWorkDirectory)/fetchCmds.sh
    echo $PGM": [INFO] ** Generate Fetch script to ${fetchCmds}"
    cmd="groovyz ${PIPELINE_SCRIPTS}/utilities/generateFetchStatements.groovy -w $(getWorkDirectory) -a ${applicationDescriptor} -o ${fetchCmds} -c ${applicationDependencyConfiguration}"
    echo $PGM": [INFO] ** cmd: ${cmd}"
    ${cmd}
    rc=$?
fi

if [ $rc -eq 0 ]; then
    echo $PGM": [INFO] ** Invoking fetch scripts"
    chmod 700 $fetchCmds
    echo $PGM": [INFO] ** Executing ${fetchCmds}"
    cmd="export GIT_ASKPASS=${askpass} && ${fetchCmds}"
    echo $PGM": [INFO] ** cmd: ${cmd}"
    export GIT_ASKPASS=${askpass} && ${fetchCmds}
    rc=$?
fi

if [ $rc -eq 0 ]; then
    ERRMSG=$PGM": [INFO] Fetch Dependencies Complete. rc="$rc
    echo $ERRMSG
fi

if [ -f "${askpass}" ]; then
 rm $askpass
fi 

exit $rc
