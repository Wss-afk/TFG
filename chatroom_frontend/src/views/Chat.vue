<template>
  <div class="chat-page">
    <div class="sidebar">
      <UserList :users="sortedUsers" @select="selectUser" />
      <GroupList :groups="groups" @select="selectGroup" />
    </div>
    <div class="main-chat">
      <ChatWindow :messages="messages" :chatUser="selectedUser">
        <div class="chat-input-area">
          <input v-model="inputMsg" @keyup.enter="send" placeholder="输入消息..." />
          <button @click="send">发送</button>
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
      selectedUser: null
    }
  },
  methods: {
    async selectUser(user) {
      this.selectedUser = user
      const { fetchMessages } = await import('../services/chat.service.js')
      const res = await fetchMessages({ receiverId: user.id })
      this.messages = res.data
    },
    selectGroup() {
      // 切换到群聊逻辑
    },
    async send() {
      if (this.inputMsg.trim() && this.selectedUser) {
        const { sendMessage } = await import('../services/chat.service.js')
        await sendMessage({ receiverId: this.selectedUser.id, content: this.inputMsg })
        this.messages.push({ sender: '我', content: this.inputMsg })
        this.inputMsg = ''
      }
    }
  },
  async mounted() {
    const { fetchUsers, fetchGroups } = await import('../services/user.service.js')
    const res = await fetchUsers()
    this.users = res.data
    const groupRes = await fetchGroups()
    this.groups = groupRes.data
  },
}
</script>

<style scoped>
.chat-page {
  display: flex;
  max-width: 1000px;
  margin: 40px auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  min-height: 600px;
}
.sidebar {
  width: 220px;
  border-right: 1px solid #eee;
  padding: 16px 0 16px 16px;
  background: #fafbfc;
}
.main-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
}
.chat-input-area {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.chat-input-area input {
  flex: 1;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #ccc;
}
.chat-input-area button {
  padding: 8px 16px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>