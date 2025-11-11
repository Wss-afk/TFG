<template>
  <aside class="app-dock" aria-label="Aplicaciones">
    <div class="brand">
      <svg class="brand-logo" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <defs>
          <linearGradient id="ch-grad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="var(--brand-gradient-start)"/>
            <stop offset="100%" stop-color="var(--brand-gradient-end)"/>
          </linearGradient>
        </defs>
        <!-- Logo moderno inventado: triángulo dinámico + círculo acento -->
        <polygon points="12,46 52,34 22,12" fill="url(#ch-grad)" opacity="0.95"/>
        <circle cx="46" cy="18" r="7" fill="var(--brand-gradient-end)" opacity="0.9"/>
      </svg>
      <span class="brand-name">ConnectHub</span>
    </div>
    <ul class="menu">
      <li v-for="item in items" :key="item.label" :class="['menu-item', isActive(item.path) && 'active']" @click="go(item.path)" :aria-label="item.label">
        <span class="icon" aria-hidden="true">{{ item.icon }}</span>
        <span class="label">{{ item.label }}</span>
      </li>
    </ul>
    <div class="dock-footer">
      <div v-if="currentUser" class="current-user" aria-label="Usuario conectado">
        <img v-if="currentUser.avatarUrl" :src="currentUser.avatarUrl" class="cu-avatar" />
        <div class="cu-info">
          <div class="cu-name">{{ currentUser.username }}</div>
          <div class="cu-sub">Conectado</div>
        </div>
        <button type="button" class="cu-menu" title="Opciones" aria-label="Opciones">⋮</button>
      </div>
      <div class="footer-divider" aria-hidden="true"></div>
      <button type="button" class="logout-btn" aria-label="Cerrar sesión" @click="logout">Cerrar sesión</button>
    </div>
  </aside>
</template>

<script>
import { mapGetters } from 'vuex'
import { disconnectWebSocket } from '../services/websocket.js'
export default {
  name: 'AppDock',
  computed: {
    ...mapGetters('auth', ['currentUser']),
    items() {
      // Si es SUPER_ADMIN, limitar el dock al panel de admin
      if (this.currentUser && this.currentUser.role === 'SUPER_ADMIN') {
        return [
          { label: 'Admin', icon: '🛡️', path: '/admin' },
          { label: 'Setting', icon: '⚙️', path: null }
        ]
      }
      return [
        { label: 'Home', icon: '🏠', path: '/home' },
        { label: 'Eventos', icon: '📆', path: '/events' },
        { label: 'Messages', icon: '📫', path: '/chat' },
        { label: 'Setting', icon: '⚙️', path: null }
      ]
    }
  },
  methods: {
    go(path) {
      if (!path) return
      const exists = this.$router && this.$router.getRoutes && this.$router.getRoutes().some(r => r.path === path)
      if (exists && this.$route.path !== path) this.$router.push(path)
    },
    isActive(path) {
      if (!path || !this.$route) return false
      return this.$route.path === path || this.$route.path.startsWith(path)
    },
    logout() {
      try {
        this.$store.dispatch('auth/logout')
      } catch (e) {
        console.error('Error al ejecutar logout en store:', e)
      }
      disconnectWebSocket()
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.app-dock {
  flex: 0 0 220px; /* ampliar caja del dock */
  background: #ffffff;
  border-right: 1px solid rgba(226, 232, 240, 0.8);
  display: flex;
  flex-direction: column;
  padding: 10px 10px; /* más espacio interior para el título */
  height: 100vh;
  overflow-y: auto;
  margin-right: 18px; /* más espacio hacia la derecha */
}

.brand {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 10px 18px 10px; /* aumenta separación del borde */
  border-bottom: 1px solid rgba(226,232,240,0.8);
}
.brand-logo { width: 32px; height: 32px; filter: drop-shadow(0 4px 10px rgba(79,70,229,.25)); }
.brand-name {
  font-size: 24px; font-weight: 800; letter-spacing: .4px; color: #1f2937;
}

.menu { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 14px; }
.menu-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border-radius: 12px; cursor: pointer;
  color: #94a3b8; background: transparent; transition: all .2s ease;
}
.menu-item .icon { width: 24px; text-align: center; font-size: 18px; }
.menu-item .label { font-weight: 600; letter-spacing: .2px; }
.menu-item:hover { background: #f1f5f9; color: #475569; }
.menu-item.active { color: var(--brand-gradient-start); background: var(--color-bg-gradient-start); }

.dock-footer {
  margin-top: auto;
  padding: 16px 0 12px;
  position: sticky;
  bottom: 0; /* coloca el botón más abajo, pegado al borde inferior */
  background: #ffffff;
}
.logout-btn {
  width: 100%;
  padding: 10px 12px;
  border-radius: 12px;
  border: none;
  background: var(--color-bg-gradient-start);
  color: var(--brand-gradient-start);
  font-weight: 700;
  cursor: pointer;
  box-shadow: var(--shadow);
}
.logout-btn:hover { background: var(--surface-alt); color: var(--brand-gradient-end); }

/* Tarjeta de usuario actual, encima del botón de cerrar sesión */
.current-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #ffffff; /* fondo blanco */
  border: 1px solid var(--border-color);
  color: #334155;
  box-shadow: var(--shadow);
}
.cu-avatar { width: 34px; height: 34px; border-radius: 50%; object-fit: cover; border: 2px solid rgba(255,255,255,0.35); }
.cu-info { flex: 1; min-width: 0; }
.cu-name { font-weight: 700; font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cu-sub { font-size: 12px; color: #64748b; }
.cu-menu {
  border: none;
  background: #f1f5f9;
  color: #334155;
  width: 28px; height: 28px; border-radius: 50%; cursor: pointer;
}
.cu-menu:hover { background: #e2e8f0; }

/* Línea divisoria entre tarjeta de usuario y botón de salida */
.footer-divider {
  height: 1px;
  background: #111827; /* negro/dark */
  opacity: 0.25;
  margin: 10px 0 12px;
  border-radius: 9999px;
}

@media (max-width: 768px) {
  .app-dock { display: none; }
}
</style>