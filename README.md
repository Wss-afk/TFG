# Chatroom Application (TFG)

A comprehensive real-time chat application built with a modern tech stack, featuring direct messaging, group chats, admin controls, and persistent read status tracking.

## 🚀 Features

### Core Chat Functionality
- **Real-time Messaging**: Powered by WebSocket (STOMP) for instant communication.
- **Direct & Group Chats**: Create groups, invite members, or chat one-on-one.
- **Read Status & Unread Counts**: 
  - Tracks the last read message for each user in every group.
  - Displays accurate unread message counters in the sidebar.
- **User Status**: Real-time Online/Offline status indicators.

### Admin & Management
- **Admin Dashboard**: Specialized interface for administrators.
- **User Management**: View, update, or ban users.
- **Group Management**: Monitor and manage chat groups.
- **Audit Logs**: Track important system events and user actions.

### Technical Highlights
- **Responsive UI**: Built with Vue 3 and Bootstrap for mobile and desktop support.
- **Secure Authentication**: Spring Security integration.
- **Data Persistence**: MySQL with Spring Data JPA.
- **File Uploads**: Support for avatar and image sharing.

## 🛠 Tech Stack

### Backend
- **Framework**: Spring Boot 3.2.5
- **Language**: Java 21
- **Database**: MySQL 8.0
- **ORM**: Spring Data JPA
- **WebSocket**: Spring WebSocket (STOMP)
- **Security**: Spring Security

### Frontend
- **Framework**: Vue.js 3
- **State Management**: Vuex
- **Routing**: Vue Router
- **HTTP Client**: Axios
- **WebSocket Client**: `webstomp-client` / `sockjs-client`
- **Styling**: Bootstrap 5

## ⚙️ Prerequisites

Ensure you have the following installed:
- **Java Development Kit (JDK) 21**
- **Node.js** (v16+ recommended) & **npm**
- **MySQL Server**

## 📦 Installation & Setup

### 1. Database Setup
Create a MySQL database named `chatroom`.
*Note: The default configuration expects MySQL on port `3308` with username `root` and password `12345678`. You can change this in `chatroom_backend/src/main/resources/application.yml`.*

```sql
CREATE DATABASE chatroom;
```

### 2. Backend Setup
Navigate to the backend directory and run the Spring Boot application.

```bash
cd chatroom_backend
mvn spring-boot:run
```
The server will start on `http://localhost:8080`.

### 3. Frontend Setup
Navigate to the frontend directory, install dependencies, and start the development server.

```bash
cd chatroom_frontend
npm install
npm run serve
```
The application will be accessible at `http://localhost:8081` (or the port specified in your terminal).

## 🧩 Architecture Insights

### Read Status Tracking
The application uses a specific entity `GroupReadStatus` to efficiently track unread messages without scanning the entire message history.
- **Location**: `com.chatroom.entity.GroupReadStatus`
- **Logic**: Stores the `lastReadMessageId` for a unique pair of `userId` and `groupId`.
- **Controller**: `ChatController` handles endpoints like `/unread-count` and `/group/mark-read` to update this status in real-time.

### WebSocket Events
- **Backend**: `WebSocketEventListener` tracks session connects/disconnects to manage online user lists.
- **Frontend**: Subscribes to topics like `/topic/public` (global events) and `/user/queue/messages` (private updates).

## 📂 Project Structure
