# Remote-Desktop-Administration
![Logo](icons/logo.svg)

Remote Desktop Administration is a JAVA based cross-platform client server stand-alone application with distributed message passing using JAVAFX where a central administrator acts as a server providing services to the connected clients within LAN / WLAN locations. 


## Features

 - Implementation of  Console Handler, File Handler, UI Event Handler for Log Recording.
 - Instant dual messaging service to allow communication between administrator and client(s).
 - Remote control access to administrator to remotely shutdown, restart, logoff client's computer(s).
 - Large file(s) or folder content(s) transfer remotely from administrator to client(s) computer masking the differences in file system architecture with SHA-256 Checksum verification in-between sent and received file.
 - Administrator full access to read client(s) desktop screen which is very much useful specially at the time of error detection and recovery.
 - Remote software installation at  client(s) end at a time which is very useful for software version control and update.
 

## External Libraries Used

Hashids, a [small open-source library](https://github.com/jiecao-fm/hashids-java) that generates short, unique, non-sequential ids from numbers, is used to create UIDs for each connected client.


## Installation / Removal

Remote Desktop Administration (RDA) has independent packages for client and server. The project was created using [Liberica JDK 16.0.1](https://bell-sw.com/).

> I recommend Liberica JDK as JavaFX is pre-bundled inside it.

Following commands are relative to this repository.

1. Jar Package

   ```sh
    java -jar <parent-path>/artifacts/Jar/rda-client.jar   
   ```
     ```sh
    java -jar <parent-path>/artifacts/Jar/rda-server.jar   
   ```

2. Linux (.deb) Package

   *Installation*

   ```sh
    sudo apt-get install '<parent-path>/artifacts/Platform specific package/Linux (deb)/rda-client_1.0-1_amd64.deb'  
    ```
     ```sh
      sudo apt-get install '<parent-path>/artifacts/Platform specific package/Linux (deb)/rda-server_1.0-1_amd64.deb'  
    ```
   > Note: *File Logger is present at location `~/.log`*
   >
   *Removal*

   ```sh
    sudo apt-get remove rda-client  
   ```
   ```sh
    sudo apt-get remove rda-server  
   ```
   > Note: *File Logger is present at location `~/.log`*
   >
   > If File Logger is not deleted upon removal, delete manually.
3. Windows (.exe) Package

   *Installation and Removal*

   > Double Click [rda-client-1.0.exe](artifacts/Platform%20specific%20package/Windows%20(exe)/rda-client-1.0.exe) for installing and removing Remote Desktop Administration Client

   > Double Click [rda-server-1.0.exe](artifacts/Platform%20specific%20package/Windows%20(exe)/rda-server-1.0.exe) for installing and removing Remote Desktop Administration Server

   Note: *After installation of related package, run as administrator to enable File Logger*
   >
   >Note: *File Logger is present at location `%ProgramFiles%\rda-client\.log` and/or `%ProgramFiles%\rda-server\.log`if default installation directory is not altered*
   >
   >If File Logger is not deleted upon removal, delete manually.

## Contributors

- [Kabindra Kattel](https://github.com/KabindraKattel)
- [Manoj Rokaya](https://github.com/manoj014)
- [Sitaram Oli](https://github.com/Sitaramoli1122)



