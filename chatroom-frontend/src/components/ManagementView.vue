<template>
  <div v-if="visible" class="modal-overlay" @click.self="closeModal">
    <div class="modal-container">
      <div class="modal-header">
        <h2 class="modal-title">{{ title }}</h2>
        <button class="close-btn" @click="closeModal">&times;</button>
      </div>
      <div class="modal-content">
        <!-- Admin Management -->
        <div v-if="mode === 'admin'">
          <AdminManagement :conversation="conversation" />
        </div>

        <!-- Join Request Management -->
        <div v-if="mode === 'join-requests'">
          <JoinRequestManagement :conversation="conversation" />
        </div>

        <!-- Blacklist Management -->
        <div v-if="mode === 'blacklist'">
          <BlacklistManagement :conversation="conversation" />
        </div>

        <!-- Mute Management -->
        <div v-if="mode === 'mute'">
          <MuteManagement :conversation="conversation" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue';
import type { ConversationSummary } from '@/types/api';

const props = defineProps<{
  visible: boolean;
  mode: 'admin' | 'join-requests' | 'blacklist' | 'mute' | null;
  conversation: ConversationSummary;
}>();

const emit = defineEmits(['close']);

// Asynchronously load sub-components to improve initial load time
const AdminManagement = defineAsyncComponent(() => import('./management/AdminManagement.vue'));
const JoinRequestManagement = defineAsyncComponent(() => import('./management/JoinRequestManagement.vue'));
const BlacklistManagement = defineAsyncComponent(() => import('./management/BlacklistManagement.vue'));
const MuteManagement = defineAsyncComponent(() => import('./management/MuteManagement.vue'));

const title = computed(() => {
  switch (props.mode) {
    case 'admin': return '设置群管理员';
    case 'join-requests': return '加群管理';
    case 'blacklist': return '群黑名单管理';
    case 'mute': return '群内禁言管理';
    default: return '群管理';
  }
});

const closeModal = () => {
  emit('close');
};
</script>

<style scoped lang="scss">
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.modal-container {
  width: 800px;
  max-width: 90vw;
  height: 600px;
  max-height: 85vh;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  padding: 20px 25px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  &:hover {
    color: #333;
  }
}

.modal-content {
  flex-grow: 1;
  overflow-y: auto;
  padding: 20px 25px;
}
</style>
