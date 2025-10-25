<template>
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
          📎 {{ message.content || 'Archivo' }}
        </a>
      </template>
      <template v-else>
        {{ message.content }}
      </template>
    </div>
    <span class="timestamp">{{ formattedTime }}</span>
  </div>
</template>

<script>
export default {
  name: 'MessageItem',
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
    formattedTime() {
      if (!this.message.timestamp) return '';
      const date = new Date(this.message.timestamp);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
  }
}
</script>

<style scoped>
.message-item {
  margin-bottom: 16px;
  padding: 12px 16px 20px 16px;
  border-radius: 18px;
  max-width: 70%;
  clear: both;
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

.message-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.message-item.mine {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  float: right;
  text-align: right;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
  border-bottom-right-radius: 4px;
}

.message-item.mine::before {
  content: '';
  position: absolute;
  bottom: 0;
  right: -8px;
  width: 0;
  height: 0;
  border: 8px solid transparent;
  border-left-color: #764ba2;
  border-bottom: none;
  border-right: none;
}

.message-item:not(.mine) {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  color: #2d3748;
  float: left;
  text-align: left;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-bottom-left-radius: 4px;
}

.message-item:not(.mine)::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: -8px;
  width: 0;
  height: 0;
  border: 8px solid transparent;
  border-right-color: #ffffff;
  border-bottom: none;
  border-left: none;
}

.sender {
  font-weight: 600;
  margin-right: 8px;
  font-size: 0.9em;
  opacity: 0.9;
}

.message-item.mine .sender {
  color: #ffd700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.message-item:not(.mine) .sender {
  color: #667eea;
}

.content {
  word-break: break-word;
  line-height: 1.4;
  font-size: 15px;
  margin: 4px 0;
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
}
.attachment-file:hover { text-decoration: underline; }

.caption {
  margin-top: 6px;
  font-size: 13px;
  opacity: 0.85;
}

.message-item.mine .timestamp {
  color: rgba(255, 255, 255, 0.8);
}

.message-item:not(.mine) .timestamp {
  color: #718096;
}

.message-item.mine .attachment-file { color: #fff; }
.message-item.mine .caption { color: rgba(255,255,255,0.9); }

.timestamp {
  position: absolute;
  right: 12px;
  bottom: 4px;
  font-size: 0.7em;
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
  
  .sender {
    font-size: 0.85em;
    margin-right: 6px;
  }
  
  .content {
    font-size: 14px;
    line-height: 1.3;
  }
  
  .timestamp {
    font-size: 0.65em;
    right: 10px;
    bottom: 3px;
  }
  
  .message-item.mine::before,
  .message-item:not(.mine)::before {
    border-width: 6px;
  }
  
  .message-item.mine::before {
    right: -6px;
  }
  
  .message-item:not(.mine)::before {
    left: -6px;
  }
}

@media (max-width: 480px) {
  .message-item {
    max-width: 90%;
    padding: 8px 12px 16px 12px;
    margin-bottom: 10px;
    border-radius: 14px;
  }
  
  .sender {
    font-size: 0.8em;
    margin-right: 4px;
  }
  
  .content {
    font-size: 13px;
    line-height: 1.25;
    margin: 2px 0;
  }
  
  .timestamp {
    font-size: 0.6em;
    right: 8px;
    bottom: 2px;
  }
  
  .message-item:hover {
    transform: none; /* 移动设备上禁用hover效果 */
  }
  
  .message-item.mine::before,
  .message-item:not(.mine)::before {
    border-width: 5px;
  }
  
  .message-item.mine::before {
    right: -5px;
  }
  
  .message-item:not(.mine)::before {
    left: -5px;
  }
}
</style>