<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h2>Iniciar sesión</h2>
        <p class="login-subtitle">Accede para continuar al chat</p>
      </div>
      <form @submit.prevent="login" class="login-form">
        <input
          v-model="username"
          placeholder="Usuario"
          aria-label="Usuario"
          autocomplete="username"
          required
          autofocus
        />
        <input
          v-model="password"
          type="password"
          placeholder="Contraseña"
          aria-label="Contraseña"
          autocomplete="current-password"
          required
        />
        <button type="submit">Entrar</button>
      </form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoginPage',
  data() {
    return {
      username: '',
      password: '',
      alertMessage: ''
    }
  },
  mounted() {
    // Comprobar si hay mensaje de logout forzado
    const msg = sessionStorage.getItem('logout_message')
    if (msg) {
      this.alertMessage = msg
      sessionStorage.removeItem('logout_message')
      setTimeout(() => { alert(msg) }, 200) // Pequeño delay para asegurar que la vista cargó
    }
  },
  methods: {
    async login() {
      try {
        const { login } = await import('../services/auth.service.js')
        const res = await login(this.username, this.password)
        this.$store.dispatch('auth/login', { user: res.data })
        const role = res.data?.role
        this.$router.push(role === 'SUPER_ADMIN' ? '/admin' : '/home')
      } catch (error) {
        if (error.response) {
          if (error.response.status === 403) {
            alert('Tu cuenta ha sido deshabilitada por un Super Admin');
          } else if (error.response.status === 401) {
            alert('Usuario o contraseña incorrectos');
          } else {
            alert('Inicio de sesión fallido: ' + (error.response.data?.message || error.message));
          }
        } else {
          alert('Inicio de sesión fallido: ' + error.message)
        }
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-bg-gradient-start), var(--color-bg-gradient-end));
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 380px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.15);
  overflow: hidden;
  animation: cardIn 300ms ease-out;
}

.login-header {
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  color: #fff;
  padding: 24px 20px 20px;
  text-align: center;
}
.login-header h2 {
  margin: 0;
  font-weight: 700;
}
.login-subtitle {
  margin-top: 6px;
  opacity: 0.9;
}

.login-form {
  padding: 20px;
}
.login-form input {
  display: block;
  width: 100%;
  margin: 12px 0;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid #dcdfe6;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.login-form input:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(32, 160, 255, 0.2);
}
.login-form button:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}
.login-form button {
  width: 100%;
  margin-top: 8px;
  padding: 12px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
  transition: transform 0.08s ease, box-shadow 0.2s ease;
}
.login-form button:hover {
  box-shadow: 0 6px 14px rgba(32, 160, 255, 0.35);
}
.login-form button:active {
  transform: translateY(1px);
}

.login-footer {
  padding: 0 20px 20px;
  text-align: center;
  color: var(--color-text);
}
.login-footer a {
  color: var(--color-primary);
  font-weight: 600;
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
