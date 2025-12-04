<template>
  <div class="admin-users">
    <div class="d-flex flex-wrap gap-2 align-items-center mb-3">
      <input v-model="search" class="form-control" style="max-width:280px" placeholder="Buscar usuario..." />
      <select v-model="roleFilter" class="form-select" style="max-width:180px">
        <option value="">Todos los roles</option>
        <option value="USER">USER</option>
        <option value="SUPER_ADMIN">SUPER_ADMIN</option>
      </select>
      <div class="ms-auto d-flex align-items-center gap-2">
        <label class="text-muted">Tamaño página</label>
        <select v-model.number="pageSize" class="form-select" style="width:100px">
          <option :value="5">5</option>
          <option :value="10">10</option>
          <option :value="20">20</option>
        </select>
      </div>
    </div>

    <div v-if="toast" class="alert" :class="toastClass" role="alert">
      {{ toast.message }}
      <button type="button" class="btn-close float-end" @click="toast = null"></button>
    </div>

    <div class="card mb-3">
      <div class="card-header">Crear usuario</div>
      <div class="card-body d-flex flex-wrap gap-2">
        <input v-model="newUser.username" class="form-control" placeholder="Usuario" style="max-width:220px" />
        <div class="input-group" style="max-width:320px">
          <input v-model="newUser.password" :type="newUser._showPassword ? 'text' : 'password'" class="form-control" placeholder="Contraseña" />
          <button class="btn btn-outline-secondary" type="button" @click="newUser._showPassword = !newUser._showPassword">{{ newUser._showPassword ? 'Ocultar' : 'Mostrar' }}</button>
        </div>
        <div class="input-group" style="max-width:320px">
          <input v-model="newUser._confirmPassword" :type="newUser._showConfirmPassword ? 'text' : 'password'" class="form-control" placeholder="Confirmar contraseña" />
          <button class="btn btn-outline-secondary" type="button" @click="newUser._showConfirmPassword = !newUser._showConfirmPassword">{{ newUser._showConfirmPassword ? 'Ocultar' : 'Mostrar' }}</button>
        </div>
        <select v-model="newUser.role" class="form-select" style="max-width:180px">
          <option value="USER">USER</option>
          <option value="SUPER_ADMIN">SUPER_ADMIN</option>
        </select>
        <button class="btn btn-success" @click="createUser">Crear</button>
      </div>
    </div>

    <div class="card">
      <div class="card-body p-0">
      <div class="table-responsive">
      <table class="table table-striped align-middle mb-0">
        <thead>
          <tr>
            <th style="width:60px">ID</th>
            <th>Usuario</th>
            <th style="width:200px">Rol</th>
            <th style="width:160px">Habilitado</th>
            <th style="width:520px">Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="5" class="text-center">
              <div class="spinner-border text-primary" role="status"><span class="visually-hidden">Cargando...</span></div>
            </td>
          </tr>
          <tr v-for="u in pagedUsers" :key="u.id">
            <td>{{ u.id }}</td>
            <td>
              <input v-model="u.username" class="form-control" />
            </td>
            <td>
              <div class="d-flex align-items-center gap-2">
                <span class="badge" :class="roleBadgeClass(u.role)">{{ formatRole(u.role) }}</span>
                <select v-model="u.role" class="form-select">
                  <option value="USER">USER</option>
                  <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                </select>
              </div>
            </td>
            <td>
              <div class="d-flex align-items-center gap-2">
                <div class="form-check form-switch">
                  <input class="form-check-input" type="checkbox" v-model="u.enabled">
                  <label class="form-check-label visually-hidden">{{ u.enabled ? 'Activo' : 'Desactivado' }}</label>
                </div>
                <span class="badge" :class="u.enabled ? 'bg-success' : 'bg-secondary'">{{ u.enabled ? 'Activo' : 'Desactivado' }}</span>
              </div>
            </td>
            <td>
              <div class="d-flex align-items-center gap-2">
                <button class="btn-icon btn-icon-success" title="Guardar cambios" @click="updateUser(u)">
                  <Icon name="check" :size="18" />
                </button>
                
                <button class="btn-icon btn-icon-warning" title="Cambiar contraseña" @click="u._showPasswordModal = true">
                  <Icon name="key" :size="18" />
                </button>

                <div class="avatar-upload-wrapper" title="Cambiar avatar">
                  <img v-if="u.avatarUrl" :src="u.avatarUrl" class="mini-avatar" @click="triggerAvatar(u)" />
                  <div v-else class="btn-icon btn-icon-primary" @click="triggerAvatar(u)">
                    <Icon name="upload" :size="18" />
                  </div>
                  <button v-if="u.avatarUrl" class="btn-remove-avatar" title="Borrar avatar" @click.stop="removeAvatar(u)">×</button>
                  <input :ref="'avatarInput'+u.id" type="file" accept="image/*" class="d-none" @change="onAvatarSelected(u, $event)" />
                </div>

                <button class="btn-icon btn-icon-danger" title="Eliminar usuario" @click="deleteUser(u)">
                  <Icon name="trash" :size="18" />
                </button>
              </div>
              
              <!-- Modal simple para contraseña -->
              <div v-if="u._showPasswordModal" class="password-modal-overlay">
                <div class="password-modal">
                  <h5>Cambiar contraseña para {{ u.username }}</h5>
                  <div class="mb-3">
                    <div class="input-group input-group-sm mb-2">
                      <input v-model="u._newPassword" :type="u._showNewPassword ? 'text' : 'password'" class="form-control" placeholder="Nueva contraseña" />
                      <button class="btn btn-outline-secondary" type="button" @click="u._showNewPassword = !u._showNewPassword">
                        <Icon :name="u._showNewPassword ? 'eye-off' : 'eye'" :size="14" />
                      </button>
                    </div>
                    <div class="input-group input-group-sm">
                      <input v-model="u._confirmPassword" :type="u._showConfirmPassword ? 'text' : 'password'" class="form-control" placeholder="Confirmar" />
                      <button class="btn btn-outline-secondary" type="button" @click="u._showConfirmPassword = !u._showConfirmPassword">
                        <Icon :name="u._showConfirmPassword ? 'eye-off' : 'eye'" :size="14" />
                      </button>
                    </div>
                  </div>
                  <div class="d-flex justify-content-end gap-2">
                    <button class="btn btn-secondary btn-sm" @click="u._showPasswordModal = false">Cancelar</button>
                    <button class="btn btn-primary btn-sm" @click="changePassword(u); u._showPasswordModal = false">Guardar</button>
                  </div>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      </div>
      </div>
    </div>

    <nav v-if="!loading && totalPages > 1" aria-label="Usuarios paginación" class="mt-2">
      <ul class="pagination pagination-sm">
        <li class="page-item" :class="{ disabled: page === 1 }">
          <button class="page-link" @click="goTo(page - 1)" :disabled="page === 1">Anterior</button>
        </li>
        <li v-for="p in totalPages" :key="p" class="page-item" :class="{ active: p === page }">
          <button class="page-link" @click="goTo(p)">{{ p }}</button>
        </li>
        <li class="page-item" :class="{ disabled: page === totalPages }">
          <button class="page-link" @click="goTo(page + 1)" :disabled="page === totalPages">Siguiente</button>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Icon from '../../components/Icon.vue'
import { 
  adminGetUsers,
  adminCreateUser,
  adminUpdateUser,
  adminDeleteUser,
  adminChangePassword
} from '../../services/admin.service.js'

export default {
  name: 'AdminUsers',
  components: { Icon },
  data() {
    return {
      loading: false,
      users: [],
      search: '',
      roleFilter: '',
      page: 1,
      pageSize: 10,
      newUser: { username: '', password: '', role: 'USER' },
      toast: null
    }
  },
  computed: {
    ...mapGetters('auth', ['currentUser']),
    filteredUsers() {
      const s = this.search.trim().toLowerCase()
      return (this.users || []).filter(u => {
        const termMatch = !s || (u.username || '').toLowerCase().includes(s)
        const roleMatch = !this.roleFilter || u.role === this.roleFilter
        return termMatch && roleMatch
      })
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.filteredUsers.length / this.pageSize))
    },
    pagedUsers() {
      const start = (this.page - 1) * this.pageSize
      return this.filteredUsers.slice(start, start + this.pageSize)
    },
    toastClass() {
      if (!this.toast) return ''
      return this.toast.type === 'error' ? 'alert-danger' : 'alert-success'
    }
  },
  watch: {
    pageSize() {
      this.page = 1
      try { localStorage.setItem('adminUsers.pageSize', String(this.pageSize)) } catch (e) { void e }
    },
    roleFilter() {
      this.page = 1
      try { localStorage.setItem('adminUsers.role', this.roleFilter) } catch (e) { void e }
    },
    search() {
      this.page = 1
      try { localStorage.setItem('adminUsers.search', this.search) } catch (e) { void e }
    }
  },
  async mounted() {
    try {
      const s = localStorage.getItem('adminUsers.search')
      if (s !== null) this.search = s
      const rf = localStorage.getItem('adminUsers.role')
      if (rf !== null) this.roleFilter = rf
      const ps = localStorage.getItem('adminUsers.pageSize')
      if (ps !== null) {
        const n = parseInt(ps, 10)
        if (!isNaN(n)) this.pageSize = n
      }
    } catch (e) { void e }
    await this.loadUsers()
  },
  methods: {
    async loadUsers() {
      try {
        this.loading = true
        const res = await adminGetUsers(this.currentUser.id)
        // Normalizar roles: mostrar ADMIN como USER en UI
        const list = Array.isArray(res.data) ? res.data : []
        this.users = list.map(u => ({ ...u, role: u.role === 'ADMIN' ? 'USER' : u.role }))
      } catch (error) {
        this.showToast('No se pudieron cargar los usuarios', 'error')
      } finally {
        this.loading = false
      }
    },
    triggerAvatar(u) {
      const el = this.$refs['avatarInput' + u.id]
      if (el && el[0]) el[0].click()
      else if (el) el.click()
    },
    async onAvatarSelected(u, evt) {
      try {
        const file = evt.target.files && evt.target.files[0]
        evt.target.value = ''
        if (!file) return
        const max = 20 * 1024 * 1024
        const isImage = /^image\//.test(file.type)
        if (!isImage) { this.showToast('Selecciona una imagen válida', 'error'); return }
        if (file.size > max) { this.showToast('La imagen supera 20MB', 'error'); return }
        u._avatarLoading = true
        const { uploadAttachment } = await import('../../services/chat.service.js')
        const res = await uploadAttachment(file)
        const url = res?.data?.url
        if (!url) { this.showToast('No se pudo subir la imagen', 'error'); return }
        const payload = { username: (u.username || '').trim(), avatarUrl: url, role: u.role, enabled: u.enabled }
        const updated = await adminUpdateUser(this.currentUser.id, u.id, payload)
        Object.assign(u, updated.data)

        // Si soy yo mismo, actualizar store
        if (u.id === this.currentUser.id) {
          this.$store.commit('auth/SET_USER', { ...this.currentUser, avatarUrl: url })
        }

        this.showToast('Avatar actualizado')
      } catch (error) {
        const msg = typeof error?.response?.data === 'string' ? error.response.data : 'Error al subir avatar'
        this.showToast(msg, 'error')
      } finally {
        u._avatarLoading = false
      }
    },
    async removeAvatar(u) {
      try {
        if (!confirm('¿Borrar avatar y volver al predeterminado?')) return
        // Send empty string to signal removal to backend
        const payload = { username: u.username, avatarUrl: '', role: u.role, enabled: u.enabled }
        const res = await adminUpdateUser(this.currentUser.id, u.id, payload)
        Object.assign(u, res.data)
        
        // Si el usuario editado soy yo mismo, actualizar el store inmediatamente
        if (u.id === this.currentUser.id) {
          this.$store.commit('auth/SET_USER', { ...this.currentUser, avatarUrl: null })
        }
        
        this.showToast('Avatar eliminado')
      } catch (error) {
        this.showToast('Error al eliminar avatar', 'error')
      }
    },
    formatRole(r) {
      const map = { USER: 'Usuario', SUPER_ADMIN: 'Super Admin' }
      return map[r] || r
    },
    roleBadgeClass(r) {
      return r === 'SUPER_ADMIN' ? 'bg-warning text-dark' : 'bg-info'
    },
    async createUser() {
      try {
        const username = (this.newUser.username || '').trim()
        const password = (this.newUser.password || '').trim()
        const confirm = (this.newUser._confirmPassword || '').trim()
        if (!username) { this.showToast('Introduce un nombre de usuario', 'error'); return }
        if (!password) { this.showToast('Introduce una contraseña', 'error'); return }
        if (password !== confirm) { this.showToast('Las contraseñas no coinciden', 'error'); return }
        const payload = { username, password, role: this.newUser.role }
        const res = await adminCreateUser(this.currentUser.id, payload)
        this.users.unshift(res.data)
        this.newUser = { username: '', password: '', role: 'USER', _confirmPassword: '', _showPassword: false, _showConfirmPassword: false }
        this.showToast('Usuario creado correctamente')
      } catch (error) {
        const msg = typeof error?.response?.data === 'string' ? error.response.data : 'Error al crear usuario'
        this.showToast(msg, 'error')
      }
    },
    async updateUser(u) {
      try {
        const username = (u.username || '').trim()
        if (!username) { this.showToast('El nombre de usuario no puede estar vacío', 'error'); return }
        const payload = { username, avatarUrl: u.avatarUrl, role: u.role, enabled: u.enabled }
        const res = await adminUpdateUser(this.currentUser.id, u.id, payload)
        Object.assign(u, res.data)

        // Si soy yo mismo, actualizar store (por si cambié username)
        if (u.id === this.currentUser.id) {
          this.$store.commit('auth/SET_USER', { ...this.currentUser, username: u.username, role: u.role })
        }

        this.showToast('Usuario actualizado')
      } catch (error) {
        const msg = typeof error?.response?.data === 'string' ? error.response.data : 'Error al actualizar usuario'
        this.showToast(msg, 'error')
      }
    },
    async deleteUser(u) {
      try {
        if (!confirm('¿Eliminar usuario?')) return
        await adminDeleteUser(this.currentUser.id, u.id)
        this.users = this.users.filter(x => x.id !== u.id)
        this.showToast('Usuario eliminado')
      } catch (error) {
        this.showToast('Error al eliminar usuario', 'error')
      }
    },
    async changePassword(u) {
      try {
        const np = (u._newPassword || '').trim()
        const confirm = (u._confirmPassword || '').trim()
        if (!np) { this.showToast('Introduce nueva contraseña', 'error'); return }
        if (np !== confirm) { this.showToast('Las contraseñas no coinciden', 'error'); return }
        await adminChangePassword(this.currentUser.id, u.id, np)
        u._newPassword = ''
        u._confirmPassword = ''
        u._showNewPassword = false
        u._showConfirmPassword = false
        this.showToast('Contraseña actualizada')
      } catch (error) {
        const msg = typeof error?.response?.data === 'string' ? error.response.data : 'Error al cambiar contraseña'
        this.showToast(msg, 'error')
      }
    },
    goTo(p) {
      this.page = Math.min(Math.max(1, p), this.totalPages)
    },
    showToast(message, type = 'success') {
      this.toast = { message, type }
      setTimeout(() => { this.toast = null }, 3000)
    }
  }
}
</script>

<style scoped>
.admin-users {
  padding: 8px;
}
.gap-2 { gap: 8px; }
.actions-row { gap: 12px; row-gap: 14px; }
.table thead th, .table tbody td { padding: 12px 16px; }
.table .form-control, .table .form-select { min-height: 38px; min-width: 200px; }
.table .badge { font-size: 12px; padding: 6px 8px; }
.table thead th { position: sticky; top: 0; background: #fff; z-index: 2; }
.table tbody tr:nth-child(even) { background-color: rgba(0,0,0,0.025); }
.pagination .page-link { min-width: 36px; }
.pagination .page-item.active .page-link { font-weight: 600; }

.password-modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center; z-index: 1100;
}
.password-modal {
  background: #fff; padding: 20px; border-radius: 8px; width: 300px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.btn-icon {
  width: 32px; height: 32px; border: none; border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s; color: #fff;
}
.btn-icon:hover { transform: translateY(-2px); filter: brightness(1.1); }
.btn-icon-success { background: #10b981; }
.btn-icon-warning { background: #f59e0b; }
.btn-icon-danger { background: #ef4444; }
.btn-icon-primary { background: #3b82f6; }

.avatar-upload-wrapper { cursor: pointer; position: relative; width: 32px; height: 32px; }
.mini-avatar { width: 32px; height: 32px; border-radius: 6px; object-fit: cover; border: 1px solid #e2e8f0; transition: transform 0.2s; display: block; }
.avatar-upload-wrapper:hover .mini-avatar { border-color: #3b82f6; }
.btn-remove-avatar {
  position: absolute; top: -6px; right: -6px; width: 16px; height: 16px;
  background: #ef4444; color: #fff; border-radius: 50%; border: none;
  font-size: 12px; line-height: 1; display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.2s; cursor: pointer;
}
.avatar-upload-wrapper:hover .btn-remove-avatar { opacity: 1; }
</style>
