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
            <li v-for="ev in todayEvents" :key="ev.id || ev.title" class="task-item">
              <label><input type="checkbox"> {{ ev.title }}</label>
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
      users: []
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
      return this.events.filter(e => (e.date || '') === this.todayISO)
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
      localStorage.setItem('user_quick_note', this.quickNote)
      this.saveStatus = 'Guardado'
      setTimeout(() => { this.saveStatus = '' }, 2000)
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
        // Suscripción a canales de grupo
        this.subscribeToAllGroupChannels()
      }, (err) => {
        console.error('Error de WebSocket en Home:', err)
        this.wsConnected = false
      })
    },
    subscribeToGlobalUserChannel() {
      const topic = '/user/queue/messages'
      this.globalSubscription = subscribe(topic, (msg) => {
        const senderId = (msg && msg.sender && (msg.sender.id ?? msg.sender.userId)) || msg.senderId
        const senderName = (msg && msg.sender && msg.sender.username) || 'Usuario'
        const text = (msg && msg.content) || '[Mensaje]'
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
        const text = (msg && msg.content) || '[Mensaje de grupo]'
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
    }
  },
  async mounted() {
    this.quickNote = localStorage.getItem('user_quick_note') || ''
    await this.loadMonth()
    await this.loadGroups()
    this.initWebSocketConnection()
  },
  watch: {
    month() { this.loadMonth() },
    year() { this.loadMonth() }
  },
  beforeUnmount() {
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
.home-page { display: flex; min-height: 100vh; background: var(--surface-alt); }
.home-content { flex: 1; padding: 18px; }

/* Animations */
@keyframes slideUpFade {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.welcome-bar {
  display: flex; align-items: center; justify-content: space-between;
  background: linear-gradient(135deg, var(--brand-gradient-start), var(--brand-gradient-end));
  color: #fff; border-radius: var(--radius); padding: 10px 14px;
  box-shadow: 0 10px 25px -5px rgba(37, 99, 235, 0.3); /* Softer colored shadow */
  margin-bottom: 16px;
  animation: slideUpFade 0.5s ease-out both;
}

.welcome-left { display: flex; align-items: center; gap: 12px; }
.greeting-icon {
  background: rgba(255,255,255,0.2);
  width: 40px; height: 40px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(4px);
}
.greeting-text { display: flex; flex-direction: column; line-height: 1.2; }
.welcome-title { font-weight: 500; opacity: .9; font-size: 0.9rem; }
.welcome-user { font-weight: 800; font-size: 1.1rem; }
.welcome-right { opacity: .9; font-weight: 600; font-size: 0.9rem; text-align: right; }
.current-date { text-transform: capitalize; }

.empty-state {
  justify-content: center; flex-direction: column; gap: 6px;
  padding: 20px 12px; color: var(--text-muted); background: transparent; border: 2px dashed #e2e8f0;
}
.empty-state:hover { transform: none; box-shadow: none; border-color: #cbd5e1; }
.empty-icon { color: #94a3b8; margin-bottom: 4px; }
.empty-text { text-align: center; display: flex; flex-direction: column; }
.empty-text .strong { font-weight: 700; color: var(--text-primary); font-size: 0.95rem; }
.empty-text .sub { font-size: 0.8rem; }

.notice.empty-notice {
  background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%); /* Grey gradient for empty state */
  display: flex; align-items: center; justify-content: center;
}
.notice-content { display: flex; flex-direction: column; align-items: center; text-align: center; gap: 8px; }
.notice-icon { background: rgba(255,255,255,0.2); padding: 8px; border-radius: 50%; }

.home-grid { display: grid; grid-template-columns: 1.1fr 0.9fr; }

.card-header { display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; }
.month-title, .card-title { font-weight: 800; color: var(--text-primary); font-size: 1.05rem; }
.muted { color: var(--text-muted); font-weight: 600; font-size: 0.9em; }
.header-actions { display: flex; align-items: center; }
.nav-btn, .dots-btn, .add-btn {
  border: none; background: #f1f5f9; cursor: pointer; color: var(--text-muted);
  border-radius: 8px; padding: 6px; transition: all 0.2s ease;
}
.nav-btn:hover, .dots-btn:hover, .add-btn:hover { background: #e2e8f0; color: var(--brand-gradient-start); transform: scale(1.05); }
.view-all { color: var(--color-secondary); text-decoration: none; font-weight: 600; transition: opacity 0.2s; }
.view-all:hover { opacity: 0.8; text-decoration: underline; }

.calendar-card, .tasks-card, .notification-card, .quicknotes-card {
  background: var(--surface);
  border: 1px solid rgba(0,0,0,0.05); /* Softer border */
  border-radius: var(--radius);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  animation: slideUpFade 0.5s ease-out both;
}

/* Staggered animation delays */
.calendar-card { animation-delay: 0.1s; }
.tasks-card { animation-delay: 0.2s; }
.notification-card { animation-delay: 0.3s; }
.quicknotes-card { animation-delay: 0.4s; }

.calendar-card:hover, .tasks-card:hover, .notification-card:hover, .quicknotes-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

/* Custom Scrollbar for lists */
.tasks-list::-webkit-scrollbar,
.notifications-list::-webkit-scrollbar,
.quicknotes-card .notes-content textarea::-webkit-scrollbar,
.mini-chat::-webkit-scrollbar {
  width: 5px;
}
.tasks-list::-webkit-scrollbar-track,
.notifications-list::-webkit-scrollbar-track,
.quicknotes-card .notes-content textarea::-webkit-scrollbar-track,
.mini-chat::-webkit-scrollbar-track {
  background: transparent;
}
.tasks-list::-webkit-scrollbar-thumb,
.notifications-list::-webkit-scrollbar-thumb,
.quicknotes-card .notes-content textarea::-webkit-scrollbar-thumb,
.mini-chat::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 10px;
}
.tasks-list::-webkit-scrollbar-thumb:hover,
.notifications-list::-webkit-scrollbar-thumb:hover,
.quicknotes-card .notes-content textarea::-webkit-scrollbar-thumb:hover,
.mini-chat::-webkit-scrollbar-thumb:hover {
  background-color: #94a3b8;
}

.calendar { padding: 10px 14px 16px; }
.weekdays { display: grid; grid-template-columns: repeat(7,1fr); color: var(--text-muted); font-weight: 700; font-size: 11px; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.5px; }
.weekdays span { text-align: center; }
.days { display: grid; grid-template-columns: repeat(7,1fr); gap: 2px; }
.day {
  border: 1px solid transparent; background: transparent; border-radius: 10px;
  padding: 8px 0; text-align: center; color: var(--text-primary);
  font-weight: 600; cursor: pointer; position: relative; transition: all 0.2s;
}
.day:hover:not(.today) { background: #f1f5f9; color: var(--brand-gradient-start); }
.day.today {
  background: linear-gradient(135deg, var(--brand-gradient-start) 0%, var(--brand-gradient-end) 100%);
  color: #fff; border: none; box-shadow: 0 4px 10px rgba(37, 99, 235, 0.3);
}
.blank { height: 32px; }

.num { font-weight: 700; font-size: 13px; }
.dots { position: absolute; bottom: 4px; left: 0; right: 0; display: flex; justify-content: center; gap: 2px; }
.dot { width: 5px; height: 5px; border-radius: 50%; }
.more { font-size: 10px; color: var(--text-muted); }

.tasks-list { list-style: none; margin: 0; padding: 6px 12px 12px; display: flex; flex-direction: column; max-height: 260px; overflow-y: auto; gap: 8px; }
.task-item {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 10px 12px;
  transition: all 0.2s;
}
.task-item:hover { border-color: #cbd5e1; transform: translateX(2px); box-shadow: 0 2px 4px rgba(0,0,0,0.02); }
.task-item label { display: flex; align-items: center; gap: 10px; font-weight: 600; color: var(--text-primary); cursor: pointer; }
.task-item input[type="checkbox"] { accent-color: var(--brand-gradient-start); width: 16px; height: 16px; cursor: pointer; }

/* Modal Styles */
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000; animation: fadeIn 0.2s ease-out;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

.modal-card {
  background: #fff; width: 90%; max-width: 500px;
  border-radius: 16px; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1);
  overflow: hidden; animation: scaleIn 0.2s ease-out;
  display: flex; flex-direction: column;
}
@keyframes scaleIn { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }

.modal-header {
  padding: 16px 20px; border-bottom: 1px solid #e2e8f0;
  display: flex; justify-content: space-between; align-items: center;
}
.modal-title { margin: 0; font-size: 1.2rem; font-weight: 800; color: var(--text-primary); }
.close-btn {
  background: transparent; border: none; font-size: 1.5rem; line-height: 1;
  color: var(--text-muted); cursor: pointer; padding: 0 4px;
}
.close-btn:hover { color: var(--text-primary); }

.modal-body { padding: 20px; max-height: 60vh; overflow-y: auto; }

.form-row { margin-bottom: 16px; }
.form-row label { display: block; margin-bottom: 6px; font-weight: 700; font-size: 0.9rem; color: var(--text-primary); }
.form-row input[type="text"], .form-row textarea {
  width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 8px;
  font-family: inherit; font-size: 0.95rem; outline: none; transition: border-color 0.2s;
}
.form-row input[type="text"]:focus, .form-row textarea:focus { border-color: var(--brand-gradient-start); box-shadow: 0 0 0 3px rgba(37,99,235,0.1); }
.form-row textarea { min-height: 80px; resize: vertical; }

.form-grid { display: grid; grid-template-columns: 1fr 100px; gap: 16px; margin-bottom: 16px; }
.hour-selects { display: flex; align-items: center; gap: 8px; }
.hour-selects select {
  padding: 8px; border: 1px solid #cbd5e1; border-radius: 8px; outline: none;
  background: #fff; font-size: 0.9rem;
}
.sep { font-weight: bold; color: var(--text-muted); }

.color-picker-wrapper { height: 38px; border: 1px solid #cbd5e1; border-radius: 8px; overflow: hidden; padding: 2px; }
.color-picker-wrapper input[type="color"] { width: 100%; height: 100%; border: none; padding: 0; cursor: pointer; }

.checkbox-list { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; max-height: 150px; overflow-y: auto; padding: 4px; border: 1px solid #e2e8f0; border-radius: 8px; }
.checkbox-item {
  display: flex; align-items: center; gap: 8px; padding: 8px;
  border: 1px solid transparent; border-radius: 6px; cursor: pointer; transition: background 0.2s;
}
.checkbox-item:hover { background: #f8fafc; }
.checkbox-label { font-size: 0.9rem; }

.modal-footer {
  padding: 16px 20px; background: #f8fafc; border-top: 1px solid #e2e8f0;
  display: flex; justify-content: flex-end; gap: 12px;
}
.btn-cancel {
  padding: 8px 16px; border: 1px solid #cbd5e1; background: #fff; color: var(--text-primary);
  border-radius: 8px; font-weight: 600; cursor: pointer;
}
.btn-primary {
  padding: 8px 16px; border: none; background: linear-gradient(135deg, var(--brand-gradient-start), var(--brand-gradient-end));
  color: #fff; border-radius: 8px; font-weight: 600; cursor: pointer;
  box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.3);
}
.btn-primary:hover { opacity: 0.9; }
.btn-cancel:hover { background: #f1f5f9; }

.form-error { color: #ef4444; font-size: 0.9rem; margin-top: 10px; font-weight: 600; }

.time { color: var(--text-muted); font-weight: 500; font-size: 0.85rem; background: #f8fafc; padding: 2px 6px; border-radius: 4px; }

.notice { margin: 8px 12px 12px; padding: 16px; border-radius: 12px; color: #fff; background: linear-gradient(135deg, var(--brand-gradient-start) 0%, var(--brand-gradient-end) 100%); position: relative; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25); }
.notice-title { font-weight: 800; margin-bottom: 6px; font-size: 1.1rem; }
.notice-date { opacity: .9; font-weight: 600; }
.assign-btn {
  margin-top: 12px; background: rgba(255,255,255,0.2); color: #fff; border: 1px solid rgba(255,255,255,0.4);
  border-radius: 8px; padding: 6px 12px; font-weight: 600; cursor: pointer; backdrop-filter: blur(4px); transition: background 0.2s;
}
.assign-btn:hover { background: rgba(255,255,255,0.3); }

.notifications-list { list-style: none; margin: 8px 12px 12px; padding: 0; display: flex; flex-direction: column; gap: 10px; max-height: 300px; overflow-y: auto; }
.notification-item {
  display: flex; justify-content: space-between; align-items: flex-start;
  background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 12px;
  transition: all 0.2s;
}
.notification-item:hover { background: #f8fafc; border-color: #cbd5e1; }
.notification-main { display: flex; flex-direction: column; gap: 4px; }
.notification-title { display: flex; align-items: center; font-weight: 700; color: var(--text-primary); font-size: 0.95rem; }
.notification-text { color: var(--text-primary); font-weight: 500; font-size: 0.9rem; line-height: 1.4; }
.notification-meta { color: var(--text-muted); font-weight: 500; font-size: 0.75rem; margin-top: 2px; }
.badge { display: inline-block; padding: 3px 8px; border-radius: 6px; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
.badge.user { background: #eff6ff; color: #1d4ed8; }
.badge.group { background: #fef2f2; color: #b91c1c; }

.action-btn {
  border: none; border-radius: 8px; padding: 6px 12px; cursor: pointer; font-weight: 600;
  display: flex; align-items: center; gap: 6px; font-size: 12px; transition: all 0.2s;
}
.save-btn { background: linear-gradient(135deg, var(--brand-gradient-start) 0%, var(--brand-gradient-end) 100%); color: #fff; box-shadow: 0 2px 5px rgba(37, 99, 235, 0.2); }
.save-btn:hover { opacity: 0.95; transform: translateY(-1px); box-shadow: 0 4px 8px rgba(37, 99, 235, 0.3); }
.clear-btn { background: #fff; border: 1px solid #fee2e2; color: #ef4444; }
.clear-btn:hover { background: #fef2f2; border-color: #fecaca; }

.quicknotes-card .notes-content { padding: 16px; display: flex; flex-direction: column; height: 240px; }
.quicknotes-card textarea {
  flex: 1; width: 100%; border: 1px solid #e2e8f0; border-radius: 12px; padding: 14px;
  resize: none; font-family: 'Inter', sans-serif; font-size: 14px; outline: none;
  background: #f8fafc; color: var(--text-primary); line-height: 1.6; transition: all 0.2s;
}
.quicknotes-card textarea:focus {
  border-color: var(--brand-gradient-start); background: #fff;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}
.save-status { font-size: 12px; color: #10b981; font-weight: 600; animation: fadeOut 2s forwards; margin-right: 8px; }

@keyframes fadeOut {
  0% { opacity: 1; } 70% { opacity: 1; } 100% { opacity: 0; }
}

.right-actions { display: flex; align-items: center; }

.today-btn {
  background: transparent; border: 1px solid #cbd5e1; color: var(--text-primary);
  font-weight: 600; font-size: 0.85rem; cursor: pointer; padding: 4px 10px;
  border-radius: 6px; transition: all 0.2s;
}
.today-btn:hover { background: #f1f5f9; border-color: #94a3b8; }

@media (max-width: 992px) { .home-grid { grid-template-columns: 1fr; } }
</style>