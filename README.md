# WaMi - WhatsApp Multi-Device Android Client

WaMi is a feature-rich, open-source Android client designed to interact with a custom WhatsApp backend server. It provides a native mobile experience for messaging, contact management, and session handling, built with modern Android development practices.

The application is architected with an offline-first approach, ensuring a seamless user experience by caching conversations, messages, and session data locally. It leverages Kotlin, Coroutines, and a ViewModel-driven UI layer for a reactive and robust performance.

---

## ✨ Key Features

- **Real-time Messaging**: Send and receive text messages, media (images/videos), and emoji reactions instantly.
- **Secure Authentication**: Easy login via QR code scanning.
- **Session Management**: Robust session handling with support for session import/export, allowing you to move your session between devices without re-scanning the QR code.
- **Offline-First Architecture**: Access your conversations and messages even without an internet connection. The app synchronizes automatically when connectivity is restored.
- **Conversation Management**: View all your individual and group chats, sorted by the most recent message. Hide conversations you don't want to see in the main list.
- **Rich Media Experience**: In-built full-screen media viewer for images and videos.
- **Modern UI**: A clean, intuitive interface with support for both Light and Dark modes.
- **Advanced Configuration**:
  - Switch between primary and fallback servers.
  - Set a custom server IP for development or private deployments.
  - Toggle offline mode manually.
- **Notifications**: Receive system notifications for new messages, even when the app is in the background.

---

## 🏛️ Architecture & Tech Stack

WaMi is built using the **MVVM** (Model-View-ViewModel) architecture pattern, ensuring a clean separation of concerns between the UI, business logic, and data layers.

- **UI Layer** (Activity/Fragment): Observes data from the ViewModel and renders the UI state. It captures user input and forwards it to the ViewModel.
- **ViewModel Layer** (ViewModel): Holds and manages UI-related data in a lifecycle-conscious way. It exposes data streams (using StateFlow) and handles user actions, delegating data operations to the repository/storage layer.
- **Data Layer** (ApiClient, storage classes): A combination of a remote data source (ApiClient) and local data sources (MessageStorage, ContactStorage, ServerConfigStorage). This layer is responsible for fetching data from the network and caching it locally.

---

## 💻 Tech Stack

- **Language**: Kotlin
- **Asynchronous Programming**: Kotlin Coroutines for managing background threads and asynchronous operations.
- **Architecture Components**:
  - ViewModel: To store and manage UI-related data.
  - Lifecycle: To create lifecycle-aware components.
- **Networking**:
  - Retrofit: For type-safe HTTP requests to the backend REST API.
  - Socket.IO Client: For real-time, bidirectional communication with the server (e.g., receiving new messages, status updates).
  - OkHttp: As the underlying HTTP client, with interceptors for logging and authentication.
- **UI**:
  - AndroidX Libraries: Core toolkit for Android development.
  - RecyclerView: For displaying lists of conversations and messages efficiently.
  - View Binding: To interact with views in a null-safe and type-safe way.
- **Image Loading**: Glide for loading, caching, and displaying images and media thumbnails.
- **Data Serialization**: Gson for converting JSON data to and from Kotlin objects.
- **Storage**: SharedPreferences for simple key-value storage of settings, contacts, and messages.

---

## 📂 Code Overview

The project is structured to separate functionalities into distinct, manageable components.

| File/Path | Description |
|---|---|
| `LoginActivity.kt` | Handles user authentication. Implements an offline-first login flow. If a valid local session exists, it proceeds directly to `MainActivity`. Otherwise, it initiates the QR code fetching and polling process from the server. Also handles session import from a file. |
| `MainActivity.kt` | The main screen. Displays the list of recent conversations using a `RecyclerView`. It leverages `ConversationListViewModel` to load, observe, and update the conversation list in real-time. Manages navigation and top-level UI actions. |
| `ChatActivity.kt` | The core messaging screen. Displays messages for a specific contact or group. Uses `ChatViewModel` to manage message history, send new messages (text/media), and listen for real-time updates. Intelligently adds date dividers to the chat list. |
| `ContactsActivity.kt` | Displays all contacts/conversations. Fetches a complete list of conversations from the server, persists them locally using `ContactStorage`, and displays them. Serves as a full contact book. |
| `SettingsActivity.kt` | Manages user preferences. Provides UI for toggling dark mode, enabling/setting a custom server IP, toggling offline mode, and initiating session export and logout. |
| `MediaViewActivity.kt` | Full-screen media viewer. A simple, immersive activity to display images and play videos passed via URL. |
| `network/ApiClient.kt` | Singleton for all network operations. Manages Retrofit and Socket.IO instances. It injects the session token into API requests, provides different OkHttpClient configurations (for normal vs. long-running downloads), and centralizes socket lifecycle management. |
| `network/SocketManager.kt` | Manages the Socket.IO connection. Listens for real-time server events like `whatsapp-message` and `whatsapp-message-status-update`. It parses the incoming data, maps it to local models, and broadcasts it to the app using `SharedFlow`. |
| `storage/ServerConfigStorage.kt` | Manages server and session configuration. Persists the primary, fallback, and custom server URLs. Crucially, it saves and retrieves the user's sessionId and login state. Includes logic to format URLs correctly for Retrofit and Socket.IO. |
| `storage/MessageStorage.kt` | Local cache for chat messages. Saves and retrieves lists of messages for each conversation using `SharedPreferences` and Gson. Provides methods to add new messages and update the status of existing ones. |
| `storage/ContactStorage.kt` | Local cache for contacts. Persists the list of contacts/conversations fetched from the server. |
| `ui/viewmodel/*ViewModel.kt` | ViewModels for `MainActivity` and `ChatActivity`. They fetch data from the network/storage, manage the UI state (isLoading, error, data lists), and expose it to the UI via `StateFlow`. |
| `adapter/*Adapter.kt` | `RecyclerView` Adapters. These classes bind the data (conversations, messages) to the UI lists, handling view creation and updates efficiently. |

---

## 🔌 Backend Requirement

This is a client-side application only. To be fully functional, WaMi requires a compatible backend server. The server is responsible for:

- Connecting to the official WhatsApp Web API.
- Providing RESTful endpoints for:
  - `/session/create`: To generate a new session ID.
  - `/session/import`: To upload and restore a session from a file.
  - `/session/export`: To download the current session as a zip file.
  - `/status`: To get the current connection status and a new QR code.
  - `/contacts` or `/conversations`: To fetch the list of all conversations.
  - `/chat/history/{jid}`: To get the message history for a specific chat.
  - `/send/text`, `/send/media`: To send messages.

- Running a Socket.IO server that emits events for real-time updates, such as:
  - `whatsapp-message`: When a new message is received.
  - `whatsapp-message-status-update`: When a message's status changes (e.g., from sent to delivered).

---

## 🚀 Setup and Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/rad1914/WaMi.git

2. (Optional) Build your own Private Server:
   https://github.com/rad1914/WaMi-Backend
   
---

~ Made with <3 by @RADWrld
