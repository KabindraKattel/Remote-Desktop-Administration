# Remote-Desktop-Administration

![Logo](icons/logo.svg)

**Remote Desktop Administration** is a **Java-based**, cross-platform, standalone client-server application built with *
*JavaFX**. It supports distributed message passing within LAN/WLAN environments, where a central administrator acts as
the server and provides services to connected clients.

---

## Features

- Console Handler, File Handler, and UI Event Handler for comprehensive log recording.
- Instant dual-messaging service enabling real-time communication between the administrator and client(s).
- Remote administrative control to shut down, restart, or log off client computers.
- Remote transfer of large files or folder contents from the administrator to client machines, with SHA-256 checksum
  verification for data integrity across different file system architectures.
- Real-time desktop screen access for administrators, useful during error detection and recovery.
- Remote software installation on client systems, supporting efficient version control and updates.

---

## External Libraries Used

- [**Hashids**](https://github.com/jiecao-fm/hashids-java): A lightweight, open-source library that generates short,
  unique, non-sequential IDs from numbers. It is used to create UIDs for each connected client.

---

## Installation / Removal

Remote Desktop Administration (RDA) has independent packages for both the **client** and **server**. The project was
developed using [**Liberica JDK 16.0.1**](https://bell-sw.com/), which includes JavaFX by default.

> ⚠️ **Recommended**: Use Liberica JDK as it comes bundled with JavaFX.

### 1. JAR Packages

```sh
  java -jar <parent-path>/artifacts/Jar/rda-client.jar
```

```sh
  java -jar <parent-path>/artifacts/Jar/rda-server.jar
```

---

### 2. Linux (.deb) Packages

#### Installation

```sh
  sudo apt-get install '<parent-path>/artifacts/Platform specific package/Linux (deb)/rda-client_1.0-1_amd64.deb'
```

```sh
  sudo apt-get install '<parent-path>/artifacts/Platform specific package/Linux (deb)/rda-server_1.0-1_amd64.deb'
```

> 📂 **Log Location**: `~/.log`

#### Removal

```sh
  sudo apt-get remove rda-client
```

```sh
  sudo apt-get remove rda-server
```

> ⚠️ If the log file is not deleted automatically, remove it manually from `~/.log`.

---

### 3. Windows (.exe) Packages

#### Installation & Removal

- Double-click [rda-client-1.0.exe](artifacts/Platform%20specific%20package/Windows%20(exe)/rda-client-1.0.exe) to
  install/uninstall the **client**.
- Double-click [rda-server-1.0.exe](artifacts/Platform%20specific%20package/Windows%20(exe)/rda-server-1.0.exe) to
  install/uninstall the **server**.

> ⚠️ **Run as Administrator** after installation to enable File Logger.

> 📂 **Log Location**:
> - `%ProgramFiles%\rda-client\.log`
> - `%ProgramFiles%\rda-server\.log`  
    > *(if the default installation directory is used)*

> ⚠️ If the log file is not deleted automatically, remove it manually.

---

## Contributors

- [Kabindra Kattel](https://github.com/KabindraKattel)
- [Manoj Rokaya](https://github.com/manoj014)
- [Parag Pandit](https://github.com/Parag2054)
- [Sitaram Oli](https://github.com/sitaramoli)
