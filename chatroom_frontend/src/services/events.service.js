import axios from 'axios'

const EVENTS_BASE = '/api/events'

/**
 * Intenta obtener los eventos del backend. Si el endpoint no existe
 * o falla la petición, devuelve eventos de ejemplo para el mes indicado.
 */
export async function fetchMonthEvents(year, month) {
  try {
    const res = await axios.get(EVENTS_BASE + '/list', { params: { year, month } })
    if (Array.isArray(res.data)) return res.data
  } catch (e) {
    // Fallback a datos locales
  }

  const y = Number(year)
  const m = Number(month) - 1

  const toISO = (yy, mm, dd) => `${yy}-${String(mm+1).padStart(2,'0')}-${String(dd).padStart(2,'0')}`

  return [
    { id: 1, date: toISO(y, m, 6), title: 'Movie Night', time: '19:00 - 21:00', color: '#f472b6' },
    { id: 2, date: toISO(y, m, 10), title: 'Franklin, 2+', time: '08:00 - 10:00', color: '#34d399' },
    { id: 3, date: toISO(y, m, 24), title: 'Navidad', time: 'Todo el día', color: '#60a5fa' },
    { id: 4, date: toISO(y, m, 6), title: 'Team Sync', time: '10:00 - 11:00', color: '#fb7185' },
    { id: 5, date: toISO(y, m, 15), title: 'Yard Sale', time: '16:00 - 17:00', color: '#f59e0b' }
  ]
}

export async function createEvent(payload) {
  const res = await axios.post(EVENTS_BASE, payload)
  return res.data
}