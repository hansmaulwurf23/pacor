# Pacor

Pace-Rechner für Android. Strecke antippen, dann an einer der beiden Walzen ziehen — die andere
Seite wird ausgerechnet. Kein Konto, kein Netz, keine Berechtigungen.

Eine bebilderte Kurzbeschreibung steht in [`docs/README.md`](docs/README.md).

## Bedienung

- **Strecke** — 1 km, 5 km, 10 km, HM (Halbmarathon, 21,0975 km) oder M (Marathon, 42,195 km).
  Beim Wechsel bleibt die Pace stehen und die Zeit wird neu gerechnet: die gewohnte Pace auf einer
  anderen Strecke ist die übliche Frage, nicht umgekehrt.
- **Eigene Strecke** — der Chip mit dem Stift öffnet ein Eingabefeld für 0,1 bis 1000 km (Komma
  oder Punkt). Danach trägt der Chip die eingegebene Länge und merkt sie sich; erneutes Antippen
  ändert sie. Die Zeit-Walze rastet dort nach denselben Stufen wie bei den festen Strecken, ab
  60 km in Zehn-Minuten-Schritten.
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
- `Distance` hält Streckenlänge und die dazu passende Rastung der Zeit-Walze — für die festen
  Strecken fest eingetragen, für eine eigene nach Länge abgestuft.
- `Prefs` sichert Strecke, eigene Strecke und Pace in den SharedPreferences.

## Download

Signierte APKs hängen an den [Releases](https://github.com/hansmaulwurf23/pacor/releases), das
neueste unter [releases/latest](https://github.com/hansmaulwurf23/pacor/releases/latest).

Zum Installieren die Datei auf dem Gerät öffnen. Android fragt einmalig nach der Erlaubnis, Apps aus
dieser Quelle zu installieren; Play Protect zeigt bei selbst signierten Apps eine Warnung, die man
bestätigen kann. Voraussetzung ist Android 8 oder neuer. Eine neuere Version wird einfach über die
installierte installiert; die gewählte Strecke und Pace bleiben erhalten.

Bis Version 1.1 lag das Projekt auf gitlab.noisebleed.de (`pub/pacor`); das Projekt dort ist
archiviert, die alten Releases sind hierher übernommen.

## Bauen und installieren

Gebraucht wird ein **JDK 17**. Das in Android Studio mitgelieferte JBR ist inzwischen ein JDK 25,
das Gradle 8.7 mit `Unsupported class file major version 69` ablehnt — der Pfad aus den älteren
Projekten (`JAVA_HOME=/opt/JetBrains/android-studio/jbr`) funktioniert deshalb nicht mehr:

```bash
JAVA_HOME=~/.jdks/jdk-17.0.20.1+1 ./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Release

Signaturschlüssel und `keystore.properties` liegen **nicht im Repo**, sondern in
`~/Documents/Android/own/pacor/` (per Syncthing gespiegelt und im Backup); `storeFile` wird relativ
zur Properties-Datei aufgelöst. Ein abweichender Pfad geht über `-Ppacor.signing=…`; fehlt der
Schlüssel ganz, bleibt das Release-APK unsigniert statt den Build zu brechen.

Ohne diesen Schlüssel lässt sich kein Update über eine installierte Version schieben — Verlust
bedeutet, dass alle Nutzer erst deinstallieren müssten.

Lokal bauen und prüfen:

```bash
JAVA_HOME=~/.jdks/jdk-17.0.20.1+1 ./gradlew assembleRelease
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

`.github/workflows/release.yml` testet, baut, signiert und veröffentlicht, sobald ein Tag `v<Ziffer>`
gepusht wird. Der Text des annotierten Tags (ohne die erste Zeile) wird zur Release-Beschreibung.
Zwei Prüfungen brechen absichtlich ab, statt ein kaputtes Release zu veröffentlichen: ein
unsigniertes APK und ein `versionName`, der nicht zum Tag passt.

Dafür braucht es die Repository-Secrets `ANDROID_KEYSTORE_B64` (`base64 -w0 pacor-release.jks`),
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` (`pacor`) und `ANDROID_KEY_PASSWORD`, gesetzt mit
`~/ops-scripts/android-github-secrets.sh pacor`; damit liegt eine zweite Kopie des Schlüssels bei
GitHub.

```bash
# versionCode und versionName in app/build.gradle anheben, committen
git tag -a v1.3 -m "Pacor 1.3" -m "- Was sich geändert hat" && git push origin main v1.3
```

## Lizenz

Der Code steht unter der [MIT-Lizenz](LICENSE).
