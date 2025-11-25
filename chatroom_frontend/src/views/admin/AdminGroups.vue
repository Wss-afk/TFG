<template>
  <div class="admin-groups">
    <div class="d-flex flex-wrap gap-2 align-items-center mb-3">
      <input v-model="searchGroup" class="form-control" style="max-width:280px" placeholder="Buscar grupo..." />
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
      <div class="card-header">Crear grupo</div>
      <div class="card-body">
        <div class="create-group">
          <div class="mb-2">
            <label class="form-label">Nombre del grupo</label>
            <input v-model="newGroup.name" class="form-control" placeholder="p.ej. Ventas Norte" />
            <small class="text-muted">Escribe un nombre para habilitar los miembros.</small>
          </div>
          <div class="d-flex align-items-center gap-2 mb-3">
            <button class="btn btn-success" @click="createGroup" :disabled="!hasName">Crear</button>
            <span class="text-muted" v-if="newGroup.memberIds.length">Miembros seleccionados: {{ newGroup.memberIds.length }}</span>
          </div>
          <div>
            <label class="form-label text-muted">Asignar miembros</label>
            <div class="members-list" :class="{ 'members-disabled': !hasName }">
              <label v-for="u in users" :key="'c-'+u.id" class="member-item">
                <input type="checkbox" :value="u.id" v-model="newGroup.memberIds" :disabled="!hasName" />
                {{ u.username }}
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="row g-3">
      <div class="col-12">
        <div class="card">
          <div class="card-body p-0">
            <div class="table-responsive">
              <div class="table-scroll">
              <table class="table table-striped align-middle mb-0">
            <thead>
              <tr>
                <th style="width:60px">ID</th>
                <th>Nombre</th>
                <th style="width:120px">Miembros</th>
                <th style="width:200px">Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="4" class="text-center">
                  <div class="spinner-border text-primary" role="status"><span class="visually-hidden">Cargando...</span></div>
                </td>
              </tr>
              <tr v-for="g in pagedGroups" :key="g.id" :class="{ 'table-active': selectedGroup && selectedGroup.id === g.id }" @click="selectGroup(g)">
                <td>{{ g.id }}</td>
                <td>
                  <span class="group-name-text">{{ g.name }}</span>
                </td>
                <td>
                  <span class="badge" :class="memberBadgeClass(g)">{{ memberBadgeText(g) }}</span>
                </td>
                <td>
                  <div class="d-flex align-items-center gap-2 flex-nowrap actions-row">
                    <button class="btn btn-secondary btn-sm" @click.stop="editGroup(g)">Editar</button>
                    <button class="btn btn-danger btn-sm" @click.stop="deleteGroup(g)">Eliminar</button>
                  </div>
                </td>
              </tr>
            </tbody>
              </table>
              </div>
            </div>
          </div>
        </div>

        <nav v-if="!loading && totalPages > 1" aria-label="Grupos paginación" class="mt-2">
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
    </div>

    <!-- Modal de edición de grupo -->
    <div v-if="editModalOpen" class="modal-backdrop-custom">
      <div class="modal-custom" role="dialog" aria-modal="true" aria-labelledby="editGroupModalTitle">
        <div class="modal-header d-flex justify-content-between align-items-center">
          <h5 id="editGroupModalTitle" class="m-0">Editar grupo</h5>
          <button type="button" class="btn-close" @click="closeEdit"></button>
        </div>
        <div class="modal-body">
          <div class="d-flex flex-wrap gap-2 align-items-start mb-3">
            <input v-model="editName" class="form-control" placeholder="Nombre del grupo" style="max-width:280px" />
            <button class="btn btn-primary" @click="saveEditName" :disabled="!editNameChanged">Guardar nombre</button>
            <button class="btn btn-outline-danger" @click="confirmDelete">Eliminar grupo</button>
          </div>

          <div class="d-flex align-items-center gap-2 mb-2">
            <input v-model="memberSearch" class="form-control" placeholder="Buscar miembros..." style="max-width:220px" />
            <button class="btn btn-success" @click="saveEditMembers" :disabled="!editMembersChanged">Guardar miembros</button>
          </div>
          <div class="members-list">
            <label v-for="u in filteredMembers" :key="'e-'+u.id" class="member-item">
              <input type="checkbox" :value="u.id" v-model="editMemberIds" />
              {{ u.username }}
            </label>
          </div>
        </div>
        <div class="modal-footer d-flex justify-content-end gap-2">
          <button class="btn btn-secondary" @click="closeEdit">Cerrar</button>
          <button class="btn btn-primary" @click="saveAllEdits" :disabled="!editNameChanged && !editMembersChanged">Guardar todo</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import {
  adminGetGroups,
  adminCreateGroup,
  adminUpdateGroup,
  adminDeleteGroup,
  adminUpdateGroupMembers,
  adminGetUsers
} from '../../services/admin.service.js'

export default {
  name: 'AdminGroups',
  data() {
    return {
      loading: false,
      groups: [],
      users: [],
      searchGroup: '',
      page: 1,
      pageSize: 10,
      newGroup: { name: '', memberIds: [] },
      selectedGroup: null,
      selectedMemberIds: [],
      memberSearch: '',
      toast: null,
      editModalOpen: false,
      editTargetGroup: null,
      editName: '',
      editMemberIds: []
    }
  },
  computed: {
    ...mapGetters('auth', ['currentUser']),
    hasName() {
      return (this.newGroup.name || '').trim().length > 0
    },
    filteredGroups() {
      const s = this.searchGroup.trim().toLowerCase()
      return (this.groups || []).filter(g => !s || (g.name || '').toLowerCase().includes(s))
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.filteredGroups.length / this.pageSize))
    },
    pagedGroups() {
      const start = (this.page - 1) * this.pageSize
      return this.filteredGroups.slice(start, start + this.pageSize)
    },
    filteredMembers() {
      const s = this.memberSearch.trim().toLowerCase()
      return (this.users || []).filter(u => !s || (u.username || '').toLowerCase().includes(s))
    },
    hasMemberChanges() {
      const originalIds = (this.selectedGroup?.users || []).map(u => u.id).sort().join(',')
      const selectedIds = [...this.selectedMemberIds].sort().join(',')
      return originalIds !== selectedIds
    },
    editNameChanged() {
      if (!this.editTargetGroup) return false
      return (this.editName || '').trim() !== (this.editTargetGroup.name || '')
    },
    editMembersChanged() {
      if (!this.editTargetGroup) return false
      const originalIds = (this.editTargetGroup?.users || []).map(u => u.id).sort().join(',')
      const selectedIds = [...this.editMemberIds].sort().join(',')
      return originalIds !== selectedIds
    },
    toastClass() {
      if (!this.toast) return ''
      return this.toast.type === 'error' ? 'alert-danger' : 'alert-success'
    }
  },
  watch: {
    pageSize() { 
      this.page = 1 
      try { localStorage.setItem('adminGroups.pageSize', String(this.pageSize)) } catch (e) { void e }
    },
    searchGroup() { 
      this.page = 1 
      try { localStorage.setItem('adminGroups.search', this.searchGroup) } catch (e) { void e }
    }
  },
  async mounted() {
    try {
      const s = localStorage.getItem('adminGroups.search')
      if (s !== null) this.searchGroup = s
      const ps = localStorage.getItem('adminGroups.pageSize')
      if (ps !== null) {
        const n = parseInt(ps, 10)
        if (!isNaN(n)) this.pageSize = n
      }
    } catch (e) { void e }
    await Promise.all([this.loadGroups(), this.loadUsers()])
  },
  methods: {
    memberBadgeText(g) {
      const n = (g.users || []).length
      return n === 0 ? 'Sin miembros' : (n + ' miembros')
    },
    memberBadgeClass(g) {
      const n = (g.users || []).length
      if (n === 0) return 'bg-secondary'
      if (n < 5) return 'bg-info'
      return 'bg-success'
    },
    editGroup(g) {
      this.editTargetGroup = g
      this.editName = g.name
      this.editMemberIds = (g.users || []).map(u => u.id)
      this.editModalOpen = true
    },
    closeEdit() {
      this.editModalOpen = false
      this.editTargetGroup = null
      this.editName = ''
      this.editMemberIds = []
    },
    async loadGroups() {
      try {
        this.loading = true
        const res = await adminGetGroups(this.currentUser.id)
        this.groups = res.data
      } catch (error) {
        this.showToast('No se pudieron cargar los grupos', 'error')
      } finally {
        this.loading = false
      }
    },
    async saveEditName() {
      if (!this.editTargetGroup) return
      try {
        const res = await adminUpdateGroup(this.currentUser.id, this.editTargetGroup.id, { name: (this.editName || '').trim() })
        Object.assign(this.editTargetGroup, res.data)
        this.showToast('Nombre actualizado')
      } catch (error) {
        this.showToast('Error al actualizar nombre', 'error')
      }
    },
    async saveEditMembers() {
      if (!this.editTargetGroup) return
      try {
        await adminUpdateGroupMembers(this.currentUser.id, this.editTargetGroup.id, this.editMemberIds)
        await this.loadGroups()
        const again = this.groups.find(x => x.id === this.editTargetGroup.id)
        this.editTargetGroup = again || this.editTargetGroup
        this.showToast('Miembros actualizados')
      } catch (error) {
        this.showToast('Error al actualizar miembros', 'error')
      }
    },
    async saveAllEdits() {
      await Promise.all([
        this.editNameChanged ? this.saveEditName() : Promise.resolve(),
        this.editMembersChanged ? this.saveEditMembers() : Promise.resolve()
      ])
      this.closeEdit()
    },
    async confirmDelete() {
      if (!this.editTargetGroup) return
      if (!confirm('¿Eliminar grupo?')) return
      await this.deleteGroup(this.editTargetGroup)
      this.closeEdit()
    },
    async loadUsers() {
      try {
        const res = await adminGetUsers(this.currentUser.id)
        this.users = res.data
      } catch (error) {
        this.showToast('No se pudieron cargar los usuarios', 'error')
      }
    },
    selectGroup(g) {
      this.selectedGroup = g
      const memberIds = (g.users || []).map(u => u.id)
      this.selectedMemberIds = memberIds
    },
    async createGroup() {
      try {
        const name = (this.newGroup.name || '').trim()
        if (!name) { this.showToast('Introduce un nombre de grupo', 'error'); return }
        const payload = { name, userIds: this.newGroup.memberIds }
        const res = await adminCreateGroup(this.currentUser.id, payload)
        this.groups.unshift(res.data)
        this.newGroup = { name: '', memberIds: [] }
        this.showToast('Grupo creado correctamente')
      } catch (error) {
        this.showToast('Error al crear grupo', 'error')
      }
    },
    async updateGroup(g) {
      try {
        const res = await adminUpdateGroup(this.currentUser.id, g.id, { name: g.name })
        Object.assign(g, res.data)
        this.showToast('Grupo actualizado')
      } catch (error) {
        this.showToast('Error al actualizar grupo', 'error')
      }
    },
    async deleteGroup(g) {
      try {
        if (!confirm('¿Eliminar grupo?')) return
        await adminDeleteGroup(this.currentUser.id, g.id)
        this.groups = this.groups.filter(x => x.id !== g.id)
        if (this.selectedGroup && this.selectedGroup.id === g.id) {
          this.selectedGroup = null
          this.selectedMemberIds = []
        }
        this.showToast('Grupo eliminado')
      } catch (error) {
        this.showToast('Error al eliminar grupo', 'error')
      }
    },
    async saveGroupMembers() {
      if (!this.selectedGroup) return
      try {
        await adminUpdateGroupMembers(this.currentUser.id, this.selectedGroup.id, this.selectedMemberIds)
        this.showToast('Miembros actualizados')
        await this.loadGroups()
        const again = this.groups.find(g => g.id === this.selectedGroup.id)
        this.selectGroup(again)
      } catch (error) {
        this.showToast('Error al actualizar miembros', 'error')
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
.admin-groups { padding: 8px; }
.members-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
  max-height: 240px;
  overflow: auto;
}
.member-item { display: flex; align-items: center; gap: 6px; }
.gap-2 { gap: 8px; }
.members-disabled { opacity: 0.65; pointer-events: none; }
.create-group .form-label { font-weight: 600; color: #495057; }
.create-group .text-muted { font-size: 12px; }

/* Modal minimalista sin dependencias */
.modal-backdrop-custom {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}
.modal-custom {
  background: #fff;
  width: min(720px, calc(100% - 32px));
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2);
}
.modal-header, .modal-footer { padding: 12px 16px; border-bottom: 1px solid #eee; }
.modal-footer { border-top: 1px solid #eee; border-bottom: 0; }
.modal-body { padding: 16px; }
.group-name-text { display: block; text-align: center; font-weight: 400; color: #495057; }
/* Título más ligero y centrado */
.card-header { text-align: center; font-weight: 700; color: #495057; }
.modal-header h5 { flex: 1; text-align: center; font-weight: 700; color: #495057; }
/* Centrar y aligerar el encabezado de la columna Nombre */
.table thead th:nth-child(2) { text-align: center; font-weight: 700; color: #495057; }
.table-scroll { max-height: 420px; overflow: auto; }
.table thead th { position: sticky; top: 0; background: #fff; z-index: 2; }
.table tbody tr:nth-child(even) { background-color: rgba(0,0,0,0.025); }
.pagination .page-link { min-width: 36px; }
.pagination .page-item.active .page-link { font-weight: 600; }
</style>
