import axios from 'axios';

// 定义一个变量来存储全局的登出函数，以便在刷新失败时调用
// 这个函数需要在 main.ts 中被设置
let globalLogoutFn: (() => void) | null = null;

export const setGlobalLogout = (fn: () => void) => {
  globalLogoutFn = fn;
};

const apiClient = axios.create({
  baseURL: 'http://localhost:8080', // 你的后端API基础URL
  withCredentials: true, // 确保跨域请求时携带cookie (用于Refresh Token)
});

// 用于管理刷新请求的状态和队列
let isRefreshing = false; // 标记是否正在刷新 Access Token
let failedQueue: Array<{ resolve: (value: any) => void; reject: (reason?: any) => void; config: any }> = []; // 存储因 token 过期而失败的请求

// 辅助函数：处理排队的请求
const processQueue = (error: any | null, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else if (token) {
      prom.config.headers['Authorization'] = `Bearer ${token}`;
      prom.resolve(apiClient(prom.config)); // 使用新的 token 重新发送请求
    }
  });
  failedQueue = []; // 清空队列
};

// --- 请求拦截器 ---
apiClient.interceptors.request.use(config => {
  // 检查请求的URL是否为登录或刷新令牌的API
  // 如果是，则不添加 Authorization 头
  const isAuthRequest = config.url?.endsWith('/api/auth/login') || config.url?.endsWith('/api/auth/refresh');
  
  if (!isAuthRequest) {
    const accessToken = localStorage.getItem('accessToken');
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
  }

  return config;
}, error => {
  return Promise.reject(error);
});

// --- 响应拦截器 ---
apiClient.interceptors.response.use(response => {
  return response;
}, async (error) => {
  const originalRequest = error.config;
  const status = error.response ? error.response.status : null;

  // 检查是否是 403 错误，并且不是登录或刷新API的请求
  if (status === 403 && !originalRequest.url.endsWith('/api/auth/login') && !originalRequest.url.endsWith('/api/auth/refresh')) {
    // 检查是否正在刷新令牌，防止无限循环
    if (isRefreshing) {
      return new Promise(function(resolve, reject) {
        failedQueue.push({ resolve, reject, config: originalRequest });
      });
    }

    isRefreshing = true; // 标记开始刷新

    try {
      // 调用后端的刷新接口
      // 这个请求会自动带上 HttpOnly Cookie 中的 Refresh Token
      const { data } = await apiClient.post('/api/auth/refresh');
      
      // 刷新成功，将新的 Access Token 保存下来
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('userInfo', JSON.stringify(data.userInfo)); // 如果有用户信息也保存
      
      // 处理队列中的请求
      processQueue(null, data.accessToken);

      // 更新原始请求的 Authorization 头，并重新发送
      originalRequest.headers['Authorization'] = `Bearer ${data.accessToken}`;
      return apiClient(originalRequest);

    } catch (refreshError: any) {
      console.error('Refresh Token failed:', refreshError);
      // 如果刷新也失败了，说明 Refresh Token 也过期了，清除本地所有信息并跳转到登录页
      localStorage.removeItem('accessToken');
      localStorage.removeItem('userInfo');
      localStorage.removeItem('refreshToken'); // 确保清除所有令牌

      // 处理队列中的请求，告知失败
      processQueue(refreshError);

      if (globalLogoutFn) {
        globalLogoutFn(); // 调用全局登出函数进行跳转
      } else {
        // 备用方案：直接跳转，但会刷新页面
        window.location.href = '/login'; 
      }
      return Promise.reject(refreshError); // 再次抛出错误以中断Promise链
    } finally {
      isRefreshing = false; // 刷新过程结束，重置状态
    }
  }

  return Promise.reject(error);
});

export default apiClient;