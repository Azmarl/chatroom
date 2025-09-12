<template>
  <div class="management-panel">
    <div v-if="isLoading" class="loading-state">正在加载群信息...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <div v-else-if="groupDetails" class="admin-management-content">
      <div class="column member-list-column">
        <div class="search-bar">
          <input type="text" v-model="memberSearch" placeholder="搜索成员" />
        </div>
        <ul class="user-list">
          <li v-for="member in filteredMembers" :key="member.id" class="user-item">
            <input
              type="checkbox"
              :id="'member-' + member.id"
              :checked="isAdmin(member.id)"
              @change="toggleAdmin(member)"
              :disabled="member.id === authStore.userInfo?.id"
            />
            <label :for="'member-' + member.id">
              <img :src="member.avatarUrl" class="avatar" />
              <span class="nickname">{{ member.nickname }}</span>
            </label>
          </li>
        </ul>
      </div>
      <div class="column admin-list-column">
        <h3 class="column-title">管理员列表</h3>
        <ul class="user-list">
          <li v-for="admin in adminList" :key="admin.id" class="user-item">
            <img :src="admin.avatarUrl" class="avatar" />
            <span class="nickname">{{ admin.nickname }}</span>
            <span class="role-tag">{{ admin.role }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import apiClient from '@/api/apiClient';
import type { ConversationSummary, GroupDetailsDto, GroupUserInfo } from '@/types/api';
import { useAuthStore } from '@/stores/auth';

const props = defineProps<{
  conversation: ConversationSummary;
}>();

const authStore = useAuthStore();
const groupDetails = ref<GroupDetailsDto | null>(null);
const isLoading = ref(false);
const error = ref<string | null>(null);
const memberSearch = ref('');

const fetchGroupDetails = async () => {
  isLoading.value = true;
  error.value = null;
  try {
    const response = await apiClient.get<GroupDetailsDto>(`/api/conversations/${props.conversation.uuid}`);
    groupDetails.value = response.data;
  } catch (err) {
    error.value = '无法加载群成员信息。';
    console.error(err);
  } finally {
    isLoading.value = false;
  }
};

const filteredMembers = computed(() => {
  if (!groupDetails.value) return [];
  return groupDetails.value.members.filter(member =>
    member.nickname.toLowerCase().includes(memberSearch.value.toLowerCase())
  );
});

const adminList = computed(() => {
    if (!groupDetails.value) return [];
    return groupDetails.value.members.filter(m => m.role === 'admin' || m.role === 'owner');
});

const isAdmin = (userId: number) => {
  return adminList.value.some(admin => admin.id === userId);
};

const toggleAdmin = async (user: GroupUserInfo) => {
  if (!groupDetails.value) return;
  const action = isAdmin(user.id) ? 'DEMOTE' : 'PROMOTE';
  
  try {
    await apiClient.post(`/api/conversations/${props.conversation.uuid}/admin`, {
      targetUserId: user.id,
      action: action
    });
    // Refresh details to get updated roles
    fetchGroupDetails(); 
  } catch (err) {
    alert(`操作失败: ${action === 'PROMOTE' ? '设置' : '取消'}管理员失败。`);
    console.error(err);
  }
};

onMounted(fetchGroupDetails);
</script>

<style scoped lang="scss">
.management-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.admin-management-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  height: 100%;
  overflow: hidden;
}
.column {
  display: flex;
  flex-direction: column;
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 15px;
  overflow-y: auto;
}
.search-bar input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
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
  border-radius: 6px;
  transition: background-color 0.2s;
  &:hover {
    background-color: #f0f0f0;
  }
  label {
    display: flex;
    align-items: center;
    cursor: pointer;
  }
  input[type="checkbox"] {
    margin-right: 12px;
    cursor: pointer;
  }
  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    margin-right: 12px;
  }
  .nickname {
    font-weight: 500;
  }
  .role-tag {
    margin-left: auto;
    background-color: #e0e0e0;
    color: #555;
    font-size: 12px;
    padding: 2px 6px;
    border-radius: 4px;
  }
}
.column-title {
    font-size: 16px;
    margin-top: 0;
    margin-bottom: 15px;
    color: #333;
}
.loading-state, .error-state {
  text-align: center;
  padding: 40px;
  color: #888;
}
</style>
