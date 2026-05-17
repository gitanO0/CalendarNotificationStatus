# Calendar Notification Status

[![GitHub Release](https://img.shields.io/github/v/release/gitanO0/CalendarNotificationStatus?style=for-the-badge&color=success)](https://github.com/gitanO0/CalendarNotificationStatus/releases)
[![Build Status](https://img.shields.io/github/actions/workflow/status/gitanO0/CalendarNotificationStatus/release.yml?style=for-the-badge)](https://github.com/gitanO0/CalendarNotificationStatus/actions)

A lightweight, fully native Android application that aggregates your upcoming calendar events and pins them to your notification drawer for quick and easy access.

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="100" alt="App Icon">

## Features
* **Status Bar Date Icon:** Dynamically renders today's date into a clean, stylized box directly in your Android status bar.
* **Smart Event Aggregation:** Queries the native Android `CalendarProvider` to display up to 90 days of upcoming events.
* **Shared Calendar Support:** Automatically detects and displays events from shared, subscribed, or unowned calendars.
* **Deep Linking:** Tap any specific event in the notification to instantly open its details page in the official Google Calendar app.
* **Decluttered UI:** Intelligently identifies and filters out annoying Google Calendar "Working Location" pseudo-events (e.g., "Home", "Office").
* **Start on Boot:** Automatically registers and spins up the notification the moment you turn your phone on.

## Download
You can download the latest compiled APK directly from the [Releases](https://github.com/gitanO0/CalendarNotificationStatus/releases) page.

## Building from Source
This project uses standard Android Gradle tools.

```bash
git clone https://github.com/gitanO0/CalendarNotificationStatus.git
cd CalendarNotificationStatus
./gradlew assembleDebug
```