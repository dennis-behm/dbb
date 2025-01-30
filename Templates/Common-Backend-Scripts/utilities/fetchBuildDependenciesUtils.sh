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

fetchBuildDependencies() {

    # Read to go to fetch build dependencies configured in application descriptor
    if [ $rc -eq 0 ]; then
        echo $PGM": [INFO] **************************************************************"
        echo $PGM": [INFO] ** Start Fetch Build Dependencies on HOST/USER: ${SYS}/${USER}"
        echo $PGM": [INFO] **                     WorkDir:" $(getWorkDirectory)
        echo $PGM": [INFO] **                 Application:" ${application}
        echo $PGM": [INFO] **                      Branch:" ${branch}
        echo $PGM": [INFO] **     Application Descriptor :" ${applicationDescriptor}
        if [ ! -z "${externalDependenciesLog}" ]; then
            echo $PGM": [INFO] **    External Dependency Log :" ${externalDependenciesLog}
        fi
        echo $PGM": [INFO] **************************************************************"
        echo ""
    fi
    #

    # Create import dir
    if [ $rc -eq 0 ]; then
        if [ ! -d "$(getWorkDirectory)/imports" ]; then
            mkdir -p $(getWorkDirectory)/imports
        fi
    fi

    # Fetch Application Dependencies
    if [ $rc -eq 0 ]; then
        echo $PGM": [INFO] ** Fetch Application Dependencies from Artifact Repository"
        cmd="groovyz ${PIPELINE_SCRIPTS}/utilities/fetchBuildDependencies.groovy -w $(getWorkDirectory) -a ${applicationDescriptor} -p ${pipelineConfiguration} -b ${Branch}"
        #
        if [ ! -z "${artifactRepositoryUrl}" ]; then
            CMD="${CMD} --artifactRepositoryUrl \"${artifactRepositoryUrl}\""
        fi
        if [ ! -z "${artifactRepositoryUser}" ]; then
            CMD="${CMD} --artifactRepositoryUser ${artifactRepositoryUser}"
        fi
        if [ ! -z "${artifactRepositoryPassword}" ]; then
            CMD="${CMD} --artifactRepositoryPassword ${artifactRepositoryPassword}"
        fi
        if [ ! -z "${artifactRepositoryNamePattern}" ]; then
            CMD="${CMD} --artifactRepositoryNamePattern ${artifactRepositoryNamePattern}"
        fi
        if [ ! -z "${artifactRepositoryDirectory}" ]; then
            CMD="${CMD} --artifactRepositoryDirectory ${artifactRepositoryDirectory}"
        fi
        if [ ! -z "${externalDependenciesLog}" ]; then
            cmd="${cmd} -d ${externalDependenciesLog}"
        fi
        echo $PGM": [INFO] ** CMD : ${cmd}"
        ${cmd}
        rc=$?
    fi

    if [ $rc -eq 0 ]; then
        ERRMSG=$PGM": [INFO] Fetch Build Dependencies Completed. rc="$rc
        echo $ERRMSG
    fi

}
