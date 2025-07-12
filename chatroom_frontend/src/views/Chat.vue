<template>
  <div class="chat-page">
    <div class="sidebar">
      <div class="sidebar-user">
        <span class="sidebar-username">{{ currentUser && currentUser.username }}</span>
        <span class="sidebar-self">(自己)</span>
      </div>
      <div class="sidebar-section">
        <div class="sidebar-title">用户列表</div>
        <UserList :users="sortedUsers" @select="selectUser" />
      </div>
      <div class="sidebar-section">
        <div class="sidebar-title">群组列表</div>
        <GroupList :groups="groups" @select="selectGroup" />
      </div>
    </div>
    <div class="main-chat">
      <ChatWindow :messages="messages" :chatUser="selectedUser" :currentUserId="currentUser && currentUser.id">
        <div class="chat-input-area">
          <input v-model="inputMsg" @keyup.enter="send" placeholder="输入消息..." />
          <button @click="send">发送</button>
        </div>
      </ChatWindow>
    </div>
  </div>
</template>

<script>
import UserList from '../components/UserList.vue'
import GroupList from '../components/GroupList.vue'
import ChatWindow from '../components/ChatWindow.vue'
import { mapGetters } from 'vuex'
import { connectWebSocket, subscribe, disconnectWebSocket } from '../services/websocket.js'

export default {
  name: 'ChatPage',
  components: { UserList, GroupList, ChatWindow },
  computed: {
    ...mapGetters('auth', ['currentUser']),
    sortedUsers() {
      if (!this.currentUser) return this.users;
      const others = this.users.filter(u => u.id !== this.currentUser.id);
      return [this.currentUser, ...others];
    }
  },
  data() {
    return {
      users: [],
      groups: [],
      messages: [],
      inputMsg: '',
      selectedUser: null,
      wsSubscription: null,
      wsConnected: false
    }
  },
  methods: {
    async selectUser(user) {
      try {
        this.selectedUser = user
        const { fetchMessages } = await import('../services/chat.service.js')
        const res = await fetchMessages({ receiverId: user.id, userId: this.currentUser && this.currentUser.id })
        this.messages = res.data.slice().sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
        // WebSocket: suscribirse al canal privado
        if (!this.wsConnected) {
          connectWebSocket('http://localhost:8080/ws', null, () => {
            this.wsConnected = true
            this.subscribeToUserChannel()
          })
        } else {
          this.subscribeToUserChannel()
        }
      } catch (e) {
        console.error('Error en selectUser:', e)
      }
    },
    subscribeToUserChannel() {
      if (this.wsSubscription) {
        this.wsSubscription.unsubscribe()
      }
      if (this.selectedUser && this.currentUser) {
        // Canal privado entre dos usuarios, ejemplo: /user/{userId}/queue/messages
        const topic = `/user/${this.currentUser.id}/queue/messages`
        this.wsSubscription = subscribe(topic, (msg) => {
          // Si el mensaje es relevante para la conversación actual, recargar mensajes
          if (
            (msg.sender && msg.sender.id === this.selectedUser.id) ||
            (msg.receiver && msg.receiver.id === this.selectedUser.id)
          ) {
            this.reloadMessages()
          }
        })
      }
    },
    async reloadMessages() {
      const { fetchMessages } = await import('../services/chat.service.js')
      const res = await fetchMessages({ receiverId: this.selectedUser.id, userId: this.currentUser && this.currentUser.id })
      this.messages = res.data.slice().sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
    },
    selectGroup() {
      // 切换到群聊逻辑
    },
    async send() {
      if (this.inputMsg.trim() && this.selectedUser) {
        const { sendMessage } = await import('../services/chat.service.js')
        const content = this.inputMsg
        await sendMessage({ receiverId: this.selectedUser.id, senderId: this.currentUser.id, content })
        // Añadir el mensaje localmente para el emisor
        this.messages.push({
          sender: { id: this.currentUser.id, username: this.currentUser.username },
          receiver: { id: this.selectedUser.id, username: this.selectedUser.username },
          content,
          timestamp: new Date().toISOString()
        })
        this.inputMsg = ''
        // No recargar manualmente, WebSocket lo hará para el receptor
      }
    }
  },
  async mounted() {
    const { fetchUsers, fetchGroups } = await import('../services/user.service.js')
    const res = await fetchUsers()
    this.users = res.data
    const groupRes = await fetchGroups()
    this.groups = groupRes.data
  },
  beforeUnmount() {
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe()
    }
    disconnectWebSocket()
  }
}
</script>

<style scoped>
.chat-page {
  display: flex;
  max-width: 1000px;
  margin: 40px auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  min-height: 600px;
}
.sidebar {
  width: 240px;
  border-right: 1px solid #eee;
  padding: 0 0 0 0;
  background: #fafbfc;
  display: flex;
  flex-direction: column;
  align-items: stretch;
}
.sidebar-user {
  background: #eaf6ff;
  padding: 18px 0 12px 0;
  text-align: center;
  border-bottom: 1px solid #e0e0e0;
  font-size: 1.15em;
  font-weight: bold;
  letter-spacing: 1px;
}
.sidebar-username {
  color: #409eff;
  font-weight: bold;
}
.sidebar-self {
  color: #888;
  font-size: 0.95em;
  margin-left: 4px;
}
.sidebar-section {
  padding: 18px 0 0 0;
  border-bottom: 1px solid #e0e0e0;
}
.sidebar-title {
  font-size: 1.05em;
  font-weight: bold;
  color: #222;
  margin: 0 0 8px 24px;
  letter-spacing: 1px;
}
.main-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
}
.chat-input-area {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.chat-input-area input {
  flex: 1;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #ccc;
}
.chat-input-area button {
  padding: 8px 16px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>