import axios from 'axios'

const API_URL = '/api/user/'

export function fetchUsers() {
  return axios.get(API_URL + 'list')
}

export function fetchGroups(userId) {
  return axios.get(API_URL + 'groups', { params: { userId } })
}

export function updateAvatar(userId, avatarUrl) {
  return axios.post(API_URL + 'avatar', null, { params: { userId, avatarUrl } })
}
