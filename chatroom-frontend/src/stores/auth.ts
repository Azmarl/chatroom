import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import apiClient from '@/api/apiClient';

export interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  avatarUrl: string;
  role?: 'owner' | 'admin' | 'member'; // 可选字段
}

// 辅助函数：安全地从 localStorage 解析 JSON
function getStoredUserInfo(): UserInfo | null {
  const stored = localStorage.getItem('userInfo');
  console.log("Stored userInfo:", stored);
  // 只有当 stored 是一个非空字符串时才尝试解析
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch (error) {
      console.error("Failed to parse userInfo from localStorage", error);
      // 如果解析失败，清除损坏的数据
      localStorage.removeItem('userInfo');
      return null;
    }
  }
  return null;
}

export const useAuthStore = defineStore('auth', () => {
  // --- State ---
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'));
  // (核心修改) 使用辅助函数进行安全的初始化
  const userInfo = ref<UserInfo | null>(getStoredUserInfo());
  const hasPendingFriendRequests = ref(false);
  const isInitialized = ref(false);

  // --- Getters ---
  const isLoggedIn = computed(() => !!accessToken.value && !!userInfo.value);

  // --- Actions ---
  function login(token: string, user: UserInfo) {
    accessToken.value = token;
    userInfo.value = user;

    localStorage.setItem('accessToken', token);
    localStorage.setItem('userInfo', JSON.stringify(user));
  }

  const updateUserInfo = (newUserInfo: UserInfo) => {
    userInfo.value = newUserInfo;
    userInfo.value.nickname = userInfo.value.nickname.replace(/^"|"$/g, '');
    // 更新到 localStorage，保持同步
    localStorage.setItem('userInfo', JSON.stringify(newUserInfo));
  };

  function logout() {
    accessToken.value = null;
    userInfo.value = null;

    localStorage.removeItem('accessToken');
    localStorage.removeItem('userInfo');
    localStorage.removeItem('conversations');
  }

  /**
   * (核心新增) 应用初始化函数
   * 检查本地是否存在token，并验证其有效性。
   * @returns Promise<void>
   */
  async function initializeApp() {
    if (accessToken.value) {
      try {
        // 尝试调用 /me 接口验证 token
        const response = await apiClient.get<UserInfo>('/api/auth/me');
        // 如果成功，说明 token 有效，更新用户信息
        userInfo.value = response.data;
      } catch (error) {
        // 如果失败，apiClient的拦截器会自动尝试刷新token。
        // 如果刷新也失败，拦截器会处理登出。
        // 我们只需要捕获错误，防止应用崩溃。
        console.error("Auth initialization failed, apiClient will handle refresh/logout.", error);
      }
    }
    // 无论成功与否，都标记初始化完成
    isInitialized.value = true;
  }

  return {
    accessToken,
    userInfo,
    isLoggedIn,
    isInitialized,
    login,
    updateUserInfo,
    logout,
    initializeApp,
    hasPendingFriendRequests,
    
    // 更新通知状态的方法
    setPendingFriendRequests(hasRequests: boolean) {
      hasPendingFriendRequests.value = hasRequests;
    }
  };
});