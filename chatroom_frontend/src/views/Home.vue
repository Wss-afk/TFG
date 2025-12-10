<template>
  <div class="home-page">
    <AppDock />
    <div class="home-content">
      <div class="welcome-bar" role="banner" aria-label="Bienvenida">
        <div class="welcome-left gap8-1">
          <div class="greeting-icon">
            <Icon :name="greetingIcon" :size="24" />
          </div>
          <div class="greeting-text">
            <span class="welcome-title">{{ greetingText }}</span>
            <span class="welcome-user">{{ currentUser?.username || 'Usuario' }}</span>
          </div>
        </div>
        <div class="welcome-right">
          <span class="current-date">{{ currentDateFull }}</span>
        </div>
      </div>
      <div class="home-grid gap8-2">
        <!-- Columna izquierda: Calendario + Today Task -->
        <section class="calendar-card">
          <header class="card-header">
            <div class="month-title">
              {{ monthLabel }}
            </div>
            <div class="header-actions gap8-1">
              <button class="nav-btn" aria-label="Mes anterior" @click="prevMonth"><Icon name="chevron-left" :size="18" /></button>
              <button class="nav-btn" aria-label="Mes siguiente" @click="nextMonth"><Icon name="chevron-right" :size="18" /></button>
              <button class="today-btn" @click="goToday">Hoy</button>
            </div>
          </header>
          <div class="calendar">
            <div class="weekdays gap8-1">
              <span v-for="d in weekdays" :key="d">{{ d }}</span>
            </div>
            <div class="days gap8-1">
              <span v-for="(d, idx) in leadingBlanks" :key="'b'+idx" class="blank"></span>
              <button v-for="day in daysInMonth" :key="day" :class="['day', isToday(day) && 'today']">
                <span class="num">{{ day }}</span>
                <div class="dots gap8-1">
                  <span
                    v-for="(ev, i) in eventsForDay(day).slice(0,3)"
                    :key="i"
                    class="dot"
                    :style="{ background: ev.color }"
                  ></span>
                  <span v-if="eventsForDay(day).length > 3" class="more">+{{ eventsForDay(day).length - 3 }}</span>
                </div>
              </button>
            </div>
          </div>
        </section>

        <section class="tasks-card">
          <header class="card-header">
            <div class="card-title">Today Task <span class="muted">({{ todayEvents.length }})</span></div>
            <button class="add-btn" aria-label="Agregar" @click="openCreateForm"><Icon name="plus" :size="18" /></button>
          </header>
          <ul class="tasks-list gap8-1">
            <li v-for="ev in todayEvents" :key="ev.id || ev.title" class="task-item clickable" @click="openTaskDetails(ev)">
              <div class="task-info">
                <span class="task-marker" :style="{ backgroundColor: ev.color || '#6366f1' }"></span>
                <span class="task-title">{{ ev.title }}</span>
              </div>
              <span class="time">{{ ev.time || 'Todo el día' }}</span>
            </li>
            <li v-if="todayEvents.length === 0" class="task-item empty-state">
              <div class="empty-icon"><Icon name="calendar" :size="24" /></div>
              <div class="empty-text">
                <span class="strong">¡Día libre!</span>
                <span class="sub">No tienes tareas para hoy</span>
              </div>
            </li>
          </ul>
        </section>

        <!-- Columna derecha: Notification + Team Chat -->
        <section class="notification-card">
          <header class="card-header">
            <div class="card-title">Notification <span class="muted">({{ notifications.length }})</span></div>
            <router-link to="/chat" class="view-all">View all</router-link>
          </header>
          <div class="notice empty-notice" v-if="notifications.length === 0">
            <div class="notice-content">
              <div class="notice-icon"><Icon name="bell" :size="32" /></div>
              <div class="notice-header">
                <div class="notice-title">Sin novedades</div>
                <div class="notice-date">Estás al día con todo</div>
              </div>
            </div>
          </div>
          <ul class="notifications-list gap8-2" v-else>
            <li class="notification-item" v-for="n in notifications" :key="n.key">
              <div class="notification-main">
                <div class="notification-title gap8-1">
                  <span class="badge" :class="n.type">{{ n.type === 'group' ? 'Grupo' : 'Mensaje' }}</span>
                  <span class="strong">{{ n.title }}</span>
                </div>
                <div class="notification-text">{{ n.text }}</div>
                <div class="notification-meta">{{ n.when }}</div>
              </div>
              <div class="notification-actions">
                <button class="assign-btn" @click="viewNotification(n)">Ver</button>
              </div>
            </li>
          </ul>
        </section>

        <section class="quicknotes-card">
          <header class="card-header">
            <div class="card-title">Quick Notes</div>
            <div class="right-actions gap8-2">
              <span v-if="saveStatus" class="save-status">{{ saveStatus }}</span>
              <button class="action-btn save-btn" @click="saveNote" aria-label="Guardar" title="Guardar">
                <Icon name="check" :size="16" /> Guardar
              </button>
              <button class="action-btn clear-btn" @click="clearNote" aria-label="Borrar todo" title="Borrar todo">
                <Icon name="trash" :size="16" />
              </button>
            </div>
          </header>
          <div class="notes-content">
            <textarea
              v-model="quickNote"
              placeholder="Escribe tus notas rápidas aquí..."
              @keydown.ctrl.s.prevent="saveNote"
            ></textarea>
          </div>
        </section>
      </div>
    </div>

    <!-- Modal de Creación de Evento -->
    <div v-if="showCreateModal" class="modal-overlay">
      <div class="modal-card">
        <header class="modal-header">
          <h3 class="modal-title">Nuevo Evento para Hoy</h3>
          <button class="close-btn" @click="cancelCreate">×</button>
        </header>
        <div class="modal-body">
          <div class="form-row">
            <label>Título*</label>
            <input type="text" v-model="newEvent.title" placeholder="¿Qué tienes que hacer hoy?" ref="titleInput" />
          </div>
          <div class="form-row">
            <label>Descripción</label>
            <textarea v-model="newEvent.description" placeholder="Detalles adicionales..."></textarea>
          </div>
          <div class="form-grid gap8-2">
            <div>
              <label>Horario</label>
              <div class="hour-selects">
                <select v-model="newEvent.startHour">
                  <option :value="null">Inicio</option>
                  <option v-for="h in hours24" :key="'sh-'+h" :value="h">{{ pad2(h) }}:00</option>
                </select>
                <span class="sep">-</span>
                <select v-model="newEvent.endHour">
                  <option :value="null">Fin</option>
                  <option v-for="h in hours24" :key="'eh-'+h" :value="h">{{ pad2(h) }}:00</option>
                </select>
              </div>
            </div>
            <div>
              <label>Color</label>
              <div class="color-picker-wrapper">
                <input v-model="newEvent.color" type="color" />
              </div>
            </div>
          </div>
          <div class="form-row">
            <label>Responsables</label>
            <div class="checkbox-list">
              <label v-for="u in users" :key="u.id" class="checkbox-item">
                <input type="checkbox" :value="u.id" v-model="newEvent.assignedToIds" />
                <span class="checkbox-label">{{ u.username }}</span>
              </label>
            </div>
          </div>
          <div v-if="createError" class="form-error">{{ createError }}</div>
        </div>
        <footer class="modal-footer">
          <button class="btn-cancel" @click="cancelCreate">Cancelar</button>
          <button class="btn-primary" @click="submitCreate">Guardar Tarea</button>
        </footer>
      </div>
    </div>

    <!-- Modal de Detalle de Evento -->
    <div v-if="showDetailModal && selectedTask" class="modal-overlay" @click.self="closeTaskDetails">
      <div class="modal-card detail-card-home">
        <header class="modal-header" :style="{ borderLeft: `6px solid ${selectedTask.color || '#6366f1'}` }">
          <div class="header-content">
             <h3 class="modal-title">{{ selectedTask.title }}</h3>
             <span class="modal-subtitle">{{ currentDateFull }}</span>
          </div>
          <button class="close-btn" @click="closeTaskDetails">×</button>
        </header>
        <div class="modal-body">
          <div class="detail-row">
            <div class="detail-icon"><Icon name="clock" :size="18" /></div>
            <div class="detail-text">
              <span class="label">Horario</span>
              <span class="value">{{ selectedTask.time || 'Todo el día' }}</span>
            </div>
          </div>
          
          <div class="detail-row" v-if="selectedTask.description">
            <div class="detail-icon"><Icon name="file-text" :size="18" /></div>
            <div class="detail-text">
              <span class="label">Descripción</span>
              <span class="value description">{{ selectedTask.description }}</span>
            </div>
          </div>

          <div class="detail-row">
             <div class="detail-icon"><Icon name="user" :size="18" /></div>
             <div class="detail-text">
               <span class="label">Creado por</span>
               <span class="value">{{ selectedTask.createdBy ? selectedTask.createdBy.username : (selectedTask.createdById ? usernameById(selectedTask.createdById) : '—') }}</span>
             </div>
          </div>

          <div class="detail-row" v-if="(selectedTask.responsibles && selectedTask.responsibles.length) || (selectedTask.responsibleIds && selectedTask.responsibleIds.length)">
             <div class="detail-icon"><Icon name="users" :size="18" /></div>
             <div class="detail-text">
               <span class="label">Responsables</span>
               <div class="responsibles-list">
                 <template v-if="selectedTask.responsibles && selectedTask.responsibles.length">
                    <span v-for="u in selectedTask.responsibles" :key="u.id" class="responsible-badge">{{ u.username }}</span>
                 </template>
                 <template v-else>
                    <span v-for="id in selectedTask.responsibleIds" :key="id" class="responsible-badge">{{ usernameById(id) }}</span>
                 </template>
               </div>
             </div>
          </div>
        </div>
        <footer class="modal-footer">
          <button class="btn-primary" @click="closeTaskDetails">Cerrar</button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script>
import AppDock from '../components/AppDock.vue'
import Icon from '../components/Icon.vue'
import { mapGetters } from 'vuex'
import { fetchMonthEvents, createEvent } from '../services/events.service.js'
import { connectWebSocket, subscribe, disconnectWebSocket } from '../services/websocket.js'
import { markMessagesAsRead } from '../services/chat.service.js'
import { fetchGroups, fetchUsers } from '../services/user.service.js'
export default {
  name: 'Home',
  components: { AppDock, Icon },
  data() {
    const now = new Date()
    const year = now.getFullYear()
    const month = now.getMonth() // 0-11
    const first = new Date(year, month, 1)
    const last = new Date(year, month + 1, 0)
    return {
      year,
      month,
      todayDay: now.getDate(),
      todayMonth: month,
      todayYear: year,
      weekdays: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
      leadingBlanks: Array(first.getDay()).fill(0),
      daysInMonth: Array.from({ length: last.getDate() }, (_, i) => i + 1),
      events: [],
      notifications: [],
      quickNote: '',
      saveStatus: '',
      wsConnected: false,
      globalSubscription: null,
      groupSubscriptions: {},
      groups: [],
      // Event Creation Data
      showCreateModal: false,
      createError: '',
      newEvent: { title: '', description: '', time: '', color: '#6366f1', assignedToIds: [], startHour: null, endHour: null },
      users: [],
      // Task Details
      showDetailModal: false,
      selectedTask: null
    }
  },
  computed: {
    ...mapGetters('auth', ['currentUser']),
    monthLabel() {
      const m = ['January','February','March','April','May','June','July','August','September','October','November','December'][this.month]
      return `${m}, ${this.year}`
    },
    todayISO() {
      return this.toISO(this.todayYear, this.todayMonth, this.todayDay)
    },
    todayEvents() {
      const uid = this.currentUser?.id
      if (!uid) return []
      return this.events.filter(e => {
        const isToday = (e.date || '') === this.todayISO
        if (!isToday) return false
        
        // Filtrar por asignación: mostrar solo si el usuario actual está asignado
        const assignedIds = (e.responsibleIds || []).map(String)
        const assignedObjs = (e.responsibles || []).map(u => String(u.id))
        
        // Combinar ambas listas de IDs por seguridad
        const allAssigned = [...new Set([...assignedIds, ...assignedObjs])]
        
        // Si no hay responsables, ¿se muestra? 
        // Según la petición "solo aparecen para los que estan asignado", 
        // si nadie está asignado, nadie lo ve en Today Task (pero sí en calendario).
        // Si queremos que el creador lo vea por defecto si no hay asignados, descomentar:
        // if (allAssigned.length === 0) return String(e.createdById) === String(uid)
        
        return allAssigned.includes(String(uid))
      })
    },
    greetingText() {
      const h = new Date().getHours()
      if (h < 12) return 'Buenos días,'
      if (h < 20) return 'Buenas tardes,'
      return 'Buenas noches,'
    },
    greetingIcon() {
      const h = new Date().getHours()
      if (h >= 6 && h < 18) return 'sun'
      return 'moon'
    },
    currentDateFull() {
      const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }
      return new Date().toLocaleDateString('es-ES', options)
    },
    hours24() {
      return Array.from({ length: 24 }, (_, i) => i)
    }
  },
  methods: {
    toISO(y,m,d) { return `${y}-${String(m+1).padStart(2,'0')}-${String(d).padStart(2,'0')}` },
    // Event Creation Methods
    openCreateForm() {
      this.showCreateModal = true
      this.createError = ''
      if (!this.newEvent.color) this.newEvent.color = '#6366f1'
      this.loadUsers()
    },
    cancelCreate() {
      this.showCreateModal = false
      this.createError = ''
      this.newEvent = { title: '', description: '', time: '', color: '#6366f1', assignedToIds: [], startHour: null, endHour: null }
    },
    openTaskDetails(task) {
      this.selectedTask = task
      this.showDetailModal = true
    },
    closeTaskDetails() {
      this.showDetailModal = false
      this.selectedTask = null
    },
    pad2(n) { return String(n).padStart(2,'0') },
    async loadUsers() {
      try {
        const res = await fetchUsers()
        this.users = Array.isArray(res?.data) ? res.data : []
      } catch (e) {
        console.warn('No se pudieron cargar usuarios', e)
      }
    },
    usernameById(id) {
      const u = this.users.find(x => String(x.id) === String(id))
      return u ? u.username : id
    },
    async submitCreate() {
      const title = (this.newEvent.title || '').trim()
      if (!title) { this.createError = 'El título es obligatorio'; return }
      
      let timeStr = null
      const sh = this.newEvent.startHour
      const eh = this.newEvent.endHour
      if (sh != null || eh != null) {
        if (sh == null || eh == null) {
          this.createError = 'Selecciona hora de inicio y fin'; return
        }
        if (Number(eh) < Number(sh)) {
          this.createError = 'La hora de fin no puede ser menor que la de inicio'; return
        }
        timeStr = `${this.pad2(Number(sh))}:00 - ${this.pad2(Number(eh))}:00`
      }
      
      const payload = {
        title,
        description: (this.newEvent.description || '').trim() || null,
        time: timeStr,
        color: this.newEvent.color || '#6366f1',
        date: this.todayISO, // Siempre para hoy
        createdById: this.currentUser?.id || null,
        responsibleIds: Array.isArray(this.newEvent.assignedToIds) ? this.newEvent.assignedToIds : []
      }
      
      try {
        const created = await createEvent(payload)
        if (created) {
          await this.loadMonth() // Recargar eventos
          this.cancelCreate()
        }
      } catch (e) {
        const msg = e?.response?.data?.message || 'No se pudo guardar el evento'
        this.createError = msg
      }
    },
    recalcCalendar() {
      const first = new Date(this.year, this.month, 1)
      const last = new Date(this.year, this.month + 1, 0)
      this.leadingBlanks = Array(first.getDay()).fill(0)
      this.daysInMonth = Array.from({ length: last.getDate() }, (_, i) => i + 1)
    },
    eventsForDay(day) {
      const key = this.toISO(this.year, this.month, day)
      return this.events.filter(e => (e.date || '') === key)
    },
    async loadMonth() {
      try {
        const data = await fetchMonthEvents(this.year, this.month + 1)
        if (Array.isArray(data)) {
          this.events = data
          return
        }
      } catch (e) {
        // fallback ya gestionado en el servicio
      }
      const data = await fetchMonthEvents(this.year, this.month + 1)
      this.events = Array.isArray(data) ? data : []
    },
    saveNote() {
      if (this.currentUser && this.currentUser.id) {
        localStorage.setItem(`user_quick_note_${this.currentUser.id}`, this.quickNote)
        this.saveStatus = 'Guardado'
        setTimeout(() => { this.saveStatus = '' }, 2000)
      }
    },
    clearNote() {
      if (confirm('¿Estás seguro de que quieres borrar todas las notas?')) {
        this.quickNote = ''
        this.saveNote()
      }
    },
    prevMonth() {
      if (this.month === 0) {
        this.month = 11
        this.year -= 1
      } else {
        this.month -= 1
      }
      this.recalcCalendar()
      this.loadMonth()
    },
    nextMonth() {
      if (this.month === 11) {
        this.month = 0
        this.year += 1
      } else {
        this.month += 1
      }
      this.recalcCalendar()
    },
    goToday() {
      const now = new Date()
      this.year = now.getFullYear()
      this.month = now.getMonth()
      this.recalcCalendar()
    },
    isToday(day) {
      return this.year === this.todayYear && this.month === this.todayMonth && day === this.todayDay
    },
    initWebSocketConnection() {
      if (!this.currentUser) return
      connectWebSocket('http://localhost:8080/ws', this.currentUser.username, null, () => {
        this.wsConnected = true
        // Suscripción a mensajes directos del usuario
        this.subscribeToGlobalUserChannel()
        // Suscripción a actualizaciones públicas
        this.subscribeToPublicChannel()
        // Suscripción a canales de grupo
        this.subscribeToAllGroupChannels()
      }, (err) => {
        console.error('Error de WebSocket en Home:', err)
        this.wsConnected = false
      })
    },
    subscribeToPublicChannel() {
      // Canal público para eventos generales como actualizaciones de perfil
      subscribe('/topic/public', (msg) => {
        if (msg && msg.action === 'user_updated') {
          // Si es el usuario actual, actualizar vuex
          if (String(msg.userId) === String(this.currentUser.id)) {
            this.$store.commit('auth/SET_USER', { ...this.currentUser, avatarUrl: msg.avatarUrl, username: msg.username })
          }
          // Si hay lógica para actualizar listas de usuarios/amigos, iría aquí
        }
      })
    },
    subscribeToGlobalUserChannel() {
      const topic = '/user/queue/messages'
      this.globalSubscription = subscribe(topic, (msg) => {
        const senderId = (msg && msg.sender && (msg.sender.id ?? msg.sender.userId)) || msg.senderId
        const senderName = (msg && msg.sender && msg.sender.username) || 'Usuario'
        let text = (msg && msg.content) || ''
        if (!text) {
          if (msg.type === 'image') text = '📷 Foto'
          else if (msg.type === 'file') text = '📎 Archivo'
          else text = '[Mensaje]'
        }
        const when = this.formatTime(msg && msg.timestamp)
        const key = `user-${senderId}-${msg.id || when}`
        this.notifications.unshift({
          key,
          type: 'user',
          title: senderName,
          text,
          when,
          senderId
        })
      })
    },
    async loadGroups() {
      try {
        const res = await fetchGroups()
        this.groups = Array.isArray(res.data) ? res.data : []
      } catch (e) {
        this.groups = []
      }
    },
    subscribeToGroupChannel(groupId) {
      const topic = `/topic/group/${groupId}`
      if (this.groupSubscriptions[groupId]) {
        this.groupSubscriptions[groupId].unsubscribe()
      }
      this.groupSubscriptions[groupId] = subscribe(topic, (msg) => {
        const group = msg && msg.group
        const title = (group && (group.name || `Grupo ${group.id}`)) || `Grupo ${groupId}`
        let text = (msg && msg.content) || ''
        if (!text) {
          if (msg.type === 'image') text = '📷 Foto'
          else if (msg.type === 'file') text = '📎 Archivo'
          else text = '[Mensaje de grupo]'
        }
        const when = this.formatTime(msg && msg.timestamp)
        const key = `group-${groupId}-${msg.id || when}`
        this.notifications.unshift({
          key,
          type: 'group',
          title,
          text,
          when,
          groupId
        })
      })
    },
    subscribeToAllGroupChannels() {
      if (!Array.isArray(this.groups)) return
      for (const g of this.groups) {
        if (g && g.id) {
          this.subscribeToGroupChannel(g.id)
        }
      }
    },
    formatTime(ts) {
      try {
        const d = ts ? new Date(ts) : new Date()
        return d.toLocaleString()
      } catch (e) {
        return '—'
      }
    },
    async viewNotification(n) {
      try {
        if (n.type === 'user' && this.currentUser && n.senderId != null) {
          await markMessagesAsRead(this.currentUser.id, n.senderId)
          // Eliminar todas las notificaciones del mismo remitente
          this.notifications = this.notifications.filter(x => !(x.type === 'user' && x.senderId === n.senderId))
        } else if (n.type === 'group' && n.groupId != null) {
          // Eliminar notificaciones del grupo
          this.notifications = this.notifications.filter(x => !(x.type === 'group' && x.groupId === n.groupId))
        }
      } catch (e) {
        console.warn('No se pudo marcar como leído:', e)
      }
      // Navegar a chat para ver el mensaje
      this.$router.push('/chat')
    },
    handleKeydown(e) {
      if (e.key === 'Escape') {
        if (this.showDetailModal) this.closeTaskDetails()
        if (this.showCreateModal) this.cancelCreate()
      }
    }
  },
  async mounted() {
    window.addEventListener('keydown', this.handleKeydown)
    if (this.currentUser && this.currentUser.id) {
      this.quickNote = localStorage.getItem(`user_quick_note_${this.currentUser.id}`) || ''
    }
    await this.loadMonth()
    await this.loadGroups()
    this.initWebSocketConnection()
  },
  watch: {
    month() { this.loadMonth() },
    year() { this.loadMonth() },
    currentUser(newUser) {
      if (newUser && newUser.id) {
        this.quickNote = localStorage.getItem(`user_quick_note_${newUser.id}`) || ''
      }
    }
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleKeydown)
    try {
      if (this.globalSubscription) this.globalSubscription.unsubscribe()
      Object.values(this.groupSubscriptions).forEach(s => s && s.unsubscribe())
      disconnectWebSocket()
    } catch (e) {
      console.error('Error al limpiar suscripciones en Home:', e)
    }
  }
}
</script>

<style scoped>
.home-page { display: flex; min-height: 100vh; background: #f8fafc; }
.home-content { flex: 1; padding: 24px; max-width: 1600px; margin: 0 auto; }

/* Animations */
@keyframes slideUpFade {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.welcome-bar {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff;
  color: #1e293b; border-radius: 20px; padding: 20px 24px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03);
  margin-bottom: 24px;
  border: 1px solid #f1f5f9;
  animation: slideUpFade 0.5s ease-out both;
  position: relative;
  overflow: hidden;
}

/* Decoration line */
.welcome-bar::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 6px;
  background: linear-gradient(180deg, var(--brand-gradient-start), var(--brand-gradient-end));
}

.welcome-left { display: flex; align-items: center; gap: 16px; }
.greeting-icon {
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  color: var(--brand-gradient-start);
  width: 48px; height: 48px; border-radius: 16px;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}
.greeting-text { display: flex; flex-direction: column; gap: 2px; }
.welcome-title { font-weight: 600; color: #64748b; font-size: 0.9rem; }
.welcome-user { font-weight: 800; font-size: 1.25rem; color: #0f172a; letter-spacing: -0.5px; }
.welcome-right { font-weight: 600; font-size: 0.95rem; color: #475569; text-align: right; background: #f8fafc; padding: 8px 16px; border-radius: 12px; }
.current-date { text-transform: capitalize; }

.empty-state {
  justify-content: center; flex-direction: column; gap: 12px;
  padding: 40px 20px; color: #94a3b8; background: #f8fafc;
  border-radius: 16px; border: 2px dashed #e2e8f0;
  transition: all 0.2s;
}
.empty-state:hover { border-color: #cbd5e1; background: #f1f5f9; }
.empty-icon { color: #cbd5e1; margin-bottom: 4px; }
.empty-text { text-align: center; display: flex; flex-direction: column; gap: 4px; }
.empty-text .strong { font-weight: 700; color: #475569; font-size: 1rem; }
.empty-text .sub { font-size: 0.85rem; }

.notice.empty-notice {
  background: #f8fafc; border: 2px dashed #e2e8f0;
  display: flex; align-items: center; justify-content: center;
  border-radius: 16px; padding: 40px 20px;
  color: #64748b; box-shadow: none;
}
.notice-content { display: flex; flex-direction: column; align-items: center; text-align: center; gap: 12px; }
.notice-icon { background: #e2e8f0; color: #94a3b8; padding: 12px; border-radius: 50%; }
.notice-header { display: flex; flex-direction: column; gap: 4px; }
.notice-title { font-weight: 700; font-size: 1rem; color: #475569; margin: 0; }
.notice-date { font-weight: 500; font-size: 0.85rem; opacity: 0.8; }

.home-grid { display: grid; grid-template-columns: 1.2fr 1fr; gap: 24px; }

.card-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #f1f5f9; }
.month-title, .card-title { font-weight: 800; color: #1e293b; font-size: 1.1rem; letter-spacing: -0.3px; }
.muted { color: #94a3b8; font-weight: 600; font-size: 0.9em; margin-left: 4px; }
.header-actions { display: flex; align-items: center; gap: 8px; }
.nav-btn, .dots-btn, .add-btn {
  border: 1px solid #e2e8f0; background: #fff; cursor: pointer; color: #64748b;
  border-radius: 10px; padding: 6px; transition: all 0.2s ease;
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px;
}
.nav-btn:hover, .dots-btn:hover, .add-btn:hover {
  background: #f8fafc; border-color: #cbd5e1; color: var(--brand-gradient-start); transform: translateY(-1px);
}
.add-btn { background: var(--brand-gradient-start); color: #fff; border: none; }
.add-btn:hover { background: var(--brand-gradient-end); color: #fff; box-shadow: 0 4px 10px rgba(37, 99, 235, 0.3); border-color: transparent; }

.view-all {
  color: var(--brand-gradient-start); text-decoration: none; font-weight: 600; font-size: 0.9rem;
  padding: 4px 12px; background: #eff6ff; border-radius: 20px; transition: all 0.2s;
}
.view-all:hover { background: #dbeafe; transform: translateY(-1px); }

.calendar-card, .tasks-card, .notification-card, .quicknotes-card {
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 24px;
  box-shadow: 0 10px 30px -10px rgba(0,0,0,0.05);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  animation: slideUpFade 0.5s ease-out both;
  overflow: hidden;
}

/* Staggered animation delays */
.calendar-card { animation-delay: 0.1s; }
.tasks-card { animation-delay: 0.2s; }
.notification-card { animation-delay: 0.3s; }
.quicknotes-card { animation-delay: 0.4s; }

.calendar-card:hover, .tasks-card:hover, .notification-card:hover, .quicknotes-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px -10px rgba(0,0,0,0.08);
}

/* Custom Scrollbar for lists */
.tasks-list::-webkit-scrollbar,
.notifications-list::-webkit-scrollbar,
.quicknotes-card .notes-content textarea::-webkit-scrollbar { width: 5px; }
.tasks-list::-webkit-scrollbar-track,
.notifications-list::-webkit-scrollbar-track,
.quicknotes-card .notes-content textarea::-webkit-scrollbar-track { background: transparent; }
.tasks-list::-webkit-scrollbar-thumb,
.notifications-list::-webkit-scrollbar-thumb,
.quicknotes-card .notes-content textarea::-webkit-scrollbar-thumb { background-color: #e2e8f0; border-radius: 10px; }
.tasks-list::-webkit-scrollbar-thumb:hover,
.notifications-list::-webkit-scrollbar-thumb:hover,
.quicknotes-card .notes-content textarea::-webkit-scrollbar-thumb:hover { background-color: #cbd5e1; }

.calendar { padding: 20px 24px 24px; }
.weekdays { display: grid; grid-template-columns: repeat(7,1fr); color: #94a3b8; font-weight: 700; font-size: 0.75rem; margin-bottom: 12px; text-transform: uppercase; letter-spacing: 1px; }
.weekdays span { text-align: center; }
.days { display: grid; grid-template-columns: repeat(7,1fr); gap: 8px; }
.day {
  border: 1px solid transparent; background: transparent; border-radius: 14px;
  padding: 0; height: 48px; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #334155; font-weight: 600; cursor: pointer; position: relative; transition: all 0.2s;
}
.day:hover:not(.today) { background: #f1f5f9; color: var(--brand-gradient-start); transform: scale(1.05); }
.day.today {
  background: var(--brand-gradient-start);
  color: #fff; border: none; box-shadow: 0 8px 16px rgba(37, 99, 235, 0.25);
  transform: scale(1.05);
}
.blank { height: 48px; }

.num { font-weight: 600; font-size: 0.95rem; z-index: 1; }
.dots { position: absolute; bottom: 6px; left: 0; right: 0; display: flex; justify-content: center; gap: 3px; }
.dot { width: 4px; height: 4px; border-radius: 50%; box-shadow: 0 1px 2px rgba(0,0,0,0.1); }
.more { font-size: 9px; color: #94a3b8; font-weight: 700; }
.day.today .more { color: rgba(255,255,255,0.8); }

.tasks-list { list-style: none; margin: 0; padding: 16px 20px 20px; display: flex; flex-direction: column; max-height: 300px; overflow-y: auto; gap: 10px; }
.task-item {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border: 1px solid #f1f5f9; border-radius: 12px; padding: 12px 16px;
  transition: all 0.2s; box-shadow: 0 2px 5px rgba(0,0,0,0.02);
}
.task-item:hover { border-color: #e2e8f0; transform: translateX(4px); box-shadow: 0 4px 12px rgba(0,0,0,0.04); }
.task-item label { display: flex; align-items: center; gap: 12px; font-weight: 600; color: #334155; cursor: pointer; font-size: 0.95rem; }
.task-item input[type="checkbox"] {
  accent-color: var(--brand-gradient-start); width: 18px; height: 18px; cursor: pointer;
  border-radius: 6px; border: 2px solid #cbd5e1;
}

.time { color: #64748b; font-weight: 600; font-size: 0.75rem; background: #f1f5f9; padding: 4px 8px; border-radius: 6px; }

.notice { margin: 0; padding: 0; border-radius: 0; color: inherit; background: transparent; position: relative; box-shadow: none; }
.notice-title { font-weight: 700; margin-bottom: 0; font-size: 1rem; }

.assign-btn {
  background: #fff; color: var(--brand-gradient-start); border: 1px solid #e2e8f0;
  border-radius: 8px; padding: 6px 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; font-size: 0.8rem;
}
.assign-btn:hover { background: #eff6ff; border-color: #dbeafe; }

.notifications-list { list-style: none; margin: 0; padding: 16px 20px; display: flex; flex-direction: column; gap: 12px; max-height: 320px; overflow-y: auto; }
.notification-item {
  display: flex; justify-content: space-between; align-items: flex-start;
  background: #fff; border: 1px solid #f1f5f9; border-radius: 16px; padding: 16px;
  transition: all 0.2s; box-shadow: 0 2px 5px rgba(0,0,0,0.02);
}
.notification-item:hover { background: #fafafa; border-color: #e2e8f0; transform: translateY(-2px); box-shadow: 0 8px 15px rgba(0,0,0,0.03); }
.notification-main { display: flex; flex-direction: column; gap: 6px; }
.notification-title { display: flex; align-items: center; font-weight: 700; color: #1e293b; font-size: 0.95rem; gap: 8px; }
.notification-text { color: #475569; font-weight: 500; font-size: 0.9rem; line-height: 1.5; }
.notification-meta { color: #94a3b8; font-weight: 600; font-size: 0.75rem; margin-top: 4px; }
.badge { display: inline-block; padding: 4px 8px; border-radius: 6px; font-size: 0.7rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
.badge.user { background: #eff6ff; color: #2563eb; }
.badge.group { background: #fef2f2; color: #dc2626; }

.action-btn {
  border: none; border-radius: 10px; padding: 8px 16px; cursor: pointer; font-weight: 600;
  display: flex; align-items: center; gap: 8px; font-size: 0.85rem; transition: all 0.2s;
}
.save-btn { background: #10b981; color: #fff; box-shadow: 0 4px 10px rgba(16, 185, 129, 0.2); }
.save-btn:hover { background: #059669; transform: translateY(-1px); box-shadow: 0 6px 15px rgba(16, 185, 129, 0.3); }
.clear-btn { background: #fff; border: 1px solid #fee2e2; color: #ef4444; padding: 8px 12px; }
.clear-btn:hover { background: #fef2f2; border-color: #fecaca; transform: translateY(-1px); }

.quicknotes-card .notes-content { padding: 20px; display: flex; flex-direction: column; height: 280px; background: #fff; }
.quicknotes-card textarea {
  flex: 1; width: 100%; border: 2px solid #f1f5f9; border-radius: 16px; padding: 16px;
  resize: none; font-family: 'Inter', sans-serif; font-size: 0.95rem; outline: none;
  background: #fff; color: #334155; line-height: 1.6; transition: all 0.2s;
  background-image: linear-gradient(#f1f5f9 1px, transparent 1px);
  background-size: 100% 32px;
  line-height: 32px;
  padding-top: 7px; /* Adjust to align text with lines */
}
.quicknotes-card textarea:focus {
  border-color: #e2e8f0;
  box-shadow: 0 0 0 4px #f1f5f9;
}
.save-status { font-size: 12px; color: #10b981; font-weight: 700; animation: fadeOut 2s forwards; margin-right: 8px; background: #ecfdf5; padding: 4px 10px; border-radius: 20px; }

@keyframes fadeOut {
  0% { opacity: 1; } 70% { opacity: 1; } 100% { opacity: 0; }
}

.right-actions { display: flex; align-items: center; gap: 12px; }

.today-btn {
  background: #fff; border: 1px solid #e2e8f0; color: #64748b;
  font-weight: 600; font-size: 0.85rem; cursor: pointer; padding: 6px 16px;
  border-radius: 8px; transition: all 0.2s; height: 32px; display: flex; align-items: center;
}
.today-btn:hover { background: #f8fafc; color: #1e293b; border-color: #cbd5e1; }

@media (max-width: 1200px) {
  .home-grid { grid-template-columns: 1fr; max-width: 800px; margin: 0 auto; }
}

/* Modal */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 1000;
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}
.modal-card {
  background: #fff; width: 100%; max-width: 480px; border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.2);
  display: flex; flex-direction: column; max-height: 90vh; overflow: hidden;
}
.modal-header {
  padding: 16px 20px; border-bottom: 1px solid #f1f5f9; display: flex;
  justify-content: space-between; align-items: center; background: #fafafa;
}
.modal-title { font-weight: 800; color: #1e293b; font-size: 1.1rem; margin: 0; }
.close-btn { background: none; border: none; font-size: 1.5rem; color: #94a3b8; cursor: pointer; line-height: 1; padding: 0 4px; }
.close-btn:hover { color: #ef4444; }

.modal-body { padding: 20px; overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 16px; }
.form-row { display: flex; flex-direction: column; gap: 6px; }
.form-row label { font-weight: 600; font-size: 0.9rem; color: #475569; }
.form-row input[type="text"],
.form-row textarea {
  width: 100%; padding: 10px 12px; border: 1px solid #cbd5e1; border-radius: 8px;
  font-family: inherit; font-size: 0.95rem; outline: none; transition: all 0.2s;
}
.form-row input[type="text"]:focus,
.form-row textarea:focus { border-color: var(--brand-gradient-start); box-shadow: 0 0 0 3px rgba(37,99,235,0.1); }
.form-row textarea { min-height: 80px; resize: vertical; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.hour-selects { display: flex; align-items: center; gap: 8px; }
.hour-selects select {
  padding: 8px; border: 1px solid #cbd5e1; border-radius: 8px;
  outline: none; font-size: 0.9rem; color: #334155; flex: 1;
}
.sep { color: #94a3b8; font-weight: 700; }
.color-picker-wrapper input[type="color"] {
  width: 100%; height: 38px; padding: 2px; border: 1px solid #cbd5e1; border-radius: 8px; cursor: pointer;
}

.checkbox-list {
  display: flex; flex-wrap: wrap; gap: 8px; max-height: 120px; overflow-y: auto;
  border: 1px solid #f1f5f9; padding: 8px; border-radius: 8px; background: #fafafa;
}
.checkbox-item {
  display: flex; align-items: center; gap: 6px; padding: 4px 8px; background: #fff;
  border: 1px solid #e2e8f0; border-radius: 6px; font-size: 0.85rem; cursor: pointer;
  transition: all 0.1s;
}
.checkbox-item:hover { border-color: #cbd5e1; }

.form-error { color: #ef4444; font-size: 0.85rem; font-weight: 600; background: #fef2f2; padding: 8px; border-radius: 6px; text-align: center; }

.modal-footer {
  padding: 16px 20px; border-top: 1px solid #f1f5f9; background: #fafafa;
  display: flex; justify-content: flex-end; gap: 12px;
}
.btn-cancel {
  background: #fff; border: 1px solid #e2e8f0; color: #64748b; padding: 8px 16px;
  border-radius: 8px; font-weight: 600; cursor: pointer;
}
.btn-cancel:hover { background: #f1f5f9; color: #1e293b; }
.btn-primary {
  background: var(--brand-gradient-start); border: none; color: #fff; padding: 8px 20px;
  border-radius: 8px; font-weight: 600; cursor: pointer; box-shadow: 0 4px 10px rgba(37,99,235,0.3);
}
.btn-primary:hover { background: var(--brand-gradient-end); transform: translateY(-1px); }

/* Task Item Clickable */
.task-item.clickable {
  cursor: pointer;
}
.task-info { display: flex; align-items: center; gap: 10px; flex: 1; }
.task-marker { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.task-title { font-weight: 600; color: #334155; font-size: 0.95rem; line-height: 1.4; }

/* Detail Modal Styles */
.detail-card-home .modal-header { background: #fff; padding: 20px 24px; }
.detail-card-home .header-content { display: flex; flex-direction: column; gap: 4px; }
.detail-card-home .modal-subtitle { font-size: 0.85rem; color: #94a3b8; font-weight: 600; text-transform: capitalize; }

.detail-row { display: flex; gap: 16px; margin-bottom: 8px; }
.detail-icon {
  width: 36px; height: 36px; background: #f8fafc; color: #64748b;
  border-radius: 10px; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.detail-text { display: flex; flex-direction: column; gap: 4px; flex: 1; justify-content: center; }
.detail-text .label { font-size: 0.75rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; }
.detail-text .value { font-size: 0.95rem; color: #334155; font-weight: 500; }
.detail-text .value.description { line-height: 1.5; color: #475569; }

.responsibles-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 4px; }
.responsible-badge {
  background: #f1f5f9; color: #475569; font-size: 0.8rem; font-weight: 600;
  padding: 4px 10px; border-radius: 6px; border: 1px solid #e2e8f0;
}
</style>