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
    <div class="messages" ref="messagesContainer">
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
    chatGroup: {
      type: Object,
      default: null
    },
    chatType: {
      type: String,
      default: 'user'
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
</style>