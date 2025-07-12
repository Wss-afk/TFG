<template>
  <div class="user-list">
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