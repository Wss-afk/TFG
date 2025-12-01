<template>
  <div :class="['message-row', isMine ? 'mine' : '', continuation ? 'continuation' : '']">
    <div
      class="avatar"
      :class="isMine ? 'mine' : ''"
      :title="senderName"
      role="img"
      :aria-label="'Avatar de ' + (senderName || 'usuario')"
    >
      <img v-if="senderAvatarUrl" :src="senderAvatarUrl" alt="avatar" class="avatar-img" />
      <span v-else>{{ senderInitial }}</span>
    </div>
    <div :class="['message-item', isMine ? 'mine' : '']">
      <span class="sender">{{ typeof message.sender === 'object' && message.sender ? message.sender.username : message.sender }}:</span>
      <div class="content">
        <template v-if="message.type === 'image' && message.fileUrl">
          <a :href="message.fileUrl" target="_blank" rel="noopener noreferrer">
            <img
              :src="message.fileUrl"
              :srcset="message.fileUrl + ' 1x, ' + message.fileUrl + ' 2x'"
              alt="imagen adjunta"
              class="attachment-image"
              :class="imageLoaded ? '' : 'image-loading'"
              loading="lazy"
              decoding="async"
              @load="imageLoaded = true"
            />
          </a>
          <div v-if="message.content" class="caption">{{ message.content }}</div>
        </template>
        <template v-else-if="message.type === 'file' && message.fileUrl">
          <a :href="message.fileUrl" target="_blank" rel="noopener noreferrer" class="attachment-file">
            <Icon name="paperclip" :size="18" />
            <span class="file-label">{{ message.content || 'Archivo' }}</span>
          </a>
        </template>
        <template v-else>
          <span v-html="highlightedContent"></span>
        </template>
      </div>
      <span class="timestamp">
        {{ formattedTime }}
        <span v-if="isMine && deliveryStatus" class="status-icon" :title="deliveryStatus.label">{{ deliveryStatus.icon }}</span>
      </span>
    </div>
  </div>
</template>

<script>
import Icon from './Icon.vue'
export default {
  name: 'MessageItem',
  components: { Icon },
  props: {
    message: {
      type: Object,
      required: true
    },
    currentUserId: {
      type: [String, Number],
      required: true
    },
    continuation: { type: Boolean, default: false },
    highlightQuery: { type: String, default: '' }
  },
  data() {
    return { imageLoaded: false }
  },
  computed: {
    isMine() {
      if (typeof this.message.sender === 'object' && this.message.sender) {
        return this.message.sender.id == this.currentUserId;
      }
      return this.message.sender == this.currentUserId;
    },
    senderName() {
      const s = this.message && this.message.sender
      if (!s) return ''
      if (typeof s === 'object') return s.username || s.name || ''
      return String(s)
    },
    senderAvatarUrl() {
      const s = this.message && this.message.sender
      if (s && typeof s === 'object') {
        return s.avatarUrl || ''
      }
      if (this.isMine && this.$store && this.$store.getters) {
        const cu = this.$store.getters['auth/currentUser']
        return (cu && cu.avatarUrl) || ''
      }
      return ''
    },
    senderInitial() {
      const n = this.senderName || ''
      return n ? n.charAt(0).toUpperCase() : '?'
    },
    formattedTime() {
      if (!this.message.timestamp) return '';
      const date = new Date(this.message.timestamp);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    },
    deliveryStatus() {
      const s = (this.message && this.message.status) || ''
      // soporta estados: 'sent' | 'delivered' | 'read'
      if (s === 'read') return { icon: '✔✔', label: 'Visto' }
      if (s === 'delivered') return { icon: '✔✔', label: 'Entregado' }
      if (s === 'sent') return { icon: '✔', label: 'Enviado' }
      return null
    },
    highlightedContent() {
      const content = (this.message && this.message.content) || ''
      const q = (this.highlightQuery || '').trim()
      if (!q) return this.escapeHtml(content)
      try {
        const re = new RegExp(this.escapeRegExp(q), 'gi')
        const escaped = this.escapeHtml(content)
        return escaped.replace(re, (m) => `<mark class="highlight">${m}</mark>`)
      } catch (e) {
        return this.escapeHtml(content)
      }
    }
  },
  methods: {
    escapeHtml(str) {
      return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;')
    },
    escapeRegExp(str) {
      return String(str).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    }
  }
}
</script>

<style scoped>
.message-row {
  display: flex;
  align-items: flex-end;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
  padding: 0 var(--space-2);
}

.message-row.mine {
  flex-direction: row-reverse;
  justify-content: flex-start;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #1f2937;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  box-shadow: var(--shadow-1);
  flex-shrink: 0;
  overflow: hidden;
  transition: transform 0.2s;
}

.avatar:hover {
  transform: scale(1.05);
}

.avatar.mine {
  background: linear-gradient(135deg, var(--color-secondary), #2563eb);
  color: #fff;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-item {
  position: relative;
  padding: 8px 12px;
  border-radius: 18px;
  max-width: min(75%, 600px);
  display: flex;
  flex-direction: column;
  word-wrap: break-word;
  animation: messageSlideIn 0.2s ease-out;
  box-shadow: var(--shadow-1);
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

@keyframes messageSlideIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-item.mine {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-hover) 100%);
  color: var(--color-primary-contrast);
  border-bottom-right-radius: 4px;
  margin-right: 4px;
}

.message-item:not(.mine) {
  background: var(--color-surface);
  color: var(--text-primary);
  border-color: var(--border-color);
  border-bottom-left-radius: 4px;
  margin-left: 4px;
}

.sender {
  font-size: 11px;
  font-weight: 700;
  margin-bottom: 2px;
  color: var(--color-primary);
  opacity: 0.9;
  margin-left: 2px;
}
.message-item.mine .sender { display: none; }

.content {
  line-height: 1.5;
  font-size: var(--font-size-md);
  position: relative;
  z-index: 1;
}

/* Ajustes de timestamp para que fluya mejor */
.timestamp {
  align-self: flex-end;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  margin-top: 2px;
  opacity: 0.7;
  user-select: none;
  line-height: 1;
}

.message-item.mine .timestamp {
  color: rgba(255, 255, 255, 0.8);
}

.message-item:not(.mine) .timestamp {
  color: var(--text-muted);
}

.status-icon {
  font-size: 12px;
}

/* Images & Files */
.attachment-image {
  max-width: 100%;
  max-height: 300px;
  border-radius: 12px;
  display: block;
  cursor: zoom-in;
  margin-bottom: 4px;
}

.attachment-file {
  background: rgba(0,0,0,0.05);
  padding: 8px 12px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: inherit;
  font-weight: 500;
  transition: background 0.2s;
}
.message-item.mine .attachment-file {
  background: rgba(255,255,255,0.15);
  color: #fff;
}
.attachment-file:hover {
  background: rgba(0,0,0,0.1);
}
.message-item.mine .attachment-file:hover {
  background: rgba(255,255,255,0.25);
}

/* Continuation Logic */
.message-row.continuation {
  margin-top: -4px; /* Acercar mensajes continuos */
  margin-bottom: var(--space-2);
}
.message-row.continuation .avatar {
  visibility: hidden;
  height: 0; /* Evitar hueco vertical si se ocultara solo visibility */
}

.message-row.continuation .message-item.mine {
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
}
.message-row.continuation:last-child .message-item.mine {
  border-bottom-right-radius: 4px; /* Mantener la cola en el último? */
}
/* Lógica simplificada para bordes en continuación */
.message-row.continuation .message-item {
  border-radius: 18px; /* Reset */
  margin-top: 2px;
}
.message-row.continuation .message-item.mine {
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
}
.message-row.continuation .message-item:not(.mine) {
  border-top-left-radius: 4px;
  border-bottom-left-radius: 4px;
}

/* Highlight */
.highlight {
  background: #fef08a;
  color: #000;
  padding: 0 2px;
  border-radius: 2px;
}

/* Mobile */
@media (max-width: 768px) {
  .message-item {
    max-width: 85%;
    font-size: 15px;
  }
  .avatar {
    width: 28px;
    height: 28px;
    font-size: 12px;
  }
  .actions {
    display: none !important; /* Ocultar acciones flotantes en móvil, usar long-press o menú en el futuro */
  }
}
</style>
