<template>
  <div class="management-panel blacklist-management-content">
    <div class="column blacklist-column">
      <h3 class="column-title">群黑名单</h3>
      <div v-if="isLoading" class="loading-state">加载中...</div>
      <ul v-else class="user-list">
        <li v-for="block in blacklist" :key="block.id" class="user-item">
          <img :src="block.blockedUser.avatarUrl" class="avatar" />
          <span class="nickname">{{ block.blockedUser.nickname }}</span>
          <button @click="unblockUser(block.blockedUser.id)" class="btn unblock-btn">移除</button>
        </li>
      </ul>
    </div>
    <div class="column search-column">
      <h3 class="column-title">添加用户到黑名单</h3>
      <div class="search-bar">
        <input type="text" v-model="userSearch" @input="searchUsers" placeholder="输入用户名搜索" />
      </div>
      <div v-if="searchResult" class="search-result">
        <div class="user-info">
          <img :src="searchResult.avatarUrl" class="avatar" />
          <span class="nickname">{{ searchResult.nickname }}</span>
        </div>
        <button @click="blockUser" class="btn block-btn">拉黑</button>
      </div>
      <div v-if="searchError" class="error-state">{{ searchError }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import apiClient from '@/api/apiClient';
import type { ConversationSummary, ConversationBlock, UserInfo } from '@/types/api';

const props = defineProps<{
  conversation: ConversationSummary;
}>();

const blacklist = ref<ConversationBlock[]>([]);
const isLoading = ref(false);
const userSearch = ref('');
const searchResult = ref<UserInfo | null>(null);
const searchError = ref<string | null>(null);

const fetchBlacklist = async () => {
  isLoading.value = true;
  try {
    // Assuming an endpoint like this exists, adjust if necessary
    const response = await apiClient.get<ConversationBlock[]>(`/api/conversations/${props.conversation.conversationId}/block`);
    blacklist.value = response.data;
  } catch (err) {
    console.error('Failed to fetch blacklist', err);
  } finally {
    isLoading.value = false;
  }
};

const searchUsers = async () => {
  if (!userSearch.value.trim()) {
    searchResult.value = null;
    searchError.value = null;
    return;
  }
  try {
    const response = await apiClient.get<UserInfo[]>(`/api/friendships/search?query=${userSearch.value}`);
    if (response.data.length > 0) {
      searchResult.value = response.data[0];
      searchError.value = null;
    } else {
      searchResult.value = null;
      searchError.value = '未找到用户。';
    }
  } catch (err) {
    searchResult.value = null;
    searchError.value = '搜索失败。';
    console.error(err);
  }
};

const blockUser = async () => {
  if (!searchResult.value) return;
  if (confirm(`确定要将用户 "${searchResult.value.nickname}" 拉入黑名单吗？`)) {
    try {
      await apiClient.post(`/api/conversations/${props.conversation.conversationId}/block`, {
        blockedUserId: searchResult.value.id,
        reason: 'Group block'
      });
      fetchBlacklist(); // Refresh blacklist
      searchResult.value = null;
      userSearch.value = '';
    } catch (err) {
      alert('拉黑失败。');
      console.error(err);
    }
  }
};

const unblockUser = async (userId: number) => {
    try {
        // This endpoint might need to be different, e.g., DELETE /api/conversations/{...}/blocks/{userId}
        await apiClient.delete(`/api/conversations/${props.conversation.conversationId}/unblock/${userId}`);
        fetchBlacklist(); // Refresh blacklist
    } catch (err) {
        alert('移除黑名单失败。');
        console.error(err);
    }
};

onMounted(fetchBlacklist);
</script>

<style scoped lang="scss">
.blacklist-management-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  height: 100%;
}
.column {
  display: flex;
  flex-direction: column;
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 15px;
}
.column-title {
  font-size: 16px;
  margin-top: 0;
  margin-bottom: 15px;
}
.user-list {
  list-style: none;
  padding: 0;
  margin: 0;
  flex-grow: 1;
  overflow-y: auto;
}
.user-item {
  display: flex;
  align-items: center;
  padding: 8px;
  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    margin-right: 12px;
  }
  .nickname {
    font-weight: 500;
    flex-grow: 1;
  }
}
.search-bar input {
  width: 90%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  margin-bottom: 15px;
}
.search-result {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px;
  background-color: #eef;
  border-radius: 6px;
}
.btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.block-btn {
  background-color: #d9534f;
  color: white;
}
.unblock-btn {
  background-color: #f0ad4e;
  color: white;
}
.loading-state, .error-state {
  text-align: center;
  padding: 20px;
  color: #888;
}
</style>
