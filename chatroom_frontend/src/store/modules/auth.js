const state = {
  // Persistencia básica: rehidratamos desde localStorage en initialize()
  user: null,
  token: null
}

const mutations = {
  SET_USER(state, user) {
    state.user = user
    try {
      if (user) {
        localStorage.setItem('auth_user', JSON.stringify(user))
      } else {
        localStorage.removeItem('auth_user')
      }
    } catch (e) {
      // Ignorar errores de almacenamiento
    }
  },
  SET_TOKEN(state, token) {
    state.token = token
    try {
      if (token) {
        localStorage.setItem('auth_token', token)
      } else {
        localStorage.removeItem('auth_token')
      }
    } catch (e) {
      // Ignorar errores de almacenamiento
    }
  },
  LOGOUT(state) {
    state.user = null
    state.token = null
    try {
      localStorage.removeItem('auth_user')
      localStorage.removeItem('auth_token')
    } catch (e) {
      // Ignorar errores de almacenamiento
    }
  }
}

const actions = {
  login({ commit }, { user, token }) {
    commit('SET_USER', user)
    commit('SET_TOKEN', token)
  },
  initialize({ commit }) {
    try {
      const userStr = localStorage.getItem('auth_user')
      const tokenStr = localStorage.getItem('auth_token')
      const user = userStr ? JSON.parse(userStr) : null
      const token = tokenStr || null
      if (user) commit('SET_USER', user)
      if (token) commit('SET_TOKEN', token)
    } catch (e) {
      // Si localStorage falla, no bloqueamos la app
    }
  },
  logout({ commit }) {
    commit('LOGOUT')
  }
}

const getters = {
  // Consideramos autenticado si hay usuario o token en memoria
  isAuthenticated: state => !!state.user || !!state.token,
  currentUser: state => state.user
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}