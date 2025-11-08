# Mod Loader for Android - Usage Guide

## Overview
This Android application automatically checks for mod library updates from GitHub and loads the latest version before launching the target game.

## Features
- **Automatic Update Checking**: Fetches the latest commit SHA from GitHub repository
- **Smart Downloading**: Downloads only when newer versions are available
- **Progress Tracking**: Shows real-time download progress with file size information
- **Overlay Permission**: Automatically requests necessary overlay permissions
- **Persistent Storage**: Saves commit information to avoid redundant downloads
- **Error Handling**: Graceful fallback to existing mods if updates fail

## Configuration Variables
Before using, configure these variables in `MainActivity.java`:

```java
private String LIBRARY_NAME = "libMagicMods.so";        // Your mod library filename
private String GITHUB_USER_NAME = "Retired-Gamer1";     // Your GitHub username
private String REPO_NAME = "libs";                      // Your repository name
private String GAME_ACTIVITY = "com.tencent.tmgp.cod.CODMainActivity"; // Target game activity
```

How It Works

1. Initial Setup

· The app requests overlay permission on first launch (Android 6.0+)
· Creates a blank layout while performing background operations

2. Update Check Process

1. Commit Verification: Fetches latest commit SHA from GitHub API
2. Comparison: Compares with locally stored commit SHA
3. Decision:
   · If newer version available → Shows update dialog and downloads
   · If up-to-date → Loads existing mod and launches game
   · If check fails → Falls back to existing mod

3. Download Process

· Shows progress dialog with percentage and file sizes
· Downloads library to app's internal storage
· Updates progress in real-time
· Creates library metadata after successful download

4. Game Launch

· Loads the mod library using System.load()
· Starts the target game activity
· Closes the loader app automatically

Setup Instructions

1. Repository Requirements

Your GitHub repository should have:

· The mod library file in the main branch
· Public access (for API calls)
· File path matching the LIBRARY_NAME variable

2. Android Manifest Requirements

Add these permissions to your AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

3. Game Activity

Ensure the GAME_ACTIVITY string matches the exact class name of your target game's main activity.

Error Handling

· Network Issues: Falls back to existing mod with toast notification
· Download Interruption: Deletes partial file and uses existing mod
· Game Activity Missing: Shows error toast and closes app
· Mod Loading Failure: Notifies user but still attempts to launch game

Storage Location

Mod libraries are stored in:

```
/data/data/your.package.name/files/libMagicMods.so
```

Customization

You can modify:

· UI Colors: Change color values in showUpdateDialog() method
· Timeouts: Adjust connection and read timeouts (currently 15 seconds)
· Buffer Size: Change download buffer size (currently 8KB)
· Progress Updates: Modify progress update frequency

Dependencies

· Android API 16+ (minimum)
· Internet permission
· Overlay permission (for Android 6.0+)
· GitHub repository with public access

Notes

· The app runs update checks on every launch
· Download can be cancelled by closing the dialog
· Commit SHA is stored in SharedPreferences for comparison
· Library metadata includes download timestamp and file size