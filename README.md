# Locaflow Android SDK

The **Locaflow Android SDK** is a lightweight, efficient, and reactive localization solution designed to synchronize translations dynamically from the Locaflow platform to your Android applications.

## Features
- **Over-the-Air (OTA) Updates**: Sync translation strings seamlessly without submitting a new app update.
- **Offline First**: Uses an advanced memory and disk caching system to ensure translations are always available, even when offline.
- **Reactive UI**: Full support for both traditional XML View layouts and Jetpack Compose state-driven UIs.
- **Background Sync**: Polling mechanism for background updates.

## Documentation

For full integration guides, configuration options, and API references, please visit our official documentation:
[Locaflow Android Documentation](https://localflow-1085648460092.asia-southeast2.run.app/docs/mobile-sdk/android-sdk/quickstart)

## Installation

Add the dependency in your app-level `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.localflow:localflow-sdk:1.0.0")
}
```

## Quick Start

Initialize the SDK in your `Application` class:

```kotlin
Localflow.initialize(
    context = this,
    config = LocalflowConfig.Builder(
        apiKey = "YOUR_API_KEY",
        baseUrl = "https://localflow-1085648460092.asia-southeast2.run.app"
    ).build()
)
```

Read strings directly:
```kotlin
val greeting = Localflow.getString("home.greeting")
```

## License
MIT License
