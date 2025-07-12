<template>
  <div :class="['message-item', isMine ? 'mine' : '']">
    <span class="sender">{{ typeof message.sender === 'object' && message.sender ? message.sender.username : message.sender }}:</span>
    <span class="content">{{ message.content }}</span>
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
  margin-bottom: 8px;
  padding: 6px 10px 18px 10px;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
  max-width: 60%;
  clear: both;
  display: inline-block;
  position: relative;
}
.message-item.mine {
  background: #d4f8e8;
  float: right;
  text-align: right;
}
.message-item:not(.mine) {
  float: left;
  text-align: left;
}
.sender {
  font-weight: bold;
  margin-right: 6px;
}
.content {
  word-break: break-all;
}
.timestamp {
  position: absolute;
  right: 10px;
  bottom: 2px;
  font-size: 0.75em;
  color: #888;
}
</style>