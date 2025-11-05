import axios from 'axios'

const API_URL = '/api/admin/'

function adminHeaders(adminUserId) {
  return {
    headers: {
      'X-Admin-UserId': String(adminUserId)
    }
  }
}

// Users
export function adminGetUsers(adminUserId) {
  return axios.get(API_URL + 'users', adminHeaders(adminUserId))
}

export function adminCreateUser(adminUserId, user) {
  return axios.post(API_URL + 'users', user, adminHeaders(adminUserId))
}

export function adminUpdateUser(adminUserId, id, data) {
  return axios.put(API_URL + `users/${id}`, data, adminHeaders(adminUserId))
}

export function adminDeleteUser(adminUserId, id) {
  return axios.delete(API_URL + `users/${id}`, adminHeaders(adminUserId))
}

export function adminChangePassword(adminUserId, id, newPassword) {
  return axios.patch(API_URL + `users/${id}/password`, { newPassword }, adminHeaders(adminUserId))
}

// Groups
export function adminGetGroups(adminUserId) {
  return axios.get(API_URL + 'groups', adminHeaders(adminUserId))
}

export function adminCreateGroup(adminUserId, group) {
  return axios.post(API_URL + 'groups', group, adminHeaders(adminUserId))
}

export function adminUpdateGroup(adminUserId, id, data) {
  return axios.put(API_URL + `groups/${id}`, data, adminHeaders(adminUserId))
}

export function adminDeleteGroup(adminUserId, id) {
  return axios.delete(API_URL + `groups/${id}`, adminHeaders(adminUserId))
}

export function adminUpdateGroupMembers(adminUserId, id, userIds) {
  return axios.put(API_URL + `groups/${id}/members`, { userIds }, adminHeaders(adminUserId))
}

// Audit
export function adminGetAuditLogs(adminUserId, {
  from = null,
  to = null,
  actorId = null,
  action = null,
  targetType = null,
  targetId = null,
  success = null,
  page = 0,
  size = 10
} = {}) {
  const config = adminHeaders(adminUserId)
  config.params = {
    from,
    to,
    actorId,
    action,
    targetType,
    targetId,
    success,
    page,
    size
  }
  return axios.get(API_URL + 'audit', config)
}