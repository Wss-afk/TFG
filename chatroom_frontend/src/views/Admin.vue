<template>
  <div class="admin-container">
    <h2>Panel de Super Admin</h2>
    <div v-if="!isSuperAdmin" class="alert alert-warning">No tienes permisos de SUPER_ADMIN.</div>

    <div v-else>
      <div class="sections">
        <section>
          <h3>Usuarios</h3>
          <div class="create-form">
            <input v-model="newUser.username" placeholder="Usuario" />
            <input v-model="newUser.password" type="password" placeholder="Contraseña" />
            <select v-model="newUser.role">
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
              <option value="SUPER_ADMIN">SUPER_ADMIN</option>
            </select>
            <button @click="createUser">Crear</button>
          </div>

          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Usuario</th>
                <th>Rol</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in users" :key="u.id">
                <td>{{ u.id }}</td>
                <td>
                  <input v-model="u.username" />
                </td>
                <td>
                  <select v-model="u.role">
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                    <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                  </select>
                </td>
                <td>
                  <button @click="updateUser(u)">Guardar</button>
                  <button @click="deleteUser(u)">Eliminar</button>
                  <input v-model="u._newPassword" type="password" placeholder="Nueva contraseña" />
                  <button @click="changePassword(u)">Cambiar contraseña</button>
                </td>
              </tr>
            </tbody>
          </table>
        </section>

        <section>
          <h3>Grupos</h3>
          <div class="create-form">
            <input v-model="newGroup.name" placeholder="Nombre del grupo" />
            <button @click="createGroup">Crear grupo</button>
          </div>

          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Nombre</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="g in groups" :key="g.id" @click="selectGroup(g)" :class="{ selected: selectedGroup && selectedGroup.id === g.id }">
                <td>{{ g.id }}</td>
                <td><input v-model="g.name" /></td>
                <td>
                  <button @click.stop="updateGroup(g)">Guardar</button>
                  <button @click.stop="deleteGroup(g)">Eliminar</button>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-if="selectedGroup" class="members-panel">
            <h4>Miembros del grupo: {{ selectedGroup.name }}</h4>
            <div class="members-list">
              <label v-for="u in users" :key="'m-'+u.id" class="member-item">
                <input type="checkbox" :value="u.id" v-model="selectedMemberIds" />
                {{ u.username }}
              </label>
            </div>
            <button @click="saveGroupMembers">Guardar miembros</button>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import {
  adminGetUsers,
  adminCreateUser,
  adminUpdateUser,
  adminDeleteUser,
  adminChangePassword,
  adminGetGroups,
  adminCreateGroup,
  adminUpdateGroup,
  adminDeleteGroup,
  adminUpdateGroupMembers
} from '../services/admin.service.js'

export default {
  name: 'Admin',
  data() {
    return {
      users: [],
      groups: [],
      newUser: { username: '', password: '', role: 'USER' },
      newGroup: { name: '' },
      selectedGroup: null,
      selectedMemberIds: []
    }
  },
  computed: {
    ...mapGetters('auth', ['currentUser']),
    isSuperAdmin() {
      return this.currentUser && this.currentUser.role === 'SUPER_ADMIN'
    }
  },
  async created() {
    if (this.isSuperAdmin) {
      await this.reloadAll()
    }
  },
  methods: {
    async reloadAll() {
      await Promise.all([this.loadUsers(), this.loadGroups()])
    },
    async loadUsers() {
      const res = await adminGetUsers(this.currentUser.id)
      this.users = res.data
      // Hydrate member selection when group selected
      this.refreshSelectedMembers()
    },
    async loadGroups() {
      const res = await adminGetGroups(this.currentUser.id)
      this.groups = res.data
    },
    async createUser() {
      const res = await adminCreateUser(this.currentUser.id, this.newUser)
      this.users.push(res.data)
      this.newUser = { username: '', password: '', role: 'USER' }
    },
    async updateUser(u) {
      const payload = { username: u.username, avatarUrl: u.avatarUrl, role: u.role }
      const res = await adminUpdateUser(this.currentUser.id, u.id, payload)
      Object.assign(u, res.data)
    },
    async deleteUser(u) {
      if (!confirm('¿Eliminar usuario?')) return
      await adminDeleteUser(this.currentUser.id, u.id)
      this.users = this.users.filter(x => x.id !== u.id)
      this.refreshSelectedMembers()
    },
    async changePassword(u) {
      if (!u._newPassword) return alert('Introduce nueva contraseña')
      await adminChangePassword(this.currentUser.id, u.id, u._newPassword)
      u._newPassword = ''
      alert('Contraseña actualizada')
    },
    async createGroup() {
      const res = await adminCreateGroup(this.currentUser.id, this.newGroup)
      this.groups.push(res.data)
      this.newGroup = { name: '' }
    },
    async updateGroup(g) {
      const res = await adminUpdateGroup(this.currentUser.id, g.id, { name: g.name })
      Object.assign(g, res.data)
    },
    async deleteGroup(g) {
      if (!confirm('¿Eliminar grupo?')) return
      await adminDeleteGroup(this.currentUser.id, g.id)
      this.groups = this.groups.filter(x => x.id !== g.id)
      if (this.selectedGroup && this.selectedGroup.id === g.id) {
        this.selectedGroup = null
        this.selectedMemberIds = []
      }
    },
    selectGroup(g) {
      this.selectedGroup = g
      this.refreshSelectedMembers()
    },
    refreshSelectedMembers() {
      if (!this.selectedGroup) return
      const memberIds = (this.selectedGroup.users || []).map(u => u.id)
      this.selectedMemberIds = memberIds
    },
    async saveGroupMembers() {
      if (!this.selectedGroup) return
      await adminUpdateGroupMembers(this.currentUser.id, this.selectedGroup.id, this.selectedMemberIds)
      alert('Miembros actualizados')
      await this.loadGroups()
      // reselect group to update member list
      const again = this.groups.find(g => g.id === this.selectedGroup.id)
      this.selectGroup(again)
    }
  }
}
</script>

<style scoped>
.admin-container {
  max-width: 1000px;
  margin: 24px auto;
  padding: 16px;
}
.sections {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
.table {
  width: 100%;
  border-collapse: collapse;
}
.table th, .table td {
  border: 1px solid #ddd;
  padding: 6px 8px;
}
.table tr.selected {
  background-color: #f7faff;
}
.create-form {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.members-panel {
  margin-top: 12px;
}
.members-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
  max-height: 240px;
  overflow: auto;
}
.member-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>