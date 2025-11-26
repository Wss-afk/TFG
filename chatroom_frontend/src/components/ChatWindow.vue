<template>
  <div class="chat-window">
    <div class="chat-topbar" v-if="chatUser && chatType === 'user'">
      <div class="left">
        <div
          class="avatar"
          :title="chatUser.username"
          role="img"
          :aria-label="'Avatar de ' + ((chatUser && (chatUser.username || chatUser.name)) || 'usuario')"
        >
          <img v-if="chatUser && chatUser.avatarUrl" :src="chatUser.avatarUrl" alt="avatar" class="avatar-img" />
          <span v-else>{{ userInitial }}</span>
        </div>
        <div class="meta">
          <div class="name">{{ chatUser.username }}</div>
          <div class="sub">{{ chatUser.email || chatUser.userEmail || 'Conversación privada' }}</div>
        </div>
      </div>
      <div class="actions">
        <input
          type="text"
          class="search"
          placeholder="Filtrar en el chat"
          aria-label="Buscar"
          :value="searchQuery"
          @input="onInputSearch"
          @keyup.enter="$emit('search', localSearch)"
        />
        <button class="icon-btn" title="Opciones" @click="$emit('menu')">⋮</button>
      </div>
    </div>
    <div class="chat-topbar" v-else-if="chatGroup && chatType === 'group'">
      <div class="left">
        <div class="avatar group" :title="chatGroup.name">{{ groupInitial }}</div>
        <div class="meta">
          <div class="name">{{ chatGroup.name }}</div>
          <div class="sub">{{ (chatGroup.users && chatGroup.users.length) ? (chatGroup.users.length + ' miembros') : 'Grupo' }}</div>
        </div>
      </div>
      <div class="actions">
        <input
          type="text"
          class="search"
          placeholder="Buscar en el grupo"
          aria-label="Buscar"
          :value="searchQuery"
          @input="onInputSearch"
          @keyup.enter="$emit('search', localSearch)"
        />
        <button class="icon-btn" title="Opciones" @click="$emit('menu')">⋮</button>
      </div>
    </div>
    <div v-if="chatGroup && chatType === 'group'" class="group-members-bar">
      <span class="members-title">Miembros:</span>
      <div class="members-list">
        <span v-for="u in (chatGroup.users || [])" :key="u.id" class="member-chip">{{ u.username }}</span>
        <span v-if="!chatGroup.users || chatGroup.users.length === 0" class="member-chip empty">Sin miembros</span>
      </div>
    </div>
    <div
      class="messages"
      ref="messagesContainer"
      @scroll="onScroll"
      role="log"
      aria-live="polite"
      aria-relevant="additions"
      aria-atomic="false"
      aria-label="Mensajes del chat"
    >
      <div v-if="loading" class="messages-skeleton">
        <div v-for="n in 6" :key="'skm-'+n" class="sk-msg" :class="n % 2 === 0 ? 'left' : 'right'">
          <div class="sk-bubble"></div>
        </div>
      </div>
      <div v-else-if="(messages && messages.length === 0)" class="messages-empty">
        <div class="empty-card" role="region" aria-label="No hay mensajes">
          <div class="empty-hero" aria-hidden="true">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M4 5.5A3.5 3.5 0 0 1 7.5 2h9A3.5 3.5 0 0 1 20 5.5v7A3.5 3.5 0 0 1 16.5 16H13l-3.5 3.5c-.6.6-1.5.18-1.5-.7V16H7.5A3.5 3.5 0 0 1 4 12.5v-7Z" fill="currentColor"/>
            </svg>
          </div>
          <div class="empty-title">No hay mensajes</div>
          <div class="empty-sub">Escribe tu primer mensaje para iniciar la conversación.</div>
          <button type="button" class="empty-cta" @click="$emit('compose')">Escribir mensaje</button>
        </div>
      </div>
      <transition-group v-else name="msg" tag="div" class="messages-list">
        <component
          v-for="(item, idx) in windowedItems"
          :is="item.type === 'separator' ? 'DateSeparator' : 'MessageItem'"
          :key="item.key || idx"
          v-bind="item.type === 'separator' 
            ? { label: item.label } 
            : { message: item.message, currentUserId: currentUserId, continuation: item.continuation, highlightQuery: searchQuery }"
        />
      </transition-group>
    </div>

    <button
      v-if="!isAtBottom"
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

    <div class="composer-bar">
      <slot></slot>
    </div>
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
    currentUserId: { type: [String, Number], required: true },
    searchQuery: { type: String, default: '' },
    loading: { type: Boolean, default: false }
  },
  data() {
    return {
      startIndex: 0,
      windowSize: 120,
      prependCount: 60,
      shouldAutoScroll: true,
      isAtBottom: true,
      nearBottomThreshold: 150,
      unreadCount: 0,
      lastMessagesLength: 0,
      localSearch: ''
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
    },
    userInitial() {
      const name = (this.chatUser && (this.chatUser.username || this.chatUser.name)) || ''
      return name ? name.charAt(0).toUpperCase() : '?'
    },
    groupInitial() {
      const name = (this.chatGroup && this.chatGroup.name) || ''
      return name ? name.charAt(0).toUpperCase() : '#'
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
    recalcWindowSize() {
      const el = this.$refs.messagesContainer
      if (!el) return
      const vh = el.clientHeight || 600
      const approxItem = 56
      const visibleCount = Math.max(60, Math.floor(vh / approxItem) * 2)
      this.windowSize = visibleCount
      this.prependCount = Math.max(30, Math.floor(visibleCount / 2))
    },
    onInputSearch(e) {
      const val = e && e.target ? e.target.value : ''
      this.localSearch = val
      this.$emit('update:searchQuery', val)
    },
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
      let prevMsg = null
      const CONTINUATION_MS = 5 * 60 * 1000 // 5 minutos para agrupar visualmente
      list.forEach((msg, i) => {
        const ts = msg.timestamp ? new Date(msg.timestamp) : new Date()
        const dayKey = `${ts.getFullYear()}-${ts.getMonth()+1}-${ts.getDate()}`
        if (dayKey !== lastDayKey) {
          result.push({ type: 'separator', label: this.formatDateLabel(ts), key: `sep-${dayKey}` })
          lastDayKey = dayKey
        }
        let continuation = false
        if (prevMsg) {
          const prevTs = prevMsg.timestamp ? new Date(prevMsg.timestamp) : ts
          const prevSender = (prevMsg.sender && (prevMsg.sender.id ?? prevMsg.sender.userId)) ?? prevMsg.senderId ?? prevMsg.sender
          const currSender = (msg.sender && (msg.sender.id ?? msg.sender.userId)) ?? msg.senderId ?? msg.sender
          const closeInTime = Math.abs(ts - prevTs) <= CONTINUATION_MS
          continuation = prevSender == currSender && closeInTime
        }
        result.push({ type: 'message', message: msg, continuation, key: `msg-${msg.id ?? i}` })
        prevMsg = msg
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
      const distance = el.scrollHeight - el.scrollTop - el.clientHeight
      const atBottom = distance <= 2
      this.isAtBottom = atBottom
      this.shouldAutoScroll = atBottom
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.scrollToBottom()
      this.updateAutoScrollState()
      this.recalcWindowSize()
      window.addEventListener('resize', this.recalcWindowSize, { passive: true })
    })
  }
}
</script>

<style scoped>
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, var(--surface-alt) 0%, var(--surface) 100%);
  border-radius: var(--radius);
  padding: 20px;
  overflow: hidden;
  box-shadow: var(--shadow);
  border: 1px solid var(--border-color);
  /* Permite que la lista de mensajes haga scroll en su interior */
  min-height: 0;
}

.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--surface);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
  box-shadow: var(--shadow-soft);
}
.chat-topbar .left { display: flex; align-items: center; gap: 12px; }
.chat-topbar .avatar { width: 40px; height: 40px; border-radius: 50%; background: #e2e8f0; color: #1f2937; display: flex; align-items: center; justify-content: center; font-weight: 700; box-shadow: 0 1px 2px rgba(0,0,0,0.06); }
.chat-topbar .avatar-img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; display: block; }
.chat-topbar .avatar.group { background: linear-gradient(135deg, #22c55e, #16a34a); }
.chat-topbar .meta { display: flex; flex-direction: column; }
.chat-topbar .name { color: #334155; font-weight: 600; }
.chat-topbar .sub { color: #64748b; font-size: 12px; }
.chat-topbar .actions { display: inline-flex; align-items: center; gap: 10px; }
.chat-topbar .actions .search {
  height: 32px;
  border-radius: 9999px;
  border: 1px solid var(--border-color);
  padding: 0 12px;
  font-size: 13px;
  background: var(--color-bg);
}
.chat-topbar .actions .icon-btn { width: 32px; height: 32px; border-radius: 50%; border: none; color: #fff; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, var(--brand-gradient-start), var(--brand-gradient-end)); cursor: pointer; }

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
  background: linear-gradient(135deg, var(--brand-gradient-start) 0%, var(--brand-gradient-end) 100%);
  border-radius: 3px;
}

.messages::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, var(--brand-gradient-start) 0%, var(--brand-gradient-end) 100%);
}

.messages-list { display: flex; flex-direction: column; gap: 0; }

/* Mensajes: skeleton */
.messages-skeleton { display: flex; flex-direction: column; gap: 12px; padding: 10px; }
.sk-msg { display: flex; }
.sk-msg.left { justify-content: flex-start; }
.sk-msg.right { justify-content: flex-end; }
.sk-bubble { width: 52%; max-width: 420px; height: 18px; border-radius: 16px; background: linear-gradient(90deg, #f1f5f9, #e2e8f0, #f1f5f9); animation: shimmer 1.2s infinite; }
@keyframes shimmer { 0% { background-position: -200px 0; } 100% { background-position: 200px 0; } }

/* Mensajes: empty */
.messages-empty { display: flex; align-items: center; justify-content: center; min-height: 280px; }
.empty-card { text-align: center; color: #64748b; }
.empty-hero { display: inline-flex; align-items: center; justify-content: center; color: #94a3b8; margin-bottom: 8px; }
.empty-title { font-weight: 800; color: #334155; }
.empty-sub { font-size: 12px; margin-top: 4px; }
.empty-cta { margin-top: 10px; padding: 8px 12px; border-radius: 10px; border: 1px solid rgba(226,232,240,.9); background: #f1f5f9; color: #334155; font-weight: 700; cursor: pointer; }
.empty-cta:hover { background: #e2e8f0; }

/* Transiciones GPU-friendly para aparición / desaparición / reordenado */
.msg-enter-active,
.msg-leave-active { transition: transform 180ms ease, opacity 180ms ease; }
.msg-enter-from,
.msg-leave-to { opacity: 0; transform: translate3d(0, 8px, 0) scale(0.98); will-change: transform, opacity; }
.msg-move { transition: transform 180ms ease; will-change: transform; }
.msg-leave-active { pointer-events: none; }
.group-members-bar {
  background: linear-gradient(135deg, var(--surface) 0%, var(--surface-alt) 100%);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 10px 12px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.members-title {
  font-weight: 600;
  color: var(--brand-gradient-start);
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
  bottom: 180px;
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
  bottom: 236px;
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

/* Composer fijo con blur y sombra */
.composer-bar {
  position: sticky;
  bottom: 0;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  border-top: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 8px;
  box-shadow: var(--shadow-2);
}

</style>
