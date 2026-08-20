# Tawakkul – Mein Begleiter

Eine persönliche, private Web-App für das iPhone, die beim Vertrauen auf Allah (Tawakkul) hilft.

## Was die App kann

- **Reden** – Du schreibst deine Gedanken oder Sorgen (z. B. „Allah gibt mir nicht“,
  Geldsorgen, Angst, Blitzgedanken). Die App erkennt das Thema und antwortet wie ein
  geduldiger Lehrer: erst eine liebevolle Korrektur des Gedankens, dann eine ganz
  einfache Erklärung, dazu ein passender Koranvers und ein authentischer Hadith
  (mit Quellenangabe, sunnitisch), und zum Schluss eine konkrete Handlung
  (z. B. 70× Istighfar).
- **Dhikr** – Tasbih-Zähler mit Istighfar, SubhanAllah, Alhamdulillah, Allahu akbar,
  Hasbunallah, La hawla und Salawat. Zählerstände bleiben gespeichert.
- **Erinnerung** – Wechselnde Tawakkul-Erinnerungen aus Koran und Sunnah, optional
  als Benachrichtigung in wählbaren Abständen.
- **Wissen** – Der Umgang mit Träumen nach der Sunnah, kurze Morgen- und
  Abend-Adhkar, und Hinweise zur App.

Alle Eingaben bleiben ausschließlich auf dem Gerät (localStorage). Es gibt keinen
Server und keine Datenübertragung. Die App funktioniert nach dem ersten Öffnen
auch offline (Service Worker).

## Gehostete Version (GitHub Pages, gratis)

Die App wird automatisch auf GitHub Pages veröffentlicht:

**https://kyy5ngtv7y-creator.github.io/Aa/**

- Der Branch `gh-pages` enthält die veröffentlichten Dateien.
- Der Workflow `.github/workflows/tawakkul-pages.yml` aktualisiert die Seite
  automatisch bei jedem Push in `tawakkul-app/`.
- Falls die Seite 404 zeigt: Repository-Einstellungen → Pages → Branch
  `gh-pages` / Ordner `/ (root)` als Quelle wählen.
- Die Web-Adresse ist öffentlich erreichbar, aber nirgendwo verlinkt. Alle
  Eingaben (Gedanken, Zähler, Checkliste) bleiben ausschließlich im
  localStorage des eigenen Geräts – der Server liefert nur die App-Dateien.

## Auf dem iPhone installieren

1. **https://kyy5ngtv7y-creator.github.io/Aa/** in **Safari** öffnen.
2. **Teilen-Symbol → „Zum Home-Bildschirm“** antippen.
3. Die App liegt nun wie eine normale App auf dem Home-Bildschirm (eigenes Icon,
   Vollbild, offline nutzbar). Benachrichtigungen sind ab iOS 16.4 möglich,
   sobald die App installiert ist.

## Push bei geschlossener App (gratis, über ntfy)

Im Tab „Erinnerung“ → „Push aufs iPhone“ erstellt die App einen zufälligen,
geheimen Kanal auf dem freien Dienst ntfy.sh und plant ihre Hodscha-Meldungen
bis zu 48 Stunden im Voraus (verzögerte Zustellung). Auf dem iPhone wird die
kostenlose App „ntfy“ installiert und dieser Kanal abonniert — die Meldungen
kommen dann auch an, wenn die Tawakkul-App geschlossen ist. Die App muss nur
ab und zu geöffnet werden, damit sie weiterplanen kann. Der Kanalname ist das
einzige Geheimnis: nicht weitergeben.

## Später auf eigenen/bezahlten Server umziehen

Die App ist rein statisch (keine Datenbank, kein Backend): Einfach den Inhalt
von `tawakkul-app/` zu einem beliebigen Host kopieren (eigener Webspace,
Netlify, Vercel, Cloudflare Pages …) – fertig. Auf dem iPhone danach einmal
neu „Zum Home-Bildschirm“ hinzufügen, da sich die Adresse ändert.

## Wichtiger Hinweis

Die App gibt keine Fatwas und ersetzt weder Gelehrte noch Ärzte. Bei religiösen
Fragen bitte an einen vertrauenswürdigen Imam wenden; bei anhaltend schwerer
Sorge oder Traurigkeit gehört auch der Weg zu Arzt oder Therapeut zur Sunnah
des „Kamel-Anbindens“.
