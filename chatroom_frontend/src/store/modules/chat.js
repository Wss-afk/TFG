const state = {
  messages: [],
  currentConversation: null
}

const mutations = {
  SET_MESSAGES(state, messages) {
    state.messages = messages
  },
  ADD_MESSAGE(state, message) {
    state.messages.push(message)
  },
  SET_CONVERSATION(state, conversation) {
    state.currentConversation = conversation
  }
}

const actions = {
  setMessages({ commit }, messages) {
    commit('SET_MESSAGES', messages)
  },
  addMessage({ commit }, message) {
    commit('ADD_MESSAGE', message)
  },
  setConversation({ commit }, conversation) {
    commit('SET_CONVERSATION', conversation)
  }
}

const getters = {
  messages: state => state.messages,
  currentConversation: state => state.currentConversation
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}