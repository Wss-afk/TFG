<template>
  <div class="chat-page">
    <div class="sidebar">
      <div class="sidebar-user">
        <span class="sidebar-username">{{ currentUser && currentUser.username }}</span>
        <span class="sidebar-self">(自己)</span>
      </div>
      <div class="sidebar-section">
        <div class="sidebar-title">用户列表</div>
        <UserList :users="sortedUsers" :unreadCounts="unreadCounts" :selectedUser="selectedUser" :onlineUsers="onlineUsers" @select="selectUser" />
      </div>
      <div class="sidebar-section">
        <div class="sidebar-title">群组列表</div>
        <GroupList :groups="groups" @select="selectGroup" />
      </div>
    </div>
    <div class="main-chat">
      <ChatWindow 
        :messages="messages" 
        :chatUser="selectedUser" 
        :chatGroup="selectedGroup"
        :chatType="chatType"
        :currentUserId="currentUser && currentUser.id">
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
import { disconnectWebSocket, setOnlineUsersCallback } from '../services/websocket.js'

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
      selectedGroup: null, // Grupo seleccionado
      chatType: 'user', // 'user' o 'group'
      globalSubscription: null,
      groupSubscriptions: {}, // Suscripciones a grupos
      wsConnected: false,
      unreadCounts: {}, // 存储每个用户的未读消息数量
      onlineUsers: [] // 存储在线用户列表
    }
  },
  methods: {
    normalizeOnlineUsers(list) {
      if (!Array.isArray(list)) return []
      return list.map(u => {
        const id = (u && (u.id ?? u.userId))
        const username = (u && (u.username ?? u.name ?? u.userName))
        return { id, username }
      }).filter(u => u.id !== undefined && u.username)
    },
    async selectUser(user) {
      try {
        this.selectedUser = user
        
        const { fetchMessages, markMessagesAsRead } = await import('../services/chat.service.js')
        const res = await fetchMessages({ receiverId: user.id, userId: this.currentUser && this.currentUser.id })
        this.messages = res.data.slice().sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
        
        // 标记消息为已读
          if (this.currentUser) {
            await markMessagesAsRead(this.currentUser.id, user.id)
            // 清除该用户的未读消息计数
            this.unreadCounts = { ...this.unreadCounts, [user.id]: 0 }
            console.log(`清除用户${user.id}的未读消息计数`)
          }
      } catch (e) {
        console.error('Error en selectUser:', e)
      }
    },
    async reloadMessages() {
      const { fetchMessages } = await import('../services/chat.service.js')
      const res = await fetchMessages({ receiverId: this.selectedUser.id, userId: this.currentUser && this.currentUser.id })
      this.messages = res.data.slice().sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
    },
    handleNewMessage(message) {
      // 解析 senderId，兼容不同结构
      const senderId = (message && message.sender && (message.sender.id ?? message.sender.userId)) || message.senderId
      const isFromSelf = senderId && this.currentUser && senderId === this.currentUser.id
      // 如果消息不是当前用户发送的，且不是当前对话窗口
      if (!isFromSelf) {
        
        // 如果不是当前选中的用户发送的消息，增加未读计数
        if (senderId && (!this.selectedUser || senderId !== this.selectedUser.id)) {
          const newCount = (this.unreadCounts[senderId] || 0) + 1
          this.unreadCounts = { ...this.unreadCounts, [senderId]: newCount }
          console.log(`用户${senderId}未读消息数量更新为:`, newCount)
        }
        
        // 显示桌面通知
        this.showNotification(message)
        
        // 播放提示音
        this.playNotificationSound()
      }
    },
    showNotification(message) {
      if ('Notification' in window && Notification.permission === 'granted') {
        new Notification(`来自 ${message.sender.username} 的新消息`, {
          body: message.content,
          icon: '/favicon.ico',
          tag: 'chat-message'
        })
      } else if ('Notification' in window && Notification.permission !== 'denied') {
        Notification.requestPermission().then(permission => {
          if (permission === 'granted') {
            this.showNotification(message)
          }
        })
      }
    },
    playNotificationSound() {
      // 创建简单的提示音
      const audioContext = new (window.AudioContext || window.webkitAudioContext)()
      const oscillator = audioContext.createOscillator()
      const gainNode = audioContext.createGain()
      
      oscillator.connect(gainNode)
      gainNode.connect(audioContext.destination)
      
      oscillator.frequency.setValueAtTime(800, audioContext.currentTime)
      oscillator.frequency.setValueAtTime(600, audioContext.currentTime + 0.1)
      
      gainNode.gain.setValueAtTime(0.3, audioContext.currentTime)
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.2)
      
      oscillator.start(audioContext.currentTime)
      oscillator.stop(audioContext.currentTime + 0.2)
    },
    async selectGroup(group) {
      try {
        this.selectedGroup = group
        this.selectedUser = null // Limpiar selección de usuario
        this.chatType = 'group'
        
        const { fetchMessages } = await import('../services/chat.service.js')
        const res = await fetchMessages({ groupId: group.id })
        this.messages = res.data.slice().sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
        
        // Suscribirse a mensajes del grupo
        this.subscribeToGroupChannel(group.id)
      } catch (e) {
        console.error('Error en selectGroup:', e)
      }
    },
    async send() {
      if (this.inputMsg.trim()) {
        const content = this.inputMsg
        
        if (this.chatType === 'user' && this.selectedUser) {
          // Envío de mensaje a usuario individual
          const { sendMessage } = require('../services/websocket.js')
          const message = {
            sender: { id: this.currentUser.id, username: this.currentUser.username },
            receiver: { id: this.selectedUser.id, username: this.selectedUser.username },
            content,
            type: 'text',
            timestamp: new Date().toISOString()
          }
          
          // Enviar a WebSocket
          sendMessage('/app/chat/single', message)
          
          // Agregar mensaje localmente
          this.messages.push(message)
        } else if (this.chatType === 'group' && this.selectedGroup) {
          // Envío de mensaje a grupo
          const { sendMessage } = require('../services/websocket.js')
          const message = {
            sender: { id: this.currentUser.id, username: this.currentUser.username },
            group: { id: this.selectedGroup.id, name: this.selectedGroup.name },
            content,
            type: 'text',
            timestamp: new Date().toISOString()
          }
          
          // Enviar a WebSocket
          sendMessage('/app/chat/group', message)
          
          // Agregar mensaje localmente
          this.messages.push(message)
        }
        
        this.inputMsg = ''
      }
    },
    subscribeToGroupChannel(groupId) {
      const { subscribe } = require('../services/websocket.js')
      const topic = `/topic/group/${groupId}`
      console.log('Suscribiendo a canal de grupo:', topic)
      
      // Desuscribirse del canal anterior si existe
      if (this.groupSubscriptions[groupId]) {
        this.groupSubscriptions[groupId].unsubscribe()
      }
      
      this.groupSubscriptions[groupId] = subscribe(topic, (msg) => {
        console.log('Mensaje de grupo recibido:', msg)
        
        // Agregar mensaje si es del grupo actual
        if (this.selectedGroup && msg.group && msg.group.id === this.selectedGroup.id) {
          this.messages.push(msg)
        }
      })
    },
     
    initWebSocketConnection() {
       if (!this.currentUser) {
         console.error('当前用户信息不存在，无法建立WebSocket连接')
         return
       }
      
      const { connectWebSocket } = require('../services/websocket.js')
      connectWebSocket('http://localhost:8080/ws', this.currentUser.username, null, () => {
        console.log('WebSocket连接成功')
        this.wsConnected = true
        
        // 设置在线用户列表更新回调
      setOnlineUsersCallback((onlineUsers) => {
        const normalized = this.normalizeOnlineUsers(onlineUsers)
        this.onlineUsers = normalized
        console.log('在线用户列表已更新(规范化):', normalized)
      })
        
        // WebSocket连接成功后，订阅当前用户的消息频道
        setTimeout(() => {
          this.subscribeToGlobalUserChannel()
        }, 100) // 稍微延迟确保连接完全建立
      }, (error) => {
        console.error('WebSocket连接失败:', error)
        this.wsConnected = false
      })
    },
    
    subscribeToGlobalUserChannel() {
      if (this.currentUser) {
        const { subscribe } = require('../services/websocket.js')
        const topic = `/user/queue/messages`
        console.log('订阅消息频道:', topic)
        this.globalSubscription = subscribe(topic, (msg) => {
          console.log('收到新消息:', msg)
          // 处理接收到的消息
          this.handleNewMessage(msg)
          
          // 当当前选中用户就是消息发送者，直接追加到当前消息列表，避免延迟
          const senderId = (msg && msg.sender && (msg.sender.id ?? msg.sender.userId)) || msg.senderId
          if (this.selectedUser && senderId === this.selectedUser.id) {
            this.messages.push(msg)
          }
        })
        
        if (this.globalSubscription) {
          console.log('全局消息订阅成功')
        } else {
          console.error('全局消息订阅失败')
        }
      } else {
        console.error('当前用户信息不存在，无法订阅消息')
      }
    },
    
    async fetchOnlineUsers() {
      try {
        const response = await fetch('http://localhost:8080/api/user/online')
        if (response.ok) {
          const data = await response.json()
          const normalized = this.normalizeOnlineUsers(data)
          this.onlineUsers = normalized
          console.log('获取在线用户列表(规范化):', normalized)
        }
      } catch (error) {
        console.error('获取在线用户列表失败:', error)
      }
    },
    
    async fetchUnreadCounts() {
      if (!this.currentUser) return
      
      try {
        const { getUnreadCount } = await import('../services/chat.service.js')
        const unreadCounts = {}
        
        for (const user of this.users) {
          if (user.id !== this.currentUser.id) {
            try {
              const response = await getUnreadCount(this.currentUser.id, user.id)
              unreadCounts[user.id] = response.data
            } catch (error) {
              console.error(`获取用户${user.id}未读消息数量失败:`, error)
              unreadCounts[user.id] = 0
            }
          }
        }
        
        this.unreadCounts = unreadCounts
        console.log('未读消息数量:', this.unreadCounts)
      } catch (error) {
        console.error('获取未读消息数量失败:', error)
      }
    }
  },
  async mounted() {
    const { fetchUsers, fetchGroups } = await import('../services/user.service.js')
    const res = await fetchUsers()
    this.users = res.data
    const groupRes = await fetchGroups()
    this.groups = groupRes.data
    
    // 获取初始在线用户列表
    await this.fetchOnlineUsers()
    
    // 获取未读消息数量
    await this.fetchUnreadCounts()
    
    // 初始化WebSocket连接
    this.initWebSocketConnection()
  },
  beforeUnmount() {
    if (this.globalSubscription) {
      this.globalSubscription.unsubscribe()
    }
    
    // Desuscribirse de todos los canales de grupo
    Object.values(this.groupSubscriptions).forEach(subscription => {
      if (subscription) {
        subscription.unsubscribe()
      }
    })
    
    disconnectWebSocket()
  }
}
</script>

<style scoped>
/* 页面整体背景渐变 */
body {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
}

.chat-page {
  display: flex;
  max-width: 1200px;
  margin: 20px auto;
  background: linear-gradient(145deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 20px;
  box-shadow: 
    0 20px 40px rgba(0,0,0,0.1),
    0 8px 16px rgba(0,0,0,0.06),
    inset 0 1px 0 rgba(255,255,255,0.8);
  /* Fijamos altura para que el área de mensajes pueda hacer scroll */
  height: calc(100vh - 40px);
  min-height: 0;
  overflow: hidden;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.2);
}

.sidebar {
  width: 280px;
  background: linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  border-right: 1px solid rgba(226, 232, 240, 0.8);
  position: relative;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.8), transparent);
}

.sidebar-user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px 0 20px 0;
  text-align: center;
  color: white;
  font-size: 1.2em;
  font-weight: 600;
  letter-spacing: 0.5px;
  position: relative;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.sidebar-user::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 20%;
  right: 20%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.6), transparent);
}

.sidebar-username {
  color: #ffffff;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.sidebar-self {
  color: rgba(255,255,255,0.8);
  font-size: 0.9em;
  margin-left: 4px;
  font-weight: 400;
}

.sidebar-section {
  padding: 24px 0 0 0;
  position: relative;
}

.sidebar-section:not(:last-child)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 20px;
  right: 20px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(148, 163, 184, 0.3), transparent);
}

.sidebar-title {
  font-size: 1.1em;
  font-weight: 600;
  color: #334155;
  margin: 0 0 16px 24px;
  letter-spacing: 0.5px;
  position: relative;
}

.sidebar-title::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 2px;
}

.main-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 24px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  position: relative;
  /* Permite que el hijo (ChatWindow) pueda contraerse y mostrar scroll interno */
  min-height: 0;
}

.main-chat::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.8), transparent);
}

.chat-input-area {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding: 20px;
  background: linear-gradient(145deg, #f8fafc 0%, #ffffff 100%);
  border-radius: 16px;
  box-shadow: 
    0 4px 12px rgba(0,0,0,0.05),
    inset 0 1px 0 rgba(255,255,255,0.8);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.chat-input-area input {
  flex: 1;
  padding: 14px 18px;
  border-radius: 12px;
  border: 2px solid rgba(226, 232, 240, 0.6);
  background: linear-gradient(145deg, #ffffff 0%, #f8fafc 100%);
  font-size: 14px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.02);
}

.chat-input-area input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 
    0 0 0 3px rgba(102, 126, 234, 0.1),
    inset 0 2px 4px rgba(0,0,0,0.02);
  transform: translateY(-1px);
}

.chat-input-area button {
  padding: 14px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 
    0 4px 12px rgba(102, 126, 234, 0.3),
    0 2px 4px rgba(0,0,0,0.1);
  position: relative;
  overflow: hidden;
}

.chat-input-area button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  transition: left 0.5s;
}

.chat-input-area button:hover {
  transform: translateY(-2px);
  box-shadow: 
    0 8px 20px rgba(102, 126, 234, 0.4),
    0 4px 8px rgba(0,0,0,0.1);
}

.chat-input-area button:hover::before {
  left: 100%;
}

.chat-input-area button:active {
  transform: translateY(0);
  box-shadow: 
    0 2px 8px rgba(102, 126, 234, 0.3),
    0 1px 2px rgba(0,0,0,0.1);
}

/* 响应式设计 - 平板设备 */
@media (max-width: 1024px) {
  .chat-page {
    margin: 10px;
    max-width: none;
    min-height: 600px;
  }
  
  .sidebar {
    width: 240px;
  }
  
  .sidebar-user {
    padding: 20px 0 16px 0;
    font-size: 1.1em;
  }
}

/* 响应式设计 - 移动设备 */
@media (max-width: 768px) {
  .chat-page {
    flex-direction: column;
    margin: 5px;
    border-radius: 16px;
    min-height: calc(100vh - 10px);
  }
  
  .sidebar {
    width: 100%;
    max-height: 200px;
    border-right: none;
    border-bottom: 1px solid rgba(226, 232, 240, 0.8);
    overflow-y: auto;
  }
  
  .sidebar-user {
    padding: 16px 0 12px 0;
    font-size: 1em;
  }
  
  .main-chat {
    flex: 1;
    min-height: 0;
  }
  
  .chat-input-area {
    padding: 16px;
    gap: 8px;
    margin-top: 12px;
  }
  
  .chat-input-area input {
    padding: 12px 16px;
    font-size: 16px; /* 防止iOS缩放 */
  }
  
  .chat-input-area button {
    padding: 12px 20px;
    font-size: 14px;
  }
}

/* 响应式设计 - 小屏幕手机 */
@media (max-width: 480px) {
  .chat-page {
    margin: 0;
    border-radius: 0;
    min-height: 100vh;
  }
  
  .sidebar {
    max-height: 150px;
  }
  
  .sidebar-user {
    padding: 12px 0 8px 0;
    font-size: 0.9em;
  }
  
  .chat-input-area {
    padding: 12px;
    gap: 6px;
    margin-top: 8px;
    border-radius: 12px;
  }
  
  .chat-input-area input {
    padding: 10px 14px;
  }
  
  .chat-input-area button {
    padding: 10px 16px;
    font-size: 13px;
  }
}

/* 横屏模式优化 */
@media (max-width: 768px) and (orientation: landscape) {
  .sidebar {
    max-height: 120px;
  }
  
  .chat-page {
    min-height: calc(100vh - 5px);
  }
}
</style>