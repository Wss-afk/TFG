import SockJS from 'sockjs-client'
import Stomp from 'webstomp-client'

let stompClient = null
let onlineUsersCallback = null

export function connectWebSocket(url, username, onMessage, onConnect, onError) {
  const socket = new SockJS(url)
  stompClient = Stomp.over(socket)
  
  // 添加用户身份信息到连接头部
  const headers = {
    username: username
  }
  
  stompClient.connect(headers, frame => {
    console.log('WebSocket连接成功:', frame)
    
    // 订阅在线用户列表更新
    subscribeToOnlineUsers()
    
    if (onConnect) onConnect(frame)
  }, error => {
    console.error('WebSocket连接失败:', error)
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
    onlineUsersCallback = null
  }
}

// 订阅在线用户列表更新
function subscribeToOnlineUsers() {
  if (stompClient && stompClient.connected) {
    stompClient.subscribe('/topic/online-users', message => {
      const onlineUsers = JSON.parse(message.body)
      console.log('收到在线用户列表更新:', onlineUsers)
      if (onlineUsersCallback) {
        onlineUsersCallback(onlineUsers)
      }
    })
  }
}

// 设置在线用户列表更新回调
export function setOnlineUsersCallback(callback) {
  onlineUsersCallback = callback
}

// 获取当前连接状态
export function isConnected() {
  return stompClient && stompClient.connected
}