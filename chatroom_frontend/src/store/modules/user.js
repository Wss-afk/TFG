const state = {
  users: [],
  groups: [],
  profile: {}
}

const mutations = {
  SET_USERS(state, users) {
    state.users = users
  },
  SET_GROUPS(state, groups) {
    state.groups = groups
  },
  SET_PROFILE(state, profile) {
    state.profile = profile
  }
}

const actions = {
  setUsers({ commit }, users) {
    commit('SET_USERS', users)
  },
  setGroups({ commit }, groups) {
    commit('SET_GROUPS', groups)
  },
  setProfile({ commit }, profile) {
    commit('SET_PROFILE', profile)
  }
}

const getters = {
  users: state => state.users,
  groups: state => state.groups,
  profile: state => state.profile
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}