<!-- eslint-disable vue/block-lang -->
<template>
  <div class="login-page">
    <div class="login-card">
      <h2>欢迎回来</h2>
      <p class="subtitle">登录您的账户</p>

      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            type="text"
            v-model="username"
            placeholder="请输入用户名"
            required
          />
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            type="password"
            v-model="password"
            placeholder="请输入密码"
            required
          />
        </div>

        <div class="options-row">
          <div class="remember-me">
            <input id="remember" type="checkbox" v-model="rememberMe" />
            <label for="remember">自动登录</label>
          </div>
          <a href="#" class="forgot-password">忘记密码？</a>
        </div>

        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>

        <button type="submit" class="login-button" :disabled="isLoading">
          {{ isLoading ? '登录中...' : '登 录' }}
        </button>
      </form>

      <div class="register-link">
        还没有账户？ <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api/apiClient'; 
import { useAuthStore } from '@/stores/auth';
import type { LoginResponse } from '@/types/api';

const username = ref<string>('');
const password = ref<string>('');
const rememberMe = ref<boolean>(true); // 默认勾选自动登录
const errorMessage = ref<string>('');
const isLoading = ref<boolean>(false);
const router = useRouter();
const authStore = useAuthStore(); 

const handleLogin = async () => {
  if (isLoading.value) return;
  isLoading.value = true;
  errorMessage.value = '';

  try {
    const loginRequest = {
      username: username.value,
      password: password.value,
      rememberMe: rememberMe.value,
    };
    
    console.log('登录请求数据:', loginRequest);

    // 调用登录接口
    const response = await apiClient.post<LoginResponse>('/api/auth/login', loginRequest);
    
    authStore.login(response.data.accessToken, response.data.userInfo);

    // 3. 跳转到聊天主页
    router.push('/chat');

  } catch (error: any) {
    if (error.response) {
      errorMessage.value = error.response.data || '登录失败，请检查您的凭证';
    } else {
      errorMessage.value = '发生未知网络错误';
    }
    authStore.logout();
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
@import '../css/LoginVue.css'; /* 假设你将 LoginView 的样式提取了 */
</style>
