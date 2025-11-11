<template>
  <div class="admin-container">
    <div class="admin-header d-flex align-items-center justify-content-between">
      <h2 class="m-0">Panel de Super Admin</h2>
      <div class="admin-actions d-flex align-items-center">
        <span v-if="currentUser" class="badge bg-primary">{{ currentUser.username }}</span>
        <button type="button" class="admin-logout-btn btn btn-sm ms-2" @click="logout">Cerrar sesión</button>
      </div>
    </div>

    <div v-if="!isSuperAdmin" class="alert alert-warning">No tienes permisos de SUPER_ADMIN.</div>

    <div v-else>
      <ul class="nav nav-tabs mb-3">
        <li class="nav-item">
          <router-link class="nav-link" :class="{ active: isActive('/admin/users') }" to="/admin/users">Usuarios</router-link>
        </li>
        <li class="nav-item">
          <router-link class="nav-link" :class="{ active: isActive('/admin/groups') }" to="/admin/groups">Grupos</router-link>
        </li>
        <li class="nav-item">
          <router-link class="nav-link" :class="{ active: isActive('/admin/audit') }" to="/admin/audit">Auditoría</router-link>
        </li>
      </ul>

      <div class="card">
        <div class="card-body">
          <router-view />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { disconnectWebSocket } from '../services/websocket.js'

export default {
  name: 'Admin',
  computed: {
    ...mapGetters('auth', ['currentUser']),
    isSuperAdmin() {
      return this.currentUser && this.currentUser.role === 'SUPER_ADMIN'
    }
  },
  methods: {
    isActive(path) {
      return this.$route.path.startsWith(path)
    },
    logout() {
      try {
        this.$store.dispatch('auth/logout')
      } catch (e) {
        console.error('Error al ejecutar logout en store:', e)
      }
      try {
        disconnectWebSocket()
      } catch (e) {
        console.error('Error al cerrar WebSocket:', e)
      }
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.admin-container {
  max-width: 1100px;
  margin: 24px auto;
  padding: 16px;
}
.admin-actions { gap: 8px; }
.nav-link {
  cursor: pointer;
}
.admin-logout-btn {
  background: #1f2937; /* gris oscuro para alto contraste */
  color: #ffffff;
  border: none;
  padding: 6px 10px;
  border-radius: 6px;
}
.admin-logout-btn:hover {
  background: #111827;
  color: #ffffff;
}
</style>