<template>
  <div class="events-page light">
    <AppDock />
    <main class="content">
      <header class="topbar">
        <div class="left">
          <h1 class="title">{{ monthLabel }}</h1>
        </div>
        <div class="controls">
          <button class="nav-btn" @click="prevMonth" aria-label="Mes anterior">
            <Icon name="chevron-left" :size="18" />
          </button>
          <button class="nav-btn" @click="nextMonth" aria-label="Mes siguiente">
            <Icon name="chevron-right" :size="18" />
          </button>
          <button class="primary" @click="goToday">Hoy</button>
        </div>
      </header>

      <section class="calendar-wrap">
        <div class="calendar">
          <div class="weekdays">
            <div v-for="d in weekdays" :key="d" class="wd">{{ d }}</div>
          </div>
          <div class="days">
            <div v-for="n in startOffset" :key="'empty-'+n" class="day empty"></div>
            <div
              v-for="day in daysInMonth"
              :key="day"
              :class="['day', isToday(day) && 'today', isSelected(day) && 'selected']"
              @click="selectDay(day)"
            >
              <div class="num">{{ day }}</div>
              <div class="dots">
                <span
                  v-for="(ev, i) in eventsForDay(day).slice(0,3)"
                  :key="i"
                  class="dot"
                  :style="{ background: ev.color }"
                ></span>
                <span v-if="eventsForDay(day).length > 3" class="more">+{{ eventsForDay(day).length - 3 }}</span>
              </div>
            </div>
          </div>
        </div>

        <aside class="sidebar">
          <div class="panel">
            <h2 class="panel-title">Eventos del mes</h2>
            <div v-if="createOpen" class="create-form">
              <div class="form-header">
                <span>Nuevo evento</span>
                <button type="button" class="close" title="Cerrar" @click="cancelCreate">×</button>
              </div>
              <div class="form-subtitle">Fecha: {{ toISO(year, month, selectedDay || 1) }}</div>
              <div class="form-row">
                <label>Título*</label>
                <input type="text" v-model="newEvent.title" placeholder="Título del evento" />
              </div>
              <div class="form-row">
                <label>Descripción</label>
                <textarea v-model="newEvent.description" placeholder="Descripción (opcional)"></textarea>
              </div>
              <div class="form-grid">
                <div>
                  <label>Hora</label>
                  <input type="text" v-model="newEvent.time" placeholder="Ej: 07:00 - 10:00" />
                </div>
                <div>
                  <label>Color</label>
                  <input v-model="newEvent.color" type="color" />
                </div>
              </div>
              <div class="form-actions">
                <button class="primary" @click="submitCreate">Guardar</button>
                <button class="cancel" @click="cancelCreate">Cancelar</button>
              </div>
              <div v-if="createError" class="form-error">{{ createError }}</div>
            </div>
            <ul class="event-list">
              <li v-for="ev in monthEvents" :key="ev.id" class="event-item">
                <div class="event-left">
                  <span class="badge" :style="{ background: ev.color }"></span>
                </div>
                <div class="event-info">
                  <div class="event-title">{{ ev.title }}</div>
                  <div class="event-time">{{ ev.time }}</div>
                  <div class="event-desc" v-if="ev.description">{{ ev.description }}</div>
                </div>
              </li>
              <li v-if="monthEvents.length === 0" class="event-empty">Sin eventos este mes</li>
            </ul>
            <div class="panel-actions">
              <button class="primary" @click="openCreateForm">Nuevo evento</button>
            </div>
          </div>
        </aside>
      </section>
    </main>
  </div>
  
</template>

<script>
import AppDock from '../components/AppDock.vue'
import Icon from '../components/Icon.vue'
import { fetchMonthEvents, createEvent } from '../services/events.service.js'

export default {
  name: 'Events',
  components: { AppDock, Icon },
  data() {
    const now = new Date()
    return {
      cursor: new Date(now.getFullYear(), now.getMonth(), 1),
      selectedDay: now.getDate(),
      events: [
        { id: 1, date: this.toISO(now.getFullYear(), now.getMonth(), 6), title: 'Movie Night', time: '19:00 - 21:00', color: '#f472b6' },
        { id: 2, date: this.toISO(now.getFullYear(), now.getMonth(), 10), title: 'Franklin, 2+', time: '08:00 - 10:00', color: '#34d399' },
        { id: 3, date: this.toISO(now.getFullYear(), now.getMonth(), 24), title: 'Navidad', time: 'Todo el día', color: '#60a5fa' },
        { id: 4, date: this.toISO(now.getFullYear(), now.getMonth(), 6), title: 'Team Sync', time: '10:00 - 11:00', color: '#fb7185' },
        { id: 5, date: this.toISO(now.getFullYear(), now.getMonth(), 15), title: 'Reunión', time: '16:00 - 17:00', color: '#f59e0b' }
      ],
      weekdays: ['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'],
      createOpen: false,
      createError: '',
      newEvent: { title: '', description: '', time: '', color: '#6366f1' }
    }
  },
  computed: {
    year() { return this.cursor.getFullYear() },
    month() { return this.cursor.getMonth() },
    daysInMonth() { return new Date(this.year, this.month + 1, 0).getDate() },
    startOffset() {
      // index Monday-based
      const jsDow = new Date(this.year, this.month, 1).getDay() // 0=Sun..6=Sat
      const mondayIndex = (jsDow + 6) % 7
      return mondayIndex
    },
    monthLabel() {
      return this.cursor.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' })
    },
    monthEvents() {
      const ym = `${this.year}-${String(this.month+1).padStart(2,'0')}`
      return this.events
        .filter(e => (e.date || '').startsWith(ym))
        .sort((a,b) => (a.date||'').localeCompare(b.date||''))
    }
  },
  methods: {
    toISO(y,m,d) { return `${y}-${String(m+1).padStart(2,'0')}-${String(d).padStart(2,'0')}` },
    isToday(day) {
      const t = new Date()
      return t.getFullYear()===this.year && t.getMonth()===this.month && t.getDate()===day
    },
    isSelected(day) { return this.selectedDay === day },
    selectDay(day) { this.selectedDay = day },
    eventsForDay(day) {
      const key = this.toISO(this.year, this.month, day)
      return this.events.filter(e => e.date === key)
    },
    prevMonth() {
      const m = new Date(this.year, this.month - 1, 1)
      this.cursor = m
      this.selectedDay = 1
    },
    nextMonth() {
      const m = new Date(this.year, this.month + 1, 1)
      this.cursor = m
      this.selectedDay = 1
    },
    goToday() {
      const now = new Date()
      this.cursor = new Date(now.getFullYear(), now.getMonth(), 1)
      this.selectedDay = now.getDate()
    },
    openCreateForm() {
      this.createOpen = true
      this.createError = ''
      if (!this.newEvent.color) this.newEvent.color = '#6366f1'
    },
    cancelCreate() {
      this.createOpen = false
      this.createError = ''
      this.newEvent = { title: '', description: '', time: '', color: '#6366f1' }
    },
    async submitCreate() {
      const d = this.selectedDay || 1
      const title = (this.newEvent.title || '').trim()
      if (!title) { this.createError = 'El título es obligatorio'; return }
      const payload = {
        title,
        description: (this.newEvent.description || '').trim() || null,
        time: (this.newEvent.time || '').trim() || null,
        color: this.newEvent.color || '#6366f1',
        date: this.toISO(this.year, this.month, d)
      }
      try {
        const created = await createEvent(payload)
        if (created) {
          await this.loadMonth()
          this.cancelCreate()
        }
      } catch (e) {
        // No crear localmente para evitar confusión entre estado local y backend
        const msg = e?.response?.data?.message || 'No se pudo guardar el evento en el servidor'
        this.createError = msg
      }
    },
    async loadMonth() {
      try {
        const list = await fetchMonthEvents(this.year, this.month + 1)
        this.events = Array.isArray(list) ? list : []
      } catch (e) {
        console.warn('No se pudieron cargar eventos del backend, usando datos locales')
      }
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
.events-page.light { display: flex; min-height: 100vh; background: #ffffff; color: #1f2937; }
.content { flex: 1; padding: 20px 20px 32px 0; }

.topbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.title { font-size: 24px; font-weight: 800; color: #111827; }
.controls { display: flex; align-items: center; gap: 8px; }
.nav-btn { width: 32px; height: 32px; border-radius: 10px; border: 1px solid rgba(226,232,240,.9); background: #f1f5f9; color: #334155; cursor: pointer; }
.nav-btn:hover { background: #e2e8f0; }
.primary { padding: 8px 12px; border-radius: 10px; border: 1px solid rgba(226,232,240,.9); background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%); color: #fff; font-weight: 700; cursor: pointer; }

.calendar-wrap { display: grid; grid-template-columns: 1fr 320px; gap: 18px; }
.calendar { background: #ffffff; border: 1px solid rgba(226,232,240,.9); border-radius: 16px; padding: 18px; }
.weekdays { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; margin-bottom: 12px; }
.wd { padding: 8px 10px; border-radius: 12px; text-align: center; background: #f8fafc; color: #475569; font-weight: 600; }
.days { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; }
.day { background: #f8fafc; border: 1px solid rgba(226,232,240,.9); border-radius: 14px; padding: 10px; min-height: 74px; position: relative; transition: transform .15s ease; }
.day:hover { transform: translateY(-2px); }
.day.today { border-color: #8b5cf6; box-shadow: 0 0 0 2px rgba(139,92,246,.25) inset; }
.day.selected { background: #eef2ff; }
.day.empty { visibility: hidden; }
.num { font-weight: 700; color: #111827; }
.dots { position: absolute; bottom: 8px; left: 10px; display: flex; gap: 6px; }
.dot { width: 8px; height: 8px; border-radius: 9999px; box-shadow: 0 2px 6px rgba(0,0,0,.15); }
.more { font-size: 12px; color: #64748b; }

.sidebar { background: #ffffff; border: 1px solid rgba(226,232,240,.9); border-radius: 16px; padding: 16px; }
.panel-title { font-size: 16px; font-weight: 800; color: #111827; margin-bottom: 12px; }
.event-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.event-item { display: grid; grid-template-columns: 20px 1fr; align-items: center; gap: 10px; background: #f8fafc; border: 1px solid rgba(226,232,240,.9); border-radius: 12px; padding: 10px; }
.badge { width: 12px; height: 12px; border-radius: 9999px; display: inline-block; }
.event-title { font-weight: 700; color: #111827; }
.event-time { font-size: 12px; color: #64748b; }
.event-empty { background: #f8fafc; border: 1px solid rgba(226,232,240,.9); border-radius: 12px; padding: 10px; color: #64748b; }
.panel-actions { margin-top: 12px; display: flex; justify-content: flex-end; }

/* Formulario de creación */
.create-form { background: #f8fafc; border: 1px solid rgba(226,232,240,.9); border-radius: 14px; padding: 12px; margin-bottom: 12px; box-shadow: 0 4px 12px rgba(0,0,0,.06); }
.form-header { display: flex; align-items: center; justify-content: space-between; font-weight: 800; color: #111827; margin-bottom: 6px; }
.form-subtitle { font-size: 12px; color: #64748b; margin-bottom: 10px; }
.close { width: 28px; height: 28px; border-radius: 8px; border: 1px solid rgba(226,232,240,.9); background: #f1f5f9; color: #334155; cursor: pointer; }
.close:hover { background: #e2e8f0; }
.form-row { margin-bottom: 10px; }
.form-row label { display: block; font-size: 12px; font-weight: 700; color: #475569; margin-bottom: 6px; }
.create-form input[type="text"], .create-form textarea { width: 100%; border: 1px solid rgba(226,232,240,.9); background: #ffffff; border-radius: 10px; padding: 8px 10px; font-size: 14px; color: #111827; outline: none; transition: box-shadow .15s ease, border-color .15s ease; }
.create-form input[type="text"]:focus, .create-form textarea:focus { border-color: #8b5cf6; box-shadow: 0 0 0 3px rgba(139,92,246,.18); }
.create-form textarea { min-height: 68px; resize: vertical; }
.form-grid { display: grid; grid-template-columns: 1fr 100px; gap: 10px; }
.form-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 10px; }
.cancel { padding: 8px 12px; border-radius: 10px; border: 1px solid rgba(226,232,240,.9); background: #f1f5f9; color: #334155; font-weight: 700; cursor: pointer; }
.cancel:hover { background: #e2e8f0; }
.form-error { margin-top: 8px; font-size: 12px; color: #ef4444; background: #fee2e2; border: 1px solid #fecaca; border-radius: 10px; padding: 8px 10px; }

@media (max-width: 1024px) {
  .calendar-wrap { grid-template-columns: 1fr; }
}
</style>