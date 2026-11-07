# AMURIA Data Analyzer

Android local-file analysis app for authorized testing and debugging.

## Included
- Import classic Ethernet PCAP files and summarize IPv4 TCP/UDP packet metadata.
- Format JSON, decode Base64, inspect hexadecimal, and decode basic Protocol Buffers wire fields.
- File size and parser bounds are limited for safer local analysis.

## Build

```bash
gradle --no-daemon assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions
Push to `main` or run **Actions → Android APK → Run workflow**. Download the APK from the completed run's **Artifacts** section.

## Scope and limitations
This first Android build analyzes user-selected local files. It does not perform live device-wide capture, decrypt TLS, bypass certificate pinning or platform integrity, extract credentials, replay arbitrary requests, or alter remote game state. PCAP support currently covers classic PCAP with Ethernet link type and IPv4 packet metadata; PCAPNG, IPv6, and deep TCP stream reassembly are not implemented.
