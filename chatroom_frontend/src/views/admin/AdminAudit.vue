<template>
  <div class="admin-audit">
    <div v-if="toast" class="alert" :class="toastClass" role="alert">
      {{ toast.message }}
      <button type="button" class="btn-close float-end" @click="toast = null"></button>
    </div>

    <div class="filters-card d-flex flex-wrap gap-2 align-items-end">
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
        <button class="btn btn-outline-danger" @click="clearHistory">Borrar Historial</button>
      </div>
    </div>

    <div class="table-card">
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table align-middle mb-0">
            <thead>
              <tr>
                <th style="width:180px">Fecha</th>
                <th>Actor</th>
                <th style="width:200px">Acción</th>
                <th style="width:120px">Target</th>
                <th>Nombre</th>
                <th style="width:90px">Éxito</th>
                <th>Detalles</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="7" class="text-center">
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
                <td>{{ formatAction(log.action) }}</td>
                <td><span class="badge bg-light text-dark">{{ formatTargetType(log.targetType) }}</span></td>
                <td>{{ log.targetName }}</td>
                <td>
                  <span class="badge" :class="log.success ? 'bg-success' : 'bg-danger'">{{ log.success ? 'Sí' : 'No' }}</span>
                </td>
                <td class="text-end">
                  <button v-if="hasDetails(log.details)" class="btn btn-sm btn-outline-primary py-1 px-3" style="border-radius:20px; font-size:0.8rem" @click="openDetails(log)">
                    Ver detalles
                  </button>
                  <span v-else class="text-muted small">—</span>
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
          <div class="d-flex flex-column">
            <span>Detalles del evento</span>
            <small class="text-muted fw-normal mt-1">{{ formatAction(selectedLog?.action) }}</small>
          </div>
          <button type="button" class="btn-close" @click="closeDetails"></button>
        </div>
        <div class="modal-body">
          <div class="details-grid" v-if="selectedDetailsEntries.length">
            <div v-for="item in selectedDetailsEntries" :key="item.key" class="detail-item">
              <div class="detail-label">{{ friendlyKey(item.key) }}</div>
              <div class="detail-value">{{ item.value }}</div>
            </div>
          </div>
          <div v-else class="text-muted text-center py-4">
            No hay detalles adicionales para mostrar.
          </div>
          
          <div class="mt-4 pt-3 border-top">
            <h6 class="text-muted mb-2" style="font-size:0.8rem; text-transform:uppercase; letter-spacing:0.5px">Metadatos Técnicos</h6>
            <div class="technical-info">
              <div><strong>Target:</strong> {{ formatTargetType(selectedLog?.targetType) }} #{{ selectedLog?.targetId }}</div>
              <div><strong>IP:</strong> {{ selectedLog?.ip || 'N/A' }}</div>
              <div><strong>User-Agent:</strong> {{ selectedLog?.userAgent || 'N/A' }}</div>
              <div><strong>Status Code:</strong> {{ selectedLog?.statusCode || '-' }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  
</template>

<script>
import { mapGetters } from 'vuex'
import { adminGetAuditLogs, adminClearAuditLogs } from '../../services/admin.service.js'

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
    selectedDetailsEntries() {
      if (!this.selectedLog) return []
      const obj = this.getDetailsObject(this.selectedLog.details)
      if (!obj) return []
      return Object.entries(obj).map(([k, v]) => ({
        key: k,
        value: v === null ? 'null' : (typeof v === 'object' ? JSON.stringify(v) : String(v))
      }))
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
    async clearHistory() {
      if (!confirm('⚠️ ¿Estás seguro? Se borrarán TODOS los registros de auditoría.\n\nEsta acción no se puede deshacer.')) return
      try {
        await adminClearAuditLogs(this.currentUser.id)
        this.showToast('Historial eliminado correctamente')
        this.page = 1
        this.search()
      } catch (error) {
        this.showToast('Error al eliminar historial', 'error')
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
.admin-audit { padding: 16px; max-width: 1400px; margin: 0 auto; }
.gap-2 { gap: 12px; }
.text-truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* Filtros más limpios */
.filters-card {
  background: #fff; border-radius: 12px; padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04); margin-bottom: 20px;
  border: 1px solid #f1f5f9;
}
.form-label { font-weight: 600; color: #64748b; font-size: 0.85rem; margin-bottom: 4px; }
.form-control {
  border-radius: 8px; border: 1px solid #e2e8f0; font-size: 0.9rem; padding: 8px 12px;
  background-color: #fff;
}
.form-select {
  border-radius: 8px; border: 1px solid #e2e8f0; font-size: 0.9rem;
  padding: 8px 36px 8px 12px; /* Extra right padding for arrow */
  background-color: #fff;
  line-height: 1.5;
}
.form-control:focus, .form-select:focus {
  border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.1);
}

/* Tabla moderna */
.table-card {
  background: #fff; border-radius: 12px; overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05); border: 1px solid #f1f5f9;
}
.table { margin-bottom: 0; }
.table thead th {
  background: #f8fafc; color: #475569; font-weight: 700; font-size: 0.85rem;
  text-transform: uppercase; letter-spacing: 0.5px; padding: 14px 16px; border-bottom: 1px solid #e2e8f0;
}
.table tbody td {
  padding: 14px 16px; color: #334155; font-size: 0.9rem; border-bottom: 1px solid #f1f5f9;
}
.table tbody tr:last-child td { border-bottom: none; }
.table tbody tr:hover { background-color: #f8fafc; }

/* Badges */
.badge { padding: 5px 10px; border-radius: 6px; font-weight: 600; font-size: 0.75rem; letter-spacing: 0.3px; }
.bg-success { background: #dcfce7 !important; color: #166534; }
.bg-danger { background: #fee2e2 !important; color: #991b1b; }
.bg-light { background: #f1f5f9 !important; color: #475569; border: 1px solid #e2e8f0; }
.bg-secondary { background: #f3f4f6 !important; color: #4b5563; border: 1px solid #e5e7eb; }

/* Modal */
.modal-backdrop {
  position: fixed; inset: 0; background: rgba(0,0,0,.4); backdrop-filter: blur(2px);
  display: flex; align-items: center; justify-content: center; z-index: 1050;
}
.modal-card {
  background: #fff; border-radius: 12px; width: min(700px, 90vw);
  box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1); overflow: hidden;
  animation: modalIn 0.2s ease-out;
}
@keyframes modalIn { from { opacity: 0; transform: scale(0.95); } to { opacity: 1; transform: scale(1); } }
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid #f1f5f9; background: #fff;
}
.modal-header span { font-weight: 700; color: #1e293b; font-size: 1.1rem; }
.modal-body { padding: 24px; max-height: 70vh; overflow-y: auto; background: #fff; }
.details-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px;
}
.detail-item {
  background: #f8fafc; padding: 12px; border-radius: 8px; border: 1px solid #e2e8f0;
}
.detail-label {
  font-size: 0.75rem; text-transform: uppercase; color: #64748b; font-weight: 700; letter-spacing: 0.5px; margin-bottom: 4px;
}
.detail-value {
  font-size: 0.9rem; color: #1e293b; word-break: break-word; font-family: 'Inter', sans-serif;
}
.technical-info {
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 0.85rem; color: #475569;
}
</style>