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
            <div class="card-title">Notification</div>
            <a href="#" class="view-all">View all</a>
          </header>
          <div class="notice">
            <div class="notice-header">
              <div class="notice-title">Emily just sent you task to assign</div>
              <div class="notice-date">18 Aug 2020 · 10:00 AM</div>
            </div>
            <button class="assign-btn">Assign here</button>
          </div>
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
      events: []
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
    }
  },
  async mounted() {
    await this.loadMonth()
  },
  watch: {
    month() { this.loadMonth() },
    year() { this.loadMonth() }
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