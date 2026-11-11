# Auralis

Auralis is an original Android music app project built for the Ziplayer repository.

## Direction
A premium listening experience combining an editorial, album-first interface with a modern glass-inspired visual language. The app name, UI, assets and implementation are original.

## Current implementation
- Android app scaffold using Kotlin, Jetpack Compose and AndroidX Media3.
- Reads the user's on-device audio library after runtime permission.
- Search across local track titles and artists.
- Queue-based playback using Media3 ExoPlayer.
- Mini-player controls for pause/resume and next track.
- GitHub Actions debug APK build.

## Music source architecture
The app is designed around replaceable music-source providers. The first working playback source is the user's local library. Online discovery/streaming providers are not enabled yet; they must be integrated through supported APIs and sources whose terms permit playback. No sample/fake songs are shown as real results.

## Build
Requires JDK 17 and Android SDK 35. From the repository root:

```bash
gradle :app:assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Inspiration and licensing
The product workflow was studied from the public SimpMusic project (https://github.com/maxrave-dev/SimpMusic), which is GPL-3.0 licensed. This repository does not copy SimpMusic source code, UI assets, branding, or proprietary service credentials. It is an independent implementation inspired by common music-player workflows. Auralis is not affiliated with Apple, YouTube, YouTube Music, or SimpMusic.
