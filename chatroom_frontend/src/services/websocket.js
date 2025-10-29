import SockJS from 'sockjs-client'
import Stomp from 'webstomp-client'

let stompClient = null
let onlineUsersCallback = null

export function connectWebSocket(url, username, onMessage, onConnect, onError) {
  const socket = new SockJS(url)
  stompClient = Stomp.over(socket)
  
  // Añadir identidad de usuario a la cabecera de conexión
  const headers = {
    username: username
  }
  
  stompClient.connect(headers, frame => {
    console.log('WebSocket conectado:', frame)
    
    // Suscribirse a actualizaciones de usuarios en línea
    subscribeToOnlineUsers()
    
    if (onConnect) onConnect(frame)
  }, error => {
    console.error('Error de conexión WebSocket:', error)
    if (onError) onError(error)
  })
}

export function subscribe(topic, callback) {
  if (stompClient && stompClient.connected) {
    return stompClient.subscribe(topic, message => {
      callback(JSON.parse(message.body))
    })
  }
}

export function sendMessage(destination, body) {
  if (stompClient && stompClient.connected) {
    stompClient.send(destination, JSON.stringify(body))
  }
}

export function disconnectWebSocket() {
  if (stompClient) {
    try {
      // Solicitar desconexión STOMP y esperar ACK
      stompClient.disconnect(() => {
        console.log('WebSocket desconectado (ACK recibido)')
      })
    } catch (e) {
      console.error('Error al desconectar STOMP:', e)
    }
    stompClient = null
    onlineUsersCallback = null
  }
}

// Suscribirse a actualizaciones de usuarios en línea
function subscribeToOnlineUsers() {
  if (stompClient && stompClient.connected) {
    stompClient.subscribe('/topic/online-users', message => {
      const onlineUsers = JSON.parse(message.body)
      console.log('Lista de usuarios en línea actualizada:', onlineUsers)
      if (onlineUsersCallback) {
        onlineUsersCallback(onlineUsers)
      }
    })
  }
}

// Configurar callback de actualización de usuarios en línea
export function setOnlineUsersCallback(callback) {
  onlineUsersCallback = callback
}

// Obtener estado de conexión actual
export function isConnected() {
  return stompClient && stompClient.connected
}