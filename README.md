# Calendar Notification Status

[![GitHub Release](https://img.shields.io/github/v/release/gitanO0/CalendarNotificationStatus?style=for-the-badge&color=success)](https://github.com/gitanO0/CalendarNotificationStatus/releases)
[![Build Status](https://img.shields.io/github/actions/workflow/status/gitanO0/CalendarNotificationStatus/release.yml?style=for-the-badge)](https://github.com/gitanO0/CalendarNotificationStatus/actions)

A lightweight, fully native Android application that aggregates your upcoming calendar events and pins them to your notification drawer for quick and easy access.

<img src="assets/icon.png" width="100" alt="App Icon">

## Features
* **Status Bar Date Icon:** Dynamically renders today's date into a clean, stylized box directly in your Android status bar.
* **Persistent Notification:** Keeps your schedule accessible via a sticky, high-priority ongoing notification.
* **Smart Event Aggregation:** Queries the native Android `CalendarProvider` to display up to 90 days of upcoming events.
* **Custom Calendar Selection:** Automatically detects available calendars (shared, subscribed, or owned) and lets you choose exactly which ones to display.
* **Deep Linking:** Tap any specific event in the notification to instantly open its details page in the official Google Calendar app.
* **Smart Actions:** Automatically generates "Join Call" buttons for Zoom, Google Meet, Teams, and Webex links, and "Map" buttons for physical locations directly inside the notification.
* **Happening Now Pulse:** Events blink to grab your attention when they are starting within 10 minutes or currently active.
* **Decluttered UI:** Intelligently identifies and filters out annoying Google Calendar "Working Location" pseudo-events (e.g., "Home", "Office"), features day separators, and applies high-visibility pastel color coding.
* **Customization:** Toggle the visibility of All-Day events and choose which meeting platforms generate smart join links.
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