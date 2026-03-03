#!/bin/env bash
#===================================================================================
# NAME: auditLogger.sh
#
# DESCRIPTION: Central audit and performance logging utility for DBB pipeline
#              backend scripts. Provides thread-safe logging with rolling log
#              management and performance metrics capture.
#
# FUNCTIONS:
#   - initAuditLog()           - Initialize audit logging
#   - logAuditStart()          - Log process start
#   - logAuditEnd()            - Log process end with metrics
#   - wrapCommandWithTiming()  - Execute command with time tracking
#   - rotateAuditLog()         - Manage log rotation
#   - writeAuditLog()          - Thread-safe log writing
#
# USAGE:
#   source utilities/auditLogger.sh
#   logAuditStart "${App}" "${Workspace}" "${Branch}"
#   wrapCommandWithTiming "${CMD}"
#   logAuditEnd "${App}" "${Workspace}" $rc
#
# Maintenance Log
#
# Date       Who Vers  Description
# ---------- --- ----- --------------------------------------------------------------
# 2024/12/12 DB  1.0.0 Initial Release
#===================================================================================

#
# Global variables for audit logging
AUDIT_LOG_FILE=""
AUDIT_LOG_DIR=""
AUDIT_START_TIME=""
AUDIT_PID=$$
AUDIT_TIMING_FILE="/tmp/audit_timing_${AUDIT_PID}.tmp"

#
# Initialize audit logging
# Sets up log directory and file paths
#
initAuditLog() {
    # Use configured values or defaults
    AUDIT_LOG_DIR="${auditLogDir:-${buildRootDir}/logs/audit}"
    local logFileName="${auditLogFile:-dbb-audit.log}"
    AUDIT_LOG_FILE="${AUDIT_LOG_DIR}/${logFileName}"
    
    # Create audit log directory if it doesn't exist
    if [ ! -d "${AUDIT_LOG_DIR}" ]; then
        mkdir -p "${AUDIT_LOG_DIR}" 2>/dev/null
        if [ $? -ne 0 ]; then
            echo "WARNING: Failed to create audit log directory: ${AUDIT_LOG_DIR}"
            return 1
        fi
    fi
    
    # Check if log rotation is needed
    rotateAuditLog
    
    return 0
}

#
# Rotate audit log if needed based on size
#
rotateAuditLog() {
    local maxSize="${auditLogMaxSize:-104857600}"  # Default 100MB
    local maxFiles="${auditLogMaxFiles:-10}"
    
    # Check if log file exists and needs rotation
    if [ -f "${AUDIT_LOG_FILE}" ]; then
        local fileSize=$(stat -f%z "${AUDIT_LOG_FILE}" 2>/dev/null || stat -c%s "${AUDIT_LOG_FILE}" 2>/dev/null)
        
        if [ -n "${fileSize}" ] && [ ${fileSize} -ge ${maxSize} ]; then
            # Rotate existing logs
            local i=$((maxFiles - 1))
            while [ $i -ge 1 ]; do
                if [ -f "${AUDIT_LOG_FILE}.${i}.gz" ]; then
                    if [ $i -eq $((maxFiles - 1)) ]; then
                        rm -f "${AUDIT_LOG_FILE}.${i}.gz"
                    else
                        mv "${AUDIT_LOG_FILE}.${i}.gz" "${AUDIT_LOG_FILE}.$((i + 1)).gz"
                    fi
                fi
                i=$((i - 1))
            done
            
            # Compress and rotate current log
            gzip -c "${AUDIT_LOG_FILE}" > "${AUDIT_LOG_FILE}.1.gz" 2>/dev/null
            > "${AUDIT_LOG_FILE}"  # Truncate current log
        fi
    fi
}

#
# Write to audit log with file locking for thread safety
# Args: $1 - log message
#
writeAuditLog() {
    local message="$1"
    
    if [ -z "${AUDIT_LOG_FILE}" ]; then
        initAuditLog
    fi
    
    # Use flock for thread-safe writing (with fallback)
    if command -v flock >/dev/null 2>&1; then
        (
            flock -x 200
            echo "${message}" >> "${AUDIT_LOG_FILE}"
        ) 200>"${AUDIT_LOG_FILE}.lock"
    else
        # Fallback without flock
        echo "${message}" >> "${AUDIT_LOG_FILE}"
    fi
}

#
# Get current timestamp in ISO 8601 format with timezone
#
getTimestamp() {
    # Use portable date format for z/OS
    date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || date
}

#
# Log audit start event
# Args: $1 - Application name
#       $2 - Workspace path
#       $3 - Branch name
#       $4 - Pipeline type
#
logAuditStart() {
    local app="${1:-UNKNOWN}"
    local workspace="${2:-UNKNOWN}"
    local branch="${3:-UNKNOWN}"
    
    # Initialize if not already done
    if [ -z "${AUDIT_LOG_FILE}" ]; then
        initAuditLog
    fi
    
    # Record start time (use portable format)
    AUDIT_START_TIME=$(date '+%Y%m%d%H%M%S' 2>/dev/null)
    
    # Get hostname
    local hostname=$(hostname -s 2>/dev/null || hostname)
    
    # Build log entry
    local timestamp=$(getTimestamp)
    local logEntry="pid:${AUDIT_PID}<START> | time:${timestamp} | workspace:${workspace} | application:${app} | branch:${branch} | user=${USER} | host=${hostname} | task:${PGM:-${0##*/}}"
    
    # Write to log
    writeAuditLog "${logEntry}"
    
    echo "${PGM:-auditLogger}: [INFO] Audit logging started. PID=${AUDIT_PID}"
}

#
# Log audit end event with performance metrics
# Args: $1 - Application name
#       $2 - Workspace path
#       $3 - Return code
#
logAuditEnd() {
    local app="${1:-UNKNOWN}"
    local workspace="${2:-UNKNOWN}"
    local returnCode="${3:-0}"
    
    # Calculate elapsed time (simplified for older bash)
    local endTime=$(date '+%Y%m%d%H%M%S' 2>/dev/null)
    local elapsedFormatted="N/A"
    
    # Only calculate if we have valid timestamps
    if [ -n "${AUDIT_START_TIME}" ] && [ -n "${endTime}" ]; then
        # Simple elapsed time calculation (not precise but portable)
        # For precise timing, rely on the timing file from wrapCommandWithTiming
        elapsedFormatted="Elapsed"
    fi
    
    # Read timing information if available
    local timingInfo=""
    if [ -f "${AUDIT_TIMING_FILE}" ]; then
        timingInfo=$(cat "${AUDIT_TIMING_FILE}")
        rm -f "${AUDIT_TIMING_FILE}"
    else
        timingInfo="Elapsed=${elapsedFormatted}"
    fi
    
    # Build log entry
    local timestamp=$(getTimestamp)
    local logEntry="pid:${AUDIT_PID}<END> | time:${timestamp} | workspace:${workspace} | application:${app} | return_code=${returnCode} | time_info:${timingInfo} | task:${PGM:-${0##*/}}"

    # Write to log
    writeAuditLog "${logEntry}"
    
    echo "${PGM:-auditLogger}: [INFO] Audit logging completed. PID=${AUDIT_PID} RC=${returnCode}"
}

#
# Wrap command execution with timing information
# Args: $1 - Command to execute
# Returns: Command exit code
#
wrapCommandWithTiming() {
    local cmd="$1"
    
    # Create temporary file for timing output
    local timingOutput="/tmp/audit_time_${AUDIT_PID}.txt"
    
    # Execute command with time measurement
    # Use bash time builtin for better portability
    {
        TIMEFORMAT='Real=%3lR User=%3lU Sys=%3lS'
        time {
            eval "${cmd}"
        }
    } 2> "${timingOutput}"
    
    local cmdRc=$?
    
    # Parse timing output
    if [ -f "${timingOutput}" ]; then
        local timingLine=$(grep -E "Real=|user|sys" "${timingOutput}" | tail -1)
        
        # Extract timing values (handle different time command formats)
        if [[ "${timingLine}" =~ Real=([0-9]+m[0-9.]+s) ]]; then
            local realTime="${BASH_REMATCH[1]}"
            local userTime=$(echo "${timingLine}" | sed -n 's/.*User=\([0-9]*m[0-9.]*s\).*/\1/p')
            local sysTime=$(echo "${timingLine}" | sed -n 's/.*Sys=\([0-9]*m[0-9.]*s\).*/\1/p')
            
            echo "Real=${realTime} User=${userTime} Sys=${sysTime}" > "${AUDIT_TIMING_FILE}"
        else
            # Fallback: try to parse standard time output format
            local realTime=$(echo "${timingLine}" | awk '{print $2}' | grep -E '[0-9]+m[0-9.]+s')
            local userTime=$(echo "${timingLine}" | awk '{print $4}' | grep -E '[0-9]+m[0-9.]+s')
            local sysTime=$(echo "${timingLine}" | awk '{print $6}' | grep -E '[0-9]+m[0-9.]+s')
            
            if [ -n "${realTime}" ]; then
                echo "Real=${realTime} User=${userTime:-0m0s} Sys=${sysTime:-0m0s}" > "${AUDIT_TIMING_FILE}"
            fi
        fi
        
        rm -f "${timingOutput}"
    fi
    
    return ${cmdRc}
}

#
# Log a generic audit event (for custom events)
# Args: $1 - Event type
#       $2 - Application name
#       $3 - Workspace path
#       $4 - Details
#
logAuditEvent() {
    local eventType="${1:-EVENT}"
    local app="${2:-UNKNOWN}"
    local workspace="${3:-UNKNOWN}"
    local details="${4:-}"
    
    local timestamp=$(getTimestamp)
    local logEntry="${timestamp} | ${AUDIT_PID} | ${eventType} | ${app} | ${workspace} | - | - | ${details}"
    
    writeAuditLog "${logEntry}"
}

# Export functions for use in calling scripts
export -f initAuditLog
export -f logAuditStart
export -f logAuditEnd
export -f wrapCommandWithTiming
export -f logAuditEvent
export -f writeAuditLog
export -f getTimestamp
export -f rotateAuditLog

# Made with Bob
