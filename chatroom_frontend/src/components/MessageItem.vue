<template>
  <div class="message-item">
    <span class="sender">{{ message.sender }}:</span>
    <span class="content">{{ message.content }}</span>
  </div>
</template>

<script>
export default {
  name: 'MessageItem',
  props: {
    message: {
      type: Object,
      required: true
    }
  }
}
</script>

<style scoped>
.message-item {
  margin-bottom: 8px;
  padding: 6px 10px;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}
.sender {
  font-weight: bold;
  margin-right: 6px;
}
.content {
  word-break: break-all;
}
</style>