import axios from 'axios'

const API_URL = '/api/chat/'

export function fetchMessages({ groupId = null, receiverId = null, userId = null }) {
  return axios.get(API_URL + 'messages', { params: { groupId, receiverId, userId } })
}


export function markMessagesAsRead(userId, senderId) {
  return axios.post(API_URL + 'mark-read', null, {
    params: { userId, senderId }
  })
}

export function getUnreadCount(userId, senderId) {
  return axios.get(API_URL + 'unread-count', {
    params: { userId, senderId }
  })
}

export function getGroupUnreadCounts(userId) {
  return axios.get(API_URL + 'group/unread-counts', {
    params: { userId }
  })
}

export function markGroupMessagesAsRead(userId, groupId) {
  return axios.post(API_URL + 'group/mark-read', null, {
    params: { userId, groupId }
  })
}

export function fetchNotifications(userId) {
  return axios.get(API_URL + 'notifications', { params: { userId } })
}

export function uploadAttachment(file) {
  const formData = new FormData()
  formData.append('file', file)
  return axios.post(API_URL + 'upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return axios.post(API_URL + 'upload?kind=avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
