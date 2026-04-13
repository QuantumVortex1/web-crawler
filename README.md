# Web Crawler

Dieses Projekt ist ein Java-basierter Web-Crawler, der Webseiten nach Bildern durchsucht und gefundene Bilder lokal speichert. Der Schwerpunkt liegt auf einer sauberen Parallelverarbeitung: Die Analyse von Webseiten und das Herunterladen von Bildern laufen getrennt und gleichzeitig.

Das Projekt entstand im Rahmen der Lehrveranstaltung Parallel Programming und setzt die geforderten Kernideen zu Thread-Safety, Aufgabenaufteilung und strukturiertem Programmaufbau um.

## Funktionen

1. Entgegennahme beliebiger Start-URLs über eine einheitliche Crawler-Schnittstelle.
2. Analyse der HTML-Seiten und Extraktion von Bildreferenzen aus img-Tags.
3. Paralleles Herunterladen der gefundenen Bilder in ein konfigurierbares Zielverzeichnis.
4. Trennung von Website-Scan und Bild-Download in zwei unabhängige Parallelitätsbereiche.
5. Strukturierte Ablage der Ergebnisse in nummerierten Unterordnern pro Start-URL.
6. Behandlung von Dateinamenskonflikten durch automatische Suffixbildung.
7. Rückmeldung des Systemzustands über eine Idle-Abfrage.

## Projektstruktur

1. API-Schicht
2. Core-Schicht

Die API-Schicht stellt die vertraglichen Schnittstellen bereit. Die Core-Schicht implementiert die fachliche Logik für Koordination, Webseitenanalyse, Download und Konfiguration.

## Qualität und Nachvollziehbarkeit

Das Projekt enthält Unit-Tests mit JUnit 5 für die zentralen Komponenten. Die Lösung ist modular aufgebaut und auf Erweiterbarkeit ausgelegt. Die Dokumente `Entwurf.md` und `Fragen.md` beschreiben Architekturentscheidungen, Parallelisierungsstrategie und Schutzmechanismen.
