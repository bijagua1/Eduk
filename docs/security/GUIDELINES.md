# Security & Privacy Guidelines

As a parental control application for the US market, Eduk adheres to the highest standards of security and privacy, particularly regarding **COPPA** compliance.

## Data Protection
- **PIN Hashing**: Parent PINs are never stored in plain text. We use SHA-256 with a unique salt per device.
- **Local First**: All student responses and progress are stored locally in the Room database. Only aggregated stats are sent to the manager's account.
- **Encryption**: Any communication with the AI Vision API or the Parent Reporting backend is performed over TLS 1.3.

## Tamper Resistance
Eduk is designed to assume the student will attempt to bypass restrictions:
- **Service Monitoring**: The app detects if the Accessibility Service or Device Admin permission is revoked and immediately notifies the manager via the reporting system.
- **Boot Persistence**: The `AppMonitoringService` restarts automatically upon device reboot.
- **Clock Sync**: To prevent "time travel" hacks (changing device date/time), Eduk syncs with a secure NTP server for all time-based logic.

## Legitimate Control
Eduk is a **Parental Control** app, not malware. 
- It provides a clear way for the parent (manager) to disable all restrictions using the authenticated PIN.
- It does not use exploits or undocumented Android vulnerabilities.
- It respects the user's ability to factory reset the device if necessary.
