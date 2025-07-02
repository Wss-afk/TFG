import axios from 'axios'

const API_URL = '/api/user/'

export function login(username, password) {
  return axios.post(API_URL + 'login', { username, password })
}

export function register(username, password) {
  return axios.post(API_URL + 'register', { username, password })
}

export function logout() {
  // No logout endpoint in backend; implement client-side logout if needed
  return Promise.resolve()
}