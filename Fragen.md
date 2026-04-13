## 1. Welche Teile des Programms müssen gegenüber parallelem Zugriff geschützt werden?

### Kritische Bereiche sind:

#### - Gemeinsame Zähler im ImageCrawler
- folderCounter (Vergabe der Ordnernummer pro crawl-Aufruf)
- activeWebsiteTasks und activeDownloadTasks (Idle-Ermittlung)

#### - Task-Einplanung in die Thread-Pools
- crawl in ImageCrawler und queueDownload in ImageDownloader werden von mehreren Threads aufgerufen und müssen konsistent bleiben.

#### - Dateisystem-Zugriff beim Speichern
- Mehrere Download-Threads können gleichzeitig in denselben Zielordner schreiben.
- Bei gleichem Dateinamen muss eine kollisionssichere Vergabe erfolgen.

<br>
<br>
<br>

## 2. Wie stellen Sie den Schutz gegenüber dem parallelen Zugriff sicher? Welche Alternativen hätte es gegeben? Warum haben Sie sich für diese Variante entschieden?

### Umsetzung im aktuellen Code:

#### - Atomare Objekte
- ImageCrawler verwendet AtomicInteger für folderCounter, activeWebsiteTasks und activeDownloadTasks.
- ImageDownloader verwendet AtomicInteger activeDownloadTasks ebenfalls atomar für Inkrement/Dekrement.
- AtomicInteger verhindert Race-Conditions bei gleichzeitigen Zugriffen, hat aber leicht erhöhten Overhead als einfache int-Variablen.

#### - Begrenzte Parallelität über ExecutorService
- websiteScanExecutor und imageDownloadExecutor sind jeweils fixed thread pools. Dadurch ist die maximale Anzahl gleichzeitiger Website-Scans und Downloads direkt durch die Konfiguration begrenzt.

#### - Kollisionsschutz beim Dateinamen über atomische Dateierzeugung
- Download versucht Datei mit Files.copy anzulegen.
- Bei FileAlreadyExistsException wird der nächste Name mit Suffix _2, _3, ... ausprobiert.
- Thread-sicher, da nicht per vorgelagertem exists-Check gearbeitet wird.

#### - Konsistente Idle-Logik auch bei Fehlern
- In crawl und queueDownload wird im Fehlerfall (RejectedExecutionException) der jeweilige Aktivzähler sofort zurückgesetzt.
- Im Worker wird in finally immer dekrementiert.

### Mögliche Alternativen:

#### - synchronized/Lock um komplette Methoden
- Einfachere Implementierung ohne atomare Klassen.
- Blockieren von crawl und queueDownload würde die Parallelität stark einschränken.

#### - Semaphore für Parallelitätsgrenzen
- Zusätzlicher Synchronisationsaufwand, komplexere Fehlerbehandlung bei Task-Ablehnungen.
- Redundant, da fixed thread pools die Begrenzung bereits sauber abbilden.

#### - Zentrale synchronisierte Namensvergabe pro Ordner
- Zusätzlicher Synchronisationsaufwand, Flaschenhals bei vielen Downloads.
- Reduziert Parallelität, da alle Downloads für denselben Ordner nacheinander warten müssten.

### Begründung für die gewählte Variante:
- Atomare Klassen bieten Thread-sicheren Zugriff mit minimalem Implementierungsaufwand. 
  - Parallelität ist auch für viele Threads möglich. 
  - Die Nutzung gewöhnlicher Int-Variablen würde Synchronisationsblöcke erfordern und damit die Parallelität stark einschränken.
- Fixed thread pools bieten konfigurationsgerechte Begrenzung der Parallelität ohne zusätzlichen Synchronisationsaufwand.
  - Verhindern Überlastung des Systems durch zu viele gleichzeitige Threads.
  - Die Alternative mit Semaphoren wäre unnötig komplex und fehleranfällig.
- Vorgelagerte exist-Checks für Dateinamen würden zwischen Check und Erstellung ein Zeitfenster für Race-Conditions öffnen. 
  - Die gewählte Methode mit atomarer Erstellung und Wiederholung bei Konflikten ist Thread-sicher und effizient
  - Bei Alternative mit zentraler synchronisierter Namensvergabe müssten die Download-Threads aufeinander warten und wären nicht mehr vollständig parallel.

<br>
<br>
<br>

## 3. Nennen Sie die Objekte und Datenstrukturen, die pro Thread zusätzlich im Arbeitsspeicher (Heap oder Stack) vorgehalten werden müssen.

### Pro aktivem Thread fallen zusätzlich an:

1. Stack (thread-lokal)
- Lokale Variablen in den ausgeführten Methoden, z. B. uri/folderNum in crawl-Task, imageUrl/folderNum in Download-Task, baseUrl/Document/Elements in analyze.

2. Heap-Objekte pro Task/Aufruf
- Runnable/Lambda-Instanz für den eingereihten Task (Website-Scan oder Download).
- Bei Website-Analyse: Jsoup Document, Elements, Element-Objekte, aufgelöste URL-Strings.
- Bei Download: URI, URLConnection, Path-Objekte (targetDir/targetPath), InputStream und ggf. Exception-Objekte.

### Geteilte Heap-Objekte:
- ExecutorServices, ImageCrawler, WebsiteAnalyzer, ImageDownloader.
- AtomicInteger-Zähler.
- Konfiguration (IImageCrawlerConfig) und Logger.