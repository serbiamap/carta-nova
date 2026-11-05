# Carta Nova - Release Notes & Deployment Guide

## Version 1.1 (Build 3)

### What's New in Version 1.1

- **Smooth & Precise GPS Tracking**: Upgraded location engine to Google's **Fused Location Provider API**. 
- **Eliminated Marker Jumping**: Replaced raw, conflicting `GPS_PROVIDER` and `NETWORK_PROVIDER` updates with intelligent location fusion, eliminating sudden position jumps while driving or walking.
- **Improved Performance & Battery Efficiency**: Standardized high-accuracy location polling intervals on Android's main loop.

---

## Step-by-Step Instructions: Building & Uploading to Google Play

### 1. Build the Signed Android App Bundle (.aab) in Android Studio

1. In Android Studio, go to the top menu:  
   **Build ➔ Generate Signed Bundle / APK...**
2. Select **Android App Bundle** and click **Next**.
3. Choose your **Key store path** (the `.jks` or `.keystore` file used for Version 1.0):
   * Key store password: `[Your Password]`
   * Key alias: `[Your Alias]`
   * Key password: `[Your Alias Password]`
4. Click **Next**.
5. Select the **`release`** Build Variant.
6. Click **Create** (or **Finish**).
7. Android Studio will build the bundle. Once complete, a popup will appear at the bottom right with a **"locate"** link. Your generated `.aab` file will be at:
   `app/release/app-release.aab`

---

## 2. Uploading to Google Play Console

1. Open **[Google Play Console](https://play.google.com/console)** and log into your developer account.
2. Select **Carta Nova** from your app list.
3. In the left navigation menu:
   * Go to **Release ➔ Production** (or **Internal testing** / **Closed testing** if you want to test first).
4. Click **Create new release** near the top right.
5. In the **App bundles** section, click **Upload** and select `app-release.aab`.
6. Verify the release details:
   * **Version name:** `1.1`
   * **Version code:** `3`
7. Under **Release notes**, enter what's new for your users:

```text
What's new in v1.1:
• Enhanced GPS positioning accuracy for smoother tracking on maps.
• Resolved sudden location marker jumps while driving or walking.
• Performance and stability improvements.
```

8. Click **Save** ➔ **Review release** ➔ **Start rollout to Production**.
