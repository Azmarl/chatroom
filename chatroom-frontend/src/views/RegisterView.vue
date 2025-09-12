<template>
  <div class="login-page"> <!-- 复用登录页样式 -->
    <div class="login-card">
      <h2>创建账户</h2>
      <p class="subtitle">加入我们，开始聊天</p>
 
      <form @submit.prevent="handleRegister">
        
        <!-- 用户名输入框 (无变化) -->
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            type="text"
            v-model="username"
            required
          />
        </div>
 
        <!-- **新增: 邮箱输入框** -->
        <div class="form-group">
          <label for="email">邮箱</label>
          <input
            id="email"
            type="text" 
            v-model="email"
            required
          />
        </div>
        
        <!-- **新增: 昵称输入框** -->
        <div class="form-group">
          <label for="nickname">昵称</label>
          <input
            id="nickname"
            type="text"
            v-model="nickname"
            required
          />
        </div>
        
        <!-- 密码输入框 (无变化) -->
        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            type="password"
            v-model="password"
            required
          />
        </div>
        
        <!-- 消息提示区域 (无变化) -->
        <div v-if="message" :class="isError ? 'error-message' : 'success-message'">
          {{ message }}
        </div>
 
        <!-- 按钮和链接 (无变化) -->
        <button type="submit" class="login-button" :disabled="isLoading">
          {{ isLoading ? '注册中...' : '创建账户' }}
        </button>
      </form>
 
      <div class="register-link">
        已有账户？ <router-link to="/login">直接登录</router-link>
      </div>
    </div>
  </div>
</template>
 
<script setup lang="ts"> // <-- IMPORTANT: lang="ts"
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import apiClient from '../api/apiClient'; // <-- 引入我们创建的全局API客户端
 
const username = ref<string>('');
const password = ref<string>('');
const email = ref<string>('');
const nickname = ref<string>('');
const message = ref<string>('');
const isError = ref<boolean>(false);
const isLoading = ref<boolean>(false);
 
const router = useRouter();
 
const handleRegister = async () => {
  if (isLoading.value) return;
  isLoading.value = true;
  message.value = '';
  isError.value = false;
 
  try {
    const registerRequest = {
      username: username.value,
      password: password.value,
      email: email.value,
      nickname: nickname.value || null, // Send null if empty
    };
    
    const response = await apiClient.post<string>('/api/auth/register', registerRequest);
 
    message.value = response.data; // e.g., "账户创建成功！..."
    isError.value = false;
 
    setTimeout(() => {
      router.push('/login');
    }, 2000);
 
  } catch (error) {
    isError.value = true;
    if (axios.isAxiosError(error) && error.response) {
      message.value = error.response.data || '注册失败，请重试';
    } else {
      message.value = '网络错误，请稍后重试';
    }
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
/* 直接复用 LoginView 的样式，可以把公用样式提取到全局 CSS 文件中 */
@import '../css/LoginVue.css'; /* 假设你将 LoginView 的样式提取了 */

/* 为成功消息添加样式 */
.success-message {
  color: #155724;
  background-color: #d4edda;
  border-color: #c3e6cb;
  padding: 0.8rem;
  border-radius: 8px;
  text-align: center;
  margin-bottom: 1.5rem;
}
</style>
