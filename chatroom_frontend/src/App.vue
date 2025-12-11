<template>
  <router-view v-slot="{ Component }">
    <transition name="view-fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script>
import { setControlCallback } from './services/websocket'
import { checkStatus } from './services/auth.service'
import { mapActions, mapGetters } from 'vuex'

export default {
  name: 'App',
  created() {
    // Configurar listener global para mensajes de control (logout forzado, etc.)
    setControlCallback(this.handleControlMessage)
    
    // Verificar estado de cuenta al iniciar (si hay usuario logueado)
    if (this.isAuthenticated && this.currentUser?.username) {
      this.verifyAccountStatus()
    }

    // Verificar periódicamente (cada 30s) para asegurar consistencia
    this.statusInterval = setInterval(() => {
      if (this.isAuthenticated && this.currentUser?.username) {
        this.verifyAccountStatus()
      }
    }, 30000)
  },
  beforeUnmount() {
    if (this.statusInterval) clearInterval(this.statusInterval)
  },
  computed: {
    ...mapGetters('auth', ['isAuthenticated', 'currentUser'])
  },
  methods: {
    ...mapActions('auth', ['logout']),
    
    async verifyAccountStatus() {
      try {
        await checkStatus(this.currentUser.username)
      } catch (error) {
        if (error.response && error.response.status === 403) {
          this.performForcedLogout('account_disabled')
        }
      }
    },

    performForcedLogout(reason) {
      this.logout()
      
      let msg = 'Tu sesión ha sido cerrada.'
      if (reason === 'account_disabled') {
        msg = 'Tu cuenta ha sido deshabilitada por un administrador.'
      } else if (reason === 'password_changed') {
        msg = 'Tu contraseña ha cambiado. Por favor inicia sesión de nuevo.'
      }
      
      // Guardar mensaje en sessionStorage para mostrarlo en Login
      sessionStorage.setItem('logout_message', msg)
      
      this.$router.push('/login')
    },

    handleControlMessage(payload) {
      console.warn('App.vue: handleControlMessage executed', payload)
      if (payload.action === 'force_logout') {
        this.performForcedLogout(payload.reason)
      }
    }
  }
}
</script>

<style>
#app, html, body {
  height: 100%;
  margin: 0;
}
#app { min-height: 100vh; }
#app {
  font-family: Inter, Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}

/* Transiciones entre vistas */
.view-fade-enter-active, .view-fade-leave-active { transition: opacity .2s ease, transform .2s ease; }
.view-fade-enter-from, .view-fade-leave-to { opacity: 0; transform: translateY(6px); }
</style>
