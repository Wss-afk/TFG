<template>
  <div class="group-list">
    <ul>
      <li v-for="group in groups" :key="group.id" @click="$emit('select', group)" class="group-item">
        <span class="group-name">{{ group.name }}</span>
        <span class="group-members">{{ group.users ? group.users.length : 0 }} 成员</span>
      </li>
    </ul>
  </div>
</template>

<script>
export default {
  name: 'GroupList',
  props: {
    groups: {
      type: Array,
      required: true
    }
  }
}
</script>

<style scoped>
.group-list {
  padding: 8px 0;
}

ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.group-item {
  padding: 12px;
  cursor: pointer;
  border-radius: 8px;
  margin-bottom: 4px;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.group-item:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.group-name {
  font-weight: 600;
  font-size: 14px;
}

.group-members {
  font-size: 12px;
  opacity: 0.7;
}
</style>