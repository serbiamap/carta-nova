# Carta Nova

**Carta Nova** is a lightweight, native Android application written in Kotlin designed to display completely offline maps. It allows users to import local map packages and interact with them seamlessly without requiring any internet connection.

You can find the working version at <a href="https://play.google.com/store/apps/details?id=net.serbiamap.cartanova" target="_blank" rel="noopener noreferrer">Carta Nova on Google Play</a>.


The app serves as a secure, offline container that renders web-based map solutions (like **LeafletJS**) locally on the device.

Majority of the code is written incrementally by ChatGPT and Gemini, therefore it looks ugly in it's initial release.
The aim was more about crisp and smooth display of vector map data, so the Kotlin code is just a wrapper around LeafletJS
functionality.

---

## Features

- **100% Offline Capability:** No internet permission or data connection required.
- **LeafletJS Support:** Smoothly renders interactive maps, including SVG vector graphics, static images, and map tiles.
- **ZIP Package Import:** Import custom maps directly through the native Android system file picker.
- **Isolated Storage:** Map packages are extracted and stored securely within the app's internal private storage.
- **Secure Web Loading:** Uses Android's `WebViewAssetLoader` to safely load local assets without exposing the device to common web vulnerabilities.
- **Path Traversal Protection:** Built-in security checks block malicious ZIP path traversal attacks during extraction.

---

## Map Package Structure

To import a map into Carta Nova, it must be compressed into a standard `.zip` file containing an entry point (`index.html`) and a metadata file (`map.json`) in the same root directory.

### Expected Layout:
```text
Check folder 'assets' for example

my-custom-map.zip
├── map.json
├── index.html
├── map-image.svg
├── css/
├── js/ (LeafletJS files)
└── tiles/  (.jpg, .png, etc. - optional)
```

### Example `map.json`
The application reads basic package information from this file. Both properties are required:
```json
{
	"title": "Novi Sad Street Map",
	"version": "1.0",
	"publisher": "Serbiamap.Net",
	"author": "Predrag Dukanac",
	"bounds": [45.20603, 19.7297044444444, 45.3139125, 19.8982938888889]
}
```

---

## How It Works (Under the Hood)

1. **Extraction & Sanitization:** When a `.zip` file is selected, the app validates the structural integrity and sanitizes the folder name based on the `title`.
2. **Storage Paths:** Extracted maps are organized in internal storage under:
   `maps/<sanitized_map_title>/<version>/`
3. **Rendering:** The app uses `WebViewAssetLoader` to map local private storage paths to a virtual domain. This allows JavaScript-heavy libraries like Leaflet to access local SVG files and tiles without triggering cross-origin (CORS) security blocks.

---

## Building and Running

### Prerequisites
- Android Studio (Ladybug or newer recommended)
- Android SDK 34+
- Gradle 8.0+

### Setup Instructions
1. Clone the repository to your local machine:
   ```bash
   git clone https://github.com.git
   ```
2. Open the project folder in **Android Studio**.
3. Allow Gradle to sync and download required dependencies.
4. Connect an Android device or launch an emulator.
5. Click **Run** (`Shift + F10`) to deploy version 1.0 to the device.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
