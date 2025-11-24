<template>
  <transition name="view-fade" mode="out-in">
    <router-view/>
  </transition>
</template>

<script>

export default {
  name: 'App'
}
</script>

<style>
#app, html, body {
  height: 100%;
  margin: 0;
}
#app { min-height: 100vh; }
#app {
  font-family: Inter, Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}

/* Transiciones entre vistas */
.view-fade-enter-active, .view-fade-leave-active { transition: opacity .2s ease, transform .2s ease; }
.view-fade-enter-from, .view-fade-leave-to { opacity: 0; transform: translateY(6px); }
</style>
