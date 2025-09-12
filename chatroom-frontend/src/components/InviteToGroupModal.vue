<template>
  <div v-if="visible" class="modal-overlay" @click.self="handleCancel">
    <div class="modal-content">
      <!-- 左侧好友选择区域 -->
      <div class="contact-list-panel">
        <div class="search-container">
          <input type="text" placeholder="搜索好友" v-model="searchTerm" />
        </div>
        <div class="contact-scroll-area">
          <div v-if="isLoadingContacts" class="loading-message">正在加载...</div>
          <div v-for="(group, key) in sortedContacts" :key="key">
            <h3 class="group-letter">{{ key }}</h3>
            <ul>
              <li v-for="contact in group" :key="contact.id">
                <label :class="{ disabled: isMember(contact.id) }">
                  <input 
                    type="checkbox" 
                    :value="contact.id" 
                    v-model="selectedContactIds"
                    :disabled="isMember(contact.id)"
                  />
                  <img :src="contact.avatarUrl" class="avatar" />
                  <span class="nickname">{{ contact.nickname }}</span>
                  <span v-if="isMember(contact.id)" class="status-tag">已加入</span>
                </label>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 右侧已选和操作区域 -->
      <div class="selection-panel">
        <div class="selection-header">
          <h2>邀请成员</h2>
          <p>已选择 {{ selectedContactIds.length }} 人</p>
        </div>
        <div class="selected-contacts-area">
           <div v-if="selectedContacts.length === 0" class="empty-selection">
             请从左侧选择要邀请的好友
           </div>
           <ul v-else>
             <li v-for="contact in selectedContacts" :key="contact.id">
                <img :src="contact.avatarUrl" class="avatar" />
                <span class="nickname">{{ contact.nickname }}</span>
             </li>
           </ul>
        </div>
        <div class="actions">
          <button class="btn-confirm" @click="handleInvite" :disabled="selectedContactIds.length === 0 || isInviting">
            {{ isInviting ? '邀请中...' : '邀请' }}
          </button>
          <button class="btn-cancel" @click="handleCancel">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import pinying from 'pinyin';
import apiClient from '@/api/apiClient';
import type { UserInfo, GroupDetailsDto } from '@/types/api';

// --- Props and Emits ---
const props = defineProps<{
  visible: boolean;
  conversation: GroupDetailsDto | null;
}>();

const emit = defineEmits(['close', 'invite-success']);

// --- State ---
const allFriends = ref<UserInfo[]>([]);
const selectedContactIds = ref<number[]>([]);
const searchTerm = ref('');
const isLoadingContacts = ref(false);
const isInviting = ref(false);
const currentMemberIds = ref<Set<number>>(new Set());

// --- Methods ---
const fetchFriends = async () => {
  isLoadingContacts.value = true;
  try {
    const response = await apiClient.get<UserInfo[]>('/api/friendships/friends');
    allFriends.value = response.data;
  } catch (err) {
    console.error('Failed to fetch friends:', err);
  } finally {
    isLoadingContacts.value = false;
  }
};

const handleCancel = () => {
  if (!isInviting.value) {
    emit('close');
  }
};

const handleInvite = async () => {
  if (!props.conversation || selectedContactIds.value.length === 0) return;

  isInviting.value = true;
  try {
    await apiClient.post(
      `/api/conversations/${props.conversation.conversationId}/invite`,
      selectedContactIds.value 
    );

    // 邀请成功后，发送全局事件通知其他组件
    window.dispatchEvent(new CustomEvent('group-invitation-sent', {
      detail: {
        groupUuid: props.conversation.uuid,
        invitedUserIds: selectedContactIds.value
      }
    }));

    emit('invite-success');
    emit('close');
  } catch (error) {
    console.error('Failed to invite members:', error);
    alert('邀请失败，请稍后重试。');
  } finally {
    isInviting.value = false;
  }
};

const isMember = (contactId: number) => {
  return currentMemberIds.value.has(contactId);
};

// --- Computed Properties ---
const filteredContacts = computed(() => {
  if (!searchTerm.value) {
    return allFriends.value;
  }
  return allFriends.value.filter(c => 
    c.nickname.toLowerCase().includes(searchTerm.value.toLowerCase())
  );
});

const sortedContacts = computed(() => {
  const grouped: Record<string, UserInfo[]> = {};
  filteredContacts.value.forEach(contact => {
    const firstLetter = pinying(contact.nickname, { style: 'first_letter' })[0][0].toUpperCase();
    if (!grouped[firstLetter]) {
      grouped[firstLetter] = [];
    }
    grouped[firstLetter].push(contact);
  });
  
  const sortedKeys = Object.keys(grouped).sort();
  const sortedGrouped: Record<string, UserInfo[]> = {};
  for (const key of sortedKeys) {
    sortedGrouped[key] = grouped[key];
  }
  return sortedGrouped;
});

const selectedContacts = computed(() => {
  return allFriends.value.filter(c => selectedContactIds.value.includes(c.id));
});

// --- Watchers ---
watch(() => props.visible, (isVisible) => {
  if (isVisible) {
    // Reset state when modal opens
    selectedContactIds.value = [];
    searchTerm.value = '';
    
    // Fetch friends if not already loaded
    if (allFriends.value.length === 0) {
      fetchFriends();
    }
    
    // Update current member list
    if (props.conversation && props.conversation.members) {
      currentMemberIds.value = new Set(props.conversation.members.map(m => m.id));
    }
  }
});

</script>

<style scoped lang="scss">
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal-content {
  width: 680px;
  height: 520px;
  background-color: #f5f5f5;
  border-radius: 4px;
  display: flex;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.contact-list-panel {
  width: 250px;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  padding: 12px;
}
.search-container {
  padding: 8px 0;
  input {
    width: 100%;
    height: 30px;
    border: 1px solid #e0e0e0;
    border-radius: 4px;
    padding: 0 8px;
    box-sizing: border-box;
  }
}
.contact-scroll-area {
  flex-grow: 1;
  overflow-y: auto;
  margin-top: 10px;
}
.group-letter {
  margin: 10px 0 5px 5px;
  font-size: 14px;
  color: #888;
}
ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
li label {
  display: flex;
  align-items: center;
  padding: 8px 5px;
  cursor: pointer;
  border-radius: 4px;
  position: relative;
  &:hover {
    background-color: #f0f0f0;
  }
  &.disabled {
    cursor: not-allowed;
    color: #aaa;
    &:hover {
      background-color: transparent;
    }
  }
  input[type="checkbox"] {
    margin-right: 15px;
    width: 16px;
    height: 16px;
  }
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  margin-right: 10px;
}
.nickname {
  font-size: 14px;
}
.status-tag {
  position: absolute;
  right: 5px;
  font-size: 12px;
  color: #999;
}
.selection-panel {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
}
.selection-header {
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 12px;
  
  h2 {
    margin: 0 0 4px 0;
    font-size: 18px;
    font-weight: 500;
  }
  p {
    margin: 0;
    font-size: 14px;
    color: #888;
  }
}
.selected-contacts-area {
  flex-grow: 1;
  overflow-y: auto;
  padding: 10px 0;

  .empty-selection {
    color: #aaa;
    text-align: center;
    margin-top: 40px;
  }
  
   li {
    display: flex;
    align-items: center;
    padding: 6px 0;
  }
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;

  button {
    padding: 8px 24px;
    border: 1px solid #ccc;
    border-radius: 4px;
    cursor: pointer;
    background-color: #f0f0f0;
    
    &:hover {
      background-color: #e0e0e0;
    }
    &:disabled {
      cursor: not-allowed;
      opacity: 0.6;
    }
  }

  .btn-confirm {
    background-color: #4CAF50;
    color: white;
    border-color: #4CAF50;

    &:hover:not(:disabled) {
      background-color: #45a049;
    }
  }
}
.loading-message {
  text-align: center;
  padding: 20px;
  color: #888;
}
</style>
