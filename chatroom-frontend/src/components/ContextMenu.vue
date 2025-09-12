<template>
  <div
    v-if="visible"
    class="context-menu"
    :style="{ top: `${y}px`, left: `${x}px` }"
    @click.stop
  >
    <ul>
      <li
        v-for="option in options"
        :key="option.label"
        @click="onSelect(option.action)"
        :class="{ danger: option.danger }"
      >
        {{ option.label }}
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';

interface MenuOption {
  label: string;
  action: string;
  danger?: boolean;
}

const props = defineProps<{
  visible: boolean;
  x: number;
  y: number;
  options: MenuOption[];
}>();

const emit = defineEmits(['close', 'select']);

const onSelect = (action: string) => {
  emit('select', action);
  emit('close');
};

const handleClickOutside = () => {
  if (props.visible) {
    emit('close');
  }
};

onMounted(() => {
  window.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  window.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
.context-menu {
  position: fixed;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  min-width: 160px;
  z-index: 1000;
  padding: 5px 0;
}
.context-menu ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.context-menu li {
  padding: 8px 15px;
  font-size: 14px;
  cursor: pointer;
}
.context-menu li:hover {
  background-color: #f5f5f5;
}
.context-menu li.danger {
  color: #f44336;
}
.context-menu li.danger:hover {
  background-color: #fdecea;
}
</style>
