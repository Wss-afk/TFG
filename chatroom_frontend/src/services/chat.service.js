import axios from 'axios'

const API_URL = '/api/chat/'

export function fetchMessages({ groupId = null, receiverId = null, userId = null }) {
  return axios.get(API_URL + 'messages', { params: { groupId, receiverId, userId } })
}

export function sendMessage({ groupId = null, receiverId = null, content, senderId = null }) {
  if (groupId) {
    // Envío de mensaje a grupo
    return axios.post(
      API_URL + 'send?senderId=' + senderId + '&groupId=' + groupId,
      {
        content,
        type: 'text',
        timestamp: new Date().toISOString()
      }
    )
  } else {
    // Envío de mensaje individual
    return axios.post(
      API_URL + 'send?senderId=' + senderId + '&receiverId=' + receiverId,
      {
        content,
        type: 'text',
        timestamp: new Date().toISOString()
      }
    )
  }
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