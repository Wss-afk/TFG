<template>
  <div :class="['message-row', isMine ? 'mine' : '']">
    <div class="avatar" :class="isMine ? 'mine' : ''" :title="senderName">{{ senderInitial }}</div>
    <div :class="['message-item', isMine ? 'mine' : '']">
      <span class="sender">{{ typeof message.sender === 'object' && message.sender ? message.sender.username : message.sender }}:</span>
      <div class="content">
        <template v-if="message.type === 'image' && message.fileUrl">
          <a :href="message.fileUrl" target="_blank" rel="noopener noreferrer">
            <img :src="message.fileUrl" alt="imagen adjunta" class="attachment-image" />
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
          {{ message.content }}
        </template>
      </div>
      <span class="timestamp">{{ formattedTime }}</span>
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
    }
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
    }
  }
}
</script>

<style scoped>
.message-row { display: flex; align-items: flex-end; gap: 8px; margin-bottom: 8px; }
.message-row.mine { flex-direction: row-reverse; justify-content: flex-start; }

.avatar { width: 28px; height: 28px; border-radius: 50%; background: #e2e8f0; color: #1f2937; display: flex; align-items: center; justify-content: center; font-weight: 700; box-shadow: 0 2px 6px rgba(0,0,0,0.12); flex: 0 0 28px; }
.avatar.mine { background: linear-gradient(135deg, var(--color-secondary), #2563eb); color: #fff; }

.message-item {
  margin-bottom: 12px;
  padding: 10px 14px;
  border-radius: 18px;
  max-width: 68%;
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
  background: var(--color-secondary);
  color: #ffffff;
  text-align: left;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
}

/* sin colas: estilo pill */

.message-item:not(.mine) {
  background: var(--color-bg-gradient-start);
  color: var(--text-primary);
  text-align: left;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--border-color);
}

/* sin colas: estilo pill */

.sender { display: none; }

/* remitente oculto para aproximar al mockup */

.content {
  word-break: break-word;
  line-height: 1.4;
  font-size: 15px;
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
.message-item:not(.mine) .timestamp { color: #64748b; }

.message-item.mine .attachment-file { color: #fff; }
.message-item.mine .caption { color: rgba(255,255,255,0.9); }

.timestamp {
  position: static;
  display: block;
  margin-top: 6px;
  font-size: 12px;
  opacity: 0.7;
  font-weight: 500;
}

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