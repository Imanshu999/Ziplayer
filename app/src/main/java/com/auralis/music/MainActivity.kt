package com.auralis.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

data class Track(val id: Long, val title: String, val artist: String, val album: String, val uri: String)

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    private var tracks by mutableStateOf<List<Track>>(emptyList())
    private var permissionGranted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (granted) loadLocalTracks()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        permissionGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted) loadLocalTracks() else permissionLauncher.launch(permission)
        setContent {
            AuralisTheme {
                AuralisApp(
                    tracks = tracks,
                    player = player,
                    onRequestAccess = { permissionLauncher.launch(permission) }
                )
            }
        }
    }

    private fun loadLocalTracks() {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM
        )
        val found = mutableListOf<Track>()
        contentResolver.query(
            collection, projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = android.content.ContentUris.withAppendedId(collection, id).toString()
                found += Track(id, cursor.getString(titleCol) ?: "Unknown track",
                    cursor.getString(artistCol) ?: "Unknown artist",
                    cursor.getString(albumCol) ?: "Unknown album", uri)
            }
        }
        tracks = found
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

@Composable
private fun AuralisTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Color(0xFFFF6B8A),
        secondary = Color(0xFFB8A5FF),
        background = Color(0xFF090A10),
        surface = Color(0xFF151722)
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
private fun AuralisApp(tracks: List<Track>, player: ExoPlayer, onRequestAccess: () -> Unit) {
    var tab by remember { mutableStateOf("Home") }
    var query by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<Track?>(null) }
    val filtered = remember(tracks, query) {
        tracks.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF11131C)) {
                listOf("Home", "Search", "Library").forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(when(item) { "Home" -> Icons.Default.Home; "Search" -> Icons.Default.Search; else -> Icons.Default.LibraryMusic }, null) },
                        label = { Text(item) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AURALIS", color = Color(0xFFFF7894), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(when(tab) { "Search" -> "Find your sound"; "Library" -> "Your collection"; else -> "Feel every frequency" },
                        color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.GraphicEq, null, tint = Color(0xFFFF7894), modifier = Modifier.size(30.dp))
            }
            when(tab) {
                "Search" -> OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    placeholder = { Text("Search songs on this device") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true, shape = RoundedCornerShape(18.dp)
                )
                "Home" -> {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(170.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFF542B54), Color(0xFF172C49), Color(0xFF11131C))), RoundedCornerShape(26.dp)).padding(20.dp)) {
                        Column(Modifier.align(Alignment.CenterStart)) {
                            Text("YOUR SOUND. YOUR SPACE.", color = Color(0xFFFFB8C7), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Music, without limits.", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("A new home for your listening.", color = Color.LightGray)
                        }
                    }
                    Text("On this device", Modifier.padding(start = 22.dp, top = 22.dp, bottom = 8.dp), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                else -> Text("Your local music library", Modifier.padding(22.dp), color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            if (tracks.isEmpty()) {
                Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.LibraryMusic, null, tint = Color(0xFF777B8C), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No local songs found", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Allow audio access or add music to your device.", color = Color.Gray)
                    TextButton(onClick = onRequestAccess) { Text("Grant audio access") }
                }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    items(filtered, key = { it.id }) { track ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                current = track
                                val index = tracks.indexOf(track)
                                player.setMediaItems(tracks.map { MediaItem.fromUri(it.uri) }, index, 0L)
                                player.prepare()
                                player.play()
                                playing = true
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(48.dp).background(Color(0xFF252333), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, null, tint = Color(0xFFFF91A8))
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(track.title, color = Color.White, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                Text(track.artist, color = Color.Gray, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = {
                                current = track
                                val index = tracks.indexOf(track)
                                player.setMediaItems(tracks.map { MediaItem.fromUri(it.uri) }, index, 0L)
                                player.prepare(); player.play(); playing = true
                            }) { Icon(Icons.Default.PlayArrow, "Play", tint = Color.White) }
                        }
                    }
                }
            }
            current?.let { track ->
                Row(Modifier.fillMaxWidth().background(Color(0xFF1A1C29)).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = Color.White, maxLines = 1, fontWeight = FontWeight.SemiBold)
                        Text(track.artist, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = {
                        if (player.isPlaying) { player.pause(); playing = false } else { player.play(); playing = true }
                    }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White) }
                    IconButton(onClick = { player.seekToNextMediaItem(); playing = true }) {
                        Icon(Icons.Default.SkipNext, null, tint = Color.White)
                    }
                }
            }
        }
    }
}
