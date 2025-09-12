// main.ts
import { createApp } from 'vue';
import { createPinia } from 'pinia'
import App from './App.vue';
import router from './router';
import { setGlobalLogout } from './api/apiClient';
import { useAuthStore } from './stores/auth';
import { webSocketService } from './service/WebSocketService';
import apiClient from '@/api/apiClient';
import '@mdi/font/css/materialdesignicons.css';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);

// 设置全局登出函数
setGlobalLogout(() => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('userInfo');
  router.push('/login');
});

async function startApp() {
  // 在挂载应用之前，先执行认证初始化
  const authStore = useAuthStore();
  await authStore.initializeApp();

  // 确保用户信息已加载后再连接WebSocket
  if (authStore.userInfo?.id) {
    // 先注册回调，再连接WebSocket
    setupWebSocketNotifications(authStore);
    webSocketService.connect(() => {
      // 连接成功后再订阅用户通知
      webSocketService.subscribeToUserNotifications();
    });
  } else {
    console.warn('User ID not available, delaying WebSocket connection');
    // 如果用户ID不可用，设置一个监听器，当用户信息可用时再连接
    const unsubscribe = authStore.$subscribe((mutation, state) => {
      if (state.userInfo?.id) {
        unsubscribe();
        setupWebSocketNotifications(authStore);
        webSocketService.connect(() => {
          webSocketService.subscribeToUserNotifications();
        });
      }
    });
  }
  
  // 初始检查一次待处理请求
  checkPendingRequests();

  // 确保在所有异步操作完成后再挂载应用
  app.mount('#app');

  async function checkPendingRequests() {
    try {
      const response = await apiClient.get('/api/user/pending-requests');
      authStore.setPendingFriendRequests(response.data.length > 0);
    } catch (error) {
      console.error('Failed to check pending requests:', error);
    }
  }
}

// 设置WebSocket通知处理
function setupWebSocketNotifications(authStore: any) {
  // 注册当收到好友请求时要执行的逻辑
  webSocketService.onFriendRequest((notification) => {
    console.log('收到好友请求:', notification);
    if (notification.type === 'FRIEND_REQUEST') {
      authStore.setPendingFriendRequests(true); // 更新Pinia store，触发红点
    }
  });
  
  // 注册当收到群聊邀请时要执行的逻辑
  webSocketService.onGroupInvitation((invitation) => {
    console.log('收到群聊邀请:', invitation);
    window.dispatchEvent(new CustomEvent('group-invitation-received', {
      detail: invitation
    }));
  });
}

// 请求通知权限
if ('Notification' in window) {
  Notification.requestPermission();
}
window.addEventListener('beforeunload', () => {
  webSocketService.disconnect(true); // 强制断开连接
});
startApp();