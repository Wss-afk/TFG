<template>
  <div class="chat-window">
    <div class="chat-header" v-if="chatUser">
      当前与 <b>{{ chatUser.username }}</b> 的对话
    </div>
    <div class="messages">
      <MessageItem v-for="(msg, idx) in messages" :key="idx" :message="msg" :currentUserId="currentUserId" />
    </div>
    <slot></slot>
  </div>
</template>

<script>
import MessageItem from './MessageItem.vue'
export default {
  name: 'ChatWindow',
  components: { MessageItem },
  props: {
    messages: {
      type: Array,
      required: true
    },
    chatUser: {
      type: Object,
      default: null
    },
    currentUserId: {
      type: [String, Number],
      required: true
    }
  }
}
</script>

<style scoped>
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f9f9f9;
  border-radius: 8px;
  padding: 16px;
  overflow: hidden;
}
.messages {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 12px;
}
</style>