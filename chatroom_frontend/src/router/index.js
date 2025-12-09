import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import store from '../store'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat',
    name: 'ChatRoom',
    component: () => import('../views/Chat.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/events',
    name: 'Events',
    component: () => import('../views/Events.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAuth: true, requiresSuperAdmin: true },
    redirect: '/admin/users',
    children: [
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('../views/admin/AdminUsers.vue'),
        meta: { requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'groups',
        name: 'AdminGroups',
        component: () => import('../views/admin/AdminGroups.vue'),
        meta: { requiresAuth: true, requiresSuperAdmin: true }
      },
      {
        path: 'audit',
        name: 'AdminAudit',
        component: () => import('../views/admin/AdminAudit.vue'),
        meta: { requiresAuth: true, requiresSuperAdmin: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

NProgress.configure({ showSpinner: false })

router.beforeEach((to, from, next) => {
  NProgress.start()
  const currentUser = store.getters['auth/currentUser']
  const hasUser = !!currentUser
  const hasToken = store.getters['auth/isAuthenticated']
  const isAuthenticated = hasUser || hasToken

  // Si ya está autenticado, evitar mostrar login
  if (to.path === '/login' && isAuthenticated) {
    // Redirigir según rol
    if (currentUser && currentUser.role === 'SUPER_ADMIN') {
      return next('/admin')
    }
    return next('/home')
  }

  // Proteger rutas que requieren autenticación
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!isAuthenticated) {
      return next('/login')
    }
  }

  // Proteger rutas de super admin
  if (to.matched.some(record => record.meta.requiresSuperAdmin)) {
    if (!isAuthenticated || !currentUser || currentUser.role !== 'SUPER_ADMIN') {
      return next('/chat')
    }
  }

  // Forzar que un SUPER_ADMIN solo navegue por /admin
  if (isAuthenticated && currentUser && currentUser.role === 'SUPER_ADMIN') {
    const isAdminRoute = to.path.startsWith('/admin')
    if (!isAdminRoute) {
      return next('/admin')
    }
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router