<template>
  <div class="login-container">
    <h2>登录</h2>
    <form @submit.prevent="login">
      <input v-model="username" placeholder="用户名" required />
      <input v-model="password" type="password" placeholder="密码" required />
      <button type="submit">登录</button>
    </form>
    <p>没有账号？<router-link to="/register">注册</router-link></p>
  </div>
</template>

<script>
export default {
  name: 'LoginPage',
  data() {
    return {
      username: '',
      password: ''
    }
  },
  methods: {
    async login() {
      try {
        const { login } = await import('../services/auth.service.js')
        const res = await login(this.username, this.password)
        this.$store.dispatch('auth/login', { user: res.data })
        const role = res.data && res.data.role
        this.$router.push(role === 'SUPER_ADMIN' ? '/admin' : '/chat')
      } catch (error) {
        if (error.response && error.response.status === 401) {
          alert('用户名或密码错误');
        } else {
          alert('登录失败: ' + (error.response?.data?.message || error.message));
        }
      }
    }
  }
}
</script>

<style scoped>
.login-container {
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
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>