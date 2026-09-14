# Mess Meal Manager 🍲📱

**Mess Meal Manager** is a modern Android application designed for managing shared hostel or mess meal calculations, member tracking, expense logging, deposit records, and automated monthly billing sheets.

---

## ✨ Features

- **Meal Tracking:** Record daily breakfast, lunch, and dinner count for all mess members.
- **Deposit & Expense Management:** Keep transparent records of money deposited by members and daily marketing/grocery expenses.
- **Automated Calculation:** Automatically compute meal rates, total costs, individual balances, and final monthly summaries.
- **Multi-Mess / Multi-Sheet Support:** Seamlessly switch between different months, sheets, or mess groups.
- **Real-Time Data:** Powered by Firebase Firestore for real-time synchronization across devices.

---

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3 Design)
- **Architecture:** MVVM (Model-View-ViewModel) + StateFlow & Coroutines
- **Backend / Database:** [Firebase Firestore](https://firebase.google.com/docs/firestore) & [Firebase Authentication](https://firebase.google.com/docs/auth)
- **Build System:** Gradle (Kotlin DSL `.gradle.kts`)

---

## 🚀 Setup & Local Installation

### Prerequisites
- Android Studio Ladybug (or modern Android Studio version)
- JDK 17 or higher
- Android SDK 24+ (Android 7.0+)

### Setup Firebase Configuration

> ⚠️ **Important Security Note:**
> The `google-services.json` file is **not included** in this repository as it contains project-specific Firebase credentials.

To run the application locally:
1. Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with the package name `com.example.messmealmanager`.
3. Download your `google-services.json` file from Firebase Console.
4. Place the downloaded `google-services.json` file inside the `app/` directory (`MessMealManager/app/google-services.json`).
5. Open the project in Android Studio and run **Sync Project with Gradle Files**.
6. Build and run on an emulator or physical device.

---

## 📄 License

This project is open source and available for personal or educational use.
