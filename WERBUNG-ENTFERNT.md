# Mimasu ohne Werbung – was geändert wurde und wie Du die APK baust

Dieser Branch enthält den Quellcode der App **Mimasu** (GPLv3, Original von
[DatL4g](https://github.com/DatL4g/Mimasu)) mit **vollständig entfernter Google-AdMob-Werbung**.

Der erste Commit ist der unveränderte Original-Stand, der zweite Commit enthält
ausschließlich die Werbe-Entfernung. So ist jede Änderung nachvollziehbar.

---

## Was entfernt wurde

| Was | Datei | Änderung |
|---|---|---|
| AdMob-SDK-Start + Einwilligungs-Dialog | `other/AdManager.kt` | Datei gelöscht |
| Werbe-Basisklasse der Activity | `AdActivity.kt` | Datei gelöscht |
| Aufruf des Einwilligungs-Dialogs | `MainActivity.kt` | entfernt, erbt jetzt von `MimasuActivity` |
| Banner-Werbung (Startseite + „Space") | `ui/ads/BannerAd.android.kt` | zeigt nichts mehr an |
| Video-Werbung vor dem Abspielen | `ui/ads/RewardAdManager.android.kt` | Video startet direkt, ohne Werbespot |
| Registrierung des AdManagers | `module/PlatformModule.android.kt` | entfernt |
| AdMob-App-ID im Manifest | `AndroidManifest.xml` | entfernt |
| Werbe-Bibliotheken | `composeApp/build.gradle.kts`, `gradle/libs.versions.toml` | `play-services-ads`, `user-messaging-platform`, `jet-ads` entfernt |

**Wichtig:** Die Werbe-Bibliotheken sind nicht nur abgeschaltet, sondern aus den
Abhängigkeiten geworfen. Der AdMob-Code landet also gar nicht erst in der APK –
die App kann dadurch keine Werbeanfragen mehr stellen.

Die Werbung vor dem Video war im Original die Gegenleistung dafür, dass man
kein Premium-Konto hat (`VideoNavigationController`: Premium-Nutzer sahen den
Spot nicht). Nach der Änderung startet das Video für alle direkt. Das entspricht
genau dem, was der Entwickler in seiner Web-Version (`RewardAdManager.js.kt`)
ohnehin schon so umgesetzt hat.

Der Entwickler finanziert das Projekt über diese Werbung. Wenn Dir die App etwas
wert ist, kannst Du ihn über [GitHub Sponsors](https://github.com/sponsors/DATL4G)
oder [PayPal](https://paypal.me/datlag) direkt unterstützen.

---

## Warum hier keine fertige APK dabei liegt

Ich konnte die APK in dieser Umgebung **nicht bauen**. Das liegt nicht an den
Änderungen, sondern an harten Blockern:

1. **`dl.google.com` ist durch die Netzwerk-Richtlinie gesperrt** (HTTP 403).
   Über diesen Server läuft sowohl das Android SDK als auch Googles Maven-Repository.
   Ohne ihn gibt es kein Android-Gradle-Plugin, kein AndroidX und kein Compose-für-Android.
2. **Kein Android SDK installiert** und keine Möglichkeit, es nachzuladen (siehe 1).
3. **`google-services.json` fehlt** – die Firebase-Konfiguration ist privat und
   liegt nicht im Quellcode.

Punkt 1 allein macht einen Android-Build unmöglich. Du musst also selbst bauen –
die Anleitung steht unten.

---

## Was Du zum Bauen brauchst

- **JDK 21**
- **Android SDK** mit API-Level 36 (z. B. über Android Studio)
- **Ein eigenes Firebase-Projekt** (kostenlos)

Der letzte Punkt ist leider Pflicht und der aufwendigste Teil. Grund: Die App
holt sich den **TMDB-API-Schlüssel** (für Filmtitel, Cover, Beschreibungen) zur
Laufzeit aus *Firebase Remote Config*. Ohne Firebase startet die App zwar, zeigt
aber keine Inhalte.

### Firebase einrichten

1. Auf [console.firebase.google.com](https://console.firebase.google.com) ein
   neues Projekt anlegen.
2. Eine **Android-App** mit dem Paketnamen `dev.datlag.mimasu` hinzufügen.
3. Die Datei **`google-services.json`** herunterladen und nach
   `composeApp/google-services.json` legen.
4. In der Firebase-Konsole unter **Authentication** die Anmeldemethode
   *Anonym* aktivieren.
5. Unter **Remote Config** einen Parameter anlegen:
   - Schlüssel: `tmdb_api_key`
   - Wert: Dein eigener TMDB-API-Schlüssel (kostenlos auf
     [themoviedb.org](https://www.themoviedb.org/settings/api))
   - Danach **Veröffentlichen** nicht vergessen.

---

## Bauen

```bash
git clone <dieses-repository>
cd Aa
git checkout claude/remove-google-ads-apk-n1a01v

# google-services.json nach composeApp/ legen (siehe oben)

# leere Sekret-Konfiguration anlegen – die restlichen Werte
# zieht Sekret automatisch aus google-services.json
printf 'secrets:\n' > composeApp/sekret.yaml

./gradlew composeApp:generateSekret
./gradlew composeApp:createAndCopySekretNativeBinary
./gradlew composeApp:assembleDebug
```

Die fertige APK liegt danach unter:

```
composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

Diese Datei kannst Du direkt auf Dein Android-Gerät kopieren und installieren
(„Installation aus unbekannten Quellen" muss erlaubt sein).

### Release-Variante (kleiner, optimiert)

Dafür brauchst Du zusätzlich einen eigenen Signatur-Schlüssel:

```bash
keytool -genkey -v -keystore keystore.jks -keyalg RSA \
  -keysize 2048 -validity 10000 -alias mimasu

export KEYSTORE_PASSWORD=dein_passwort
export KEY_ALIAS=mimasu
export KEY_PASSWORD=dein_passwort

./gradlew composeApp:assembleRelease
```

---

## Falls trotzdem noch Werbung auftaucht

Dann kommt sie **nicht aus der App**, sondern von den Streaming-Webseiten
(bs.to, aniworld, serienstream, streamkiste), die Mimasu im Hintergrund öffnet.
Diese Werbung steckt in den Webseiten selbst und lässt sich nicht über die App
entfernen. Beachte außerdem den Hinweis des Entwicklers: Werbeblocker per App
oder DNS können das Abspielen der Videos kaputt machen.

---

## Prüfen, dass wirklich keine Werbung drin ist

Nach dem Bauen kannst Du das selbst kontrollieren:

```bash
# sollte keine Treffer liefern
unzip -p composeApp/build/outputs/apk/debug/composeApp-debug.apk classes*.dex \
  | strings | grep -i "googleads\|admob"
```

Im Quellcode ist es schon jetzt nachprüfbar:

```bash
grep -rn "com.jet.ads\|gms.ads\|admob" --include=*.kt --include=*.kts --include=*.xml .
# liefert keine Treffer
```
