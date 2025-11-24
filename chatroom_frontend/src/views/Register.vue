<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h2>Crear cuenta</h2>
        <p class="register-subtitle">Regístrate para comenzar a chatear</p>
      </div>
      <form @submit.prevent="register" class="register-form">
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
          autocomplete="new-password"
          required
        />
        <input
          v-model="confirmPassword"
          type="password"
          placeholder="Confirmar contraseña"
          aria-label="Confirmar contraseña"
          autocomplete="new-password"
          required
        />
        <button type="submit">Registrarse</button>
      </form>
      <p class="register-footer">¿Ya tienes cuenta? <router-link to="/login">Inicia sesión</router-link></p>
    </div>
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
        alert('Las contraseñas no coinciden')
        return
      }
      try {
        const { register } = await import('../services/auth.service.js')
        await register(this.username, this.password)
        this.$router.push('/login')
      } catch (error) {
        alert('Registro fallido: ' + (error.response?.data?.message || error.message))
      }
    }
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-bg-gradient-start), var(--color-bg-gradient-end));
  padding: 24px;
}

.register-card {
  width: 100%;
  max-width: 420px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.15);
  overflow: hidden;
  animation: cardIn 300ms ease-out;
}

.register-header {
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  color: #fff;
  padding: 24px 20px 20px;
  text-align: center;
}
.register-header h2 {
  margin: 0;
  font-weight: 700;
}
.register-subtitle { margin-top: 6px; opacity: 0.9; }

.register-form { padding: 20px; }
.register-form input {
  display: block;
  width: 100%;
  margin: 12px 0;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid #dcdfe6;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.register-form input:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(32, 160, 255, 0.2);
}
.register-form button:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}
.register-form button {
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
.register-form button:hover { box-shadow: 0 6px 14px rgba(32, 160, 255, 0.35); }
.register-form button:active { transform: translateY(1px); }

.register-footer { padding: 0 20px 20px; text-align: center; color: var(--color-text); }
.register-footer a { color: var(--color-primary); font-weight: 600; }

@keyframes cardIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
