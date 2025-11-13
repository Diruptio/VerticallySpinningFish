# VerticallySpinningFish

Ein dynamisches Container-Management-System für Minecraft-Server, das automatisch Docker-Container erstellt, verwaltet und skaliert.

## Überblick

VerticallySpinningFish ist ein System zur automatischen Verwaltung von Minecraft-Server-Containern. Es ermöglicht die dynamische Erstellung und Verwaltung von Server-Instanzen basierend auf konfigurierbaren Gruppen. Das System besteht aus:

- **API-Server**: Zentrale Verwaltungskomponente, die Docker-Container orchestriert
- **Velocity Plugin**: Integration mit Velocity Proxy-Servern
- **Paper Plugin**: Integration mit Paper Minecraft-Servern
- **REST API**: HTTP-API zur Verwaltung von Containern und Gruppen

## Funktionen

- **Automatische Container-Verwaltung**: Erstellt und verwaltet Container basierend auf konfigurierbaren Gruppen
- **Dynamische Skalierung**: Hält eine Mindestanzahl von Containern pro Gruppe aufrecht
- **Template-System**: Flexible Template-Konfiguration für Container-Inhalte
- **Live-Updates**: WebSocket-Verbindung für Echtzeit-Updates über Container-Status
- **Docker-Integration**: Nutzt Docker für Container-Isolation und -Verwaltung
- **REST API**: Vollständige API zur Steuerung von Containern und Gruppen
- **Velocity-Integration**: Automatische Server-Registrierung im Velocity-Proxy

## Systemanforderungen

- Docker
- Java 21 oder höher
- Zugriff auf Docker Socket (`/var/run/docker.sock`)
- Gradle (für das Bauen aus dem Quellcode)

## Installation

### Mit Docker (empfohlen)

1. Docker-Image bauen:
```bash
./gradlew jar
docker build -t verticallyspinningfish .
```

2. Container starten:
```bash
docker run -d \
  --name vsf \
  --volume ./data:/data \
  --volume /var/run/docker.sock:/var/run/docker.sock \
  --publish 7000:7000 \
  verticallyspinningfish
```

### Aus dem Quellcode

1. Repository klonen:
```bash
git clone https://github.com/Diruptio/VerticallySpinningFish.git
cd VerticallySpinningFish
```

2. Bauen:
```bash
./gradlew jar
```

3. Ausführen:
```bash
./gradlew run
```

## Konfiguration

### Hauptkonfiguration

Erstellen Sie eine `config.yml` im Datenverzeichnis (`/data` im Container):

```yaml
# Docker-Host-Verbindung
docker-host: "unix:///var/run/docker.sock"

# Geheimnis für API-Authentifizierung (wird automatisch generiert, falls nicht angegeben)
secret: "your-secret-key"

# Präfix für Container-Namen
container-prefix: "vsf-"
```

### Container-Gruppen

Container-Gruppen werden im Verzeichnis `groups/` konfiguriert. Jede Gruppe ist eine YAML-Datei.

#### Beispiel: Lobby-Server (`groups/lobby.yml`)

```yaml
min-count: 1          # Mindestanzahl laufender Container
min-port: 40000       # Minimaler Port für Port-Zuweisungen
tags:                 # Tags zur Steuerung des Verhaltens
  - register-velocity # Bei Velocity-Proxy registrieren
  - velocity-fallback # Als Fallback-Server verwenden
delete-on-stop: true  # Container beim Stoppen löschen

template:
  - type: papermc-fill
    project: paper
    version: "1.21.8"
    file: server.jar
  - type: papermc-hangar
    project: ViaVersion
    platform: paper
    file: plugins/ViaVersion.jar
  - type: papermc-hangar
    project: ViaBackwards
    platform: paper
    file: plugins/ViaBackwards.jar
  - type: copy
    from: templates/lobby
    into: .
```

#### Beispiel: Velocity-Proxy (`groups/velo.yml`)

```yaml
min-count: 1
min-port: 25565
delete-on-stop: true

template:
  - type: velocity-plugin  # Fügt das VSF Velocity-Plugin hinzu
  - type: papermc-fill
    project: velocity
    file: server.jar
  - type: copy
    from: templates/velo
    into: .
```

### Template-Typen

- **`papermc-fill`**: Lädt Paper/Velocity Server von PaperMC herunter
  - `project`: paper, velocity, waterfall, etc.
  - `version`: Versions-String
  - `file`: Zieldatei

- **`papermc-hangar`**: Lädt Plugins von Hangar (PaperMC Plugin-Repository)
  - `project`: Plugin-Name
  - `platform`: paper, velocity, etc.
  - `file`: Zieldatei

- **`velocity-plugin`**: Fügt das VerticallySpinningFish Velocity-Plugin hinzu

- **`copy`**: Kopiert Dateien aus einem Template-Verzeichnis
  - `from`: Quellverzeichnis (relativ zu `templates/`)
  - `into`: Zielverzeichnis im Container

## Verwendung

### API-Endpunkte

Die API läuft standardmäßig auf Port 7000. Alle Anfragen benötigen den `Authorization`-Header mit dem konfigurierten Secret.

#### Container verwalten

```bash
# Alle Container auflisten
curl -H "Authorization: your-secret" http://localhost:7000/containers

# Container erstellen
curl -X POST -H "Authorization: your-secret" \
  -H "Content-Type: application/json" \
  -d '{"group":"lobby"}' \
  http://localhost:7000/container

# Container starten
curl -X POST -H "Authorization: your-secret" \
  -H "Content-Type: application/json" \
  -d '{"containerId":"abc123"}' \
  http://localhost:7000/container/start

# Container stoppen
curl -X POST -H "Authorization: your-secret" \
  -H "Content-Type: application/json" \
  -d '{"containerId":"abc123"}' \
  http://localhost:7000/container/stop
```

#### Gruppen verwalten

```bash
# Alle Gruppen auflisten
curl -H "Authorization: your-secret" http://localhost:7000/groups

# Minimum-Anzahl aktualisieren
curl -X PATCH -H "Authorization: your-secret" \
  -H "Content-Type: application/json" \
  -d '{"name":"lobby","minCount":2}' \
  http://localhost:7000/group/min-count
```

#### Live-Updates

WebSocket-Verbindung für Echtzeit-Updates:

```
ws://localhost:7000/live-updates
```

### Verwendung in Minecraft-Plugins

#### Velocity Plugin

Das Velocity-Plugin registriert automatisch Container als Server im Proxy:

```java
// Das Plugin wird automatisch geladen, wenn es im Template enthalten ist
// Container mit dem Tag "register-velocity" werden automatisch registriert
```

#### Paper Plugin

Das Paper-Plugin meldet den Container-Status an die API:

```java
// Das Plugin setzt automatisch den Status auf AVAILABLE beim Start
// Keine weitere Konfiguration erforderlich
```

#### API-Client in eigenen Plugins

```java
import diruptio.verticallyspinningfish.VerticallySpinningFishApi;
import diruptio.verticallyspinningfish.api.*;

// API-Client aus Container-Umgebung erstellen
VerticallySpinningFishApi api = VerticallySpinningFishApi.fromCurrentContainer();

// Container erstellen
Container container = api.createContainer("lobby");

// Container auflisten
List<Container> containers = api.getContainers();

// Gruppen abrufen
List<Group> groups = api.getGroups();

// Auf Live-Updates reagieren
api.getContainerAddListeners().add(container -> {
    System.out.println("Neuer Container: " + container.getName());
});
```

## Architektur

```
┌─────────────────────────────────────────────┐
│  VerticallySpinningFish (Docker Container)  │
│  - API Server (Port 7000)                   │
│  - Container-Orchestrierung                 │
│  - Template-Management                      │
└──────────────┬──────────────────────────────┘
               │
               │ Docker API
               ▼
┌─────────────────────────────────────────────┐
│           Docker Engine                      │
│  ┌─────────────┐  ┌─────────────┐          │
│  │  Velocity   │  │   Lobby 1   │          │
│  │   Proxy     │  ├─────────────┤          │
│  │             │  │   Lobby 2   │  ...     │
│  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────┘
```

## Verzeichnisstruktur

```
/data/
├── config.yml              # Hauptkonfiguration
├── groups/                 # Gruppen-Definitionen
│   ├── lobby.yml
│   └── velo.yml
├── templates/              # Template-Dateien
│   ├── lobby/
│   └── velo/
└── running/                # Laufende Container-Daten (automatisch erstellt)
    ├── vsf-lobby-1/
    └── vsf-velo-1/
```

## Entwicklung

### Bauen

```bash
./gradlew build
```

### Tests ausführen

```bash
./gradlew test
```

### Docker-Image veröffentlichen

```bash
./gradlew publishDockerImage
```

## Umgebungsvariablen

Folgende Umgebungsvariablen werden automatisch in erstellten Containern gesetzt:

- `VSF_PREFIX`: Container-Präfix
- `VSF_API_PORT`: Port der VSF-API
- `VSF_SECRET`: Authentifizierungs-Secret
- `HOST_UID`: (optional) UID des Host-Benutzers
- `HOST_GID`: (optional) GID des Host-Benutzers

## Lizenz

Dieses Projekt wird von Diruptio entwickelt und gewartet.

## Autor

- Fabi.exe

## Links

- GitHub: https://github.com/Diruptio/VerticallySpinningFish
- Docker Registry: docker-public.diruptio.de/diruptio/vertically-spinning-fish
