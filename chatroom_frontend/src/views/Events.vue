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
        <div class="calendar-column">
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
                  <div class="event-indicators">
                    <div
                      v-for="(ev, i) in eventsForDay(day).slice(0, 3)"
                      :key="i"
                      class="event-indicator"
                      :style="{ backgroundColor: ev.color }"
                      :title="ev.title + ' (' + ev.time + ')'"
                    ></div>
                    <div v-if="eventsForDay(day).length > 3" class="event-indicator-more">
                      +{{ eventsForDay(day).length - 3 }}
                    </div>
                  </div>
                </div>
              </div>
            </transition>
          </div>

          <div class="month-agenda-section">
            <h2 class="section-title">Agenda de {{ monthLabel }}</h2>
            <div class="agenda-list">
              <div v-if="monthEvents.length === 0" class="agenda-empty">
                No hay eventos programados para este mes.
              </div>
              <div v-else v-for="ev in monthEvents" :key="'m-'+ev.id" :class="['agenda-item', isPastEvent(ev) && 'is-past']" @click="selectDayAndEvent(ev)">
                <div class="agenda-date">
                  <span class="agenda-day">{{ new Date(ev.date).getDate() }}</span>
                  <span class="agenda-weekday">{{ getWeekdayShort(ev.date) }}</span>
                </div>
                <div class="agenda-content">
                  <div class="agenda-marker" :style="{ backgroundColor: ev.color }"></div>
                  <div class="agenda-details">
                    <div class="agenda-title">{{ ev.title }}</div>
                    <div class="agenda-time">{{ ev.time || 'Todo el día' }}</div>
                  </div>
                </div>
                <div class="agenda-meta">
                   <div class="agenda-user" v-if="ev.createdById">
                     <Icon name="user" :size="12" /> {{ usernameById(ev.createdById) }}
                   </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <aside class="sidebar">
          <div class="panel">
            <div class="panel-header-row">
              <h2 class="panel-title">{{ selectedDay ? `Eventos: ${selectedDayLabel}` : 'Selecciona un día' }}</h2>
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
                <li v-for="ev in filteredEvents" :key="ev.id" :class="['event-item', isPastEvent(ev) && 'is-past']" @click="openEventDetails(ev)" style="cursor: pointer">
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
                <li v-if="filteredEvents.length === 0" class="event-empty-state">
                  <div class="empty-illustration">
                    <Icon name="calendar" :size="48" />
                  </div>
                  <h3 class="empty-title">{{ selectedDay ? 'Sin planes para hoy' : 'Sin eventos este mes' }}</h3>
                  <p class="empty-desc" v-if="selectedDay">Parece un día tranquilo. ¿Por qué no añades un nuevo evento?</p>
                  <p class="empty-desc" v-else>No hay eventos programados para este mes.</p>
                  <div v-if="selectedDay" style="margin-top: 20px">
                    <button class="primary-sm" @click="openCreateForm">
                      <Icon name="plus" :size="16" /> Crear Evento
                    </button>
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
              <button class="primary full-width" @click="openCreateForm">
                <Icon name="plus" :size="18" style="margin-right: 6px"/> Nuevo evento
              </button>
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
      const s = this.cursor.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' })
      return s.charAt(0).toUpperCase() + s.slice(1)
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
      return [] // Always empty if no day selected, though selectedDay is init to today
    }
  },
  methods: {
    getWeekdayShort(dateStr) {
        if (!dateStr) return '';
        const d = new Date(dateStr);
        return d.toLocaleDateString('es-ES', { weekday: 'short' }).toUpperCase();
    },
    selectDayAndEvent(ev) {
        if (ev.date) {
            const d = new Date(ev.date);
            // Ensure we switch month if needed (though monthEvents only shows current month)
            this.selectedDay = d.getDate();
        }
        this.openEventDetails(ev);
    },
    isPastEvent(ev) {
      if (!ev.date) return false
      const now = new Date()
      const today = this.toISO(now.getFullYear(), now.getMonth(), now.getDate())
      return ev.date < today
    },
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
    },
    handleKeydown(e) {
      if (e.key === 'Escape') {
        if (this.detailOpen) this.closeEventDetails()
        if (this.createOpen) this.cancelCreate()
      }
    }
  },
  async mounted() {
    window.addEventListener('keydown', this.handleKeydown)
    await this.loadMonth()
    await this.loadUsers()
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleKeydown)
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

<style scoped src="./Events.css"></style>