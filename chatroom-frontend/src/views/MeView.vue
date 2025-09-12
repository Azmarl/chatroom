<template>
  <div class="me-view">
    <div v-if="authStore.userInfo" class="profile-card">
      <!-- 头像部分 -->
      <div class="avatar-section" @click="triggerAvatarUpload">
        <img :src="avatarPreview || authStore.userInfo.avatarUrl" class="avatar" alt="User Avatar">
        <div class="avatar-overlay">
          <i class="mdi mdi-camera"></i>
          <span>更换头像</span>
        </div>
        <div v-if="isUploadingAvatar" class="upload-spinner">
          <div class="spinner"></div>
        </div>
        <input
          type="file"
          ref="avatarInput"
          @change="handleAvatarChange"
          accept="image/png, image/jpeg, image/gif"
          style="display: none"
        />
      </div>

      <!-- 用户信息部分 -->
      <div class="info-section">
        <div class="info-item">
          <label>昵称</label>
          <div v-if="!isEditingNickname" class="info-value">
            <span>{{ authStore.userInfo.nickname }}</span>
            <button @click="startEditNickname" class="edit-btn"><i class="mdi mdi-pencil"></i></button>
          </div>
          <div v-else class="edit-mode">
            <input type="text" v-model="newNickname" ref="nicknameInput" @keyup.esc="cancelEditNickname">
            <button @click="saveNickname" class="save-btn" :disabled="isSavingNickname">
              {{ isSavingNickname ? '保存中...' : '保存' }}
            </button>
            <button @click="cancelEditNickname" class="cancel-btn">取消</button>
          </div>
        </div>
        <div class="info-item">
          <label>用户名</label>
          <div class="info-value readonly">
            <span>{{ authStore.userInfo.username }}</span>
          </div>
        </div>
      </div>

      <!-- 登出按钮 -->
      <div class="actions">
        <button @click="handleLogout" class="logout-btn">退出登录</button>
      </div>
    </div>
    <div v-else class="loading-placeholder">
      <p>正在加载用户信息...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore, type UserInfo } from '@/stores/auth';
import apiClient from '@/api/apiClient';

const router = useRouter();
const authStore = useAuthStore();

// --- 状态管理 ---
const isEditingNickname = ref(false);
const newNickname = ref('');
const nicknameInput = ref<HTMLInputElement | null>(null);

const avatarInput = ref<HTMLInputElement | null>(null);
const avatarPreview = ref<string | null>(null);
const isUploadingAvatar = ref(false);
const isSavingNickname = ref(false);

// --- 方法 ---

// 触发隐藏的文件上传输入框
const triggerAvatarUpload = () => {
  if (isUploadingAvatar.value) return;
  avatarInput.value?.click();
};

// 处理头像文件选择
const handleAvatarChange = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  // 创建本地预览图
  const reader = new FileReader();
  reader.onload = (e) => {
    avatarPreview.value = e.target?.result as string;
  };
  reader.readAsDataURL(file);

  // 上传文件
  isUploadingAvatar.value = true;
  const formData = new FormData();
  formData.append('avatar', file);

  try {
    const response = await apiClient.post('/api/user/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    // 使用后端返回的最新用户信息更新Store
    authStore.updateUserInfo(response.data);
  } catch (error) {
    console.error('Failed to upload avatar:', error);
    alert('头像上传失败，请重试。');
    avatarPreview.value = null; // 上传失败则移除预览
  } finally {
    isUploadingAvatar.value = false;
  }
};

// 开始编辑昵称
const startEditNickname = () => {
  isEditingNickname.value = true;
  newNickname.value = authStore.userInfo?.nickname || '';
  // DOM更新后自动聚焦到输入框
  nextTick(() => {
    nicknameInput.value?.focus();
  });
};

// 取消编辑昵称
const cancelEditNickname = () => {
  isEditingNickname.value = false;
};

// 保存新昵称
const saveNickname = async () => {
  if (!newNickname.value.trim() || newNickname.value === authStore.userInfo?.nickname) {
    cancelEditNickname();
    return;
  }

  isSavingNickname.value = true;
  try {
    const response = await apiClient.put('/api/user/profile', newNickname.value, {
      headers: {
        'Content-Type': 'application/json',  // 确保请求头是 JSON 格式
      },
    });
    
    authStore.updateUserInfo(response.data);
    isEditingNickname.value = false;
  } catch (error) {
    console.error('Failed to update nickname:', error);
    alert('昵称更新失败，请重试。');
  } finally {
    isSavingNickname.value = false;
  }
};

// 退出登录
const handleLogout = async () => {
  await apiClient.post('/api/auth/logout');
  authStore.logout();
  router.push('/login');
};
</script>

<style scoped lang="scss">
@import '@mdi/font/css/materialdesignicons.min.css';

.me-view {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-color: #f5f5f5;
}

.profile-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-section {
  position: relative;
  cursor: pointer;
  margin-bottom: 30px;

  .avatar {
    width: 120px;
    height: 120px;
    border-radius: 50%;
    object-fit: cover;
    border: 4px solid #fff;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
    transition: filter 0.3s ease;
  }

  .avatar-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    background-color: rgba(0, 0, 0, 0.5);
    color: white;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    opacity: 0;
    transition: opacity 0.3s ease;

    .mdi {
      font-size: 28px;
    }
    span {
      font-size: 14px;
      margin-top: 5px;
    }
  }

  &:hover .avatar-overlay {
    opacity: 1;
  }
  &:hover .avatar {
    filter: brightness(0.8);
  }
}

.upload-spinner {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  .spinner {
    border: 4px solid rgba(255, 255, 255, 0.3);
    border-top: 4px solid #fff;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.info-section {
  width: 100%;
  margin-bottom: 30px;
}

.info-item {
  margin-bottom: 20px;
  label {
    display: block;
    font-size: 14px;
    color: #888;
    margin-bottom: 8px;
  }
  .info-value {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 16px;
    padding: 10px;
    background-color: #f9f9f9;
    border-radius: 6px;
  }
  .readonly {
    color: #555;
  }
}

.edit-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #aaa;
  font-size: 18px;
  &:hover {
    color: #333;
  }
}

.edit-mode {
  display: flex;
  gap: 10px;
  input {
    flex-grow: 1;
    padding: 10px;
    border: 1px solid #ddd;
    border-radius: 6px;
    font-size: 16px;
    &:focus {
      outline: none;
      border-color: #07C160;
    }
  }
  button {
    padding: 0 15px;
    border-radius: 6px;
    border: 1px solid #ccc;
    cursor: pointer;
    background: #f0f0f0;
    &:hover {
      background: #e0e0e0;
    }
  }
  .save-btn {
    background: #07C160;
    color: white;
    border-color: #07C160;
    &:hover {
      background: #06a853;
    }
    &:disabled {
      background: #ccc;
      cursor: not-allowed;
    }
  }
}

.actions {
  width: 100%;
}

.logout-btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 6px;
  background-color: #f44336;
  color: white;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
  &:hover {
    background-color: #d32f2f;
  }
}
</style>
