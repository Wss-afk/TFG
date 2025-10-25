<template>
  <div class="chat-window">
    <div class="chat-header" v-if="chatUser && chatType === 'user'">
      Conversación con <b>{{ chatUser.username }}</b>
    </div>
    <div class="chat-header" v-else-if="chatGroup && chatType === 'group'">
      Grupo: <b>{{ chatGroup.name }}</b>
    </div>
    <div v-if="chatGroup && chatType === 'group'" class="group-members-bar">
      <span class="members-title">Miembros:</span>
      <div class="members-list">
        <span v-for="u in (chatGroup.users || [])" :key="u.id" class="member-chip">{{ u.username }}</span>
        <span v-if="!chatGroup.users || chatGroup.users.length === 0" class="member-chip empty">Sin miembros</span>
      </div>
    </div>
    <div class="messages" ref="messagesContainer" @scroll="onScroll">
      <template v-for="(item, idx) in windowedItems" :key="item.key || idx">
        <DateSeparator v-if="item.type === 'separator'" :label="item.label" />
        <MessageItem v-else :message="item.message" :currentUserId="currentUserId" />
      </template>

      <button
        v-if="!shouldAutoScroll"
        class="scroll-bottom-btn"
        @click="jumpToBottom"
        aria-label="Ir al último"
      >
        ↓ Ir al último
      </button>

      <div
        v-if="unreadCount > 0 && !shouldAutoScroll"
        class="new-messages-banner"
        @click="jumpToBottom"
        aria-live="polite"
      >
        Nuevos mensajes · {{ unreadCount }}
      </div>
    </div>

    <slot></slot>
  </div>
</template>

<script>
import MessageItem from './MessageItem.vue'
import DateSeparator from './DateSeparator.vue'

export default {
  name: 'ChatWindow',
  components: { MessageItem, DateSeparator },
  props: {
    messages: { type: Array, required: true },
    chatUser: { type: Object, default: null },
    chatGroup: { type: Object, default: null },
    chatType: { type: String, default: 'user' },
    currentUserId: { type: [String, Number], required: true }
  },
  data() {
    return {
      startIndex: 0,
      windowSize: 120,
      prependCount: 60,
      shouldAutoScroll: true,
      nearBottomThreshold: 150,
      unreadCount: 0,
      lastMessagesLength: 0
    }
  },
  computed: {
    allItems() {
      return this.decorateMessages(this.messages || [])
    },
    windowedItems() {
      const end = this.allItems.length
      const start = Math.max(0, Math.min(this.startIndex, end))
      return this.allItems.slice(start, end)
    }
  },
  watch: {
    messages: {
      immediate: true,
      handler(newVal, oldVal) {
        const prevLen = Array.isArray(oldVal) ? oldVal.length : this.lastMessagesLength || 0
        const currLen = Array.isArray(newVal) ? newVal.length : 0
        const end = this.allItems.length

        if (this.shouldAutoScroll) {
          this.startIndex = Math.max(0, end - this.windowSize)
          this.$nextTick(() => this.scrollToBottom())
          this.unreadCount = 0
        } else {
          if (currLen > prevLen) {
            const added = Array.isArray(newVal) ? newVal.slice(prevLen) : []
            const newlyFromOthers = added.filter(m => {
              const sid = (m && m.sender && (m.sender.id ?? m.sender.userId)) || m.senderId
              return sid != this.currentUserId
            }).length
            this.unreadCount += newlyFromOthers
          }
          // Mantener ventana estable cuando el usuario está leyendo arriba
        }
        this.lastMessagesLength = currLen
      }
    }
  },
  methods: {
    onScroll() {
      this.updateAutoScrollState()
      const el = this.$refs.messagesContainer
      if (!el) return

      // Carga progresiva de mensajes más antiguos cuando se acerca al top
      if (el.scrollTop < 50 && this.startIndex > 0) {
        const prevScrollHeight = el.scrollHeight
        const prevTop = el.scrollTop
        this.startIndex = Math.max(0, this.startIndex - this.prependCount)
        this.$nextTick(() => {
          const newScrollHeight = el.scrollHeight
          // Mantener la posición visible del usuario
          el.scrollTop = newScrollHeight - prevScrollHeight + prevTop
          this.updateAutoScrollState()
        })
      }
    },
    scrollToBottom() {
      const el = this.$refs.messagesContainer
      if (!el) return
      el.scrollTop = el.scrollHeight
      this.updateAutoScrollState()
    },
    jumpToBottom() {
      this.shouldAutoScroll = true
      this.unreadCount = 0
      this.$nextTick(() => {
        this.scrollToBottom()
        this.updateAutoScrollState()
      })
    },
    decorateMessages(list) {
      const result = []
      let lastDayKey = null
      list.forEach((msg, i) => {
        const ts = msg.timestamp ? new Date(msg.timestamp) : new Date()
        const dayKey = `${ts.getFullYear()}-${ts.getMonth()+1}-${ts.getDate()}`
        if (dayKey !== lastDayKey) {
          result.push({ type: 'separator', label: this.formatDateLabel(ts), key: `sep-${dayKey}` })
          lastDayKey = dayKey
        }
        result.push({ type: 'message', message: msg, key: `msg-${msg.id ?? i}` })
      })
      return result
    },
    formatDateLabel(date) {
      const d = new Date(date)
      const today = new Date()
      const yesterday = new Date()
      yesterday.setDate(today.getDate() - 1)

      const sameDay = (a,b) => a.getFullYear()===b.getFullYear() && a.getMonth()===b.getMonth() && a.getDate()===b.getDate()
      if (sameDay(d, today)) return 'Hoy'
      if (sameDay(d, yesterday)) return 'Ayer'
      return d.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' })
    },
    updateAutoScrollState() {
      const el = this.$refs.messagesContainer
      if (!el) return
      const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight
      this.shouldAutoScroll = distanceFromBottom < this.nearBottomThreshold
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.scrollToBottom()
      this.updateAutoScrollState()
    })
  }
}
</script>

<style scoped>
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  border-radius: 16px;
  padding: 20px;
  overflow: hidden;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  /* Permite que la lista de mensajes haga scroll en su interior */
  min-height: 0;
}

.chat-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  text-align: center;
  font-weight: 600;
  font-size: 16px;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
  backdrop-filter: blur(10px);
}

.chat-header b {
  color: #ffd700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.messages {
  position: relative;
  flex: 1;
  overflow-y: auto;
  /* Evita crecimiento infinito y asegura scroll visible */
  min-height: 0;
  margin-bottom: 12px;
  padding: 10px;
  scrollbar-width: thin;
  scrollbar-color: rgba(102, 126, 234, 0.3) transparent;
}

.messages::-webkit-scrollbar {
  width: 6px;
}

.messages::-webkit-scrollbar-track {
  background: transparent;
}

.messages::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 3px;
}

.messages::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #5a67d8 0%, #6b46c1 100%);
}
.group-members-bar {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 12px;
  padding: 10px 12px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.members-title {
  font-weight: 600;
  color: #667eea;
}

.members-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.member-chip {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 4px 8px;
  border-radius: 9999px;
  font-size: 12px;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
}

.member-chip.empty {
  background: #e2e8f0;
  color: #334155;
}

/* Controles flotantes */
.scroll-bottom-btn {
  position: absolute;
  right: 12px;
  bottom: 12px;
  z-index: 30;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
  border: none;
  border-radius: 9999px;
  padding: 8px 14px;
  font-weight: 600;
  box-shadow: 0 6px 18px rgba(16,185,129,0.35);
  cursor: pointer;
}

.scroll-bottom-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 22px rgba(16,185,129,0.45);
}

.new-messages-banner {
  position: absolute;
  left: 50%;
  bottom: 64px;
  transform: translateX(-50%);
  z-index: 25;
  background: linear-gradient(180deg, #fef3c7 0%, #fde68a 100%);
  color: #7c2d12;
  border: 1px solid #fcd34d;
  border-radius: 9999px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 8px 18px rgba(250, 204, 21, 0.35);
  cursor: pointer;
}

.chat-window {
  position: relative;
}

</style>