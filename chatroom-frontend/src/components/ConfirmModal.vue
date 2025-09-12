<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('cancel')">
    <div class="modal-content">
      <h4>{{ title }}</h4>
      <p>{{ message }}</p>
      <div class="modal-actions">
        <button class="btn-cancel" @click="$emit('cancel')">取消</button>
        <button class="btn-confirm" @click="$emit('confirm')">确认</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean;
  title: string;
  message: string;
}>();

defineEmits(['confirm', 'cancel']);
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}
.modal-content {
  background: #fff;
  padding: 20px 30px;
  border-radius: 8px;
  text-align: center;
  width: 320px;
}
.modal-content h4 {
  margin-top: 0;
  font-size: 18px;
}
.modal-content p {
  color: #666;
  margin-bottom: 25px;
}
.modal-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
}
.modal-actions button {
  padding: 8px 24px;
  border-radius: 4px;
  border: 1px solid #ccc;
  cursor: pointer;
}
.btn-confirm {
  background-color: #d9534f;
  color: white;
  border-color: #d43f3a;
}
</style>
