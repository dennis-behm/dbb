# Audit and Performance Logging

## Overview

The audit logging utility provides centralized audit trails and performance metrics for all DBB pipeline backend scripts. It captures start/end events with detailed timing information in a rolling log format.

## Features

- **Centralized Logging**: Single audit log for all backend script executions
- **Performance Metrics**: Captures Real, User, and System CPU time using bash `time` command
- **Rolling Logs**: Automatic log rotation based on file size
- **Thread-Safe**: Uses file locking for concurrent script executions
- **Structured Format**: Easy-to-parse log format with pipe-delimited fields
- **Zero Overhead**: No performance impact when disabled

## Configuration

### Enable Audit Logging

Edit `pipelineBackend.config`:

```bash
# Enable audit logging
auditLogEnabled=true

# Configure log directory (relative to buildRootDir or absolute path)
auditLogDir="${buildRootDir}/logs/audit"

# Log file name
auditLogFile="dbb-audit.log"

# Maximum size per log file (100MB default)
auditLogMaxSize=104857600

# Maximum number of rotated files to keep
auditLogMaxFiles=10
```

### Disable Audit Logging

```bash
auditLogEnabled=false
```

## Log Format

```
TIMESTAMP | PID | EVENT | APPLICATION | WORKSPACE | BRANCH | TYPE | DETAILS
```

### Field Descriptions

| Field | Description | Example |
|-------|-------------|---------|
| TIMESTAMP | ISO 8601 timestamp with timezone | `2024-12-12T10:15:23.456+0100` |
| PID | Process ID | `12345` |
| EVENT | Event type (START/END) | `START` or `END` |
| APPLICATION | Application name | `MortgageApplication` |
| WORKSPACE | Workspace path | `main/build-1` |
| BRANCH | Git branch name | `main` |
| TYPE | Pipeline type | `build`, `release`, `preview` |
| DETAILS | Additional information | `User=DBEHM Host=ZSYS1` |

### Example Log Entries

**Start Event:**
```
2024-12-12T10:15:23.456+0100 | 12345 | START | MortgageApp | main/build-1 | main | build | User=DBEHM Host=ZSYS1 Script=zBuilder.sh
```

**End Event:**
```
2024-12-12T10:18:45.789+0100 | 12345 | END | MortgageApp | main/build-1 | - | - | RC=0 Real=3m22.567s User=2m15.234s Sys=0m8.123s
```

## Performance Metrics

The audit logger captures the following timing metrics using the bash `time` command:

- **Real Time**: Wall clock time (elapsed time)
- **User CPU Time**: CPU time spent in user mode
- **System CPU Time**: CPU time spent in kernel mode

These metrics are captured automatically when wrapping command execution with `wrapCommandWithTiming()`.

## Log Rotation

### Automatic Rotation

Logs are automatically rotated when the current log file exceeds `auditLogMaxSize`:

1. Current log is compressed: `dbb-audit.log` → `dbb-audit.log.1.gz`
2. Existing rotated logs are shifted: `.1.gz` → `.2.gz`, `.2.gz` → `.3.gz`, etc.
3. Oldest log is deleted when exceeding `auditLogMaxFiles`
4. New entries continue in the truncated current log

### Rotation Example

```
dbb-audit.log              # Current active log (50MB)
dbb-audit.log.1.gz         # Most recent rotated (100MB compressed)
dbb-audit.log.2.gz         # Second most recent (100MB compressed)
...
dbb-audit.log.10.gz        # Oldest (deleted when new rotation occurs)
```

## Integration Guide

### For Existing Scripts

To add audit logging to any backend script, follow these three steps:

#### Step 1: Source the Audit Logger (after sourcing config)

```bash
# Initialize audit logging if enabled
auditLoggerUtilities="${SCRIPT_HOME}/utilities/auditLogger.sh"
if [ "${auditLogEnabled}" = "true" ] && [ -f "${auditLoggerUtilities}" ]; then
    source $auditLoggerUtilities
fi
```

#### Step 2: Log Start Event (after validating options)

```bash
# Log audit start after options are validated
if [ $rc -eq 0 ] && [ "${auditLogEnabled}" = "true" ]; then
    logAuditStart "${App}" "${Workspace}" "${Branch}"
fi
```

#### Step 3: Wrap Main Command with Timing

```bash
# Execute with performance timing if audit logging is enabled
if [ "${auditLogEnabled}" = "true" ]; then
    wrapCommandWithTiming "${CMD}"
    rc=$?
else
    ${CMD}
    rc=$?
fi
```

#### Step 4: Log End Event (before exit)

```bash
# Log audit end with metrics before exit
if [ "${auditLogEnabled}" = "true" ]; then
    logAuditEnd "${App}" "${Workspace}" $rc
fi

exit $rc
```

## Scripts with Audit Logging

The following scripts have been integrated with audit logging:

- ✅ `zBuilder.sh` - DBB zBuilder build framework
- ✅ `gitClone.sh` - Git clone operations
- ✅ `packageBuildOutputs.sh` - Package build outputs
- ✅ `wazideploy-generate.sh` - Wazi Deploy deployment plan generation
- ✅ `wazideploy-deploy.sh` - Wazi Deploy deployment execution
- ✅ `wazideploy-evidence.sh` - Wazi Deploy evidence reporting
- ⏳ `dbbBuild.sh` - DBB build (pending)
- ⏳ `ucdDeploy.sh` - UCD deployment (pending)
- ⏳ `ucdPackaging.sh` - UCD packaging (pending)

## API Reference

### Functions

#### `initAuditLog()`
Initialize audit logging system. Creates log directory and checks for rotation.

**Returns:** 0 on success, 1 on failure

#### `logAuditStart(app, workspace, branch, type)`
Log a process start event.

**Parameters:**
- `app` - Application name
- `workspace` - Workspace path
- `branch` - Git branch name
- `type` - Pipeline type (build/release/preview)

**Returns:** 0 on success

#### `logAuditEnd(app, workspace, rc)`
Log a process end event with performance metrics.

**Parameters:**
- `app` - Application name
- `workspace` - Workspace path
- `rc` - Return code from the process

**Returns:** 0 on success

#### `wrapCommandWithTiming(command)`
Execute a command and capture timing metrics.

**Parameters:**
- `command` - Command string to execute

**Returns:** Exit code from the command

#### `logAuditEvent(eventType, app, workspace, details)`
Log a custom audit event.

**Parameters:**
- `eventType` - Event type identifier
- `app` - Application name
- `workspace` - Workspace path
- `details` - Additional details string

**Returns:** 0 on success

## Testing

A test script is provided to verify the audit logging functionality:

```bash
cd Templates/Common-Backend-Scripts
chmod +x test-audit-logging.sh
./test-audit-logging.sh
```

The test script validates:
1. Audit log initialization
2. Start event logging
3. Command execution with timing
4. End event logging
5. Log file creation and content
6. Log rotation functionality

## Troubleshooting

### Audit Log Not Created

**Problem:** No audit log file is created.

**Solutions:**
1. Verify `auditLogEnabled=true` in `pipelineBackend.config`
2. Check that `auditLogDir` is writable
3. Ensure `utilities/auditLogger.sh` exists and is sourced

### No Timing Information

**Problem:** End events don't show timing metrics.

**Solutions:**
1. Verify the command is wrapped with `wrapCommandWithTiming()`
2. Check that bash `time` builtin is available
3. Ensure `/tmp` directory is writable for timing files

### Log Rotation Not Working

**Problem:** Log file grows beyond `auditLogMaxSize`.

**Solutions:**
1. Verify `gzip` command is available
2. Check write permissions in `auditLogDir`
3. Ensure sufficient disk space for compressed logs

### Permission Denied Errors

**Problem:** Cannot write to audit log.

**Solutions:**
1. Check directory permissions: `chmod 755 ${auditLogDir}`
2. Verify file permissions: `chmod 644 ${auditLogFile}`
3. Ensure the pipeline user has write access

## Performance Impact

The audit logging system is designed for minimal performance overhead:

- **When Disabled**: Zero overhead (no code execution)
- **When Enabled**: ~1-2% overhead for:
  - Timestamp generation
  - File I/O operations
  - Time command execution

The performance impact is negligible compared to the actual build operations.

## Security Considerations

1. **File Permissions**: Audit logs may contain sensitive information (usernames, paths). Protect with appropriate permissions:
   ```bash
   chmod 750 ${auditLogDir}
   chmod 640 ${auditLogFile}
   ```

2. **Log Retention**: Configure `auditLogMaxFiles` based on compliance requirements

3. **Sensitive Data**: The logger does NOT capture:
   - Passwords or credentials
   - Command arguments (only command name)
   - File contents

## Future Enhancements

Potential future improvements:

- [ ] Daily log rotation in addition to size-based
- [ ] Syslog integration for centralized logging
- [ ] JSON output format option
- [ ] Memory usage tracking
- [ ] Network I/O metrics
- [ ] Integration with monitoring systems (Prometheus, Grafana)

## Support

For issues or questions:
1. Check this README
2. Review the test script output
3. Examine the audit log for error messages
4. Contact the DBB pipeline team

---

**Version:** 1.0.0  
**Last Updated:** 2024-12-12  
**Maintainer:** DBB Pipeline Team