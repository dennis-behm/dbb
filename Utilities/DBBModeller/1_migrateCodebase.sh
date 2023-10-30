#!/bin/env bash
# Script to migrate the entire codebase for the componentization process
# from a datasets to the codebase directory.

# exports
export DBB_HOME=/usr/lpp/dbb/v2r0
export DBB_CONF=/usr/lpp/dbb/v2r0/conf

#################################
# configuration
#################################

# hlq from where to pick up files
hlq=DBEHM.MIG

# path for storing all files
SCRIPT_HOME="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
PARENT_DIR="$(dirname "$SCRIPT_HOME")"
codebaseDir="${PARENT_DIR}/work_codebase"

if [ ! -d "$codebaseDir" ]; then
    mkdir $codebaseDir
fi
#################################

# Migrate all COBOL SRC
$DBB_HOME/migration/bin/migrate.sh -o $codebaseDir/migrated-cobol-mapping.txt -l $codebaseDir/cobol.log -np info -r $codebaseDir -m MappingRule[hlq:$hlq,extension:cbl,toLower:true,pdsEncoding:IBM-037] COBOL

# Migrate all Cobol Copybooks
$DBB_HOME/migration/bin/migrate.sh -o $codebaseDir/migrated-copybook-mapping.txt -l $codebaseDir/copybook.log -np info -r $codebaseDir -m MappingRule[hlq:$hlq,extension:cpy,toLower:true,pdsEncoding:IBM-037] COPY

# Add any additional library
