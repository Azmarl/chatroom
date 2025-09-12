<template>
  <nav class="sidebar">
    <ul class="nav-links">
      <li>
        <router-link to="/chat" class="chat-link">
          <i class="mdi mdi-chat"></i>
          <!-- 聊天未读消息红点提示 -->
          <span v-if="totalUnreadCount > 0" class="nav-badge chat-badge">
            {{ totalUnreadCount > 99 ? '99+' : totalUnreadCount }}
          </span>
        </router-link>
      </li>
      <li>
        <router-link to="/contacts" class="contacts-link">
          <i class="mdi mdi-account-group"></i>
          <!-- 好友请求红点提示 -->
          <span v-if="hasPendingRequests" class="nav-badge"></span>
        </router-link>
      </li>
      <li>
        <router-link to="/discover">
          <i class="mdi mdi-compass"></i>
        </router-link>
      </li>
       <li class="profile-link">
        <router-link to="/me">
          <i class="mdi mdi-account-circle"></i>
        </router-link>
      </li>

      <!-- 一个看不见的弹性元素，将后面的所有内容推到底部 -->
      <li class="spacer"></li>
 
      <!-- 退出登录按钮 -->
      <li class="logout-link">
        <i class="mdi mdi-logout" @click="handleLogout" title="退出登录"></i>
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import apiClient from '@/api/apiClient'; 
import { useAuthStore } from '@/stores/auth';
import { useChatStore } from '@/stores/chat'; // 假设有聊天状态管理
import { computed, onMounted, onUnmounted } from 'vue';
 
// 获取 Vue Router 的实例
const router = useRouter();
const authStore = useAuthStore(); 
const chatStore = useChatStore(); // 获取聊天状态

const hasPendingRequests = computed(() => authStore.hasPendingFriendRequests);
// 计算所有会话的未读消息总数
const totalUnreadCount = computed(() => chatStore.totalUnreadCount);

// 监听未读消息变化事件
const handleUnreadCountChange = () => {
  // 这里可以添加额外的处理逻辑，如果需要的话
  console.log('未读消息数量变化:', chatStore.totalUnreadCount);
};

onMounted(() => {
  // 监听未读消息变化
  window.addEventListener('unread-count-changed', handleUnreadCountChange);
});

onUnmounted(() => {
  // 移除事件监听
  window.removeEventListener('unread-count-changed', handleUnreadCountChange);
});
 
/**
 * 处理退出登录的点击事件
 */
const handleLogout = async () => {
  try {
    // 调用后端的登出API
    await apiClient.post('/api/auth/logout');
    console.log('已成功通知后端登出');
  } catch (error) {
    console.error('调用登出API失败:', error);
    // 即使API调用失败，我们仍然要继续执行前端的清理操作
  } finally {
    // 无论后端调用是否成功，都清理前端的存储并跳转
    authStore.logout();
    // 清除聊天状态
    chatStore.clearChatState();
    console.log('本地存储已清除');
    
    router.push('/login');
  }
};
</script>

<style scoped lang="scss">
/* 红点样式 */
.nav-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  background-color: #ff3b30;
  border-radius: 50%;
  border: 1px solid #88c5e9;
  
  /* 聊天未读消息数字红点样式 */
  &.chat-badge {
    width: auto;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    font-size: 10px;
    font-weight: bold;
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
  }
}

.sidebar {
  width: 60px; /* 侧边栏宽度 */
  height: 100vh; /* 撑满整个视口高度 */
  background-color: #88c5e9; /* 一个比较舒服的深蓝灰色 */
  display: flex;
  flex-direction: column; /* 垂直排列 */
  align-items: center; /* 图标水平居中 */
  padding: 20px 0; /* 上下留出内边距 */
  flex-shrink: 0; /* 防止在 flex 布局中被压缩 */
  box-sizing: border-box; /* 让 padding 不会增加总高度 */
}
 
.nav-links {
  list-style: none; /* 去掉 li 的小圆点 */
  padding: 0;
  margin: 0;
  width: 100%;
  text-align: center;
  
  // 让 ul 成为一个 flex 容器来使用 spacer
  display: flex;
  flex-direction: column;
  height: 100%; // 撑满父容器高度
 
  li {
    margin-bottom: 25px; /* 链接之间的间距 */
    position: relative;
 
    a { // a 标签是 router-link 渲染后的结果
      color: #ffffff; /* 图标白色 */
      font-size: 28px; /* 图标大小 */
      text-decoration: none;
      opacity: 0.7;
      transition: opacity 0.2s ease;
      position: relative;
      display: inline-block;
 
      &:hover {
        opacity: 1; /* 鼠标悬停时变亮 */
      }
    }
  }
}
 
/* vue-router 会为当前激活的链接添加这个 class */
.router-link-exact-active {
  color: #203ea0 !important; /* 用一个亮蓝色来表示激活状态 */
  opacity: 1 !important;
}
 
/* 移除 .profile-link 的 margin-top: auto */
.profile-link {
  margin-bottom: 20px;
}
 
/* 弹性空白项，自动填充空间 */
.spacer {
  margin-top: auto;
  margin-bottom: 0; // 重置一下可能继承的 margin
}
 
/* 退出登录链接的样式 */
.logout-link {
  i {
    color: #ffffff;
    font-size: 28px;
    opacity: 0.7;
    transition: opacity 0.2s ease;
    cursor: pointer; // 鼠标变成小手，表示可点击
 
    &:hover {
      opacity: 1;
    }
  }
}
</style>