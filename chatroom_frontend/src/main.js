import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import 'bootstrap/dist/css/bootstrap.min.css'
import './assets/theme.css'

async function bootstrap() {
  const app = createApp(App)
  app.use(store)
  // Rehidratar sesión antes de registrar el router para que los guards
  // detecten correctamente que el usuario ya está autenticado tras F5
  try { await store.dispatch('auth/initialize') } catch (e) {
    // Ignorar fallos de almacenamiento/permisos de navegador
    console.warn('Auth initialize failed:', e)
  }
  app.use(router)
  app.mount('#app')
}

bootstrap()
