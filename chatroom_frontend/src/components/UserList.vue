<template>
  <div class="user-list">
    <div class="current-user-card" v-if="currentUser">
      <img v-if="currentUser.avatarUrl" :src="currentUser.avatarUrl" class="avatar" />
      <span class="username">{{ currentUser.username }}</span>
      <span class="self-label">(自己)</span>
    </div>
    <h4>用户列表</h4>
    <ul>
      <li v-for="user in otherUsers" :key="user.id" @click="$emit('select', user)">
        <img v-if="user.avatarUrl" :src="user.avatarUrl" class="avatar" />
        {{ user.username }}
      </li>
    </ul>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
export default {
  name: 'UserList',
  props: {
    users: {
      type: Array,
      required: true
    }
  },
  computed: {
    ...mapGetters('auth', ['currentUser']),
    otherUsers() {
      return this.users.filter(u => !this.currentUser || u.id !== this.currentUser.id)
    }
  }
}
</script>

<style scoped>
.user-list {
  padding: 8px 0;
}
.avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}
.current-user-card {
  display: flex;
  align-items: center;
  background: #f0f9ff;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 12px;
  font-weight: bold;
  box-shadow: 0 1px 4px rgba(64,158,255,0.08);
}
.current-user-card .avatar {
  width: 32px;
  height: 32px;
  margin-right: 10px;
}
.current-user-card .username {
  margin-right: 8px;
}
.current-user-card .self-label {
  color: #409eff;
}
ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
li {
  padding: 6px 12px;
  cursor: pointer;
  border-radius: 4px;
}
li:hover {
  background: #e6f7ff;
}
</style>