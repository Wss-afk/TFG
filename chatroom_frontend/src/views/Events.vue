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
          <transition name="fade-slide" mode="out-in">
            <div class="days gap8-1" :key="monthLabel">
              <div v-for="n in startOffset" :key="'empty-'+n" class="day empty"></div>
              <div
                v-for="day in daysInMonth"
                :key="day"
                :class="['day', isToday(day) && 'today', isSelected(day) && 'selected']"
                @click="selectDay(day)"
                @dblclick="openCreateForm"
              >
                <div class="day-header">
                  <span class="num">{{ day }}</span>
                </div>
                <div class="dots gap8-1">
                  <span
                    v-for="(ev, i) in eventsForDay(day).slice(0,3)"
                    :key="i"
                    class="dot"
                    :style="{ background: ev.color }"
                    :title="ev.title + ' (' + ev.time + ')'"
                  ></span>
                  <span v-if="eventsForDay(day).length > 3" class="more" :title="eventsForDay(day).length - 3 + ' eventos más'">+{{ eventsForDay(day).length - 3 }}</span>
                </div>
              </div>
            </div>
          </transition>
        </div>

        <aside class="sidebar">
          <div class="panel">
            <div class="panel-header-row">
              <h2 class="panel-title">{{ selectedDay ? `Eventos: ${selectedDayLabel}` : 'Eventos del mes' }}</h2>
              <button v-if="selectedDay" class="text-btn" @click="clearSelection">Ver Mes</button>
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
                <transition-group name="list" tag="div" style="display:contents">
                <li v-for="ev in filteredEvents" :key="ev.id" class="event-item gap8-2" @click="openEventDetails(ev)" style="cursor: pointer">
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
                </transition-group>
                <li v-if="filteredEvents.length === 0" class="event-empty">
                  {{ selectedDay ? 'No hay eventos para este día' : 'Sin eventos este mes' }}
                  <div v-if="selectedDay" style="margin-top: 12px">
                    <button class="text-btn-primary" @click="openCreateForm">Crear Evento</button>
                  </div>
                </li>
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
                  <div class="detail-row">
            <strong>Creador:</strong> 
            {{ detailEvent.createdBy ? detailEvent.createdBy.username : (detailEvent.createdById ? usernameById(detailEvent.createdById) : '—') }}
          </div>
          <div class="detail-row">
            <strong>Responsables:</strong>
            <template v-if="detailEvent.responsibles && detailEvent.responsibles.length">
              {{ detailEvent.responsibles.map(u => u.username).join(', ') }}
            </template>
            <template v-else-if="detailEvent.responsibleIds && detailEvent.responsibleIds.length">
              {{ detailEvent.responsibleIds.map(id => usernameById(id)).join(', ') }}
            </template>
            <span v-else>—</span>
          </div>
                </div>
                <div class="detail-actions">
                  <button class="danger" :disabled="deleting" @click="removeEvent">{{ deleting ? 'Eliminando…' : 'Eliminar' }}</button>
                  <button class="cancel" :disabled="deleting" @click="closeEventDetails">Cerrar</button>
                </div>
                <div v-if="deleteError" class="form-error">{{ deleteError }}</div>
              </div>
            </div>

            <!-- Create Modal -->
            <transition name="modal">
            <div v-if="createOpen" class="detail-modal" style="z-index: 60;">
              <div class="detail-card create-card">
                <div class="detail-header">
                  <div class="detail-title"><Icon name="plus" :size="20" /> Nuevo evento</div>
                  <button type="button" class="close" title="Cerrar" @click="cancelCreate">×</button>
                </div>
                <div class="detail-body">
                  <div class="form-info-bar">
                    <div class="info-item">
                        <Icon name="calendar" :size="14" />
                        <span>{{ toISO(year, month, selectedDay || 1) }}</span>
                    </div>
                    <div class="info-item">
                        <Icon name="user" :size="14" />
                        <span>{{ currentUser?.username || '—' }}</span>
                    </div>
                  </div>

                  <div class="form-section">
                    <div class="form-row">
                        <label>Título</label>
                        <div class="input-wrapper">
                            <input type="text" v-model="newEvent.title" placeholder="Ej. Reunión de equipo" autofocus />
                        </div>
                    </div>
                    
                    <div class="form-grid-2">
                         <div class="form-row">
                            <label>Horario</label>
                            <div class="time-range">
                                <select v-model="newEvent.startHour">
                                <option :value="null">Inicio</option>
                                <option v-for="h in hours24" :key="'sh-'+h" :value="h">{{ pad2(h) }}:00</option>
                                </select>
                                <span class="arrow">→</span>
                                <select v-model="newEvent.endHour">
                                <option :value="null">Fin</option>
                                <option v-for="h in hours24" :key="'eh-'+h" :value="h">{{ pad2(h) }}:00</option>
                                </select>
                            </div>
                        </div>
                        <div class="form-row">
                            <label>Color</label>
                            <div class="color-input-wrapper">
                                <input v-model="newEvent.color" type="color" title="Elige un color" />
                            </div>
                        </div>
                    </div>

                    <div class="form-row">
                        <label>Descripción</label>
                        <textarea v-model="newEvent.description" placeholder="Añade detalles…"></textarea>
                    </div>

                    <div class="form-row">
                        <label>Participantes</label>
                        <div class="users-select-container">
                            <div class="users-list">
                                <label v-for="u in users" :key="u.id" class="user-option" :class="{checked: newEvent.assignedToIds.includes(u.id)}">
                                    <input type="checkbox" :value="u.id" v-model="newEvent.assignedToIds" hidden />
                                    <span class="user-avatar">{{ u.username.charAt(0).toUpperCase() }}</span>
                                    <span class="user-name">{{ u.username }}</span>
                                    <Icon v-if="newEvent.assignedToIds.includes(u.id)" name="check" :size="14" class="check-icon" />
                                </label>
                            </div>
                        </div>
                    </div>
                  </div>
                </div>
                <div class="detail-actions">
                  <button class="cancel" @click="cancelCreate">Cancelar</button>
                  <button class="primary" @click="submitCreate">Crear Evento</button>
                </div>
                <div v-if="createError" class="form-error-banner">
                    <Icon name="alert-circle" :size="16" /> {{ createError }}
                </div>
              </div>
            </div>
            </transition>

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
    selectedDayLabel() {
      if (!this.selectedDay) return ''
      const d = new Date(this.year, this.month, this.selectedDay)
      return d.toLocaleDateString('es-ES', { weekday: 'short', day: 'numeric', month: 'short' })
    },
    hours24() {
      return Array.from({ length: 24 }, (_, i) => i)
    },
    monthEvents() {
      const ym = `${this.year}-${String(this.month+1).padStart(2,'0')}`
      return this.events
        .filter(e => (e.date || '').startsWith(ym))
        .sort((a,b) => (a.date||'').localeCompare(b.date||''))
    },
    filteredEvents() {
      if (this.selectedDay) {
        const key = this.toISO(this.year, this.month, this.selectedDay)
        return this.events.filter(e => e.date === key)
      }
      return this.monthEvents
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
    selectDay(day) { 
      // Si ya está seleccionado, solo mantener (para doble clic)
      // Si es nuevo, seleccionar
      this.selectedDay = day 
    },
    clearSelection() {
      this.selectedDay = null
    },
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
    year() { this.loadMonth() },
    selectedDay() {
      // Reset create form date if open
      if (this.createOpen) {
        this.createError = ''
      }
    }
  }
}
</script>

<style scoped>
.events-page.light { display: flex; min-height: 100vh; background: #f8fafc; color: #1e293b; }
.content { flex: 1; padding: 24px 32px; overflow-y: auto; }

/* Transitions */
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.3s ease; }
.fade-slide-enter-from { opacity: 0; transform: translateX(10px); }
.fade-slide-leave-to { opacity: 0; transform: translateX(-10px); }

.list-enter-active, .list-leave-active { transition: all 0.3s ease; }
.list-enter-from, .list-leave-to { opacity: 0; transform: translateX(20px); }

.topbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.title { font-size: 28px; font-weight: 800; color: #0f172a; letter-spacing: -0.5px; }
.controls { display: flex; align-items: center; gap: 12px; }
.nav-btn {
  width: 36px; height: 36px; border-radius: 12px; border: 1px solid #e2e8f0;
  background: #fff; color: #64748b; cursor: pointer; transition: all 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.nav-btn:hover { background: #f1f5f9; color: #0f172a; border-color: #cbd5e1; }
.primary {
  padding: 8px 20px; border-radius: 12px; border: none;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff; font-weight: 600; cursor: pointer;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
  transition: all 0.2s;
}
.primary:hover { transform: translateY(-1px); box-shadow: 0 6px 16px rgba(99, 102, 241, 0.35); }

.calendar-wrap { display: grid; grid-template-columns: 1fr 360px; gap: 24px; }
.calendar {
  background: #ffffff; border-radius: 24px; padding: 24px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.01);
  border: 1px solid #f1f5f9;
}
.weekdays { display: grid; grid-template-columns: repeat(7, 1fr); margin-bottom: 16px; }
.wd {
  padding: 10px; text-align: center; color: #94a3b8; font-weight: 700; font-size: 0.85rem; text-transform: uppercase; letter-spacing: 0.5px;
}

.days { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; }
.day {
  background: #fff; border: 1px solid #f1f5f9; border-radius: 16px;
  padding: 8px; min-height: 100px; position: relative;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer; display: flex; flex-direction: column;
}
.day:hover {
  border-color: #e2e8f0; transform: translateY(-4px);
  box-shadow: 0 12px 20px -10px rgba(0, 0, 0, 0.1);
  z-index: 2;
}
.day.today {
  background: #f0fdfa; border-color: #14b8a6;
}
.day.today .num {
  background: linear-gradient(135deg, #14b8a6 0%, #0d9488 100%);
  color: white; width: 28px; height: 28px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 6px rgba(20, 184, 166, 0.3);
}
.day.selected {
  border-color: #6366f1; background: #eef2ff;
}
.day.empty { visibility: hidden; pointer-events: none; }

.day-header { display: flex; justify-content: flex-end; margin-bottom: 6px; }
.num { font-weight: 700; color: #475569; font-size: 0.95rem; transition: color 0.2s; padding: 4px; }

.dots { display: flex; gap: 4px; flex-wrap: wrap; align-content: flex-start; }
.dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.more { font-size: 10px; color: #64748b; font-weight: 600; background: #f1f5f9; padding: 2px 4px; border-radius: 4px; }

.sidebar {
  background: #ffffff; border-radius: 24px; padding: 24px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);
  border: 1px solid #f1f5f9; display: flex; flex-direction: column;
}
.panel-header-row {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
}
.panel-title { font-size: 18px; font-weight: 800; color: #0f172a; margin: 0; }
.text-btn { background: none; border: none; color: #6366f1; font-weight: 600; cursor: pointer; font-size: 0.9rem; }
.text-btn:hover { text-decoration: underline; }
.text-btn-primary {
  background: none; border: 2px solid #6366f1; color: #6366f1;
  font-weight: 700; cursor: pointer; font-size: 0.9rem;
  padding: 6px 12px; border-radius: 8px; transition: all 0.2s;
}
.text-btn-primary:hover { background: #6366f1; color: #fff; }

.event-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 12px; overflow-y: auto; max-height: 600px; padding-right: 4px; }
.event-item {
  display: grid; grid-template-columns: 4px 1fr; gap: 12px;
  background: #fff; border: 1px solid #f1f5f9; border-radius: 12px;
  padding: 12px 16px; transition: all 0.2s; position: relative; overflow: hidden;
}
.event-item:hover {
  transform: translateY(-2px) scale(1.01);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05);
  border-color: #e2e8f0;
}
.event-left { display: none; } /* Hide old badge, use border instead */
.event-item::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 4px;
  background: var(--event-color, #6366f1);
}
/* Since I can't easily set var(--event-color) in CSS without style binding on the item, I will use the badge approach but styled differently or stick to inline style for the strip */
/* Revert grid to include badge but styled better */
.event-item {
  display: flex; align-items: flex-start; gap: 12px;
  background: #fff; border: 1px solid #f1f5f9; border-radius: 12px;
  padding: 14px; transition: all 0.2s;
}
.badge { width: 10px; height: 10px; border-radius: 50%; margin-top: 5px; flex-shrink: 0; }

.event-title { font-weight: 700; color: #1e293b; font-size: 0.95rem; margin-bottom: 2px; }
.event-time { font-size: 0.8rem; color: #64748b; font-weight: 500; }
.event-meta { font-size: 0.75rem; color: #94a3b8; margin-top: 4px; }
.event-desc { font-size: 0.8rem; color: #64748b; margin-top: 6px; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.event-empty { text-align: center; padding: 32px; color: #94a3b8; font-style: italic; background: #f8fafc; border-radius: 12px; border: 1px dashed #cbd5e1; }

/* Form Styles */
.create-card { max-width: 500px; }

.form-header { margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #f1f5f9; }
.form-row input, .form-row textarea, select {
  border: 1px solid #cbd5e1; background: #f8fafc;
  transition: all 0.2s;
  width: 100%; padding: 8px 12px; border-radius: 8px; font-size: 0.9rem;
}
.form-row input:focus, .form-row textarea:focus, select:focus {
  background: #fff; border-color: #6366f1; box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1); outline: none;
}

/* Detail Modal */
.detail-modal {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(15, 23, 42, 0.4); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center; z-index: 50;
  animation: fadeIn 0.2s ease-out;
}
.detail-card {
  background: #fff; width: 90%; max-width: 450px;
  border-radius: 20px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden; animation: scaleIn 0.2s ease-out;
}
.detail-header {
  padding: 20px 24px; border-bottom: 1px solid #f1f5f9;
  display: flex; justify-content: space-between; align-items: center;
  background: #f8fafc;
}
.detail-title { font-size: 1.2rem; font-weight: 800; color: #0f172a; display: flex; align-items: center; gap: 12px; }
.detail-body { padding: 24px; }
.detail-row { margin-bottom: 12px; font-size: 0.95rem; color: #334155; }
.detail-row strong { color: #0f172a; font-weight: 600; margin-right: 6px; }
.detail-actions {
  padding: 16px 24px; background: #f8fafc; border-top: 1px solid #f1f5f9;
  display: flex; justify-content: flex-end; gap: 12px;
}
.danger { background: #fee2e2; color: #ef4444; border: none; padding: 8px 16px; border-radius: 8px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
.danger:hover { background: #fecaca; }
.cancel { background: #fff; border: 1px solid #cbd5e1; padding: 8px 16px; border-radius: 8px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
.cancel:hover { background: #f1f5f9; }

/* Skeleton */
.skeleton-badge { background: #e2e8f0 !important; }
.skeleton-line { background: linear-gradient(90deg, #f1f5f9, #e2e8f0, #f1f5f9); background-size: 200% 100%; animation: shimmer 1.5s infinite; }

/* Scrollbars */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #94a3b8; }

@media (max-width: 1024px) {
  .calendar-wrap { grid-template-columns: 1fr; }
  .sidebar { order: 2; }
}

/* Modal Transition */
.modal-enter-active, .modal-leave-active { transition: opacity 0.3s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-active .detail-card, .modal-leave-active .detail-card { transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.modal-enter-from .detail-card, .modal-leave-to .detail-card { transform: scale(0.95) translateY(10px); }

/* New Form Styles */
.detail-modal {
  backdrop-filter: blur(4px);
  background: rgba(15, 23, 42, 0.6); /* Darker overlay for focus */
}

.create-card {
  max-width: 520px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255,255,255,0.5); /* Glass border effect */
}

.form-info-bar {
  display: flex; gap: 16px; background: #f1f5f9; padding: 10px 16px; border-radius: 8px; margin-bottom: 20px;
  border: 1px solid #e2e8f0;
}
.info-item { display: flex; align-items: center; gap: 6px; color: #64748b; font-size: 0.85rem; font-weight: 600; }

.form-section { display: flex; flex-direction: column; gap: 18px; }

.input-wrapper { position: relative; }
.form-row input[type="text"] {
  width: 100%; padding: 12px 16px; border: 1px solid #cbd5e1; border-radius: 10px;
  font-size: 1rem; transition: all 0.2s; background: #fcfcfc;
}
.form-row input[type="text"]:focus { 
  border-color: #6366f1; background: #fff;
  box-shadow: 0 0 0 4px rgba(99,102,241,0.1); outline: none; 
}

.form-grid-2 { display: grid; grid-template-columns: 1fr 140px; gap: 16px; }

.time-range { display: flex; align-items: center; gap: 8px; background: #fcfcfc; border: 1px solid #cbd5e1; padding: 4px 8px; border-radius: 10px; }
.time-range:focus-within { border-color: #6366f1; box-shadow: 0 0 0 4px rgba(99,102,241,0.1); }
.time-range select { 
  flex: 1; min-width: 0; padding: 8px 4px; border-radius: 6px; border: none; background: transparent; font-weight: 500; color: #334155;
  cursor: pointer;
}
.time-range select:focus { outline: none; }
.arrow { color: #94a3b8; font-weight: bold; font-size: 0.9rem; }

.color-input-wrapper { 
  display: flex; align-items: center; height: 42px; 
  border: 1px solid #cbd5e1; border-radius: 10px; padding: 4px; background: #fff;
  transition: all 0.2s;
}
.color-input-wrapper:hover { border-color: #94a3b8; }
.color-input-wrapper input[type="color"] { 
  -webkit-appearance: none; border: none; width: 100%; height: 100%; padding: 0; 
  border-radius: 6px; overflow: hidden; cursor: pointer; 
}
.color-input-wrapper input[type="color"]::-webkit-color-swatch-wrapper { padding: 0; }
.color-input-wrapper input[type="color"]::-webkit-color-swatch { border: none; }

.form-row textarea {
    width: 100%; padding: 12px 16px; border: 1px solid #cbd5e1; border-radius: 10px;
    font-size: 0.95rem; transition: all 0.2s; background: #fcfcfc; min-height: 80px; resize: vertical; font-family: inherit;
}
.form-row textarea:focus {
    border-color: #6366f1; background: #fff;
    box-shadow: 0 0 0 4px rgba(99,102,241,0.1); outline: none;
}

.users-select-container {
  border: 1px solid #e2e8f0; border-radius: 10px; padding: 6px; max-height: 150px; overflow-y: auto; background: #fff;
}
.users-list { display: flex; flex-direction: column; gap: 2px; }
.user-option {
  display: flex; align-items: center; gap: 12px; padding: 8px 12px; border-radius: 8px; cursor: pointer; transition: all 0.2s;
  user-select: none;
}
.user-option:hover { background: #f8fafc; }
.user-option.checked { background: #eff6ff; }
.user-avatar {
  width: 28px; height: 28px; background: #cbd5e1; color: #fff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
.user-option.checked .user-avatar { background: #6366f1; }
.user-name { font-size: 0.95rem; flex: 1; color: #334155; font-weight: 500; }
.check-icon { color: #6366f1; }

.form-error-banner {
  margin: 0 24px 16px; background: #fef2f2; color: #ef4444; padding: 12px 16px; border-radius: 8px;
  display: flex; align-items: center; gap: 10px; font-size: 0.9rem; border: 1px solid #fecaca;
}
</style>
