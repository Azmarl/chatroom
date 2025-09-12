import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import LoginView from '@/views/LoginView.vue';
import RegisterView from '@/views/RegisterView.vue';
import ChatView from '@/views/ChatView.vue';

const routes: Array<RouteRecordRaw> = [
  {
    // (核心修改) 根路径现在直接重定向到/chat
    // 导航守卫会确保如果用户未登录，则会先跳转到/login
    path: '/',
    redirect: '/chat',
  },
  {
    path: '/login',
    name: 'Login', // 您的 App.vue 依赖这个name来隐藏侧边栏
    component: LoginView,
    meta: { isPublic: true } // 标记为公共页面
  },
  {
    path: '/register',
    name: 'Register', // 您的 App.vue 依赖这个name来隐藏侧边栏
    component: RegisterView,
    meta: { isPublic: true } // 标记为公共页面
  },
  {
    path: '/chat',
    name: 'chat',
    component: ChatView,
    meta: { requiresAuth: true }
  },
  {
    path: '/contacts',
    name: 'contacts',
    component: () => import('../views/ContactsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/discover',
    name: 'discover',
    component: () => import('../views/DiscoverView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/me',
    name: 'me',
    component: () => import('../views/MeView.vue'),
    meta: { requiresAuth: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('accessToken');
  const authRequired = to.meta.requiresAuth;
  const isPublicPage = to.meta.isPublic;

  // 1. 如果用户已登录
  if (token) {
    // 并且他们想访问公共页面（如登录页），则重定向到聊天主页
    if (isPublicPage) {
      next('/chat');
    } else {
      // 否则，正常访问受保护的页面
      next();
    }
  } 
  // 2. 如果用户未登录
  else {
    // 并且他们想访问受保护的页面，则重定向到登录页
    if (authRequired) {
      next('/login');
    } else {
      // 否则，正常访问公共页面
      next();
    }
  }
});

export default router;