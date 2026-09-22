# Vagabundo 0.2

Vagabundo is a small native Android WebView application for displaying offline SerbiaMap-style map packages.

## 0.2 features

- 2-second simple `Vagabundo` splash screen.
- Map selection/import screen.
- Built-in Novi Sad test map remains available.
- Import ZIP map packages through Android's file picker.
- Package metadata comes from `map.json` with required `title` and `version` properties.
- Imported maps are stored under the app's private storage as:

  `maps/<sanitized_map_title>/<version>/`

- The package must contain `map.json` and `index.html` in the same directory. Other files/directories are preserved.
- ZIP path traversal is rejected.
- Imported maps are displayed directly from private storage through `WebViewAssetLoader`.
- Simple title bar on selection and map pages.
- Back/overflow actions return to map selection.

## Example map.json

```json
{
  "title": "Novi Sad",
  "version": "1.0"
}
```

## Build

Open this folder in Android Studio, allow Gradle sync, and run on the connected Android device.
