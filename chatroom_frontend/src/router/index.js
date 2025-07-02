import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/chat',
    name: 'ChatRoom',
    component: () => import('../views/Chat.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router