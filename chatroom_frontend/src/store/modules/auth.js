const userFromStorage = JSON.parse(localStorage.getItem('user'))
const tokenFromStorage = localStorage.getItem('token')
const state = {
  user: userFromStorage,
  token: tokenFromStorage
}

const mutations = {
  SET_USER(state, user) {
    state.user = user
    localStorage.setItem('user', JSON.stringify(user))
  },
  SET_TOKEN(state, token) {
    state.token = token
    localStorage.setItem('token', token)
  },
  LOGOUT(state) {
    state.user = null
    state.token = null
    localStorage.removeItem('user')
    localStorage.removeItem('token')
  }
}

const actions = {
  login({ commit }, { user, token }) {
    commit('SET_USER', user)
    commit('SET_TOKEN', token)
  },
  logout({ commit }) {
    commit('LOGOUT')
  }
}

const getters = {
  isAuthenticated: state => !!state.token,
  currentUser: state => state.user
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}