# GitPocket

A native Android app that turns GitHub repositories into a personal file-sync system.
Add a repo with its clone URL, browse the files on your phone, edit text files, and push
changes back to GitHub — all without leaving the app.

## Features

- **Add repositories** — paste an HTTPS clone URL; works with public and private repos
- **Private repo support** — GitHub personal access tokens, stored encrypted in the Android Keystore
- **File browser** — expandable tree of the working directory; binary files open via the system
- **Built-in text editor** — edit text files and commit + push your changes with a custom commit message
- **Manual sync** — swipe to refresh to pull the latest changes from GitHub
- **Safe conflict handling** — fast-forward pulls, rebase-on-divergence with automatic abort on conflict; nothing is silently overwritten
- **No server required** — syncs directly over HTTPS using JGit (pure Java, no native dependencies)

## How it works

- Clones live in **app-private storage** (`filesDir/repos/{id}`), so no storage permissions are needed.
- All Git operations run on a background thread via **JGit**.
- Repo metadata (URL, branch, name) is stored in **Room**; tokens live in **EncryptedSharedPreferences**.

## Tech stack

| Layer      | Technology                                    |
|------------|-----------------------------------------------|
| Language   | Kotlin                                        |
| UI         | Jetpack Compose + Material 3                  |
| Git        | JGit 7.x                                      |
| Database   | Room                                          |
| Storage    | EncryptedSharedPreferences (Android Keystore) |
| minSdk     | 26 (required by JGit's `java.time` support)   |

## Getting started

1. Open the project in Android Studio (or run `./gradlew installDebug`).
2. Grant nothing — no permissions required.
3. In the app, set your **author name/email** under Settings (used for commits you push).
4. Tap **+**, paste a clone URL like `https://github.com/yourname/yourrepo.git`.
   For private repos, generate a GitHub
   [fine-grained personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
   with **Contents: Read and write** and paste it in the token field.
5. Tap **Clone repository**, then browse, edit, and push.

## Building

```bash
./gradlew assembleDebug
# APK output: app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and the Android SDK (compileSdk 35).

## License

MIT