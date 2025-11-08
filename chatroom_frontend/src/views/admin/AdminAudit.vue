<template>
  <div class="admin-audit">
    <div v-if="toast" class="alert" :class="toastClass" role="alert">
      {{ toast.message }}
      <button type="button" class="btn-close float-end" @click="toast = null"></button>
    </div>

    <div class="d-flex flex-wrap gap-2 align-items-end mb-3">
      <div>
        <label class="form-label">Desde</label>
        <input v-model="filters.fromLocal" type="datetime-local" class="form-control" style="max-width:220px" />
      </div>
      <div>
        <label class="form-label">Hasta</label>
        <input v-model="filters.toLocal" type="datetime-local" class="form-control" style="max-width:220px" />
      </div>
      <div>
        <label class="form-label">Actor ID</label>
        <input v-model.number="filters.actorId" type="number" class="form-control" style="max-width:140px" />
      </div>
      <div>
        <label class="form-label">Acción</label>
        <select v-model="filters.action" class="form-select" style="max-width:200px">
          <option value="">Todas</option>
          <option value="USER_CREATE">USER_CREATE</option>
          <option value="USER_UPDATE">USER_UPDATE</option>
          <option value="PASSWORD_CHANGE">PASSWORD_CHANGE</option>
          <option value="USER_DELETE">USER_DELETE</option>
          <option value="GROUP_MEMBER_UPDATE">GROUP_MEMBER_UPDATE</option>
          <option value="ACCOUNT_DISABLE">ACCOUNT_DISABLE</option>
        </select>
      </div>
      <div>
        <label class="form-label">Target Type</label>
        <select v-model="filters.targetType" class="form-select" style="max-width:160px">
          <option value="">Todos</option>
          <option value="USER">USER</option>
          <option value="GROUP">GROUP</option>
        </select>
      </div>
      <div>
        <label class="form-label">Target ID</label>
        <input v-model.number="filters.targetId" type="number" class="form-control" style="max-width:140px" />
      </div>
      <div>
        <label class="form-label">Éxito</label>
        <select v-model="filters.success" class="form-select" style="max-width:140px">
          <option value="">Todos</option>
          <option value="true">Éxito</option>
          <option value="false">Error</option>
        </select>
      </div>
      <div class="ms-auto d-flex align-items-end gap-2">
        <div>
          <label class="form-label">Tamaño página</label>
          <select v-model.number="pageSize" class="form-select" style="width:100px">
            <option :value="5">5</option>
            <option :value="10">10</option>
            <option :value="20">20</option>
          </select>
        </div>
        <button class="btn btn-primary" @click="search">Buscar</button>
      </div>
    </div>

    <div class="card">
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-striped align-middle mb-0">
            <thead>
              <tr>
                <th style="width:180px">Fecha</th>
                <th>Actor</th>
                <th style="width:140px">Rol</th>
                <th style="width:200px">Acción</th>
                <th style="width:120px">Target</th>
                <th style="width:80px">ID</th>
                <th>Nombre</th>
                <th style="width:90px">Éxito</th>
                <th style="width:110px">Status</th>
                <th>IP</th>
                <th>User-Agent</th>
                <th>Detalles</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="12" class="text-center">
                  <div class="spinner-border text-primary" role="status"><span class="visually-hidden">Cargando...</span></div>
                </td>
              </tr>
              <tr v-for="log in logs" :key="log.id">
                <td>{{ formatDate(log.timestamp) }}</td>
                <td>
                  <div class="d-flex flex-column">
                    <span>{{ log.actorUsername || ('ID ' + log.actorId) }}</span>
                    <small v-if="log.actorId" class="text-muted">ID {{ log.actorId }}</small>
                  </div>
                </td>
                <td>
                  <span class="badge bg-info">{{ formatRole(log.actorRole) }}</span>
                </td>
                <td>{{ formatAction(log.action) }}</td>
                <td><span class="badge bg-light text-dark">{{ formatTargetType(log.targetType) }}</span></td>
                <td>{{ log.targetId }}</td>
                <td>{{ log.targetName }}</td>
                <td>
                  <span class="badge" :class="log.success ? 'bg-success' : 'bg-danger'">{{ log.success ? 'Sí' : 'No' }}</span>
                </td>
                <td><span class="badge bg-light text-dark">{{ log.statusCode || '-' }}</span></td>
                <td>{{ log.ip }}</td>
                <td class="text-truncate" style="max-width:220px" :title="log.userAgent">{{ log.userAgent }}</td>
                <td style="max-width:260px">
                  <div class="d-flex flex-wrap align-items-center gap-1">
                    <template v-if="inlineDetails(log.details).length">
                      <span v-for="d in inlineDetails(log.details)" :key="d.k" class="badge bg-secondary">
                        {{ friendlyKey(d.k) }}: {{ friendlyValue(d.v) }}
                      </span>
                    </template>
                    <button v-if="hasDetails(log.details)" class="btn btn-link btn-sm p-0 ms-1" @click="openDetails(log)">Ver</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <nav v-if="!loading && totalPages > 1" aria-label="Auditoría paginación" class="mt-2">
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
    
    <!-- Modal Detalles -->
    <div v-if="detailsModal" class="modal-backdrop" @click.self="closeDetails">
      <div class="modal-card">
        <div class="modal-header">
          <span>Detalles del evento</span>
          <button type="button" class="btn-close" @click="closeDetails"></button>
        </div>
        <div class="modal-body">
          <div class="mb-2 small text-muted">
            Acción: {{ formatAction(selectedLog?.action) }} · Target: {{ formatTargetType(selectedLog?.targetType) }} #{{ selectedLog?.targetId }}
          </div>
          <pre>{{ prettySelectedDetails }}</pre>
        </div>
      </div>
    </div>
  </div>
  
</template>

<script>
import { mapGetters } from 'vuex'
import { adminGetAuditLogs } from '../../services/admin.service.js'

export default {
  name: 'AdminAudit',
  data() {
    return {
      loading: false,
      logs: [],
      page: 1,
      pageSize: 10,
      totalPages: 1,
      totalElements: 0,
      toast: null,
      selectedLog: null,
      detailsModal: false,
      filters: {
        fromLocal: '',
        toLocal: '',
        actorId: '',
        action: '',
        targetType: '',
        targetId: '',
        success: ''
      }
    }
  },
  computed: {
    ...mapGetters('auth', ['currentUser']),
    toastClass() {
      if (!this.toast) return ''
      return this.toast.type === 'error' ? 'alert-danger' : 'alert-success'
    },
    prettySelectedDetails() {
      if (!this.selectedLog) return '—'
      const obj = this.getDetailsObject(this.selectedLog.details)
      if (!obj) return '—'
      try { return JSON.stringify(obj, null, 2) } catch { return String(this.selectedLog.details) }
    }
  },
  watch: {
    pageSize() { this.page = 1; this.search() }
  },
  async mounted() {
    await this.search()
  },
  methods: {
    formatDate(iso) {
      try { return new Date(iso).toLocaleString() } catch { return iso }
    },
    formatRole(role) {
      // Unificar roles: excepto SUPER_ADMIN, todo se muestra como Usuario
      return role === 'SUPER_ADMIN' ? 'Super Admin' : 'Usuario'
    },
    formatAction(a) {
      const map = {
        USER_CREATE: 'Creación de usuario',
        USER_UPDATE: 'Actualización de usuario',
        PASSWORD_CHANGE: 'Cambio de contraseña',
        USER_DELETE: 'Eliminación de usuario',
        GROUP_MEMBER_UPDATE: 'Actualización de miembros',
        ACCOUNT_DISABLE: 'Deshabilitar cuenta'
      }
      return map[a] || a
    },
    formatTargetType(t) {
      const map = { USER: 'Usuario', GROUP: 'Grupo' }
      return map[t] || t
    },
    friendlyKey(k) {
      if (!k) return ''
      return (k + '').replace(/_/g, ' ').replace(/\s+/g, ' ').trim().replace(/(^|\s)[a-z]/g, s => s.toUpperCase())
    },
    friendlyValue(v) {
      if (typeof v === 'boolean') return v ? 'Sí' : 'No'
      if (Array.isArray(v)) return `[${v.length} elementos]`
      if (v && typeof v === 'object') return '{...}'
      const s = v == null ? '' : String(v)
      return s.length > 40 ? s.slice(0, 37) + '…' : s
    },
    getDetailsObject(details) {
      if (!details) return null
      if (typeof details === 'object') return details
      const text = String(details).trim()
      if (!text || text === '-' || text.toLowerCase() === 'null') return null
      try { return JSON.parse(text) } catch { return { texto: text } }
    },
    inlineDetails(details) {
      const obj = this.getDetailsObject(details)
      if (!obj) return []
      const entries = Object.entries(obj)
      return entries.slice(0, 3).map(([k, v]) => ({ k, v }))
    },
    hasDetails(details) {
      return !!this.getDetailsObject(details)
    },
    openDetails(log) {
      this.selectedLog = log
      this.detailsModal = true
    },
    closeDetails() {
      this.detailsModal = false
      this.selectedLog = null
    },
    toIsoOrNull(localStr) {
      if (!localStr) return null
      const d = new Date(localStr)
      if (isNaN(d.getTime())) return null
      return d.toISOString()
    },
    async search() {
      try {
        this.loading = true
        const params = {
          from: this.toIsoOrNull(this.filters.fromLocal),
          to: this.toIsoOrNull(this.filters.toLocal),
          actorId: this.filters.actorId || null,
          action: this.filters.action || null,
          targetType: this.filters.targetType || null,
          targetId: this.filters.targetId || null,
          success: this.filters.success === '' ? null : (this.filters.success === 'true'),
          page: this.page - 1,
          size: this.pageSize
        }
        const res = await adminGetAuditLogs(this.currentUser.id, params)
        const data = res.data || {}
        this.logs = data.content || []
        this.totalPages = data.totalPages || 1
        this.totalElements = data.totalElements || this.logs.length
      } catch (error) {
        const msg = typeof error?.response?.data === 'string' ? error.response.data : 'Error al cargar auditoría'
        this.showToast(msg, 'error')
      } finally {
        this.loading = false
      }
    },
    goTo(p) {
      const newPage = Math.min(Math.max(1, p), this.totalPages)
      if (newPage !== this.page) { this.page = newPage; this.search() }
    },
    showToast(message, type = 'success') {
      this.toast = { message, type }
      setTimeout(() => { this.toast = null }, 3000)
    }
  }
}
</script>

<style scoped>
.admin-audit { padding: 8px; }
.gap-2 { gap: 8px; }
.text-truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.modal-backdrop {
  position: fixed; inset: 0; background: rgba(0,0,0,.35);
  display: flex; align-items: center; justify-content: center; z-index: 1050;
}
.modal-card {
  background: #fff; border-radius: 8px; width: min(800px, 92vw);
  max-height: 80vh; overflow: hidden; box-shadow: 0 8px 24px rgba(0,0,0,.2);
}
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:10px 12px; border-bottom: 1px solid #eee; }
.modal-body { padding: 12px; overflow: auto; }
.modal-body pre { margin: 0; font-size: 12px; background: #f8f9fa; padding: 12px; border-radius: 6px; }
</style>