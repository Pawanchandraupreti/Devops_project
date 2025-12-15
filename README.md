# 💬 Java Socket-Based Chat Application

A real-time **client–server chat application** built using **Java Sockets**, **Multithreading**, and **Swing GUI**.
Multiple clients can connect to a central server and exchange messages instantly through a clean, modern chat interface.

---

## 🚀 Features

* Real-time messaging using TCP sockets
* Multiple client support
* Modern chat UI with message bubbles
* Separate server and client architecture
* Lightweight & fast
* Built using pure Java (no external frameworks)

---

## 🛠️ Technologies Used

* Java (JDK 17)
* Java Sockets (`ServerSocket`, `Socket`)
* Multithreading
* Swing (GUI)
* Maven (Build Tool)

---

## 📂 Project Structure

```
chat-application/
│
├── src/main/java/com/chatapp/
│   ├── ChatServer.java
│   ├── ChatClient.java
│   └── ChatClientGUI.java
│
├── pom.xml
└── README.md
```

---

## ▶️ How to Run the Project

### 1️⃣ Start the Server

Run the server first:

```bash
java -cp target/classes chatapp.ChatServer
```

Server runs on **port 5000** by default.

---

### 2️⃣ Run the Client (GUI)

After building the project:

```bash
java -jar target/chat-application-1.0.jar
```

Run this command multiple times to open multiple chat clients.

---

## 🔧 Build Using Maven

```bash
mvn clean package
```

The generated JAR file will be available in the `target/` directory.

---

## 📸 Screenshots

> Modern chat UI inspired by popular messaging applications
> (Add screenshots here)

---

## 📌 Future Enhancements

* User authentication
* Message timestamps
* Emojis & file sharing
* Encrypted communication
* Online/offline status

---

## 👨‍💻 Author

**Pawan Chandra Upreti**

---

## 📄 License

This project is for educational purposes.
