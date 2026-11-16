package com.xivox.player

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xivox.player.ui.navigation.XivoxAppRoot
import com.xivox.player.ui.state.XivoxMainViewModel
import com.xivox.player.ui.theme.XivoxTheme

class MainActivity : ComponentActivity() {

    private val mainViewModel: XivoxMainViewModel by viewModels()

    private var incomingExternalUri by mutableStateOf<Uri?>(null)
    private var isInPipMode by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        mainViewModel.setStoragePermissionGranted(granted)
    }

    private val safFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            takePersistableReadPermission(uri)
            incomingExternalUri = uri
        }
    }

    private val safFolderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            takePersistableReadPermission(treeUri)
            mainViewModel.addSafFolder(treeUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
        )

        handleIncomingIntent(intent)
        checkAndRequestStoragePermission(requestIfMissing = true)

        setContent {
            val settings by mainViewModel.settingsFlow.collectAsStateWithLifecycle()

            XivoxTheme(
                themeMode = settings.themeMode,
                accentTheme = settings.accentTheme
            ) {
                XivoxAppRoot(
                    mainViewModel = mainViewModel,
                    incomingExternalUri = incomingExternalUri,
                    onClearIncomingUri = { incomingExternalUri = null },
                    isInPictureInPictureMode = isInPipMode,
                    onRequestStoragePermission = { checkAndRequestStoragePermission(requestIfMissing = true) },
                    onPickSafFile = {
                        safFilePickerLauncher.launch(arrayOf("video/*", "application/octet-stream"))
                    },
                    onPickSafFolder = {
                        safFolderPickerLauncher.launch(null)
                    },
                    onEnterPip = ::enterPictureInPictureIfSupported
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestStoragePermission(requestIfMissing = false)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    private fun enterPictureInPictureIfSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                incomingExternalUri = uri
            }
        }
    }

    private fun requiredStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun checkAndRequestStoragePermission(requestIfMissing: Boolean) {
        val perm = requiredStoragePermission()
        val granted = ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        mainViewModel.setStoragePermissionGranted(granted)
        if (!granted && requestIfMissing) {
            permissionLauncher.launch(perm)
        }
    }

    private fun takePersistableReadPermission(uri: Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
}
