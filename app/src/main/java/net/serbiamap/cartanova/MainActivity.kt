package net.serbiamap.cartanova

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.HandlerThread
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.graphics.Typeface
import android.view.View

import android.widget.ImageView
import androidx.activity.OnBackPressedCallback

import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AlertDialog

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.view.WindowManager
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class MapBridge {
    private var webView: WebView? = null

    fun attach(webView: WebView) {
        this.webView = webView
    }

    fun setGpsLocation(
        latitude: Double,
        longitude: Double
    ) {
        val js = """
        if (typeof window.setGpsLocation === 'function') {
            window.setGpsLocation($latitude, $longitude);
        }
        """.trimIndent()

        webView?.post {
            webView?.evaluateJavascript(js, null)
        }
    }

    fun detach() {
        webView = null
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var backCallback: OnBackPressedCallback
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var webView: WebView
    private lateinit var mapsRoot: File
    private var currentMapTitle: String = ""
    private var locationThread: HandlerThread? = null
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var latestLocation: Location? = null
    private var currentMapBounds: List<Double>? = null
    private var mapBridge: MapBridge? = null

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            startLocationAcquisition()
        }
    }

    private val openZip = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importMap(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mapsRoot = File(filesDir, "maps")
        if (!mapsRoot.exists()) mapsRoot.mkdirs()

        // Setup navigation handling before shifting layouts
        setupBackNavigation()

        showSplash()
        startLocationAcquisitionIfPermitted()
    }

    private fun setupBackNavigation() {
        // Start as FALSE because you begin on the Splash screen, not the MapView
        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                // This condition is now a safety check, but the state management guarantees it
                if (::webView.isInitialized && webView.parent != null) {
                    mapBridge?.detach()
                    mapBridge = null
                    currentMapBounds = null

                    // Go back to the selection list
                    showSelection()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)
    }

private fun showSplash() {
    val splash = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.WHITE)
    }

    // 1. Create the ImageView for your large splash logo
    val logoSize = dp(160) // Enforce a crisp, large size (e.g., 160dp)
    val logoImage = ImageView(this).apply {
        setImageResource(R.drawable.app_logo) // Loads from res/drawable/app_logo.png

        // Ensure the bounds match an exact square touch/view frame
        layoutParams = LinearLayout.LayoutParams(logoSize, logoSize).apply {
            // Add a clean bottom margin so it doesn't crowd the title text
            bottomMargin = dp(24)
        }
    }

    val title = TextView(this).apply {
        text = "Carta Nova"
        textSize = 34f
        setTextColor(Color.DKGRAY)
        gravity = Gravity.CENTER
    }

    // 2. Add the logo first so it renders right above the title text
    splash.addView(logoImage)
    splash.addView(title)

    setContentView(splash)

    Handler(Looper.getMainLooper()).postDelayed({ showSelection() }, 2000)
}

    private fun setupRoot(title: String, actionText: String = "⋮", action: () -> Unit) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.rgb(245, 245, 245))
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 20f
            setTextColor(Color.DKGRAY)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL
        }
        bar.addView(titleView, LinearLayout.LayoutParams(0, dp(48), 1f))

        val actionTargetSize = dp(44)
        val actionButton = Button(this).apply {
            // 1. Assign the text (passed via your variable) and force bold styling
            text = actionText
            textSize = 24f    // Slightly larger size makes the vertical ellipsis highly readable
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)

            // 2. Remove default background completely for transparency
            background = null

            // 3. Absolute center registration & strip font metrics padding
            gravity = Gravity.CENTER
            includeFontPadding = false

            // 4. Wipe out all implicit minimum constraints and padding limits
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(0, 0, 0, 0)

            // 5. Force layout bounds to maintain a strict square touch target
            layoutParams = android.view.ViewGroup.LayoutParams(actionTargetSize, actionTargetSize)

            setOnClickListener { action() }
        }
        bar.addView(actionButton)

        root.addView(bar)

        val border = View(this).apply {
            setBackgroundColor(Color.rgb(105, 105, 105)) // Dark gray color
        }
        root.addView(border, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun showSelection() {
        setupRoot("Maps") {
            showImportMenu()
        }

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        scroll.addView(list)
        content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val imported = loadImportedMaps()
        if (imported.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No imported maps yet.\n\nUse the Import button below to add a map package, or try demo map."
                textSize = 17f
                setTextColor(Color.DKGRAY)
                setPadding(dp(8), dp(16), dp(8), dp(16))
            }
            list.addView(empty)
        } else {
            imported.forEach { map ->
                addMapRow(list, map)
            }
        }

        // The original test map remains bundled in the APK.
        val bundled = TextView(this).apply {
            text = "Kraljevo (Serbia) Map\nDemo"
            textSize = 17f
            setTextColor(Color.DKGRAY)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(Color.rgb(248, 248, 248))
            val bundledMap = loadBundledMap()
            setOnClickListener { showMap(bundledMap, null, bundled = true) }
        }
        list.addView(bundled, LinearLayout.LayoutParams(-1, dp(72)).apply {
            topMargin = dp(12)
        })

        val importButton = Button(this).apply {
            text = "Import"
            setOnClickListener { chooseZip() }
        }
        content.addView(importButton, LinearLayout.LayoutParams(-1, dp(52)).apply {
            setMargins(dp(16), dp(8), dp(16), dp(16))
        })

        backCallback.isEnabled = false
    }

    private data class MapInfo(
        val title: String,
        val version: String,
        val author: String,
        val publisher: String,
        val bounds: List<Double>?,
        val directory: File?
    )

    private fun parseBounds(json: JSONObject): List<Double>? {
        val boundsArray = json.optJSONArray("bounds") ?: return null
        if (boundsArray.length() != 4) return null
        return (0 until 4).map { boundsArray.optDouble(it, Double.NaN) }
            .takeIf { values -> values.all { !it.isNaN() } }
    }

    private fun loadImportedMaps(): List<MapInfo> {
        val result = mutableListOf<MapInfo>()
        val titleDirs = mapsRoot.listFiles { f -> f.isDirectory } ?: return result
        for (titleDir in titleDirs) {
            val versions = titleDir.listFiles { f -> f.isDirectory } ?: continue
            for (versionDir in versions) {
                val metadata = File(versionDir, "map.json")
                val index = File(versionDir, "index.html")
                if (!metadata.isFile || !index.isFile) continue
                try {
                    val json = JSONObject(metadata.readText(Charsets.UTF_8))
                    val title = json.optString("title").trim()
                    val version = json.optString("version").trim()
                    if (title.isNotEmpty() && version.isNotEmpty()) {
                        val author = json.optString("author").trim()
                        val publisher = json.optString("publisher").trim()
                        val bounds = parseBounds(json)
                        result.add(MapInfo(title, version, author, publisher, bounds, versionDir))
                    }
                } catch (_: Exception) {
                    // Ignore malformed map packages in the list.
                }
            }
        }
        return result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }

    private fun addMapRow(list: LinearLayout, map: MapInfo) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(4), 0)
            setBackgroundColor(Color.rgb(248, 248, 248))
        }

        val mapText = TextView(this).apply {
            text = "${map.title}\nVersion ${map.version}"
            textSize = 17f
            setTextColor(Color.DKGRAY)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setGravity(Gravity.CENTER_VERTICAL)
            setOnClickListener { showMap(map, map.directory, false) }
        }
        row.addView(mapText, LinearLayout.LayoutParams(0, -1, 1f))

        val targetSize = dp(44)
        val menuButton = Button(this).apply {
            text = "…"
            textSize = 22f // Slightly increased size to make the transparent icon pop
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)

            // 2. Clear out default Android background to make it completely transparent
            background = null

            // 3. Absolute center registration & strip typography metrics padding
            gravity = Gravity.CENTER
            includeFontPadding = false

            // 4. Wipe out all implicit padding & minimum constraint locks
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(0, 0, 0, 0)

            // 5. Force layout bounds to maintain a strict square
            layoutParams = android.view.ViewGroup.LayoutParams(targetSize, targetSize)

            setOnClickListener { showMapContextMenu(this, map) }
        }
        row.addView(menuButton)

        row.setOnClickListener { showMap(map, map.directory, false) }

        list.addView(row, LinearLayout.LayoutParams(-1, dp(72)).apply {
            bottomMargin = dp(8)
        })
    }

    private fun showMapContextMenu(anchor: View, map: MapInfo) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Info")
        popup.menu.add("Delete")
        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "Info" -> showMapInfo(map)
                "Delete" -> confirmDeleteMap(map)
            }
            true
        }
        popup.show()
    }

    private fun showMapInfo(map: MapInfo) {
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(8))
        }

        fun addPair(label: String, value: String) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
            }
            val labelView = TextView(this@MainActivity).apply {
                text = label
                textSize = 16f
                setTextColor(Color.DKGRAY)
            }
            val valueView = TextView(this@MainActivity).apply {
                text = value
                textSize = 16f
                setTextColor(Color.DKGRAY)
                setPadding(dp(12), 0, 0, dp(10))
            }
            row.addView(labelView, LinearLayout.LayoutParams(dp(92), -2))
            row.addView(valueView, LinearLayout.LayoutParams(0, -2, 1f))
            info.addView(row)
        }

        addPair("Title", map.title)
        addPair("Version", map.version)
        if (map.author.isNotEmpty()) addPair("Author", map.author)
        if (map.publisher.isNotEmpty()) addPair("Publisher", map.publisher)
        map.bounds?.let { b ->
            val boundsText = "SW: %.6f, %.6f\nNE: %.6f, %.6f".format(
                java.util.Locale.US, b[0], b[1], b[2], b[3]
            )
            addPair("Bounds", boundsText)
        }

        android.app.AlertDialog.Builder(this)
            .setTitle(map.title)
            .setView(info)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun confirmDeleteMap(map: MapInfo) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete map")
            .setMessage("Are you sure you want to delete map ${map.title}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                if (map.directory != null && map.directory.exists()) {
                    map.directory.deleteRecursively()
                    map.directory.parentFile?.let { parent ->
                        if (parent.isDirectory && parent.listFiles()?.isEmpty() == true) {
                            parent.delete()
                        }
                    }
                }
                showSelection()
            }
            .show()
    }

    private fun startLocationAcquisitionIfPermitted() {
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            startLocationAcquisition()
        } else {
            requestLocationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startLocationAcquisition() {
        if (locationThread != null) return

        val thread = HandlerThread("VagabundoLocation").also { it.start() }
        locationThread = thread
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager = manager

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                latestLocation = location
                runOnUiThread {
                    updateGpsOnMap()
                }
            }
        }
        locationListener = listener

        try {
            val looper = thread.looper
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000L,
                    0f,
                    listener,
                    looper
                )
            }
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    10000L,
                    0f,
                    listener,
                    looper
                )
            }
        } catch (_: SecurityException) {
            stopLocationAcquisition()
        }
    }

    private fun stopLocationAcquisition() {
        val manager = locationManager
        val listener = locationListener
        if (manager != null && listener != null) {
            try {
                manager.removeUpdates(listener)
            } catch (_: SecurityException) {
            }
        }
        locationListener = null
        locationManager = null
        locationThread?.quitSafely()
        locationThread = null
    }

    private fun showImportMenu() {
        val options = arrayOf("Import map", "About", "Exit")
        android.app.AlertDialog.Builder(this)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> chooseZip()
                    1 -> {
                        dialog.dismiss()
                        showAboutDialog()
                    }
                    2 -> {
                        dialog.dismiss()
                        moveTaskToBack(true)
                    }
                }
            }
            .show()
    }

    private fun showAboutDialog() {
        val aboutMessage = """
        <h3><b>Carta Nova</b></h3>
        <p>Version 1.0<br/>
        Offline Mapping Application</p>
        
        <p><b>Author:</b> Predrag Dukanac<br/>
        <b>Publisher:</b> Serbiamap.Net</p>
        
        <hr/>
        
        <p>This application heavily depends on and is made possible by the incredible open-source mapping project:</p>
        <p>• <a href="https://leafletjs.com"><b>LeafletJS</b></a> — an open-source JavaScript library for mobile-friendly interactive maps.</p>
        
        <p style="font-size: 11sp; color: #64748b;">Licensed under the MIT License.</p>
    """.trimIndent()

        // 2. Build a modern Material alert dialog container
        val builder = AlertDialog.Builder(this)
            .setTitle("About Application")
            .setMessage(Html.fromHtml(aboutMessage, Html.FROM_HTML_MODE_LEGACY))
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.show()

        // 3. CRITICAL CRADLE: Enable clickable links inside the alert text scope
        val messageTextView = alertDialog.findViewById<TextView>(android.R.id.message)
        messageTextView?.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun chooseZip() {
        openZip.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
    }

    private fun importMap(uri: Uri) {
        val temp = File(cacheDir, "map_import_${System.currentTimeMillis()}")
        try {
            temp.mkdirs()
            contentResolver.openInputStream(uri)?.use { input ->
                unzipSafely(input, temp)
            } ?: throw Exception("Unable to open selected file")

            val metadataFile = findFile(temp, "map.json") ?: throw Exception("map.json was not found")
            val packageRoot = metadataFile.parentFile ?: throw Exception("Invalid package")
            val indexFile = File(packageRoot, "index.html")
            if (!indexFile.isFile) throw Exception("index.html must be in the same folder as map.json")

            val json = JSONObject(metadataFile.readText(Charsets.UTF_8))
            val title = json.optString("title").trim()
            val version = json.optString("version").trim()
            val author = json.optString("author").trim()
            val publisher = json.optString("publisher").trim()
            if (title.isEmpty()) throw Exception("map.json: title is missing")
            if (version.isEmpty()) throw Exception("map.json: version is missing")

            val safeTitle = sanitizePathPart(title)
            val safeVersion = sanitizePathPart(version)
            val destination = File(File(mapsRoot, safeTitle), safeVersion)
            if (destination.exists()) throw Exception("Map version is already imported")

            destination.parentFile?.mkdirs()
            copyDirectory(packageRoot, destination)

            Toast.makeText(this, "Imported: $title ($version)", Toast.LENGTH_SHORT).show()
            val map = MapInfo(title, version, author, publisher, parseBounds(json), null)
            showMap(map , destination, false)
        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            temp.deleteRecursively()
        }
    }

    private fun unzipSafely(input: java.io.InputStream, destination: File) {
        ZipInputStream(input).use { zip ->
            val buffer = ByteArray(64 * 1024)
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = entry.name.replace('\\', '/')
                if (name.startsWith("/") || name.split('/').any { it == ".." }) {
                    throw SecurityException("Unsafe ZIP entry")
                }
                val out = File(destination, name)
                val canonicalDestination = destination.canonicalFile
                val canonicalOut = out.canonicalFile
                if (!canonicalOut.path.startsWith(canonicalDestination.path + File.separator)) {
                    throw SecurityException("Unsafe ZIP entry")
                }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { output ->
                        var count = zip.read(buffer)
                        while (count != -1) {
                            output.write(buffer, 0, count)
                            count = zip.read(buffer)
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun findFile(root: File, fileName: String): File? {
        val children = root.listFiles() ?: return null
        for (child in children) {
            if (child.isFile && child.name.equals(fileName, ignoreCase = true)) return child
            if (child.isDirectory) {
                val result = findFile(child, fileName)
                if (result != null) return result
            }
        }
        return null
    }

    private fun copyDirectory(source: File, destination: File) {
        destination.mkdirs()
        source.listFiles()?.forEach { child ->
            val target = File(destination, child.name)
            if (child.isDirectory) copyDirectory(child, target)
            else FileInputStream(child).use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
        }
    }

    private fun sanitizePathPart(value: String): String {
        var s = value.trim().replace(Regex("[^A-Za-z0-9._-]+"), "_")
        s = s.trim('.', ' ')
        if (s.isEmpty()) s = "map"
        return s.take(80)
    }

    private fun loadBundledBounds(): List<Double>? {
        return try {
            val json = JSONObject(assets.open("kraljevo/map.json").bufferedReader().use { it.readText() })
            val a = json.optJSONArray("bounds") ?: return null
            if (a.length() != 4) return null
            (0 until 4).map { a.getDouble(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadBundledMap(): MapInfo? {
        return try {
            val json = JSONObject(assets.open("kraljevo/map.json").bufferedReader().use { it.readText() })
            val title = json.optString("title").trim()
            val version = json.optString("version").trim()
            if (title.isEmpty()) throw Exception("map.json: title is missing")
            if (version.isEmpty()) throw Exception("map.json: version is missing")

            val safeTitle = sanitizePathPart(title)
            val safeVersion = sanitizePathPart(version)

            val author = json.optString("author").trim()
            val publisher = json.optString("publisher").trim()
            val bounds = parseBounds(json)
            return MapInfo(title, version, author, publisher, bounds, null)
        } catch (_: Exception) {
            null
        }
    }

    private fun updateGpsOnMap() {
        val location = latestLocation ?: return
        if (!::webView.isInitialized || webView.parent == null) return
        mapBridge?.setGpsLocation(
            location.latitude,
            location.longitude
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showMap(map: MapInfo?,  directory: File? = null, bundled: Boolean) {
        if (map == null) return

        currentMapTitle = map.title

        // Fallback bounds assignment for bundled Kraljevo map matching your json assets
        currentMapBounds = map.bounds

        setupRoot(map.title) {
            val mapOptions = arrayOf("Info", "About", "Close Map")

            android.app.AlertDialog.Builder(this@MainActivity)
                .setItems(mapOptions) { dialog, which ->
                    when (which) {
                        0 -> {
                            dialog.dismiss()
                            showMapInfo(map)
                        }
                        1 -> {
                            dialog.dismiss()
                            showAboutDialog()
                        }
                        2 -> {
                            dialog.dismiss()
                            mapBridge?.detach()
                            mapBridge = null
                            currentMapBounds = null
                            showSelection()
                        }
                    }
                }
                .show()
        }

        webView = WebView(this)
        mapBridge = MapBridge()
        val assetLoaderBuilder = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
        if (directory != null) {
            // The handler is rooted at the common maps directory, so the URL can
            // address the selected map by its sanitized title/version path.
            assetLoaderBuilder.addPathHandler("/maps/", WebViewAssetLoader.InternalStoragePathHandler(this, mapsRoot))
        }
        val assetLoader = assetLoaderBuilder.build()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            builtInZoomControls = false
            displayZoomControls = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                mapBridge?.attach(view)
                updateGpsOnMap()
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)

            @Suppress("DEPRECATION")
            override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(Uri.parse(url))
        }

        content.addView(webView, LinearLayout.LayoutParams(-1, -1))

        val url = if (bundled) {
            "https://appassets.androidplatform.net/assets/kraljevo/index.html"
        } else {
            val rel = directory!!.relativeTo(mapsRoot).path.replace(File.separatorChar, '/')
            "https://appassets.androidplatform.net/maps/$rel/index.html"
        }
        webView.loadUrl(url)
        backCallback.isEnabled = true
    }

    override fun onDestroy() {
        stopLocationAcquisition()
        mapBridge?.detach()
        mapBridge = null
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
