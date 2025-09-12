<template>
  <div class="management-panel">
    <div v-if="isLoading" class="loading-state">正在加载加群请求...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <div v-else-if="requests.length === 0" class="empty-state">暂无新的加群请求</div>
    <ul v-else class="request-list">
      <li v-for="request in requests" :key="request.requesterId" class="request-item">
        <div class="user-info">
          <img :src="request.avatarUrl" class="avatar" />
          <span class="nickname">{{ request.nickname }}</span>
        </div>
        <div class="actions">
          <button @click="handleRequest(request.requesterId, 'ACCEPT')" class="btn accept-btn">同意</button>
          <button @click="handleRequest(request.requesterId, 'REJECT')" class="btn reject-btn">拒绝</button>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import apiClient from '@/api/apiClient';
import type { ConversationSummary, GroupJoinRequestDto } from '@/types/api';

const props = defineProps<{
  conversation: ConversationSummary;
}>();

const requests = ref<GroupJoinRequestDto[]>([]);
const isLoading = ref(false);
const error = ref<string | null>(null);

const fetchJoinRequests = async () => {
  isLoading.value = true;
  error.value = null;
  try {
    const response = await apiClient.get<GroupJoinRequestDto[]>(`/api/conversations/${props.conversation.uuid}/requests`);
    requests.value = response.data;
  } catch (err) {
    error.value = '无法加载加群请求。';
    console.error(err);
  } finally {
    isLoading.value = false;
  }
};

const handleRequest = async (requesterId: number, action: 'ACCEPT' | 'REJECT') => {
  try {
    await apiClient.post(`/api/conversations/${props.conversation.uuid}/handle-request`, {
      requesterId,
      action
    });
    // Remove the handled request from the list
    requests.value = requests.value.filter(r => r.requesterId !== requesterId);
  } catch (err) {
    alert('操作失败。');
    console.error(err);
  }
};

onMounted(fetchJoinRequests);
</script>

<style scoped lang="scss">
.management-panel {
  height: 100%;
}
.request-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.request-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 8px;
  border-bottom: 1px solid #f0f0f0;
  &:last-child {
    border-bottom: none;
  }
}
.user-info {
  display: flex;
  align-items: center;
  .avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    margin-right: 15px;
  }
  .nickname {
    font-weight: 500;
  }
}
.actions {
  display: flex;
  gap: 10px;
}
.btn {
  padding: 6px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: opacity 0.2s;
  &:hover {
    opacity: 0.8;
  }
}
.accept-btn {
  background-color: #4caf50;
  color: white;
}
.reject-btn {
  background-color: #f44336;
  color: white;
}
.loading-state, .error-state, .empty-state {
  text-align: center;
  padding: 40px;
  color: #888;
}
</style>
