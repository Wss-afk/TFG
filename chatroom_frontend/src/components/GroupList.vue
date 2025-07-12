<template>
  <div class="group-list">
    <ul>
      <li v-for="group in groups" :key="group.id" @click="$emit('select', group)">
        {{ group.name }}
      </li>
    </ul>
  </div>
</template>

<script>
export default {
  name: 'GroupList',
  props: {
    groups: {
      type: Array,
      required: true
    }
  }
}
</script>

<style scoped>
.group-list {
  padding: 8px 0;
}
ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
li {
  padding: 6px 12px;
  cursor: pointer;
  border-radius: 4px;
}
li:hover {
  background: #f6ffed;
}
</style>