<template>
  <div class="register-container">
    <h2>注册</h2>
    <form @submit.prevent="register">
      <input v-model="username" placeholder="用户名" required />
      <input v-model="password" type="password" placeholder="密码" required />
      <input v-model="confirmPassword" type="password" placeholder="确认密码" required />
      <button type="submit">注册</button>
    </form>
    <p>已有账号？<router-link to="/login">登录</router-link></p>
  </div>
</template>

<script>
export default {
  name: 'RegisterPage',
  data() {
    return {
      username: '',
      password: '',
      confirmPassword: ''
    }
  },
  methods: {
    async register() {
      if (this.password !== this.confirmPassword) {
        alert('两次输入的密码不一致')
        return
      }
      try {
        const { register } = await import('../services/auth.service.js')
        await register(this.username, this.password)
        this.$router.push('/login')
      } catch (error) {
        alert('注册失败: ' + (error.response?.data?.message || error.message))
      }
    }
  }
}
</script>

<style scoped>
.register-container {
  max-width: 320px;
  margin: 80px auto;
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