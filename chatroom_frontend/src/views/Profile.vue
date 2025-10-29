<template>
  <div class="profile-container">
    <h2>Perfil</h2>
    <form @submit.prevent="save">
      <input v-model="username" placeholder="Usuario" />
      <input v-model="email" placeholder="Correo electrónico" />
      <button type="submit">Guardar</button>
    </form>
  </div>
</template>

<script>
export default {
  name: 'Profile',
  data() {
    return {
      username: '',
      email: ''
    }
  },
  methods: {
    save() {
      // Lógica de guardado por implementar
      this.$message && this.$message.success && this.$message.success('¡Guardado con éxito!')
    }
  }
}
</script>

<style scoped>
.profile-container {
  max-width: 400px;
  margin: 60px auto;
  padding: 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  text-align: center;
}
input {
  display: block;
  width: 100%;
  margin: 12px 0;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #ccc;
}
button {
  width: 100%;
  padding: 8px;
  background: #67c23a;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>