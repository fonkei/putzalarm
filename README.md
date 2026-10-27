# Putzalarm (Android)

Zahnputz-App für Kinder im Stil „Monsterjagd“. Die Frontkamera zeigt das Kind, und solange es
schrubbt, fliegen bunte Bakterien schreiend aus dem Mund. Gebaut für Tablets im Querformat.

## Was drin ist

- **Wer putzt?** Kinderprofile mit Name, Alter und Foto (oder Monster als Bild). Anlegen und
  Ändern nur über den Elternbereich (kleine Rechenaufgabe).
- **Start** mit Maskottchen Super Bürsti, Wochen-Serie und Zugang zum Album.
- **Putzen**: Live-Bild der Frontkamera, Mund-Erkennung mit ML Kit (auf dem Gerät),
  Bewegungserkennung rund um den Mund. Bakterien fliegen nur bei echter Putzbewegung.
  2 Minuten (Alter 3: 90 Sekunden), 4 Zonen, Gebiss-Grafik ohne Text, Punkte, Combos,
  Sprachansagen und quiekende Monster über die Android-Sprachausgabe.
- **Ergebnis** mit Sternen, geputzten und verpassten Zonen und einem neuen Sticker.
- **Monster-Album** mit 24 Monstern zum Sammeln.

Kamerabilder werden nur live ausgewertet und nie gespeichert oder hochgeladen.
Profile und Profilfotos liegen nur im App-Speicher des Geräts.

## Bauen

- In Android Studio öffnen und auf ▶ drücken, oder
- `./gradlew assembleDebug` (braucht JDK 17 und das Android SDK), oder
- auf GitHub: Jeder Push baut die APK automatisch. Sie liegt unter **Actions → letzter Lauf →
  Artifacts → putzalarm-apk**.

Technik: Kotlin, Jetpack Compose, CameraX, ML Kit Face Detection. minSdk 26, targetSdk 35.

Super Bürsti und die Monster sind fertig gerenderte 3D-Bilder aus `design/super-buersti-3d-modell.html`
(in `res/drawable-nodpi`). Neu erzeugen: `node tools/render_sprites.js <pfad/zu/three.min.js>` mit
three@0.149.0 (z. B. aus `npm pack three@0.149.0`) und Playwright.

Schriften: Lilita One und Nunito (SIL Open Font License, siehe `licenses/`).
