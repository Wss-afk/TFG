<template>
  <div class="user-list">
    <ul>
      <li v-for="user in otherUsers" :key="user.id" @click="$emit('select', user)" class="user-item" :class="{ active: selectedUser && selectedUser.id === user.id, online: isUserOnline(user), offline: !isUserOnline(user) }">
        <img v-if="user.avatarUrl" :src="user.avatarUrl" class="avatar" />
        <span class="username">{{ user.username }}</span>
        <span v-if="unreadCounts[user.id] > 0" class="unread-badge">{{ unreadCounts[user.id] }}</span>
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
    },
    unreadCounts: {
      type: Object,
      default: () => ({})
    },
    selectedUser: {
      type: Object,
      default: null
    },
    onlineUsers: {
      type: Array,
      default: () => []
    }
  },
  computed: {
    ...mapGetters('auth', ['currentUser']),
    otherUsers() {
      return this.users.filter(u => !this.currentUser || u.id !== this.currentUser.id)
    }
  },
  methods: {
    isUserOnline(user) {
      // 基于真实的在线用户列表判断用户是否在线（兼容不同字段）
      if (!Array.isArray(this.onlineUsers) || this.onlineUsers.length === 0) return false
      return this.onlineUsers.some(ou => {
        const ouId = ou.id ?? ou.userId
        return (ouId !== undefined && ouId === user.id) || (ou.username && ou.username === user.username)
      })
    }
  }
}
</script>

<style scoped>
.user-list {
  padding: 12px 16px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  margin-right: 12px;
  border: 2px solid rgba(255,255,255,0.8);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

li {
  padding: 16px 20px;
  cursor: pointer;
  border-radius: 16px;
  display: flex;
  align-items: center;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(145deg, rgba(255,255,255,0.9) 0%, rgba(248,250,252,0.9) 100%);
  border: 1px solid rgba(226, 232, 240, 0.6);
  backdrop-filter: blur(10px);
  box-shadow: 
    0 2px 8px rgba(0,0,0,0.04),
    inset 0 1px 0 rgba(255,255,255,0.8);
}

li::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 16px;
  background: linear-gradient(145deg, transparent, rgba(102, 126, 234, 0.02));
  opacity: 0;
  transition: opacity 0.3s ease;
}

li:hover {
  transform: translateY(-2px);
  box-shadow: 
    0 8px 24px rgba(0,0,0,0.08),
    0 4px 12px rgba(102, 126, 234, 0.1),
    inset 0 1px 0 rgba(255,255,255,0.9);
  border-color: rgba(102, 126, 234, 0.2);
}

li:hover::before {
  opacity: 1;
}

li:hover .avatar {
  transform: scale(1.05);
  border-color: rgba(102, 126, 234, 0.3);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

li.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: rgba(102, 126, 234, 0.3);
  box-shadow: 
    0 8px 24px rgba(102, 126, 234, 0.3),
    0 4px 12px rgba(0,0,0,0.1),
    inset 0 1px 0 rgba(255,255,255,0.2);
  transform: translateY(-1px);
}

li.active::before {
  background: linear-gradient(145deg, rgba(255,255,255,0.1), transparent);
  opacity: 1;
}

li.active:hover {
  background: linear-gradient(135deg, #7c8aed 0%, #8b5fbf 100%);
  transform: translateY(-3px);
  box-shadow: 
    0 12px 32px rgba(102, 126, 234, 0.4),
    0 6px 16px rgba(0,0,0,0.1),
    inset 0 1px 0 rgba(255,255,255,0.3);
}

li.active .avatar {
  border-color: rgba(255,255,255,0.9);
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}

li.active:hover .avatar {
  transform: scale(1.08);
  box-shadow: 0 6px 16px rgba(0,0,0,0.25);
}

.user-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  position: relative;
  z-index: 1;
}

.username {
  flex: 1;
  font-weight: 500;
  font-size: 14px;
  color: #334155;
  transition: color 0.3s ease;
  letter-spacing: 0.2px;
}

li.active .username {
  color: white;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.unread-badge {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  border-radius: 50%;
  min-width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  margin-left: 12px;
  box-shadow: 
    0 2px 8px rgba(239, 68, 68, 0.4),
    0 1px 3px rgba(0,0,0,0.1);
  animation: pulse 2s infinite;
  position: relative;
  overflow: hidden;
}

.unread-badge::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent, rgba(255,255,255,0.3), transparent);
  animation: shine 2s infinite;
}

@keyframes pulse {
  0% { 
    transform: scale(1);
    box-shadow: 
      0 2px 8px rgba(239, 68, 68, 0.4),
      0 1px 3px rgba(0,0,0,0.1);
  }
  50% { 
    transform: scale(1.1);
    box-shadow: 
      0 4px 12px rgba(239, 68, 68, 0.6),
      0 2px 6px rgba(0,0,0,0.15);
  }
  100% { 
    transform: scale(1);
    box-shadow: 
      0 2px 8px rgba(239, 68, 68, 0.4),
      0 1px 3px rgba(0,0,0,0.1);
  }
}

@keyframes shine {
  0% { transform: translateX(-100%) translateY(-100%) rotate(45deg); }
  50% { transform: translateX(100%) translateY(100%) rotate(45deg); }
  100% { transform: translateX(-100%) translateY(-100%) rotate(45deg); }
}

/* 添加用户状态指示器 */
li::after {
  content: '';
  position: absolute;
  right: 8px;
  top: 8px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  opacity: 0.8;
}

/* 在线状态 - 绿色 */
li.online::after {
  background: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
  animation: online-pulse 3s infinite;
}

/* 离线状态 - 灰色 */
li.offline::after {
  background: #9ca3af;
  box-shadow: 0 0 0 2px rgba(156, 163, 175, 0.2);
}

/* 选中状态下保持原有颜色，只是增加白色边框 */
li.active.online::after {
  background: #10b981;
  box-shadow: 0 0 0 2px rgba(255,255,255,0.8), 0 0 0 4px rgba(16, 185, 129, 0.3);
  animation: online-pulse 3s infinite;
}

li.active.offline::after {
  background: #9ca3af;
  box-shadow: 0 0 0 2px rgba(255,255,255,0.8), 0 0 0 4px rgba(156, 163, 175, 0.3);
}

@keyframes online-pulse {
  0% { box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2); }
  50% { box-shadow: 0 0 0 6px rgba(16, 185, 129, 0.1); }
  100% { box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-list {
    padding: 8px 12px;
  }
  
  li {
    padding: 12px 16px;
  }
  
  .avatar {
    width: 28px;
    height: 28px;
    margin-right: 10px;
  }
  
  .username {
    font-size: 13px;
  }
  
  .unread-badge {
    min-width: 18px;
    height: 18px;
    font-size: 10px;
    margin-left: 8px;
  }
}

@media (max-width: 480px) {
  .user-list {
    padding: 6px 8px;
  }
  
  li {
    padding: 10px 12px;
    border-radius: 12px;
  }
  
  .avatar {
    width: 24px;
    height: 24px;
    margin-right: 8px;
  }
  
  .username {
    font-size: 12px;
  }
  
  .unread-badge {
    min-width: 16px;
    height: 16px;
    font-size: 9px;
    margin-left: 6px;
  }
  
  li::after {
    width: 6px;
    height: 6px;
    right: 6px;
    top: 6px;
  }
}
</style>