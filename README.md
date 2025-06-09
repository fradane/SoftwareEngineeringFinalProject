# Progetto Finale di Ingegneria del Software / Final Project for Software Engineering  
**Corso di Laurea in Ingegneria Informatica – Politecnico di Milano (A.A. 2024/2025)**

---

## Versione Italiana [ENGLISH FOLLOWS]

### Gioco da Tavolo: Galaxy Trucker (by Cranio Creations)

#### Autori del Progetto
- **Marco De Negri**  
  *Email:* [marco.denegri@mail.polimi.it](mailto:marco.denegri@mail.polimi.it)  
- **Alice Berta**  
  *Email:* [alice.berta@mail.polimi.it](mailto:alice.berta@mail.polimi.it)  
- **Luca Di Profio**  
  *Email:* [luca.diprofio@mail.polimi.it](mailto:luca.diprofio@mail.polimi.it)  
- **Francesco Danese**  
  *Email:* [francesco.danese@mail.polimi.it](mailto:francesco.danese@mail.polimi.it)

### Descrizione del Progetto
Questo progetto consiste nello sviluppo di una versione software del gioco da tavolo **Galaxy Trucker**.  
Il software deve:
1. **Gestire** le meccaniche di gioco (dall’assemblaggio della nave spaziale alla fase di volo).
2. **Implementare** le regole di Galaxy Trucker, in conformità con la documentazione fornita (in particolare le “regole semplificate” e le “regole complete”).
3. **Prevedere** un’architettura **client-server** (basata su JavaSE) che sfrutti un protocollo di rete (socket TCP/IP e/o RMI).
4. **Consentire** l’utilizzo del **pattern MVC** per la suddivisione delle responsabilità (Model, View, it.polimi.ingsw.is25am33.Controller).
5. **Fornire** un’interfaccia utente **grafica (GUI)** o **testuale (TUI)** realizzata con JavaFX/Swing (per la GUI) o console (per la TUI).
6. **Essere corredato** di documentazione UML, JavaDoc, test di unità, peer review e documentazione del protocollo di comunicazione.

Inoltre, sono previste **funzionalità avanzate** (opzionali) quali:
- **Volo di prova**: modalità semplificata di partita.
- **Partite multiple**: possibilità per il server di gestire più partite contemporaneamente.
- **Persistenza**: salvataggio periodico dello stato di gioco su disco.
- **Resilienza alle disconnessioni**: meccanismi di riconnessione dei client senza perdere la partita.

### Esecuzione del Jar

Per avviare l’applicazione client da linea di comando è necessario eseguire il file `.jar` con i seguenti parametri:

```
java -jar nomefile.jar [rmi|socket] [cli|gui] [-ip indirizzo_server] [-port porta_server]
```

#### Parametri obbligatori
- `rmi` / `socket` → seleziona il **protocollo di comunicazione**.
- `cli` / `gui` → seleziona il tipo di **interfaccia utente**.

Se questi parametri non vengono forniti, l’applicazione **non verrà eseguita**.

#### Parametri opzionali
- `-ip` → specifica l’indirizzo IP del server (default: `localhost`).
- `-port` → specifica la porta del server (default: `1099` per RMI, `3000` per Socket).


### Struttura e Materiale Richiesto
Il progetto dovrà includere:
- **Diagrammi UML** (di alto livello e di dettaglio).
- **Implementazione** conforme alle regole di Galaxy Trucker.
- **Documentazione** del protocollo di comunicazione.
- **Documenti di peer review** (prima e seconda revisione).
- **Codice sorgente** (in JavaSE).
- **Documentazione JavaDoc** generata dal codice.
- **Test di unità**.

### Strumenti Utilizzati
- **JavaSE** (versione 17 o superiore consigliata)
- **IDE IntelliJ IDEA** (o Eclipse/NetBeans, a scelta del gruppo)
- **Maven** (per la gestione delle dipendenze e il build del progetto)
- **Git** (per il versionamento del codice)
- **UML tool** (ad esempio, Visual Paradigm, StarUML, IntelliJ UML plugin)
- **Swing/JavaFX** (per la GUI)
- **Console** (per la TUI, se implementata)
- **Socket TCP/IP** e/o **RMI** (per la comunicazione client-server)

### Legenda Stato di Sviluppo

| Simbolo | Significato                           |
|---------|---------------------------------------|
| ✅       | **Completato**                       |
| ☑️       | **Pianificato, non ancora iniziato** |
| 🚧       | **In corso di sviluppo**             |
| ❌       | **Non pianificato da implementare**  |

### Tabella Funzionalità e Stato di Implementazione

| Funzionalità                                                     | Stato  |
|------------------------------------------------------------------|:------:|
| Implementazione delle **Regole Semplificate**                    |   🚧   |
| Implementazione delle **Regole Complete**                        |   🚧   |
| **TUI** (interfaccia testuale)                                   |   🚧   |
| **GUI** (interfaccia grafica in Swing/JavaFX)                    |  🚧️   |
| Comunicazione **RMI**                                            |   ✅    |
| Comunicazione **Socket**                                         |   ✅️   |
| **Volo di prova** (funzionalità avanzata)                        |   🚧️    |
| **Partite multiple** (funzionalità avanzata)                     |   ✅️   |
| **Persistenza** (funzionalità avanzata)                          |   ☑️   |
| **Resilienza alle disconnessioni** (funzionalità avanzata)       |   ☑️   |

*Aggiorneremo progressivamente lo stato di sviluppo delle singole funzionalità.*

---

## English Version

### Board Game: Galaxy Trucker (by Cranio Creations)

#### Project Authors
- **Marco De Negri**  
  *Email:* [marco.denegri@mail.polimi.it](mailto:marco.denegri@mail.polimi.it)  
- **Alice Berta**  
  *Email:* [alice.berta@mail.polimi.it](mailto:alice.berta@mail.polimi.it)  
- **Luca Di Profio**  
  *Email:* [luca.diprofio@mail.polimi.it](mailto:luca.diprofio@mail.polimi.it)  
- **Francesco Danese**  
  *Email:* [francesco.danese@mail.polimi.it](mailto:francesco.danese@mail.polimi.it)

### Project Description
This project involves developing a software version of the board gameModel **Galaxy Trucker**.  
The software must:
1. **Handle** the gameModel mechanics (from assembling the spaceship to the flight phase).
2. **Implement** the rules of Galaxy Trucker, following the provided documentation (specifically both the "simplified rules" and "complete rules").
3. **Adopt** a **client-server** architecture (based on JavaSE) that uses a network protocol (TCP/IP sockets and/or RMI).
4. **Utilize** the **MVC pattern** (Model, View, it.polimi.ingsw.is25am33.Controller) to separate responsibilities.
5. **Provide** a user interface in either **Graphical (GUI)** or **Textual (TUI)** form, using JavaFX/Swing (for GUI) or the console (for TUI).
6. **Include** supporting materials such as UML documentation, JavaDoc, unit tests, peer review documents, and communication protocol documentation.

Additionally, the project may feature **advanced functionalities** (optional) such as:
- **Test Flight**: A simplified gameModel mode.
- **Multiple Games**: The ability for the server to manage several games simultaneously.
- **Persistence**: Periodic saving of the gameModel state to disk.
- **Disconnection Resilience**: Mechanisms to allow clients to reconnect without losing their gameModel session.

### Running the Jar

To start the client application from the command line, you must execute the `.jar` file with the following parameters:

```
java -jar filename.jar [rmi|socket] [cli|gui] [-ip server_address] [-port server_port]
```

#### Required parameters
- `rmi` / `socket` → selects the **communication protocol**.
- `cli` / `gui` → selects the **user interface** type.

If these parameters are not provided, the application **will not start**.

#### Optional parameters
- `-ip` → specifies the server IP address (default: `localhost`).
- `-port` → specifies the server port (default: `1099` for RMI, `3000` for Socket).


### Project Materials and Deliverables
The project should include:
- **UML Diagrams** (both high-level and detailed).
- **Implementation** adhering to the Galaxy Trucker rules.
- **Documentation** of the communication protocol.
- **Peer Review Documents** (first and second review).
- **Source Code** (in JavaSE).
- **JavaDoc Documentation** generated from the code.
- **Unit Tests**.

### Tools and Technologies
- **JavaSE** (version 17 or higher is recommended)
- **IDE IntelliJ IDEA** (or Eclipse/NetBeans, as per team preference)
- **Maven** (for dependency management and project build)
- **Git** (for version control)
- **UML Tools** (e.g., Visual Paradigm, StarUML, IntelliJ UML plugin)
- **Swing/JavaFX** (for GUI)
- **Console** (for TUI, if implemented)
- **TCP/IP Sockets** and/or **RMI** (for client-server communication)

### Development Status Legend

| Symbol | Meaning                                    |
|--------|--------------------------------------------|
| ✅      | **Completed**                              |
| ☑️      | **Planned, not started yet**               |
| 🚧      | **In development**                         |
| ❌      | **Not planned for implementation**         |

### Features and Implementation Status Table

| Feature                                                          | Status |
|------------------------------------------------------------------|:------:|
| Implementation of **Simplified Rules**                           |   🚧   |
| Implementation of **Complete Rules**                             |   🚧   |
| **TUI** (Textual User Interface)                                 |   🚧   |
| **GUI** (Graphical User Interface using Swing/JavaFX)            |   🚧   |
| **RMI Communication**                                            |   ✅️   |
| **Socket Communication**                                         |   ✅️   |
| **Test Flight** (advanced functionality)                         |   🚧️    |
| **Multiple Games** (advanced functionality)                      |   ✅️   |
| **Persistence** (advanced functionality)                         |   ☑️   |
| **Disconnection Resilience** (advanced functionality)            |   ☑️   |

*The development status of each feature will be updated progressively.*