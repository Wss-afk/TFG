import axios from 'axios'

const API_URL = '/api/chat/'

export function fetchMessages({ groupId = null, receiverId = null }) {
  return axios.get(API_URL + 'messages', { params: { groupId, receiverId } })
}

export function sendMessage({ groupId = null, receiverId = null, content, senderId = null }) {
  return axios.post(API_URL + 'send', {
    groupId,
    receiverId,
    content,
    senderId,
    type: 'text',
    timestamp: new Date().toISOString()
  })
}