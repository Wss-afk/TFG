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
    // Sin fallback a datos locales: la vista mostrará skeleton/vacío
    return []
  }
  return []
}

export async function createEvent(payload) {
  const res = await axios.post(EVENTS_BASE, payload)
  return res.data
}

export async function deleteEvent(id) {
  const res = await axios.delete(`${EVENTS_BASE}/${id}`)
  return res.data
}