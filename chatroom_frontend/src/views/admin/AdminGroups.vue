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
      <div class="card-body d-flex flex-wrap gap-2">
        <input v-model="newGroup.name" class="form-control" placeholder="Nombre del grupo" style="max-width:260px" />
        <button class="btn btn-success" @click="createGroup">Crear</button>
      </div>
    </div>

    <div class="row g-3">
      <div class="col-lg-6">
        <div class="card">
          <div class="card-body p-0">
            <div class="table-responsive">
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
                  <input v-model="g.name" class="form-control form-control-sm" />
                </td>
                <td>{{ (g.users || []).length }}</td>
                <td>
                  <div class="d-flex flex-wrap gap-2">
                    <button class="btn btn-primary btn-sm" @click.stop="updateGroup(g)">Guardar</button>
                    <button class="btn btn-danger btn-sm" @click.stop="deleteGroup(g)">Eliminar</button>
                  </div>
                </td>
              </tr>
            </tbody>
              </table>
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

      <div class="col-lg-6" v-if="selectedGroup">
        <div class="card">
          <div class="card-header d-flex align-items-center justify-content-between">
            <span>Miembros del grupo: <b>{{ selectedGroup.name }}</b></span>
            <div class="d-flex gap-2 align-items-center">
              <input v-model="memberSearch" class="form-control form-control-sm" placeholder="Buscar miembros..." style="max-width:180px" />
              <button class="btn btn-success btn-sm" :disabled="!hasMemberChanges" @click="saveGroupMembers">Guardar cambios</button>
            </div>
          </div>
          <div class="card-body" style="max-height:380px; overflow:auto">
            <div class="members-list">
              <label v-for="u in filteredMembers" :key="'m-'+u.id" class="member-item">
                <input type="checkbox" :value="u.id" v-model="selectedMemberIds" />
                {{ u.username }}
              </label>
            </div>
          </div>
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
      newGroup: { name: '' },
      selectedGroup: null,
      selectedMemberIds: [],
      memberSearch: '',
      toast: null
    }
  },
  computed: {
    ...mapGetters('auth', ['currentUser']),
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
    toastClass() {
      if (!this.toast) return ''
      return this.toast.type === 'error' ? 'alert-danger' : 'alert-success'
    }
  },
  watch: {
    pageSize() { this.page = 1 },
    searchGroup() { this.page = 1 }
  },
  async mounted() {
    await Promise.all([this.loadGroups(), this.loadUsers()])
  },
  methods: {
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
        const res = await adminCreateGroup(this.currentUser.id, this.newGroup)
        this.groups.unshift(res.data)
        this.newGroup = { name: '' }
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
}
.member-item { display: flex; align-items: center; gap: 6px; }
.gap-2 { gap: 8px; }
</style>