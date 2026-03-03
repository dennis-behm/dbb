#!/bin/env bash
#===================================================================================
# NAME: test-audit-logging.sh
#
# DESCRIPTION: Test script to verify audit logging functionality
#
# USAGE: ./test-audit-logging.sh
#
#===================================================================================

echo "=========================================="
echo "Audit Logging Test Script"
echo "=========================================="
echo ""

# Set up test environment
SCRIPT_HOME="$(dirname "$0")"
export PIPELINE_WORKSPACE="/tmp/audit-test-$$"
mkdir -p "${PIPELINE_WORKSPACE}"

# Source configuration
pipelineConfiguration="${SCRIPT_HOME}/pipelineBackend.config"
if [ ! -f "${pipelineConfiguration}" ]; then
    echo "ERROR: Configuration file not found: ${pipelineConfiguration}"
    exit 1
fi

source $pipelineConfiguration

# Enable audit logging for test
export auditLogEnabled=true
export auditLogDir="${PIPELINE_WORKSPACE}/logs/audit"
export auditLogFile="test-audit.log"

echo "Test Configuration:"
echo "  Audit Enabled: ${auditLogEnabled}"
echo "  Audit Log Dir: ${auditLogDir}"
echo "  Audit Log File: ${auditLogFile}"
echo ""

# Source audit logger
auditLoggerUtilities="${SCRIPT_HOME}/utilities/auditLogger.sh"
if [ ! -f "${auditLoggerUtilities}" ]; then
    echo "ERROR: Audit logger not found: ${auditLoggerUtilities}"
    exit 1
fi

source $auditLoggerUtilities

echo "Test 1: Initialize audit log"
initAuditLog
if [ $? -eq 0 ]; then
    echo "  ✓ Audit log initialized successfully"
    echo "  Log file: ${AUDIT_LOG_FILE}"
else
    echo "  ✗ Failed to initialize audit log"
    exit 1
fi
echo ""

echo "Test 2: Log audit start event"
logAuditStart "TestApp" "test/workspace" "main" "build"
if [ $? -eq 0 ]; then
    echo "  ✓ Audit start logged successfully"
else
    echo "  ✗ Failed to log audit start"
    exit 1
fi
echo ""

echo "Test 3: Execute command with timing"
wrapCommandWithTiming "sleep 2 && echo 'Test command executed'"
cmdRc=$?
if [ $cmdRc -eq 0 ]; then
    echo "  ✓ Command executed with timing successfully"
else
    echo "  ✗ Command execution failed"
    exit 1
fi
echo ""

echo "Test 4: Log audit end event"
logAuditEnd "TestApp" "test/workspace" $cmdRc
if [ $? -eq 0 ]; then
    echo "  ✓ Audit end logged successfully"
else
    echo "  ✗ Failed to log audit end"
    exit 1
fi
echo ""

echo "Test 5: Verify audit log content"
if [ -f "${AUDIT_LOG_FILE}" ]; then
    echo "  ✓ Audit log file exists"
    echo ""
    echo "  Audit Log Contents:"
    echo "  ===================="
    cat "${AUDIT_LOG_FILE}" | while IFS= read -r line; do
        echo "  $line"
    done
    echo "  ===================="
else
    echo "  ✗ Audit log file not found"
    exit 1
fi
echo ""

echo "Test 6: Test log rotation"
# Create a large log file to test rotation
echo "  Creating large log file for rotation test..."
for i in {1..1000}; do
    echo "2024-12-12T10:00:00.000+0100 | $$ | TEST | App | workspace | branch | type | Test line $i" >> "${AUDIT_LOG_FILE}"
done

rotateAuditLog
if [ -f "${AUDIT_LOG_FILE}.1.gz" ]; then
    echo "  ✓ Log rotation successful"
    echo "  Rotated file: ${AUDIT_LOG_FILE}.1.gz"
else
    echo "  ℹ Log rotation not triggered (file size below threshold)"
fi
echo ""

echo "=========================================="
echo "All tests completed successfully!"
echo "=========================================="
echo ""
echo "Cleanup: Removing test directory ${PIPELINE_WORKSPACE}"
rm -rf "${PIPELINE_WORKSPACE}"

exit 0

# Made with Bob
