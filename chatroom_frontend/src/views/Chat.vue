<template>
  <div class="chat-page">
    <div class="sidebar">
      <div class="sidebar-brand">
        <img class="brand-logo" src="@/assets/chatroom-logo.svg" alt="Chatroom" />
        <div class="brand-text">
          <span class="brand-name">Chatroom</span>
          <span class="brand-sub">mensajería</span>
        </div>
      </div>
      <div class="sidebar-user">
        <img v-if="currentUser && currentUser.avatarUrl" :src="currentUser.avatarUrl" class="sidebar-avatar" />
        <span class="sidebar-username">{{ currentUser && currentUser.username }}</span>
        <span class="self-badge" title="Este eres tú">Tú</span>
      </div>
      <div class="sidebar-section">
        <div class="sidebar-title">Usuarios</div>
        <UserList :users="sortedUsers" :unreadCounts="unreadCounts" :selectedUser="selectedUser" :onlineUsers="onlineUsers" @select="selectUser" />
      </div>
      <div class="sidebar-section">
        <div class="sidebar-title">Grupos</div>
        <GroupList :groups="groups" :groupUnreadCounts="groupUnreadCounts" :selectedGroup="selectedGroup" @select="selectGroup" />
      </div>
    </div>
    <div class="main-chat">
      <ChatWindow 
        ref="chatWindow"
        :messages="messages" 
        :chatUser="selectedUser" 
        :chatGroup="selectedGroup"
        :chatType="chatType"
        :currentUserId="currentUser && currentUser.id">
        <div class="chat-input-area">
          <div class="chat-input-actions">
            <button class="icon-btn" type="button" title="Emoji" @click="toggleEmojiPicker">😊</button>
            <button class="icon-btn" type="button" title="Adjuntar" @click="triggerAttach">📎</button>
            <input ref="fileInput" type="file" style="display:none" @change="handleFileSelected" />
          </div>
          <input v-model="inputMsg" @keyup.enter="send" placeholder="Escribe un mensaje..." />
          <button @click="send">Enviar</button>
        </div>
        <div v-if="showEmojiPicker" class="emoji-picker">
          <button v-for="e in emojis" :key="e" class="emoji-btn" @click="insertEmoji(e)" type="button">{{ e }}</button>
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
import { 
  disconnectWebSocket, 
  setOnlineUsersCallback, 
  sendMessage, 
  subscribe, 
  connectWebSocket 
} from '../services/websocket.js'

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
      groupUnreadCounts: {}, // 存储每个群组的未读消息数量
      onlineUsers: [], // 存储在线用户列表
      audioContext: null, // AudioContext para sonidos de notificación
      userHasInteracted: false, // Flag para saber si el usuario ha interactuado
      showEmojiPicker: false,
      emojis: ['😀','😁','😂','😊','😍','😎','😘','😅','😉','🤩','🥳','😇','🙌','👍','👏','🔥','✨','❤️','💯','🎉']
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
        this.selectedGroup = null
        this.chatType = 'user'
        
        const { fetchMessages, markMessagesAsRead } = await import('../services/chat.service.js')
        const res = await fetchMessages({ receiverId: user.id, userId: this.currentUser && this.currentUser.id })
        this.messages = res.data.slice().sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
        
        // 标记消息为已读
          if (this.currentUser) {
            await markMessagesAsRead(this.currentUser.id, user.id)
            // 清除该用户的未读消息计数
            this.unreadCounts[user.id] = 0
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
      // Resolver senderId, compatible con diferentes estructuras
      const senderId = (message && message.sender && (message.sender.id ?? message.sender.userId)) || message.senderId
      const isFromSelf = senderId && this.currentUser && senderId === this.currentUser.id
      
      console.log('Procesando mensaje nuevo:', {
        senderId,
        isFromSelf,
        isGroup: !!message.group,
        selectedUserId: this.selectedUser?.id,
        chatType: this.chatType
      })
      
      // Si el mensaje no es del usuario actual
      if (!isFromSelf) {
        
        // Solo procesar mensajes individuales para conteo de no leídos
        // Los mensajes de grupo no deben aparecer como no leídos en usuarios individuales
        if (!message.group && senderId) {
          // Incrementar contador solo si no es el chat actualmente seleccionado
          const isCurrentChat = this.chatType === 'user' && this.selectedUser && senderId === this.selectedUser.id
          
          if (!isCurrentChat) {
             const newCount = (this.unreadCounts[senderId] || 0) + 1
             // En Vue 3, usar asignación directa para reactividad
             this.unreadCounts[senderId] = newCount
             console.log(`Usuario ${senderId} mensajes no leídos actualizados a:`, newCount)
           }
        }
        
        // Mostrar notificación de escritorio solo para mensajes individuales
        if (!message.group) {
          this.showNotification(message)
        }
        
        // Reproducir sonido de notificación
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
      // Solo reproducir sonido si el usuario ha interactuado con la página
      if (!this.userHasInteracted) {
        console.log('No se puede reproducir sonido: el usuario no ha interactuado con la página')
        return
      }

      try {
        // Crear o reutilizar AudioContext
        if (!this.audioContext) {
          this.audioContext = new (window.AudioContext || window.webkitAudioContext)()
        }

        // Reanudar AudioContext si está suspendido
        if (this.audioContext.state === 'suspended') {
          this.audioContext.resume().then(() => {
            this.createNotificationSound()
          }).catch(error => {
            console.error('Error al reanudar AudioContext:', error)
          })
        } else {
          this.createNotificationSound()
        }
      } catch (error) {
        console.error('Error al crear AudioContext:', error)
      }
    },
    
    createNotificationSound() {
      if (!this.audioContext) return

      try {
        const oscillator = this.audioContext.createOscillator()
        const gainNode = this.audioContext.createGain()
        
        oscillator.connect(gainNode)
        gainNode.connect(this.audioContext.destination)
        
        oscillator.frequency.setValueAtTime(800, this.audioContext.currentTime)
        oscillator.frequency.setValueAtTime(600, this.audioContext.currentTime + 0.1)
        
        gainNode.gain.setValueAtTime(0.3, this.audioContext.currentTime)
        gainNode.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.2)
        
        oscillator.start(this.audioContext.currentTime)
        oscillator.stop(this.audioContext.currentTime + 0.2)
      } catch (error) {
        console.error('Error al crear sonido de notificación:', error)
      }
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
        
        // 清除该群组的未读消息计数
        if (group && group.id) {
          this.groupUnreadCounts[group.id] = 0
        }
      } catch (e) {
        console.error('Error en selectGroup:', e)
      }
    },
    async send() {
      if (this.inputMsg.trim()) {
        const content = this.inputMsg
        
        if (this.chatType === 'user' && this.selectedUser) {
          const message = {
            sender: { id: this.currentUser.id, username: this.currentUser.username },
            receiver: { id: this.selectedUser.id, username: this.selectedUser.username },
            content,
            type: 'text',
            timestamp: new Date().toISOString()
          }
          sendMessage('/app/chat/single', message)
          this.messages.push(message)
          this.$nextTick(() => {
            this.scrollToBottom()
          })
        } else if (this.chatType === 'group' && this.selectedGroup) {
          const message = {
            sender: { id: this.currentUser.id, username: this.currentUser.username },
            group: { id: this.selectedGroup.id, name: this.selectedGroup.name },
            content,
            type: 'text',
            timestamp: new Date().toISOString()
          }
          sendMessage('/app/chat/group', message)
        }
        this.inputMsg = ''
      }
    },
    toggleEmojiPicker() {
      this.showEmojiPicker = !this.showEmojiPicker
    },
    insertEmoji(e) {
      this.inputMsg = (this.inputMsg || '') + e
    },
    triggerAttach() {
      const el = this.$refs.fileInput
      if (el) el.click()
    },
    async handleFileSelected(evt) {
      try {
        const file = evt.target.files && evt.target.files[0]
        if (!file) return
        const { uploadAttachment } = await import('../services/chat.service.js')
        const res = await uploadAttachment(file)
        const { url, type } = res.data || {}
        if (!url || !type) return
        const baseMessage = {
          sender: { id: this.currentUser.id, username: this.currentUser.username },
          content: file.name,
          type,
          fileUrl: url,
          timestamp: new Date().toISOString()
        }
        if (this.chatType === 'user' && this.selectedUser) {
          const message = { ...baseMessage, receiver: { id: this.selectedUser.id, username: this.selectedUser.username } }
          sendMessage('/app/chat/single', message)
          // Añadir optimista para el emisor
          this.messages.push(message)
          this.$nextTick(() => this.scrollToBottom())
        } else if (this.chatType === 'group' && this.selectedGroup) {
          const message = { ...baseMessage, group: { id: this.selectedGroup.id, name: this.selectedGroup.name } }
          sendMessage('/app/chat/group', message)
        }
        // limpiar input de archivo
        evt.target.value = ''
      } catch (err) {
        console.error('Error al enviar adjunto:', err)
      }
    },
    subscribeToGroupChannel(groupId) {
      const topic = `/topic/group/${groupId}`
      console.log('Suscribiendo a canal de grupo:', topic)
      
      // Desuscribirse del canal anterior si existe
      if (this.groupSubscriptions[groupId]) {
        this.groupSubscriptions[groupId].unsubscribe()
      }
      
      this.groupSubscriptions[groupId] = subscribe(topic, (msg) => {
        console.log('Mensaje de grupo recibido:', msg)
        const msgGroupId = (msg && msg.group && msg.group.id) || groupId
        const isCurrentGroup = this.chatType === 'group' && this.selectedGroup && msg.group && msg.group.id === this.selectedGroup.id
        
        if (isCurrentGroup) {
          this.messages.push(msg)
          // Scroll to bottom after adding message
          this.$nextTick(() => {
            this.scrollToBottom()
          })
        } else {
          // Incrementar contador de no leídos para el grupo
          const newCount = (this.groupUnreadCounts[msgGroupId] || 0) + 1
          this.groupUnreadCounts[msgGroupId] = newCount
          console.log(`Grupo ${msgGroupId} mensajes no leídos:`, newCount)
          
          // Reproducir sonido de notificación para mensajes de grupo no visibles
          this.playNotificationSound()
        }
      })
    },
    
    subscribeToAllGroupChannels() {
      if (!Array.isArray(this.groups)) return
      for (const g of this.groups) {
        if (g && g.id) {
          this.subscribeToGroupChannel(g.id)
        }
      }
    },
    
    initWebSocketConnection() {
      if (!this.currentUser) {
        console.error('当前用户信息不存在，无法建立WebSocket连接')
        return
      }
     
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
          // 订阅所有群组频道以统计未读
          this.subscribeToAllGroupChannels()
        }, 100) // 稍微延迟确保连接完全建立
      }, (error) => {
        console.error('WebSocket连接失败:', error)
        this.wsConnected = false
      })
    },
    
    subscribeToGlobalUserChannel() {
      if (this.currentUser) {
        const topic = `/user/queue/messages`
        console.log('订阅消息频道:', topic)
        this.globalSubscription = subscribe(topic, (msg) => {
          console.log('收到新消息:', msg)
          // 处理接收到的消息
          this.handleNewMessage(msg)
          
          // 当当前选中用户就是消息发送者，直接追加到当前消息列表，避免延迟
          const senderId = (msg && msg.sender && (msg.sender.id ?? msg.sender.userId)) || msg.senderId
          if (this.chatType === 'user' && this.selectedUser && senderId === this.selectedUser.id) {
            this.messages.push(msg)
            // Scroll to bottom after adding message
            this.$nextTick(() => {
              this.scrollToBottom()
            })
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
    },
    
    scrollToBottom() {
      this.$nextTick(() => {
        const chatWindow = this.$refs.chatWindow
        if (chatWindow && chatWindow.$refs.messagesContainer) {
          const messagesContainer = chatWindow.$refs.messagesContainer
          messagesContainer.scrollTop = messagesContainer.scrollHeight
        }
      })
    },
    
    setupUserInteractionDetection() {
      // Detectar cualquier interacción del usuario (click, keydown, touchstart)
      const enableAudio = () => {
        this.userHasInteracted = true
        console.log('Usuario ha interactuado - AudioContext habilitado')
        
        // Remover los event listeners una vez que se detecta la interacción
        document.removeEventListener('click', enableAudio)
        document.removeEventListener('keydown', enableAudio)
        document.removeEventListener('touchstart', enableAudio)
      }
      
      document.addEventListener('click', enableAudio)
      document.addEventListener('keydown', enableAudio)
      document.addEventListener('touchstart', enableAudio)
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
    
    // Detectar interacción del usuario para habilitar AudioContext
    this.setupUserInteractionDetection()
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

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #ffffff;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
}

.brand-logo {
  width: 28px;
  height: 28px;
}

.brand-text { display: flex; flex-direction: column; line-height: 1; }
.brand-name { font-weight: 700; color: #334155; font-size: 16px; }
.brand-sub { color: #94a3b8; font-size: 12px; }

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
  color: #334155;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.2px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: none;
}

.sidebar-user::after { content: none; }

.sidebar-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #c7d2fe;
  border: 2px solid #eef2ff;
}

.sidebar-username { color: #334155; font-weight: 600; text-shadow: none; }
.sidebar-self { color: #64748b; font-size: 12px; margin-left: 4px; font-weight: 500; }

.sidebar-username {
  color: #334155;
  font-weight: 600;
  text-shadow: none;
}

.sidebar-self {
  color: #64748b;
  font-size: 12px;
  margin-left: 4px;
  font-weight: 500;
}

.self-badge {
  background: linear-gradient(135deg, #22c55e 0%, #10b981 100%);
  color: #ffffff;
  border-radius: 9999px;
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 700;
  margin-left: auto;
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.25);
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

.chat-input-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: #eef2f7;
  color: #334155;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.7);
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-btn:hover { background: #e2e8f0; transform: translateY(-1px); }
.icon-btn:active { transform: translateY(0); }

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

.emoji-picker {
  margin-top: 8px;
  padding: 8px;
  background: #fff;
  border: 1px solid rgba(226,232,240,0.6);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.emoji-btn {
  padding: 6px 8px;
  border: none;
  border-radius: 8px;
  background: #f1f5f9;
  cursor: pointer;
}
.emoji-btn:hover { background: #e2e8f0; }
</style>