<template>
  <div class="events-page light">
    <AppDock />
    <main class="content">
      <header class="topbar">
        <div class="left">
          <h1 class="title">{{ monthLabel }}</h1>
        </div>
        <div class="controls gap8-2">
          <button class="nav-btn" @click="prevMonth" aria-label="Mes anterior">
            <Icon name="chevron-left" :size="18" />
          </button>
          <button class="nav-btn" @click="nextMonth" aria-label="Mes siguiente">
            <Icon name="chevron-right" :size="18" />
          </button>
          <button class="primary" @click="goToday">Hoy</button>
        </div>
      </header>

      <section class="calendar-wrap gap8-2">
        <div class="calendar">
          <div class="weekdays gap8-1">
            <div v-for="d in weekdays" :key="d" class="wd">{{ d }}</div>
          </div>
          <div class="days gap8-1">
            <div v-for="n in startOffset" :key="'empty-'+n" class="day empty"></div>
            <div
              v-for="day in daysInMonth"
              :key="day"
              :class="['day', isToday(day) && 'today', isSelected(day) && 'selected']"
              @click="selectDay(day)"
            >
              <div class="num">{{ day }}</div>
              <div class="dots gap8-1">
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
              <div class="form-subtitle">Fecha: {{ toISO(year, month, selectedDay || 1) }} · Creador: {{ currentUser?.username || '—' }}</div>
              <div class="form-row">
                <label>Título*</label>
                <input type="text" v-model="newEvent.title" placeholder="Título del evento" />
              </div>
              <div class="form-row">
                <label>Descripción</label>
                <textarea v-model="newEvent.description" placeholder="Descripción (opcional)"></textarea>
              </div>
              <div class="form-grid gap8-2">
                <div>
                  <label>Hora</label>
                  <div class="hour-selects">
                    <select v-model="newEvent.startHour">
                      <option :value="null">Inicio</option>
                      <option v-for="h in hours24" :key="'sh-'+h" :value="h">{{ pad2(h) }}:00</option>
                    </select>
                    <span class="hour-sep">a</span>
                    <select v-model="newEvent.endHour">
                      <option :value="null">Fin</option>
                      <option v-for="h in hours24" :key="'eh-'+h" :value="h">{{ pad2(h) }}:00</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label>Color</label>
                  <input v-model="newEvent.color" type="color" />
                </div>
              </div>
          <div class="form-row">
            <label>Responsables</label>
            <div class="checkbox-list gap8-1">
              <label v-for="u in users" :key="u.id" class="checkbox-item gap8-2">
                <input type="checkbox" :value="u.id" v-model="newEvent.assignedToIds" />
                <span class="checkbox-label">{{ u.username }}</span>
              </label>
            </div>
            <div class="selected-chips" v-if="newEvent.assignedToIds && newEvent.assignedToIds.length">
              <span class="chip" v-for="id in newEvent.assignedToIds" :key="'chip-'+id">{{ usernameById(id) }}</span>
            </div>
          </div>
              <div class="form-actions">
                <button class="primary" @click="submitCreate">Guardar</button>
                <button class="cancel" @click="cancelCreate">Cancelar</button>
              </div>
              <div v-if="createError" class="form-error">{{ createError }}</div>
            </div>
            <ul class="event-list gap8-2">
              <template v-if="loadingEvents">
                <li v-for="n in 5" :key="'sk-'+n" class="event-item skeleton">
                  <div class="event-left">
                    <span class="badge skeleton-badge"></span>
                  </div>
                  <div class="event-info">
                    <div class="event-title skeleton-line" style="width: 50%"></div>
                    <div class="event-time skeleton-line" style="width: 35%; margin-top: 6px"></div>
                  </div>
                </li>
              </template>
              <template v-else>
                <li v-for="ev in monthEvents" :key="ev.id" class="event-item gap8-2" @click="openEventDetails(ev)" style="cursor: pointer">
                  <div class="event-left">
                    <span class="badge" :style="{ background: ev.color }"></span>
                  </div>
                  <div class="event-info">
                    <div class="event-title">{{ ev.title }}</div>
                    <div class="event-time">{{ ev.time }}</div>
                    <div class="event-meta">
                      <span v-if="ev.createdById">Creador: {{ usernameById(ev.createdById) }}</span>
                      <span v-if="(ev.responsibleIds && ev.responsibleIds.length) || (ev.assignedToIds && ev.assignedToIds.length)" style="margin-left: 8px">
                        Responsables: {{ ((ev.responsibleIds || ev.assignedToIds) || []).map(id => usernameById(id)).join(', ') }}
                      </span>
                    </div>
                    <div class="event-desc" v-if="ev.description">{{ ev.description }}</div>
                  </div>
                </li>
                <li v-if="monthEvents.length === 0" class="event-empty">Sin eventos este mes</li>
              </template>
            </ul>
            <div v-if="detailOpen" class="detail-modal">
              <div class="detail-card">
                <div class="detail-header">
                  <div class="detail-title">
                    <span class="badge" :style="{ background: detailEvent.color }"></span>
                    {{ detailEvent.title }}
                  </div>
                  <button class="close" title="Cerrar" @click="closeEventDetails">×</button>
                </div>
                <div class="detail-body">
                  <div class="detail-row"><strong>Fecha:</strong> {{ (detailEvent && detailEvent.date) || '—' }}</div>
                  <div class="detail-row"><strong>Hora:</strong> {{ detailEvent.time || '—' }}</div>
                  <div class="detail-row"><strong>Descripción:</strong> {{ detailEvent.description || '—' }}</div>
                  <div class="detail-row"><strong>Creador:</strong> {{ detailEvent.createdById ? usernameById(detailEvent.createdById) : '—' }}</div>
                  <div class="detail-row"><strong>Responsables:</strong> {{ ((detailEvent.responsibleIds || detailEvent.assignedToIds) || []).length ? ((detailEvent.responsibleIds || detailEvent.assignedToIds).map(id => usernameById(id)).join(', ')) : '—' }}</div>
                </div>
                <div class="detail-actions">
                  <button class="danger" :disabled="deleting" @click="removeEvent">{{ deleting ? 'Eliminando…' : 'Eliminar' }}</button>
                  <button class="cancel" :disabled="deleting" @click="closeEventDetails">Cerrar</button>
                </div>
                <div v-if="deleteError" class="form-error">{{ deleteError }}</div>
              </div>
            </div>
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
import { fetchMonthEvents, createEvent, deleteEvent } from '../services/events.service.js'
import { fetchUsers } from '../services/user.service.js'
import { mapGetters } from 'vuex'

export default {
  name: 'Events',
  components: { AppDock, Icon },
  data() {
    const now = new Date()
    return {
      cursor: new Date(now.getFullYear(), now.getMonth(), 1),
      selectedDay: now.getDate(),
      events: [],
      loadingEvents: false,
      weekdays: ['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'],
      createOpen: false,
      createError: '',
      newEvent: { title: '', description: '', time: '', color: '#6366f1', assignedToIds: [], startHour: null, endHour: null },
      users: [],
      detailOpen: false,
      detailEvent: null,
      deleteError: '',
      deleting: false
    }
  },
  computed: {
    ...mapGetters('auth', ['currentUser']),
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
    hours24() {
      return Array.from({ length: 24 }, (_, i) => i)
    },
    monthEvents() {
      const ym = `${this.year}-${String(this.month+1).padStart(2,'0')}`
      return this.events
        .filter(e => (e.date || '').startsWith(ym))
        .sort((a,b) => (a.date||'').localeCompare(b.date||''))
    }
  },
  methods: {
    pad2(n) { return String(n).padStart(2,'0') },
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
      this.newEvent = { title: '', description: '', time: '', color: '#6366f1', assignedToIds: [], startHour: null, endHour: null }
    },
    async submitCreate() {
      const d = this.selectedDay || 1
      const title = (this.newEvent.title || '').trim()
      if (!title) { this.createError = 'El título es obligatorio'; return }
      // Construir la cadena de tiempo a partir de los selects
      let timeStr = null
      const sh = this.newEvent.startHour
      const eh = this.newEvent.endHour
      if (sh != null || eh != null) {
        if (sh == null || eh == null) {
          this.createError = 'Selecciona hora de inicio y fin';
          return
        }
        if (Number(eh) < Number(sh)) {
          this.createError = 'La hora de fin no puede ser menor que la de inicio';
          return
        }
        timeStr = `${this.pad2(Number(sh))}:00 - ${this.pad2(Number(eh))}:00`
      }
      const payload = {
        title,
        description: (this.newEvent.description || '').trim() || null,
        time: timeStr,
        color: this.newEvent.color || '#6366f1',
        date: this.toISO(this.year, this.month, d),
        createdById: this.currentUser?.id || null,
        responsibleIds: Array.isArray(this.newEvent.assignedToIds) ? this.newEvent.assignedToIds : []
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
      this.loadingEvents = true
      try {
        const list = await fetchMonthEvents(this.year, this.month + 1)
        this.events = Array.isArray(list) ? list : []
      } catch (e) {
        console.warn('No se pudieron cargar eventos del backend')
        this.events = []
      } finally {
        this.loadingEvents = false
      }
    },
    async loadUsers() {
      try {
        const res = await fetchUsers()
        const arr = Array.isArray(res?.data) ? res.data : []
        this.users = arr
      } catch (e) {
        console.warn('No se pudieron cargar usuarios para asignación')
        this.users = []
      }
    },
    usernameById(id) {
      const u = this.users.find(x => String(x.id) === String(id))
      return u ? u.username : id
    },
    openEventDetails(ev) {
      this.detailEvent = ev
      this.detailOpen = true
      this.deleteError = ''
    },
    closeEventDetails() {
      this.detailOpen = false
      this.detailEvent = null
      this.deleteError = ''
    },
    async removeEvent() {
      if (!this.detailEvent?.id) return
      this.deleting = true
      this.deleteError = ''
      try {
        await deleteEvent(this.detailEvent.id)
        this.closeEventDetails()
        await this.loadMonth()
      } catch (e) {
        const msg = e?.response?.data?.message || 'No se pudo eliminar el evento en el servidor'
        this.deleteError = msg
      } finally {
        this.deleting = false
      }
    }
  },
  async mounted() {
    await this.loadMonth()
    await this.loadUsers()
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
.controls { display: flex; align-items: center; }
.nav-btn { width: 32px; height: 32px; border-radius: 10px; border: 1px solid rgba(226,232,240,.9); background: #f1f5f9; color: #334155; cursor: pointer; }
.nav-btn:hover { background: #e2e8f0; }
.primary { padding: 8px 12px; border-radius: 10px; border: 1px solid rgba(226,232,240,.9); background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%); color: #fff; font-weight: 700; cursor: pointer; }

.calendar-wrap { display: grid; grid-template-columns: 1fr 320px; }
.calendar { background: #ffffff; border: 1px solid rgba(226,232,240,.9); border-radius: 16px; padding: 18px; }
.weekdays { display: grid; grid-template-columns: repeat(7, 1fr); margin-bottom: 12px; }
.wd { padding: 8px 10px; border-radius: 12px; text-align: center; background: #f8fafc; color: #475569; font-weight: 600; }
.days { display: grid; grid-template-columns: repeat(7, 1fr); }
.day { background: #f8fafc; border: 1px solid rgba(226,232,240,.9); border-radius: 14px; padding: 10px; min-height: 74px; position: relative; transition: transform .15s ease; }
.day:hover { transform: translateY(-2px); }
.day.today { border-color: #8b5cf6; box-shadow: 0 0 0 2px rgba(139,92,246,.25) inset; }
.day.selected { background: #eef2ff; }
.day.empty { visibility: hidden; }
.num { font-weight: 700; color: #111827; }
.dots { position: absolute; bottom: 8px; left: 10px; display: flex; }
.dot { width: 8px; height: 8px; border-radius: 9999px; box-shadow: 0 2px 6px rgba(0,0,0,.15); }
.more { font-size: 12px; color: #64748b; }

.sidebar { background: #ffffff; border: 1px solid rgba(226,232,240,.9); border-radius: 16px; padding: 16px; }
.panel-title { font-size: 16px; font-weight: 800; color: #111827; margin-bottom: 12px; }
.event-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; }
.event-item { display: grid; grid-template-columns: 20px 1fr; align-items: center; background: #f8fafc; border: 1px solid rgba(226,232,240,.9); border-radius: 12px; padding: 10px; }
.badge { width: 12px; height: 12px; border-radius: 9999px; display: inline-block; }
.event-title { font-weight: 700; color: #111827; }
.event-time { font-size: 12px; color: #64748b; }
.event-meta { font-size: 12px; color: #64748b; }
.event-empty { background: #f8fafc; border: 1px solid rgba(226,232,240,.9); border-radius: 12px; padding: 10px; color: #64748b; }
.panel-actions { margin-top: 12px; display: flex; justify-content: flex-end; }

/* Skeleton loading */
.skeleton-badge { background: #e5e7eb !important; }
.skeleton-line { height: 12px; background: linear-gradient(90deg, #f1f5f9, #e2e8f0, #f1f5f9); border-radius: 8px; animation: shimmer 1.2s infinite; }
@keyframes shimmer { 0% { background-position: -200px 0; } 100% { background-position: 200px 0; } }
.event-item.skeleton .event-title, .event-item.skeleton .event-time { background: linear-gradient(90deg, #f1f5f9, #e2e8f0, #f1f5f9); }

/* Formulario de creación */
.create-form { background: #f8fafc; border: 1px solid rgba(226,232,240,.9); border-radius: 14px; padding: 16px; margin-bottom: 12px; box-shadow: 0 4px 12px rgba(0,0,0,.06); border-left: 4px solid #8b5cf6; }
.form-header { display: flex; align-items: center; justify-content: space-between; font-weight: 800; color: #111827; margin-bottom: 6px; }
.form-subtitle { font-size: 12px; color: #64748b; margin-bottom: 10px; }
.close { width: 28px; height: 28px; border-radius: 8px; border: 1px solid rgba(226,232,240,.9); background: #f1f5f9; color: #334155; cursor: pointer; }
.close:hover { background: #e2e8f0; }
.form-row { margin-bottom: 10px; }
.form-row label { display: block; font-size: 12px; font-weight: 700; color: #475569; margin-bottom: 6px; }
.create-form input[type="text"], .create-form textarea { width: 100%; border: 1px solid rgba(226,232,240,.9); background: #ffffff; border-radius: 10px; padding: 8px 10px; font-size: 14px; color: #111827; outline: none; transition: box-shadow .15s ease, border-color .15s ease; }
.create-form input[type="text"]:focus, .create-form textarea:focus { border-color: #8b5cf6; box-shadow: 0 0 0 3px rgba(139,92,246,.18); }
.create-form textarea { min-height: 68px; resize: vertical; }
.form-grid { display: grid; grid-template-columns: 1fr 88px; align-items: end; }
.create-form input[type="color"] { width: 100%; height: 40px; border: 1px solid rgba(226,232,240,.9); border-radius: 10px; padding: 0; box-sizing: border-box; }
.create-form input[type="color"]:focus { border-color: #8b5cf6; box-shadow: 0 0 0 3px rgba(139,92,246,.18); }
.create-form select { width: 100%; border: 1px solid rgba(226,232,240,.9); background: #ffffff; border-radius: 10px; padding: 8px 10px; font-size: 14px; color: #111827; outline: none; }
.create-form select[multiple] { min-height: 96px; }

/* Lista moderna de checkboxes para Responsables */
.checkbox-list { display: grid; grid-template-columns: 1fr; }
@media (min-width: 640px) { .checkbox-list { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
.checkbox-item { display: flex; align-items: center; padding: 10px; border: 1px solid rgba(226,232,240,.9); border-radius: 10px; background: #ffffff; transition: border-color .2s ease, box-shadow .2s ease, transform .05s ease; min-height: 44px; }
.checkbox-item:hover { border-color: #cbd5e1; box-shadow: 0 2px 6px rgba(0,0,0,.06); }
.checkbox-item input[type="checkbox"] { width: 18px; height: 18px; accent-color: #8b5cf6; }
.checkbox-label { color: #0f172a; }
.selected-chips { margin-top: 10px; display: flex; flex-wrap: wrap; gap: 8px; }
.chip { display: inline-flex; align-items: center; gap: 6px; padding: 6px 10px; border-radius: 9999px; background: #ede9fe; color: #5b21b6; font-size: 12px; border: 1px solid #ddd6fe; }
.form-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 10px; }
.cancel { padding: 8px 12px; border-radius: 10px; border: 1px solid rgba(226,232,240,.9); background: #f1f5f9; color: #334155; font-weight: 700; cursor: pointer; }
.cancel:hover { background: #e2e8f0; }
.form-error { margin-top: 8px; font-size: 12px; color: #ef4444; background: #fee2e2; border: 1px solid #fecaca; border-radius: 10px; padding: 8px 10px; }

/* Detalle de evento */
.detail-modal { position: fixed; inset: 0; background: rgba(2,6,23,.35); display: flex; align-items: center; justify-content: center; z-index: 50; }
.detail-card { width: 560px; max-width: calc(100% - 32px); background: #ffffff; border: 1px solid rgba(226,232,240,.9); border-radius: 14px; box-shadow: 0 10px 24px rgba(0,0,0,.12); padding: 14px; }
.detail-header { display: flex; align-items: center; justify-content: space-between; }
.detail-title { display: flex; align-items: center; gap: 10px; font-weight: 800; color: #111827; }
.detail-body { margin-top: 10px; color: #334155; }
.detail-row { margin-bottom: 8px; }
.detail-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 12px; }
.detail-actions .danger { padding: 8px 12px; border-radius: 10px; background: #ef4444; color: #fff; font-weight: 700; border: 1px solid #ef4444; cursor: pointer; }
.detail-actions .danger:hover { filter: brightness(0.95); }

@media (max-width: 1024px) {
  .calendar-wrap { grid-template-columns: 1fr; }
}
</style>
