# Mane Kelsa App (ಮನೆ-ಕೆಲಸ)

## Overview
**Mane Kelsa (ಮನೆ-ಕೆಲಸ)** is a bilingual Android application designed to connect household workers with residents in Bangalore, supporting both English and Kannada languages. The app features a robust dual-role system allowing users to seamlessly switch between hiring workers or finding work as service providers.

## Features

### Core Functionality
- **Dual-Role System**: Users can operate as either `HIRER` (to find workers) or `WORKER` (to find jobs).
- **Worker Discovery**: Search and filter workers by skill, location, and availability.
- **Profile Management**: Create and manage robust worker profiles featuring photos, skills, ratings, and availability status.
- **Call History**: Track and manage previous contacts between users.
- **Availability Management**: Workers can toggle their daily availability status for real-time accurate hiring.

### Localization
- **Bilingual Support**: Full English and Kannada language support with instant runtime switching.
- **Dynamic Translation**: Worker data (skills, areas) is translated seamlessly at the UI layer.
- **Voice Search Localization**: Speech recognition adapts to the selected language dialect (`en-IN` / `kn-IN`).

## Tech Stack

### Mobile Framework
- **Android SDK** (API 21+)
- **Jetpack Compose**: Modern declarative UI toolkit.
- **Kotlin**: Primary programming language.
- **Hilt**: Dependency Injection framework for scalable state management.

### Architecture
- **MVVM (Model-View-ViewModel)**: Clean architecture principles ensuring separation of concerns.
- **Repository Pattern**: Data layer abstraction for offline-first caching and cloud synchronization.
- **Jetpack Navigation Compose**: Type-safe screen routing.

### Data Storage & Backend
- **Firebase**: Cloud backend, authentication, and cloud storage (Firestore & Firebase Storage).
- **Room Database**: Local SQLite data persistence acting as the single source of truth for the UI.

## Navigation Structure

The app uses a sealed class `Screen` to define all navigation routes and destinations:

| Screen | Route | Purpose |
|--------|-------|---------|
| **Home** | `home` | Main dashboard tailored to the active role |
| **Search** | `search` | Worker discovery, filtering, and hiring |
| **Profile** | `profile` | Worker profile management and photo uploads |
| **CallHistory** | `call_history` | Historical contact logs |
| **Availability** | `availability` | Worker status management toggle |
| **ResidentProfile** | `resident_profile` | Hirer profile and request tracking |
| **Requests** | `requests` | Work request list for incoming worker jobs |

### Role-Based Navigation
Bottom navigation items dynamically adjust based on the user's active role:
- **HIRER**: Home, Search, Resident Profile
- **WORKER**: Home, Requests, Profile

## Localization Implementation

### Resource Files
Static UI text is strictly typed and maintained in standard Android resource files:
- **English**: `app/src/main/res/values/strings.xml`
- **Kannada**: `app/src/main/res/values-kn/strings.xml`

### Language Switching
The `LocalizationManager` handles runtime language switching using `SharedPreferences` and modern `AppCompatDelegate.setApplicationLocales()` APIs for Android 13+ compatibility.

### Dynamic Data Translation
Dynamic user-generated or backend data stored in Firebase/Room is translated at the UI layer using `TranslationUtils`, which maps English domain entities to their localized counterparts safely based on the current context's locale.

## Setup Instructions

### Prerequisites
- Android Studio (Arctic Fox or later)
- JDK 11 or higher
- Android SDK (API 21+)
- An active Firebase project

### Installation Steps
1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   ```
2. **Open the project** in Android Studio.
3. **Configure Firebase:** Download your `google-services.json` file from your Firebase console and place it inside the `app/` directory.
4. **Sync Gradle** to fetch all dependencies.
5. **Build and run** the application on an emulator or physical Android device.
