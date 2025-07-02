<template>
  <div class="profile-container">
    <h2>个人信息</h2>
    <form @submit.prevent="save">
      <input v-model="username" placeholder="用户名" />
      <input v-model="email" placeholder="邮箱" />
      <button type="submit">保存</button>
    </form>
  </div>
</template>

<script>
export default {
  name: 'Profile',
  data() {
    return {
      username: '',
      email: ''
    }
  },
  methods: {
    save() {
      // 保存逻辑待实现
      this.$message.success('保存成功！')
    }
  }
}
</script>

<style scoped>
.profile-container {
  max-width: 400px;
  margin: 60px auto;
  padding: 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  text-align: center;
}
input {
  display: block;
  width: 100%;
  margin: 12px 0;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #ccc;
}
button {
  width: 100%;
  padding: 8px;
  background: #67c23a;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>