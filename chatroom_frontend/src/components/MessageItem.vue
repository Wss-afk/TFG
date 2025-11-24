<template>
  <div :class="['message-row', isMine ? 'mine' : '', continuation ? 'continuation' : '']">
    <div
      class="avatar"
      :class="isMine ? 'mine' : ''"
      :title="senderName"
      role="img"
      :aria-label="'Avatar de ' + (senderName || 'usuario')"
    >
      {{ senderInitial }}
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
      <div class="actions" aria-hidden="true">
        <span class="action-btn" title="Añadir reacción">🙂</span>
        <Icon name="reply" :size="16" />
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
.message-row { display: flex; align-items: flex-end; gap: var(--space-2); margin-bottom: var(--space-2); }
.message-row.mine { flex-direction: row-reverse; justify-content: flex-start; }

.avatar { width: 28px; height: 28px; border-radius: 50%; background: #e2e8f0; color: #1f2937; display: flex; align-items: center; justify-content: center; font-weight: 700; box-shadow: var(--shadow-1); flex: 0 0 28px; }
.avatar.mine { background: linear-gradient(135deg, var(--color-secondary), #2563eb); color: #fff; }

.message-item {
  margin-bottom: var(--space-3);
  padding: 10px 14px;
  border-radius: 16px;
  max-width: min(68%, 48ch);
  display: inline-block;
  position: relative;
  word-wrap: break-word;
  animation: messageSlideIn 0.3s ease-out;
  transition: all 0.2s ease;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-item:hover { transform: translateY(-1px); }

.message-item.mine {
  background: var(--color-primary);
  color: var(--color-primary-contrast);
  text-align: left;
  box-shadow: var(--shadow-2);
}

/* sin colas: estilo pill */

.message-item:not(.mine) {
  background: var(--color-surface);
  color: var(--text-primary);
  text-align: left;
  box-shadow: var(--shadow-1);
  border: 1px solid var(--border-color);
}

/* sin colas: estilo pill */

.sender { display: none; }

/* remitente oculto para aproximar al mockup */

.content {
  word-break: break-word;
  line-height: 1.4;
  font-size: var(--font-size-md);
}

.highlight {
  background: #fff59a;
  color: inherit;
  border-radius: 2px;
  padding: 0 1px;
}

.attachment-image {
  max-width: 220px;
  max-height: 220px;
  border-radius: 12px;
  display: block;
}

.attachment-file {
  color: #2b6cb0;
  text-decoration: none;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.attachment-file:hover { text-decoration: underline; }

.caption {
  margin-top: 6px;
  font-size: 13px;
  opacity: 0.85;
}

.message-item.mine .timestamp { color: rgba(255, 255, 255, 0.85); }
.message-item:not(.mine) .timestamp { color: var(--color-text-muted); }

.message-item.mine .attachment-file { color: #fff; }
.message-item.mine .caption { color: rgba(255,255,255,0.9); }

.timestamp {
  position: static;
  display: flex;
  gap: 6px;
  align-items: center;
  margin-top: 6px;
  font-size: var(--font-size-sm);
  opacity: 0.75;
  font-weight: 500;
}
.status-icon {
  font-size: 11px;
  opacity: 0.85;
}

/* Agrupación visual de mensajes consecutivos */
.message-row.continuation .avatar { visibility: hidden; width: 0; height: 0; margin: 0; }
.message-row.continuation .message-item { margin-top: 2px; border-top-left-radius: 10px; border-top-right-radius: 10px; }
.message-row.mine.continuation .message-item { border-top-right-radius: 10px; }

/* Barra de acciones (reacciones / responder) */
.message-item:hover .actions { opacity: 1; pointer-events: auto; }
.actions {
  position: absolute;
  top: -18px;
  right: 6px;
  display: inline-flex;
  gap: 6px;
  background: var(--color-surface);
  border: 1px solid var(--border-color);
  border-radius: 9999px;
  padding: 2px 6px;
  box-shadow: var(--shadow-1);
  opacity: 0;
  transition: opacity .15s ease;
}
.action-btn { cursor: pointer; font-size: 14px; line-height: 1; }

/* 响应式设计 */
@media (max-width: 768px) {
  .message-item {
    max-width: 85%;
    padding: 10px 14px 18px 14px;
    margin-bottom: 12px;
    border-radius: 16px;
  }
  
  .sender { display: none; }
  
  .content {
    font-size: 14px;
    line-height: 1.3;
  }
  
  .timestamp { font-size: 11px; }
}

@media (max-width: 480px) {
  .message-item {
    max-width: 90%;
    padding: 8px 12px 16px 12px;
    margin-bottom: 10px;
    border-radius: 14px;
  }
  
  .sender { display: none; }
  
  .content {
    font-size: 13px;
    line-height: 1.25;
    margin: 2px 0;
  }
  
  .timestamp { font-size: 10px; }
  
  .message-item:hover {
    transform: none; /* 移动设备上禁用hover效果 */
  }
}
</style>
