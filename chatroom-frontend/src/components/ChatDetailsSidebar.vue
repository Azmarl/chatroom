<template>
  <!-- 遮罩层，点击可以关闭侧边栏 -->
  <div 
    class="sidebar-overlay" 
    :class="{ visible: visible }" 
    @click="handleClose"
  ></div>
  
  <!-- 侧边栏主体，使用 transition 实现滑入滑出效果 -->
  <transition name="slide-fade">
    <div v-if="visible && chat" class="details-sidebar">
      <div class="sidebar-content">

        <!-- 加载状态 -->
        <div v-if="isLoading" class="loading-spinner">
          <p>正在加载...</p>
        </div>

        <template v-else-if="details">
          <!-- A. 私聊布局 -->
          <template v-if="chat.type === 'PRIVATE'">
            <div class="participant-section">
                <div class="participant-item">
                  <img :src="(details as UserInfo).avatarUrl" class="avatar" />
                  <span class="name">{{ (details as UserInfo).nickname }}</span>
                </div>
                <div class="participant-item add-button" @click="openInviteModal">
                  <div class="add-icon-wrapper">+</div>
                  <span class="name">添加</span>
                </div>
              </div>
            <div class="action-list">
              <div class="action-item">
                <span>查找聊天内容</span>
                <i class="mdi mdi-chevron-right"></i>
              </div>
            </div>
            <div class="action-list">
              <div class="action-item">
                <span>消息免打扰</span>
                <div class="switch"></div>
              </div>
              <div class="action-item" @click="togglePinStatus">
                <span>置顶聊天</span>
                <div class="switch" :class="{ active: chat.isPinned }"></div>
              </div>
            </div>
            <div class="action-list danger">
              <div class="action-item">
                <span>清空聊天记录</span>
              </div>
            </div>
          </template>

          <!-- B. 群聊布局 -->
          <template v-if="chat.type === 'GROUP'">
            <div class="group-header">
                <h3>{{ (details as GroupDetailsDto).name }}</h3>
                <p class="group-uuid">群号: {{ (details as GroupDetailsDto).uuid }}</p>
              </div>
            <div class="participant-section group">
                <div 
                  v-for="member in (details as GroupDetailsDto).members" 
                  :key="member.id" 
                  class="participant-item"
                >
                  <img :src="member.avatarUrl" class="avatar" />
                  <span class="name">{{ member.nickname }}</span>
                </div>
                <div class="participant-item add-button" @click="openInviteModal">
                  <div class="add-icon-wrapper">+</div>
                  <span class="name">添加</span>
                </div>
              </div>
            <div class="action-list">
              <div class="action-item"><span>群聊名称</span><span class="value">{{ chat.name }}</span></div>
              <div class="action-item"><span>群公告</span><span class="value">未设置</span></div>
              <div class="action-item"><span>备注</span><span class="value"></span></div>
              <div class="action-item"><span>我在本群的昵称</span><span class="value">{{ authStore.userInfo?.nickname }}</span></div>
            </div>
            <div class="action-list">
              <div class="action-item"><span>查找聊天内容</span><i class="mdi mdi-chevron-right"></i></div>
            </div>
            
            <!-- 群管理功能入口 -->
            <div v-if="isGroupAdmin" class="action-list">
              <div class="action-item" @click="openManagementModal('admin')" :class="{ disabled: !isOwner }">
                <span>设置管理员</span>
                <small v-if="!isOwner">(仅群主)</small>
                <i class="mdi mdi-chevron-right"></i>
              </div>
              <div class="action-item" @click="openManagementModal('join-requests')">
                <span>加群管理</span>
                <i class="mdi mdi-chevron-right"></i>
              </div>
              <div class="action-item" @click="openManagementModal('blacklist')">
                <span>群黑名单</span>
                <i class="mdi mdi-chevron-right"></i>
              </div>
              <div class="action-item" @click="openManagementModal('mute')">
                <span>群内禁言</span>
                <i class="mdi mdi-chevron-right"></i>
              </div>
            </div>

            <div class="action-list">
              <div class="action-item"><span>消息免打扰</span><div class="switch"></div></div>
              <div class="action-item" @click="togglePinStatus">
                <span>置顶聊天</span>
                <div class="switch" :class="{ active: chat.isPinned }"></div>
              </div>
              <div class="action-item"><span>保存到通讯录</span><div class="switch"></div></div>
              <div class="action-item"><span>显示群成员昵称</span><div class="switch active"></div></div>
            </div>
            <div class="action-list danger">
              <div class="action-item"><span>清空聊天记录</span></div>
            </div>
            <!-- 修改退出群聊按钮，添加点击事件 -->
            <div class="action-list danger">
              <div class="action-item" @click="confirmLeaveGroup">
                <span>退出群聊</span>
              </div>
            </div>
          </template>
        </template>
        
      </div>
    </div>
  </transition>

  <!-- 群管理模态框 -->
  <ManagementView
    v-if="chat"
    :visible="isManagementModalVisible"
    :mode="managementModalMode"
    :conversation="chat"
    @close="closeManagementModal"
  />

  <!-- 邀请好友加入群聊模态框 -->
  <InviteToGroupModal
    :visible="isInviteModalVisible"
    :conversation="details as GroupDetailsDto"
    @close="closeInviteModal"
    @invite-success="handleInviteSuccess"
  />

  <!-- 退出群聊确认模态框 -->
  <div v-if="showLeaveConfirm" class="modal-overlay" @click.self="cancelLeaveGroup">
    <div class="confirm-modal">
      <div class="confirm-header">
        <h3>退出群聊</h3>
      </div>
      <div class="confirm-body">
        <p>确定要退出群聊 "{{ chat?.name }}" 吗？退出后将不再接收该群聊的消息。</p>
      </div>
      <div class="confirm-actions">
        <button class="btn-cancel" @click="cancelLeaveGroup">取消</button>
        <button class="btn-confirm" @click="leaveGroup" :disabled="isLeaving">
          {{ isLeaving ? '退出中...' : '确定退出' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { useAuthStore } from '@/stores/auth';
import apiClient from '@/api/apiClient';
import type { ConversationSummary, GroupDetailsDto, UserInfo } from '@/types/api';
import ManagementView from './ManagementView.vue';
import InviteToGroupModal from './InviteToGroupModal.vue';

const props = defineProps<{
  visible: boolean;
  chat: ConversationSummary | null;
}>();

const emit = defineEmits(['close', 'chat-updated', 'conversation-left']);

const authStore = useAuthStore();

const details = ref<GroupDetailsDto | UserInfo | null>(null);
const isLoading = ref(false);

// --- Modal State ---
const isManagementModalVisible = ref(false);
const managementModalMode = ref<'admin' | 'join-requests' | 'blacklist' | 'mute' | null>(null);
const isInviteModalVisible = ref(false);

// 退出群聊相关状态
const showLeaveConfirm = ref(false);
const isLeaving = ref(false);

const handleClose = () => {
  emit('close');
};

const togglePinStatus = async () => {
  if (!props.chat) return;

  try {
    await apiClient.post(`/api/conversations/${props.chat.conversationId}/pin`);

    const updatedChat: ConversationSummary = {
      ...props.chat,
      isPinned: !props.chat.isPinned
    };

    emit('chat-updated', updatedChat);

  } catch (error) {
    console.error('Failed to toggle pin status:', error);
    alert('操作失败，请稍后重试。');
  }
};

const isGroupAdmin = computed(() => {
  if (props.chat?.type === 'GROUP' && details.value) {
    const role = (details.value as GroupDetailsDto).currentUserRole;
    return role === 'owner' || role === 'admin';
  }
  return false;
});

const isOwner = computed(() => {
  return props.chat?.type === 'GROUP' && details.value && (details.value as GroupDetailsDto).currentUserRole === 'owner';
});

const openManagementModal = (mode: 'admin' | 'join-requests' | 'blacklist' | 'mute') => {
  if (mode === 'admin' && !isOwner.value) {
    alert('只有群主才能设置管理员。');
    return;
  }
  managementModalMode.value = mode;
  isManagementModalVisible.value = true;
};

const closeManagementModal = () => {
  isManagementModalVisible.value = false;
  managementModalMode.value = null;
};

const openInviteModal = () => {
  isInviteModalVisible.value = true;
};

const closeInviteModal = () => {
  isInviteModalVisible.value = false;
};

const handleInviteSuccess = () => {
  fetchChatDetails();
};

// 退出群聊相关方法
const confirmLeaveGroup = () => {
  showLeaveConfirm.value = true;
};

const cancelLeaveGroup = () => {
  showLeaveConfirm.value = false;
};

const leaveGroup = async () => {
  if (!props.chat) return;
  
  isLeaving.value = true;
  try {
    await apiClient.post(`/api/conversations/${props.chat.conversationId}/leave`);

    // 从 localStorage 中删除该群聊
    removeConversationFromLocalStorage(props.chat.conversationId);
    
    // 退出成功后，关闭确认弹窗和侧边栏
    showLeaveConfirm.value = false;
    emit('conversation-left', props.chat.conversationId);
    handleClose();
    
  } catch (error: any) {
    console.error('Failed to leave group:', error);
    
    // 显示具体的错误信息
    const errorMessage = error.response?.data?.message || error.message || '退出群聊失败，请稍后重试';
    alert(errorMessage);
  } finally {
    isLeaving.value = false;
  }
};

const fetchChatDetails = async () => {
  if (props.chat) {
    isLoading.value = true;
    details.value = null;
    try {
      if (props.chat.type === 'GROUP') {
        const response = await apiClient.get<GroupDetailsDto>(`/api/conversations/${props.chat.uuid}`);
        details.value = response.data;
      } else if (props.chat.type === 'PRIVATE') {
        const response = await apiClient.get<UserInfo>(`/api/user/summary/${props.chat.uuid}`);
        details.value = response.data;
      }
    } catch (error) {
      console.error('Failed to fetch chat details:', error);
    } finally {
      isLoading.value = false;
    }
  }
};

const removeConversationFromLocalStorage = (conversationId: number) => {
  try {
    const conversationsStr = localStorage.getItem('conversations');
    if (conversationsStr) {
      const conversations: ConversationSummary[] = JSON.parse(conversationsStr);
      const updatedConversations = conversations.filter(conv => conv.conversationId !== conversationId);
      localStorage.setItem('conversations', JSON.stringify(updatedConversations));
      console.log('Removed conversation from localStorage:', conversationId);
    }
  } catch (error) {
    console.error('Failed to remove conversation from localStorage:', error);
  }
};

watch(() => props.visible, (isVisible) => {
  if (isVisible) {
    fetchChatDetails();
  } else {
    handleClose();
  }
}, { immediate: true });
</script>

<style scoped lang="scss">
/* 原有样式保持不变，添加以下新样式 */

/* 确认模态框样式 */
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
  z-index: 3000; /* 确保在侧边栏之上 */
}

.confirm-modal {
  width: 400px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.confirm-header {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  text-align: center;
  
  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #333;
  }
}

.confirm-body {
  padding: 20px;
  
  p {
    margin: 0;
    font-size: 14px;
    line-height: 1.5;
    color: #666;
  }
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 15px 20px;
  border-top: 1px solid #e0e0e0;
  
  button {
    padding: 8px 16px;
    border-radius: 4px;
    font-size: 14px;
    cursor: pointer;
    transition: background-color 0.2s;
    
    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
  
  .btn-cancel {
    background-color: #f5f5f5;
    border: 1px solid #d9d9d9;
    color: #333;
    
    &:hover:not(:disabled) {
      background-color: #e6e6e6;
    }
  }
  
  .btn-confirm {
    background-color: #ff4d4f;
    border: 1px solid #ff4d4f;
    color: #fff;
    
    &:hover:not(:disabled) {
      background-color: #d9363e;
    }
  }
}

/* 其他原有样式保持不变 */
.details-sidebar {
  position: fixed;
  top: 0;
  right: 0;
  width: 280px;
  height: 100%;
  background-color: #f5f5f5;
  border-left: 1px solid #e0e0e0;
  z-index: 2000;
  display: flex;
  flex-direction: column;
}

/* 侧边栏滑入滑出动画 */
.slide-fade-enter-active, .slide-fade-leave-active {
  transition: transform 0.3s ease;
}
.slide-fade-enter-from, .slide-fade-leave-to {
  transform: translateX(100%);
}

.sidebar-content {
  flex-grow: 1;
  overflow-y: auto;
  padding: 20px 0;
}

.participant-section {
  padding: 0 20px 20px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
  border-bottom: 1px solid #e7e7e7;

  &.group {
    grid-template-columns: repeat(auto-fill, minmax(50px, 1fr));
  }
}

.participant-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  font-size: 12px;
  color: #888;

  .avatar {
    width: 50px;
    height: 50px;
    border-radius: 6px;
    margin-bottom: 5px;
  }
  .name {
    width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.add-button {
  cursor: pointer;
  .add-icon-wrapper {
    width: 50px;
    height: 50px;
    border: 1px dashed #ccc;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: #aaa;
    margin-bottom: 5px;
    &:hover {
      border-color: #999;
      color: #888;
    }
  }
}

.group-header {
  padding: 0 20px 15px;
  h3 {
    margin: 0;
    font-size: 18px;
  }
}

.action-list {
  background-color: #fff;
  margin-top: 15px;
  border-top: 1px solid #e7e7e7;
  border-bottom: 1px solid #e7e7e7;
}

.action-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  font-size: 14px;
  cursor: pointer;

  &:not(:last-child) {
    border-bottom: 1px solid #f0f0f0;
  }
  &:hover {
    background-color: #fafafa;
  }

  .value {
    color: #888;
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .mdi {
    color: #ccc;
  }
}

.action-list.danger .action-item {
  justify-content: center;
  color: #fa5151;
}

/* 模拟开关样式 */
.switch {
  width: 40px;
  height: 22px;
  border-radius: 11px;
  background-color: #e0e0e0;
  position: relative;
  transition: background-color 0.3s;
  &::after {
    content: '';
    position: absolute;
    top: 2px;
    left: 2px;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background-color: white;
    transition: transform 0.3s;
  }
  &.active {
    background-color: #07C160;
    &::after {
      transform: translateX(18px);
    }
  }
}

.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0);
  z-index: 1999;
  transition: background-color 0.3s ease;
  pointer-events: none;

  &.visible {
    background-color: rgba(0, 0, 0, 0.2);
    pointer-events: auto;
  }
}

.loading-spinner {
  text-align: center;
  padding: 40px;
  color: #888;
}
</style>