import { createRouter, createWebHistory } from 'vue-router'
import store from '../store'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/chat',
    name: 'ChatRoom',
    component: () => import('../views/Chat.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAuth: true, requiresSuperAdmin: true }
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

router.beforeEach((to, from, next) => {
  const currentUser = store.getters['auth/currentUser']
  const isLoggedIn = !!currentUser

  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!isLoggedIn) {
      return next('/login')
    }
  }

  if (to.matched.some(record => record.meta.requiresSuperAdmin)) {
    if (!isLoggedIn || currentUser.role !== 'SUPER_ADMIN') {
      return next('/chat')
    }
  }

  next()
})

export default router