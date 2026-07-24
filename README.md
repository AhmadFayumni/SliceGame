# Fruit Ninja: Dojo Edition

A multi-client networked fruit-slicing game built in Java with a server-authoritative game engine, AES-encrypted messaging, image-puzzle two-factor authentication, real-time chat, and a live leaderboard.

Developed as a mini project for **NMK30703 Programming for Networking**, Faculty of Electronic Engineering and Technology (FKTEN), Universiti Malaysia Perlis.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
- [Playing over a LAN](#playing-over-a-lan)
- [How to Play](#how-to-play)
- [Chat Commands](#chat-commands)
- [Communication Protocol](#communication-protocol)
- [Security Notes](#security-notes)
- [Team](#team)
- [License](#license)

---

## Overview

Fruit Ninja: Dojo Edition is a client-server application built on Java TCP sockets. A single server process hosts the game world, spawns fruit, validates every slice, and broadcasts state to all connected clients. Clients are Swing GUI applications that render the game, send player input, and exchange chat messages.

All traffic between client and server is encrypted with AES before being written to the socket, and every login is followed by a server-generated image-puzzle challenge that acts as a second authentication factor.

A round requires **at least two connected players** to begin and lasts **60 seconds**.

---

## Features

**Networking**
- Multi-threaded TCP server on port `12345`, one handler thread per client
- Concurrent client registry using `CopyOnWriteArrayList`
- Broadcast and targeted unicast message delivery
- Graceful handling of disconnects and port-in-use errors

**Security**
- AES symmetric encryption applied to every message in both directions
- Base64 transport encoding so messages remain line-safe
- Two-factor authentication: a 3x3 image puzzle is shuffled server-side after a correct password, and the client must reassemble it before the session is authenticated
- Commands are rejected until the client is fully authenticated

**Game Engine (server-authoritative)**
- Fixed-tick loop (approximately 33 FPS) driving physics, spawning, and timing
- Four fruit types with different base scores (10 / 12 / 15 / 20 points)
- Projectile motion with gravity; fruit expires after 4 seconds
- Combo system: 3+ consecutive slices within 1 second grants a multiplier, 5+ triggers a 5-second Frenzy with faster spawn rates
- All hit detection and scoring performed on the server, so clients cannot forge scores

**Client**
- Animated splash screen, login and registration, MFA panel, and game screen via `CardLayout`
- Custom-painted game canvas with floating score text and combo banners
- Real-time chat sidebar with private messaging
- Live top-5 leaderboard
- Retry button to start a new round after time expires

---

## Architecture

```
+---------------------+                       +----------------------------+
|  FruitNinjaClient   |  AES + Base64 / TCP   |          Server            |
|  (Swing GUI)        | <===================> |  ServerSocket :12345       |
|   - GamePanel       |                       |   - ClientHandler (thread) |
|   - CryptoUtil      |                       |   - GameEngine (thread)    |
|   - SplashScreen    |                       |   - Leaderboard            |
+---------------------+                       |   - CryptoUtil             |
                                              +----------------------------+
          ^                                                 |
          |                 broadcast to all clients        |
          +-------------------------------------------------+
```

The server owns all authoritative state: fruit positions, scores, round timer, and player sessions. Clients render a local copy and forward only intent (`SLASH`, `CHAT`, `RESTART`).

---

## Project Structure

```
SliceGame/
├── src/
│   ├── client/
│   │   ├── FruitNinjaClientGUI.java   # Main client window, protocol handler
│   │   ├── GamePanel.java             # Game canvas, rendering, input
│   │   ├── SplashScreen.java          # Startup splash, launches the client
│   │   └── CryptoUtil.java            # Client-side AES helper
│   └── server/
│       ├── Server.java                # Socket accept loop, auth, MFA, broadcast
│       ├── ClientHandler.java         # Per-client thread and command dispatch
│       ├── GameEngine.java            # Game loop, spawning, scoring, rounds
│       ├── Fruit.java                 # Fruit entity and physics
│       ├── Leaderboard.java           # Score aggregation and ranking
│       └── CryptoUtil.java            # Server-side AES helper
├── nbproject/                         # NetBeans project metadata
├── build.xml                          # Ant build script
└── README.md
```

---

## Requirements

| Component | Version |
|---|---|
| JDK | 17 or newer (the code uses switch expressions) |
| IDE | Apache NetBeans 12+ (recommended) |
| Build | Apache Ant (bundled with NetBeans) |
| OS | Windows, macOS, or Linux |

No external libraries are required. The project uses only the Java standard library (`java.net`, `javax.crypto`, `javax.swing`).

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/AhmadFayumni/SliceGame.git
cd SliceGame
```

### 2. Run in NetBeans

1. Open the project folder in NetBeans (**File > Open Project**).
2. Right-click `src/server/Server.java` and select **Run File**. Wait for the console to print that the server has started on port 12345.
3. Right-click `src/client/SplashScreen.java` and select **Run File**.
4. Repeat step 3 to launch a **second client**. A round will not begin until two players are authenticated.

### 3. Run from the command line

```bash
# Compile
mkdir -p build
javac -d build src/server/*.java src/client/*.java

# Terminal 1 - start the server
java -cp build server.Server

# Terminal 2 and 3 - start two clients
java -cp build client.SplashScreen
```

> The server must be started before any client. If a client is launched first it will report that the server is not running.

---

## Playing over a LAN

By default the client connects to `127.0.0.1`. To play across multiple machines on the same network:

1. On the host machine, find the local IPv4 address (`ipconfig` on Windows, `ip addr` on Linux/macOS), for example `192.168.1.10`.
2. In `src/client/FruitNinjaClientGUI.java`, locate `connectToServer()` and change the socket address:

   ```java
   socket = new Socket("192.168.1.10", 12345);
   ```

3. Rebuild and distribute the client to each machine.
4. Allow inbound TCP traffic on port `12345` through the host firewall.
5. All machines must be on the same subnet.

---

## How to Play

1. **Register** a new account, or log in with one of the built-in test accounts.
2. **Solve the puzzle.** Click any two tiles to swap them. Rebuild the original image and the client automatically submits the solution for verification.
3. **Wait for a second player.** The round starts once two players are authenticated.
4. **Slice fruit** by clicking on it before it falls off-screen. Missing resets your combo.
5. **Chat** with the other player in the sidebar while you play.
6. When the 60-second timer ends, view the leaderboard and press **Retry** for another round.

### Default test accounts

| Username | Password |
|---|---|
| `admin` | `admin` |
| `user1` | `pass1` |
| `user2` | `pass2` |

### Scoring

| Fruit type | Base points |
|---|---|
| 0 - Apple | 10 |
| 1 - Orange | 12 |
| 2 - Banana | 15 |
| 3 - Watermelon | 20 |

Slices landed within one second of each other build a combo. A combo of 3 or more applies a multiplier; a combo of 5 or more doubles points and triggers Frenzy mode, which increases the spawn rate for 5 seconds.

---

## Chat Commands

| Command | Description |
|---|---|
| `/tell <username> <message>` | Send a private message to another online player |
| `/deleteaccount` | Permanently remove your account and disconnect |
| Keys `1` - `5` | Send a preset quick message while the game canvas is focused |

---

## Communication Protocol

Messages are plain text with pipe-delimited fields, AES-encrypted and Base64-encoded before transmission. Each message occupies one line.

**Client to server**

| Command | Format |
|---|---|
| Register | `REGISTER\|username\|password` |
| Login | `LOGIN\|username\|password` |
| MFA answer | `MFA_CONFIRM\|0,1,2,3,4,5,6,7,8` |
| Slice fruit | `SLASH\|fruitId` |
| Chat | `CHAT\|message` |
| Private message | `TELL\|username\|message` |
| Request leaderboard | `GET_LEADERBOARD` |
| Restart round | `RESTART` |
| Delete account | `DELETE_ACCOUNT` |

**Server to client**

| Message | Format |
|---|---|
| MFA challenge | `MFA_REQUIRED\|shuffledOrder` |
| Auth result | `AUTH_SUCCESS` / `AUTH_FAIL\|reason` / `MFA_FAIL\|reason` |
| Fruit spawn | `FRUIT_SPAWN\|id\|x\|y\|vx\|vy\|radius\|type` |
| Fruit sliced | `FRUIT_SLICED\|id\|user\|combo\|points\|bonusTag` |
| Slice feedback | `SLASH_RESULT\|SUCCESS\|text` or `SLASH_RESULT\|MISS\|text` |
| Score update | `SCORE\|value` |
| Round timer | `TIME\|secondsRemaining` |
| Round control | `GAME_STARTING\|text`, `GAME_OVER\|text`, `WAITING\|text` |
| Chat | `CHAT\|sender\|message` |
| Private | `PRIVATE\|sender\|message` |
| Leaderboard | `LEADERBOARD_DATA\|user:score,user:score,...` |
| Error | `ERROR\|reason` |

---

## Security Notes

This project demonstrates applied network security concepts within an academic scope. The following are known limitations and are **not** suitable for production use:

- AES is used in ECB mode with a hardcoded 128-bit key shared by both client and server. A production system would use a per-session key negotiated via TLS or a key exchange, with an authenticated mode such as GCM.
- Passwords are stored in memory as plain text and are not persisted. A production system would store a salted hash (bcrypt, scrypt, or Argon2).
- The image-puzzle factor is a possession/knowledge challenge issued over the same channel; it is not equivalent to TOTP or a hardware token.
- The user store is reset every time the server restarts.

---

## Team

| No. | Name |
|---|---|
| 1 | Mohamad Ibrahim Adham bin Rohaidi |
| 2 | Ahmad Fayumni bin Mohamad Nasir |
| 3 | Muhammad Afif Izzuddin bin Mohd Najib |
| 4 | Muhammad Ayman Hazim bin Mohd Hairy |

**Course:** NMK30703 Programming for Networking, Semester II 2024/2025
**Programme:** UR6523009
**Lecturers:** Dr. Mohamed Elshaikh Elobaid Said Ahmed, Ts. Dr. Abdul Hafiizh Bin Ismail
