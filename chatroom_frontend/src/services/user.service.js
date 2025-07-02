import axios from 'axios'

const API_URL = '/api/user/'

export function fetchUsers() {
  return axios.get(API_URL + 'list')
}

export function fetchGroups() {
  return axios.get(API_URL + 'groups')
}

export function fetchProfile() {
  return axios.get(API_URL + 'profile')
}

export function updateProfile(data) {
  return axios.post(API_URL + 'profile', data)
}