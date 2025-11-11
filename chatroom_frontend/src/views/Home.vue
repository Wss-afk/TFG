<template>
  <div class="home-page">
    <AppDock />
    <div class="home-content">
      <div class="welcome-bar" role="banner" aria-label="Bienvenida">
        <div class="welcome-left">
          <span class="welcome-title">Bienvenido</span>
          <span class="welcome-user">{{ currentUser?.username || 'Usuario' }}</span>
        </div>
        <div class="welcome-right">
          <Icon name="smile" :size="20" />
        </div>
      </div>
      <div class="home-grid">
        <!-- Columna izquierda: Calendario + Today Task -->
        <section class="calendar-card">
          <header class="card-header">
            <div class="month-title">
              {{ monthLabel }}
            </div>
            <div class="header-actions">
              <button class="nav-btn" aria-label="Mes anterior" @click="prevMonth"><Icon name="chevron-left" :size="18" /></button>
              <button class="nav-btn" aria-label="Mes siguiente" @click="nextMonth"><Icon name="chevron-right" :size="18" /></button>
              <button class="dots-btn" aria-label="Más opciones"><Icon name="dots" :size="18" /></button>
            </div>
          </header>
          <div class="calendar">
            <div class="weekdays">
              <span v-for="d in weekdays" :key="d">{{ d }}</span>
            </div>
            <div class="days">
              <span v-for="(d, idx) in leadingBlanks" :key="'b'+idx" class="blank"></span>
              <button v-for="day in daysInMonth" :key="day" :class="['day', isToday(day) && 'today']">
                <span class="num">{{ day }}</span>
                <div class="dots">
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
            <button class="add-btn" aria-label="Agregar"><Icon name="plus" :size="18" /></button>
          </header>
          <ul class="tasks-list">
            <li v-for="ev in todayEvents" :key="ev.id || ev.title" class="task-item">
              <label><input type="checkbox"> {{ ev.title }}</label>
              <span class="time">{{ ev.time || 'Todo el día' }}</span>
            </li>
            <li v-if="todayEvents.length === 0" class="task-item">
              <label><input type="checkbox" disabled> No hay eventos hoy</label>
              <span class="time"></span>
            </li>
          </ul>
        </section>

        <!-- Columna derecha: Notification + Team Chat -->
        <section class="notification-card">
          <header class="card-header">
            <div class="card-title">Notification <span class="muted">({{ notifications.length }})</span></div>
            <router-link to="/chat" class="view-all">View all</router-link>
          </header>
          <div class="notice" v-if="notifications.length === 0">
            <div class="notice-header">
              <div class="notice-title">No hay nuevas notificaciones</div>
              <div class="notice-date">—</div>
            </div>
          </div>
          <ul class="notifications-list" v-else>
            <li class="notification-item" v-for="n in notifications" :key="n.key">
              <div class="notification-main">
                <div class="notification-title">
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

        <section class="teamchat-card">
          <header class="card-header">
            <div class="card-title">Team Chat</div>
            <div class="right-actions">
              <div class="avatars">
                <span class="avatar" v-for="i in 5" :key="i">{{ ['E','A','S','D','M'][i-1] }}</span>
              </div>
              <button class="invite-btn"><Icon name="plus" :size="18" /> Invite People</button>
            </div>
          </header>
          <div class="mini-chat">
            <div class="bubble">
              <div class="sender">@members</div>
              <div class="text">Hey, how's the project going?</div>
              <div class="meta">3 minutes ago</div>
            </div>
            <div class="bubble mine">
              <div class="text">I'm preparing for user research</div>
              <div class="meta">7 minutes ago</div>
            </div>
          </div>
          <div class="mini-input">
            <input type="text" placeholder="Type a message..." />
            <button class="send-mini">Send</button>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script>
import AppDock from '../components/AppDock.vue'
import Icon from '../components/Icon.vue'
import { mapGetters } from 'vuex'
import { fetchMonthEvents } from '../services/events.service.js'
import { connectWebSocket, subscribe, disconnectWebSocket } from '../services/websocket.js'
import { markMessagesAsRead } from '../services/chat.service.js'
import { fetchGroups } from '../services/user.service.js'
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
      wsConnected: false,
      globalSubscription: null,
      groupSubscriptions: {},
      groups: []
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
    }
  },
  methods: {
    toISO(y,m,d) { return `${y}-${String(m+1).padStart(2,'0')}-${String(d).padStart(2,'0')}` },
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
      this.loadMonth()
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
.welcome-bar { display: flex; align-items: center; justify-content: space-between; background: linear-gradient(135deg, var(--brand-gradient-start), var(--brand-gradient-end)); color: #fff; border-radius: var(--radius); padding: 10px 14px; box-shadow: var(--shadow); margin-bottom: 16px; }
.welcome-left { display: flex; align-items: baseline; gap: 8px; }
.welcome-title { font-weight: 700; opacity: .95; }
.welcome-user { font-weight: 800; }
.welcome-right { opacity: .95; }
.home-grid { display: grid; grid-template-columns: 1.1fr 0.9fr; gap: 18px; }

.card-header { display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; }
.month-title, .card-title { font-weight: 800; color: var(--text-primary); }
.muted { color: var(--text-muted); font-weight: 600; }
.header-actions { display: flex; gap: 8px; align-items: center; }
.nav-btn, .dots-btn, .add-btn { border: none; background: #f1f5f9; cursor: pointer; color: var(--text-muted); border-radius: 10px; padding: 6px; }
.nav-btn:hover, .dots-btn:hover, .add-btn:hover { background: #e5e7eb; }
.view-all { color: var(--color-secondary); text-decoration: none; font-weight: 600; }

.calendar-card, .tasks-card, .notification-card, .teamchat-card {
  background: var(--surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.calendar { padding: 10px 14px 16px; }
.weekdays { display: grid; grid-template-columns: repeat(7,1fr); color: var(--text-muted); font-weight: 700; font-size: 11px; margin-bottom: 6px; }
.weekdays span { text-align: center; }
.days { display: grid; grid-template-columns: repeat(7,1fr); gap: 5px; }
.day { border: 1px solid var(--border-color); background: #f9fafb; border-radius: 10px; padding: 8px 0; text-align: center; color: var(--text-primary); font-weight: 700; cursor: pointer; position: relative; }
.day.today { background: linear-gradient(135deg, var(--brand-gradient-start) 0%, var(--brand-gradient-end) 100%); color: #fff; border: none; }
.blank { height: 32px; }

.num { font-weight: 700; }
.dots { position: absolute; bottom: 6px; left: 8px; display: flex; gap: 6px; }
.dot { width: 8px; height: 8px; border-radius: 9999px; box-shadow: 0 2px 6px rgba(0,0,0,.15); }
.more { font-size: 12px; color: var(--text-muted); }

.tasks-list { list-style: none; margin: 0; padding: 6px 12px 12px; display: flex; flex-direction: column; gap: 8px; max-height: 260px; overflow-y: auto; }
.task-item { display: flex; justify-content: space-between; align-items: center; background: #f8fafc; border: 1px solid var(--border-color); border-radius: 10px; padding: 8px 10px; }
.task-item label { display: flex; align-items: center; gap: 8px; font-weight: 600; color: var(--text-primary); }
.task-item .time { color: var(--text-muted); font-weight: 600; }

.notice { margin: 8px 12px 12px; padding: 14px; border-radius: 12px; color: #fff; background: linear-gradient(135deg, var(--brand-gradient-start) 0%, var(--brand-gradient-end) 100%); position: relative; }
.notice-title { font-weight: 800; margin-bottom: 6px; }
.notice-date { opacity: .9; font-weight: 600; }
.assign-btn { margin-top: 10px; background: #fff; color: var(--brand-gradient-start); border: none; border-radius: 10px; padding: 6px 10px; font-weight: 700; cursor: pointer; }
.notifications-list { list-style: none; margin: 8px 12px 12px; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.notification-item { display: flex; justify-content: space-between; align-items: center; background: #f8fafc; border: 1px solid var(--border-color); border-radius: 12px; padding: 10px 12px; }
.notification-main { display: flex; flex-direction: column; gap: 4px; }
.notification-title { display: flex; align-items: center; gap: 8px; font-weight: 800; color: var(--text-primary); }
.notification-text { color: var(--text-primary); font-weight: 600; }
.notification-meta { color: var(--text-muted); font-weight: 600; font-size: 12px; }
.badge { display: inline-block; padding: 2px 8px; border-radius: 9999px; font-size: 12px; font-weight: 800; }
.badge.user { background: #dbeafe; color: #1e40af; }
.badge.group { background: #fee2e2; color: #991b1b; }

.teamchat-card .mini-chat { padding: 6px 12px; display: flex; flex-direction: column; gap: 10px; max-height: 240px; overflow-y: auto; }
.right-actions { display: flex; align-items: center; gap: 10px; }
.avatars { display: flex; align-items: center; }
.avatar { width: 28px; height: 28px; border-radius: 50%; background: var(--color-bg-gradient-end); color: var(--brand-gradient-start); display: inline-flex; align-items: center; justify-content: center; font-weight: 800; margin-left: -6px; box-shadow: 0 0 0 2px #fff; }
.bubble { background: var(--color-bg-gradient-start); border: 1px solid var(--border-color); border-radius: 14px; padding: 8px 10px; max-width: 90%; }
.bubble .sender { color: var(--color-secondary); font-weight: 700; margin-bottom: 2px; }
.bubble .text { color: var(--text-primary); }
.bubble .meta { color: var(--text-muted); font-size: 12px; margin-top: 4px; }
.bubble.mine { background: linear-gradient(135deg, var(--brand-gradient-start) 0%, var(--brand-gradient-end) 100%); color: #fff; border: none; align-self: flex-end; }

.mini-input { display: flex; gap: 10px; padding: 12px; border-top: 1px solid var(--border-color); }
.mini-input input { flex: 1; border: 1px solid var(--border-color); border-radius: 12px; padding: 8px 10px; }
.mini-input .send-mini { border: none; background: linear-gradient(135deg, var(--brand-gradient-start) 0%, var(--brand-gradient-end) 100%); color: #fff; border-radius: 12px; padding: 8px 12px; font-weight: 700; cursor: pointer; }

@media (max-width: 992px) { .home-grid { grid-template-columns: 1fr; } }
</style>