import SockJS from 'sockjs-client'
import Stomp from 'webstomp-client'

let stompClient = null

export function connectWebSocket(url, onMessage, onConnect, onError) {
  const socket = new SockJS(url)
  stompClient = Stomp.over(socket)
  stompClient.connect({}, frame => {
    if (onConnect) onConnect(frame)
  }, error => {
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
    stompClient.disconnect()
    stompClient = null
  }
}