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

<style scoped src="./Home.css"></style>