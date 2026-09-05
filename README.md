# Pacor

Pace-Rechner für Android. Strecke antippen, dann an einer der beiden Walzen ziehen — die andere
Seite wird ausgerechnet. Kein Konto, kein Netz, keine Berechtigungen.

## Bedienung

- **Strecke** — 1 km, 5 km, 10 km, Halbmarathon (21,0975 km) oder Marathon (42,195 km). Beim
  Wechsel bleibt die Pace stehen und die Zeit wird neu gerechnet: die gewohnte Pace auf einer
  anderen Strecke ist die übliche Frage, nicht umgekehrt.
- **Walze ziehen** — waagerecht nach links oder rechts, mit Schwung auch weiter rollend. Wer die
  Zeit-Walze anfasst, gibt die Zielzeit vor und bekommt die Pace; wer die Pace-Walze anfasst,
  bekommt die Zeit. Einen Umschalter für die Richtung gibt es deshalb nicht — die angefasste
  Walze *ist* die Eingabe, die andere trägt die Marke „berechnet“.
- **Rastung** — die Pace rastet durchgehend in Fünf-Sekunden-Schritten. Die Zeit rastet auf
  Halbmarathon und Marathon in Fünf-Minuten-Schritten (3:15, 3:20, 3:25 …), auf den kurzen
  Strecken feiner: 1 Minute auf 10 km, 30 Sekunden auf 5 km, 5 Sekunden auf 1 km. Fünf Minuten
  wären dort sinnlos — ein einziger Schritt würde die Pace auf einem Kilometer um fünf Minuten
  verschieben. Zwischenwerte sind trotzdem erreichbar: die berechnete Seite zeigt den exakten
  Wert und darf zwischen zwei Rasten stehen.
- Unter der Pace steht dieselbe Geschwindigkeit in km/h.

## Technik

- Java, Android Views (kein Compose), Material 3, minSdk 26 / targetSdk 34.
- `WheelSliderView` ist die eigene View für die Walze: Skala, Zeiger, Fangen auf die Raste,
  Schwungrollen über einen `OverScroller` und ein Haptik-Tick pro übersprungener Raste. Sie führt
  zwei Werte — eine stufenlose Position, die dem Finger folgt, und den gerasteten Wert, der nach
  außen gemeldet wird.
- `PaceMath` rechnet und formatiert, ganz ohne Android-Klassen, und ist deshalb in gewöhnlichen
  JVM-Tests geprüft (`app/src/test`).
- `Distance` hält Streckenlänge und die dazu passende Rastung der Zeit-Walze.
- `Prefs` sichert Strecke und Pace in den SharedPreferences.

## Download

Signierte APKs hängen an den [Releases](https://gitlab.noisebleed.de/pub/pacor/-/releases). Der
Direktlink der jeweils aktuellen Version:

```
https://gitlab.noisebleed.de/api/v4/projects/54/packages/generic/pacor/1.0/Pacor-1.0.apk
```

Zum Installieren die Datei auf dem Gerät öffnen. Android fragt einmalig nach der Erlaubnis, Apps aus
dieser Quelle zu installieren; Play Protect zeigt bei selbst signierten Apps eine Warnung, die man
bestätigen kann. Voraussetzung ist Android 8 oder neuer.

## Bauen und installieren

Gebraucht wird ein **JDK 17**. Das in Android Studio mitgelieferte JBR ist inzwischen ein JDK 25,
das Gradle 8.7 mit `Unsupported class file major version 69` ablehnt — der Pfad aus den älteren
Projekten (`JAVA_HOME=/opt/JetBrains/android-studio/jbr`) funktioniert deshalb nicht mehr:

```bash
JAVA_HOME=~/.jdks/jdk-17.0.20.1+1 ./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Release

Der Signaturschlüssel liegt **nicht im Repo** (das ist öffentlich), sondern in
`~/Documents/Android/own/pacor/` — per Syncthing zwischen den eigenen Rechnern gespiegelt und im
Backup. `keystore.properties` daneben nennt Alias und Passwörter, `storeFile` wird relativ zu dieser
Datei aufgelöst, der Ordner darf also auf jedem Rechner anders heißen. Ein abweichender Pfad geht
über `-Ppacor.signing=…`; fehlt der Schlüssel ganz, bleibt das Release-APK unsigniert statt den
Build zu brechen.

Ohne diesen Schlüssel lässt sich kein Update über eine installierte Version schieben — Verlust
bedeutet, dass alle Nutzer erst deinstallieren müssten.

```bash
JAVA_HOME=~/.jdks/jdk-17.0.20.1+1 ./gradlew assembleRelease
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

Anschließend Tag setzen, die APK in die Paket-Registry legen und das Release anlegen. Die Registry
statt der Projekt-Uploads, weil nur ihre URL bei öffentlichen Projekten ohne Anmeldung funktioniert
und einen Namespace-Wechsel übersteht:

```bash
git tag -a v1.0 -m "Pacor 1.0" && git push origin v1.0
mv app/build/outputs/apk/release/app-release.apk Pacor-1.0.apk
curl --request PUT --header "PRIVATE-TOKEN: $TOKEN" --upload-file Pacor-1.0.apk \
  "https://gitlab.noisebleed.de/api/v4/projects/54/packages/generic/pacor/1.0/Pacor-1.0.apk"
glab release create v1.0 --name "Pacor 1.0" --notes "…" \
  --assets-links "[{\"name\":\"Pacor 1.0 (Android-APK)\",\"url\":\"…\",\"link_type\":\"package\"}]"
```

Achtung: `glab release create` mit angehängter Datei schreibt die Asset-URL mit dem Host
`gitlab.com` statt dem der eigenen Instanz — deshalb der Weg über `--assets-links` mit fertiger URL.

## Release über CI

`.gitlab-ci.yml` erledigt das alles selbst, sobald ein Tag der Form `v<Ziffer>` gepusht wird: bauen,
signieren, in die Paket-Registry laden, Release mit Asset-Link anlegen. Für Branches läuft die
Pipeline bewusst gar nicht.

Solange auf der Instanz kein Runner mit dem Tag `docker` registriert ist, bleibt der Job allerdings
liegen und die Pipeline wird rot — dann entsteht das Release von Hand wie oben beschrieben. Bei
Focus ist das bis heute so. Der übliche Ablauf, sobald ein Runner da ist:

```bash
# versionCode und versionName in app/build.gradle anheben, committen
git tag -a v1.1 -m "Pacor 1.1" && git push origin main && git push origin v1.1
```

Zwei Prüfungen brechen den Job absichtlich ab, statt ein kaputtes Release zu veröffentlichen: das
Umbenennen der APK schlägt fehl, wenn mangels Schlüssel nur `app-release-unsigned.apk` entstanden
ist, und `versionName` im APK muss zum Tag passen — sonst zeigt der About-Dialog etwas anderes an,
als der Download heißt.

### Einmalige Einrichtung

Wie bei [Focus](https://gitlab.noisebleed.de/pub/focus): Runner mit Docker-Executor und dem Tag
`docker`, dazu die geschützten CI-Variablen `ANDROID_KEYSTORE_B64` (Typ File, Inhalt
`base64 -w0 pacor-release.jks`), `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` und
`ANDROID_KEY_PASSWORD`. Das Tag-Muster `v*` muss geschützt sein, sonst gibt GitLab die Variablen
nicht heraus.
