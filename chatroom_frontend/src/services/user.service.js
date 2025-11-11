import axios from 'axios'

const API_URL = '/api/user/'

export function fetchUsers() {
  return axios.get(API_URL + 'list')
}

export function fetchGroups(userId) {
  return axios.get(API_URL + 'groups', { params: { userId } })
}

export function fetchProfile() {
  return axios.get(API_URL + 'profile')
}

export function updateProfile(data) {
  return axios.post(API_URL + 'profile', data)
}