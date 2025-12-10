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
        <img v-if="currentUser.avatarUrl" :src="currentUser.avatarUrl" class="cu-avatar" @click="openAvatarPreview" />
        <div v-else class="cu-avatar placeholder" title="Añadir avatar" @click="triggerAvatarChange">
          <span class="cu-initial">{{ avatarInitial() }}</span>
        </div>
        <div class="cu-info">
          <div class="cu-name">{{ currentUser.username }}</div>
          <div class="cu-sub">Conectado</div>
        </div>
        <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="handleAvatarSelected" />
      </div>
      <div class="footer-divider" aria-hidden="true"></div>
      <button type="button" class="logout-btn" aria-label="Cerrar sesión" @click="logout">Cerrar sesión</button>
    </div>
    <div v-if="avatarPreviewOpen && currentUser && currentUser.avatarUrl" class="modal-backdrop" @click.self="closeAvatarPreview">
      <div class="modal-card">
        <img :src="currentUser.avatarUrl" class="avatar-preview" alt="Avatar" />
        <div class="modal-actions">
          <button type="button" class="modal-btn" @click="triggerAvatarChange">Cambiar avatar</button>
          <button type="button" class="modal-btn" @click="removeAvatar">Quitar avatar</button>
          <button type="button" class="modal-btn" @click="closeAvatarPreview">Cerrar</button>
        </div>
      </div>
    </div>
  </aside>
</template>

<script>
import { mapGetters } from 'vuex'
import { disconnectWebSocket } from '../services/websocket.js'
import { uploadAvatar } from '../services/chat.service.js'
import { updateAvatar } from '../services/user.service.js'
export default {
  name: 'AppDock',
  data() {
    return { avatarPreviewOpen: false }
  },
  computed: {
    ...mapGetters('auth', ['currentUser']),
    items() {
      // Si es SUPER_ADMIN, limitar el dock al panel de admin
      if (this.currentUser && this.currentUser.role === 'SUPER_ADMIN') {
        return [
          { label: 'Admin', icon: '🛡️', path: '/admin' }
        ]
      }
      return [
        { label: 'Inicio', icon: '🏠', path: '/home' },
        { label: 'Eventos', icon: '📆', path: '/events' },
        { label: 'Mensajes', icon: '📫', path: '/chat' }
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
    },
    openAvatarPreview() {
      this.avatarPreviewOpen = true
    },
    closeAvatarPreview() {
      this.avatarPreviewOpen = false
    },
    triggerAvatarChange() {
      const el = this.$refs.avatarInput
      if (el) el.click()
    },
    async handleAvatarSelected(evt) {
      try {
        const file = evt.target.files && evt.target.files[0]
        if (!file) return
        const res = await uploadAvatar(file)
        const { url, type } = res.data || {}
        if (!url || type !== 'image') {
          alert('Selecciona una imagen válida para tu avatar')
          return
        }
        const userId = this.currentUser && this.currentUser.id
        if (!userId) return
        const upd = await updateAvatar(userId, url)
        const updatedUser = upd?.data || { ...this.currentUser, avatarUrl: url }
        this.$store.commit('auth/SET_USER', updatedUser)
        evt.target.value = ''
        this.avatarPreviewOpen = false
      } catch (e) {
        console.error('Error actualizando avatar:', e)
        alert('No se pudo actualizar el avatar')
      }
    },
    async removeAvatar() {
      try {
        const userId = this.currentUser && this.currentUser.id
        if (!userId) return
        const upd = await updateAvatar(userId, '')
        const updatedUser = upd?.data || { ...this.currentUser, avatarUrl: null }
        this.$store.commit('auth/SET_USER', updatedUser)
        this.avatarPreviewOpen = false
      } catch (e) {
        console.error('Error quitando avatar:', e)
        alert('No se pudo quitar el avatar')
      }
    },
    avatarInitial() {
      const n = (this.currentUser && this.currentUser.username) || ''
      return n ? n.charAt(0).toUpperCase() : '👤'
    }
  }
  
}
</script>

<style scoped>
.app-dock {
  flex: 0 0 260px; /* ampliar caja del dock */
  background: #ffffff;
  border-right: 1px solid rgba(226, 232, 240, 0.8);
  display: flex;
  flex-direction: column;
  padding: 16px 20px; /* más espacio interior para el título */
  height: 100vh;
  overflow-y: auto;
  margin-right: 0; /* Reset margin, layout handled by parent */
  position: sticky; top: 0;
  z-index: 1000; /* Ensure dock and its modals stay on top of content */
}

.brand {
  display: flex; align-items: center; gap: 14px;
  padding: 12px 4px 24px 4px; /* aumenta separación del borde */
  border-bottom: 1px solid rgba(226,232,240,0.6);
  margin-bottom: 24px;
}
.brand-logo { width: 36px; height: 36px; filter: drop-shadow(0 4px 10px rgba(79,70,229,.25)); }
.brand-name {
  font-size: 22px; font-weight: 800; letter-spacing: -0.5px; color: #1e293b;
}

.menu { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.menu-item {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px; border-radius: 14px; cursor: pointer;
  color: #64748b; background: transparent; transition: all .2s ease;
  font-weight: 600;
}
.menu-item .icon { width: 24px; text-align: center; font-size: 20px; display: flex; align-items: center; justify-content: center; }
.menu-item .label { font-weight: 600; font-size: 0.95rem; letter-spacing: 0.2px; }
.menu-item:hover { background: #f8fafc; color: #334155; transform: translateX(4px); }
.menu-item.active {
  color: var(--brand-gradient-start);
  background: #eff6ff;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.1);
}

.dock-footer {
  margin-top: auto;
  padding: 24px 0 16px;
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
.cu-avatar.placeholder { display:flex; align-items:center; justify-content:center; background:#e2e8f0; color:#334155; cursor:pointer; }
.cu-initial { font-weight: 800; }
.cu-info { flex: 1; min-width: 0; }
.cu-name { font-weight: 700; font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cu-sub { font-size: 12px; color: #64748b; }
/* Menú eliminado; acciones disponibles en el modal de preview */

.modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; z-index: 1050; }
.modal-card { background: #fff; border-radius: 12px; padding: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.2); max-width: 90vw; }
.avatar-preview { max-width: 56vw; max-height: 70vh; display: block; border-radius: 12px; }
.modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 10px; }
.modal-btn { padding: 8px 12px; border-radius: 10px; border: 1px solid var(--border-color); background: #f1f5f9; cursor: pointer; }
.modal-btn:hover { background: #e2e8f0; }

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
