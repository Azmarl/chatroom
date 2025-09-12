<template>
  <div class="contacts-view">
    <!-- 左侧通讯录列表 -->
    <div class="contacts-sidebar">
      <!-- ... (这部分没有变化) ... -->
      <div class="sidebar-header">
        <button class="manage-btn">通讯录管理</button>
      </div>
 
      <div
        class="sidebar-item"
        @click="selectSpecialItem('add_friend')"
        :class="{ active: selectedItemType === 'add_friend' }"
      >
        <div class="avatar-placeholder add-friend-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M15 6a3 3 0 1 1-6 0 3 3 0 0 1 6 0ZM4 18c0-2.822 2.134-5.176 5-5.834V11a5.002 5.002 0 0 0-3.323-4.722A5.002 5.002 0 0 1 13 6a5 5 0 0 1 1 9.9v1.267A5.002 5.002 0 0 1 15 22H4a1 1 0 0 1-1-1v-3zm13 1h-3v-3a1 1 0 1 1 2 0v1h1a1 1 0 1 1 0 2z"></path></svg>
        </div>
        <span>添加好友/群聊</span>
      </div>
      
      <div 
        class="sidebar-item" 
        @click="selectSpecialItem('new_friends')"
        :class="{ active: selectedItemType === 'new_friends' }"
      >
        <div class="avatar-placeholder new-friends-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M16 11a5 5 0 1 1-10 0 5 5 0 0 1 10 0m-2 0a3 3 0 1 0-6 0 3 3 0 0 0 6 0m-3-8C6.477 3 2 7.477 2 13s4.477 10 10 10a9.96 9.96 0 0 0 6-2.053V17a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3v-2.58A9.963 9.963 0 0 0 3 13c0-4.411 3.589-8 8-8s8 3.589 8 8a8.01 8.01 0 0 1-1.42 4.45A2.985 2.985 0 0 1 17 20h1.438a3 3 0 0 0 2.94-2.556 8 8 0 0 0-7.378-9.444zM20 8a2 2 0 1 1-4 0 2 2 0 0 1 4 0"></path></svg>
        </div>
        <span>新的朋友</span>
        <!-- 红点提示 -->
        <span v-if="hasPendingRequests" class="sidebar-badge"></span>
      </div>
 
      <div class="list-section">
        <div class="list-title">群聊</div>
        <!-- 加载状态 -->
        <div v-if="groupsLoading" class="loading-message-sidebar">加载群聊中...</div>
        <!-- 列表为空 -->
        <div v-else-if="groups.length === 0" class="placeholder-sidebar">暂无群聊</div>
        <!-- 渲染列表 -->
        <div
          v-else
          v-for="group in groups"
          :key="group.uuid"
          class="sidebar-item"
          @click="selectItem(group, 'group')"
          :class="{ active: selectedItemType === 'group' && (selectedItem as GroupDetailsDto)?.uuid === group.uuid }"
        >
          <img :src="group.avatarUrl" class="avatar" alt="group avatar">
          <span>{{ group.name }}</span>
        </div>
      </div>

      <div class="list-section">
        <div v-if="friendsLoading" class="loading-message-sidebar">加载好友中...</div>
        <template v-else-if="friends.length > 0">
          <template v-for="(friendGroup, letter) in groupedFriends" :key="letter">
            <div class="list-title letter-group">{{ letter }}</div>
            <div v-for="friend in friendGroup" :key="friend.id" class="sidebar-item" @click="selectItem(friend, 'friend')" :class="{ active: selectedItemType === 'friend' && (selectedItem as UserSearchResultDto)?.id === friend.id }">
              <img :src="friend.avatarUrl" class="avatar" :alt="friend.nickname">
              <span>{{ friend.nickname }}</span>
            </div>
          </template>
        </template>
        <div v-else class="placeholder-sidebar">暂无好友</div>
      </div>
    </div>
 
    <!-- 右侧详情页 -->
    <div class="contact-details">
      <!-- (修改) 添加/搜索视图 -->
      <div v-if="selectedItemType === 'add_friend'" class="details-content add-friend-view">
        <div class="search-bar">
          <input type="text" v-model="searchQuery" :placeholder="searchType === 'user' ? '输入对方用户名' : '输入群聊id'" @keyup.enter="handleSearch">
          <select v-model="searchType" class="search-type-select">
            <option value="user">用户</option>
            <option value="group">群聊</option>
          </select>
          <button @click="handleSearch" class="search-btn" :disabled="searchStatus === 'searching'">
            {{ searchStatus === 'searching' ? '搜索中...' : '搜索' }}
          </button>
        </div>
 
        <div class="search-result">
          <div v-if="searchStatus === 'searching'" class="loading-message">
            <p>正在搜索...</p>
          </div>
          <div v-else-if="searchStatus === 'not_found'" class="not-found-message">
            <p>未找到匹配的{{ searchType === 'user' ? '用户' : '群聊' }}</p>
          </div>
          <div v-else-if="searchStatus === 'found' && searchResult" class="search-result-container">
            <!-- 用户结果 -->
            <div v-if="'username' in searchResult" class="search-result-item">
              <img :src="searchResult.avatarUrl" class="large-avatar" :alt="searchResult.nickname">
              <div class="info-text">
                <h2>{{ searchResult.nickname }}</h2>
                <p>用户名: {{ searchResult.username }}</p>
              </div>
              <button 
                @click="handleSearchResultAction(searchResult)" 
                class="action-btn"
                :disabled="friendRequestStatus !== 'idle'"
              >
                {{ friendRequestButtonText }}
              </button>
            </div>
            <!-- 群聊结果 -->
            <div v-else-if="'members' in searchResult" class="search-result-item">
              <img :src="searchResult.avatarUrl" class="large-avatar" :alt="searchResult.name">
              <div class="info-text">
                <h2>{{ searchResult.name }}</h2>
                <p>群成员: {{ searchResult.members.length }} 人</p>
              </div>
              <button 
                @click="handleSearchResultAction(searchResult)" 
                class="action-btn"
                :disabled="groupRequestStatus !== 'idle'"
              >
                {{ groupRequestButtonText }}
              </button>
            </div>
          </div>
          <div v-if="requestMessage" class="request-status-message" :class="{ 'error-message': isRequestError }">
            {{ requestMessage }}
          </div>
        </div>
      </div>
 
      <!-- ... (其他视图 v-else-if 没有变化) ... -->
      <div v-else-if="selectedItemType === 'friend' && selectedItem" class="details-content friend-details">
        <div class="friend-info">
          <img :src="(selectedItem as UserSearchResultDto).avatarUrl" class="large-avatar" :alt="(selectedItem as UserSearchResultDto).nickname">
          <div class="info-text">
            <h2>{{ (selectedItem as UserSearchResultDto).nickname }}</h2>
            <p>用户名: {{ (selectedItem as UserSearchResultDto).username }}</p>
          </div>
          <div class="more-options">
            <button class="options-btn" @click="toggleFriendOptions">...</button>
            <!-- 下拉菜单 -->
            <div v-if="showFriendOptions" class="options-dropdown">
              <div class="dropdown-item" @click="confirmDeleteFriend">删除好友</div>
            </div>
          </div>
        </div>
        <div class="action-bar"><button @click="sendMessage" class="send-message-btn" :disabled="isSendingMessage">发消息</button></div>
      </div>

      <div v-else-if="selectedItemType === 'group' && selectedItem" class="details-content group-details">
        <div class="group-header">
           <h3>{{ (selectedItem as GroupDetailsDto).name }}</h3>
           <p>共 {{ (selectedItem as GroupDetailsDto).memberCount }} 人</p>
        </div>
        <div class="group-members-grid">
          <div v-for="member in (selectedItem as GroupDetailsDto).members" :key="member.id" class="member-item">
            <img :src="member.avatarUrl" class="avatar" :alt="member.nickname">
            <span>{{ member.nickname }}</span>
          </div>
        </div>
        <div class="action-bar">
          <button @click="sendMessage" class="send-message-btn" :disabled="isSendingMessage">发消息</button>
        </div>
      </div>
      <div v-if="selectedItemType === 'new_friends'" class="details-content new-friends-view">
        <div v-if="pendingRequestsStatus === 'loading'" class="loading-message">
          <p>正在加载新的朋友请求...</p>
        </div>
        <div v-else-if="pendingRequestsStatus === 'error'" class="error-message">
          <p>加载请求失败，请稍后重试。</p>
        </div>
        <div v-else-if="pendingRequestsStatus === 'loaded' && pendingRequests.length === 0" class="placeholder">
          <p>暂无新的朋友请求</p>
        </div>
        <div v-else-if="pendingRequestsStatus === 'loaded'" class="request-list">
          <div v-for="request in pendingRequests" :key="request.friendshipId || request.conversationId" class="request-item">
            <img :src="request.requesterAvatarUrl" class="large-avatar" :alt="request.requesterNickname">
            <div class="info-text">
              <h2>{{ request.requesterNickname }}</h2>
              <p v-if="request.requestType === 'FRIEND_REQUEST'">请求添加你为好友</p>
              <p v-else>申请加入群聊: {{ request.conversationName }}</p>
            </div>
            <div class="action-buttons">
              <span v-if="request.actionStatus === 'accepted'" class="status-text accepted">已同意</span>
              <span v-else-if="request.actionStatus === 'rejected'" class="status-text rejected">已拒绝</span>
              <template v-else>
                <button 
                  @click="handlePendingRequest(request, 'REJECT')" 
                  class="reject-btn"
                  :disabled="request.actionStatus === 'processing'"
                >
                  拒绝
                </button>
                <button 
                  @click="handlePendingRequest(request, 'ACCEPT')" 
                  class="accept-btn"
                  :disabled="request.actionStatus === 'processing'"
                >
                  {{ request.actionStatus === 'processing' ? '处理中...' : '同意' }}
                </button>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-if="showDeleteConfirm" class="modal-overlay">
      <div class="confirm-modal">
        <h3>确认删除好友</h3>
        <p>删除后，将清空与该好友的聊天记录，且对方的好友列表也会移除你。确定删除吗？</p>
        <div class="modal-actions">
          <button @click="showDeleteConfirm = false" class="cancel-btn">取消</button>
          <button @click="deleteFriend" class="confirm-btn" :disabled="isDeletingFriend">
            {{ isDeletingFriend ? '删除中...' : '确定删除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
 
<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue';
import pinyin from 'pinyin';
import apiClient from '../api/apiClient';
import { useRouter } from 'vue-router';
import type { ConversationSummary } from '@/types/api';
import { useAuthStore } from '@/stores/auth';
const authStore = useAuthStore();
const router = useRouter();

interface UserSearchResultDto {
  id: number;
  username: string;
  nickname: string;
  avatarUrl: string;
}

interface GroupDetailsDto {
  conversationId: number;
  uuid: string;
  name: string;
  avatarUrl: string;
  description: string;
  memberCount: number;
  members: UserSearchResultDto[];
}

interface PendingRequest {
  requestType: 'FRIEND_REQUEST' | 'GROUP_JOIN_REQUEST';
  timestamp: string;
  requesterId: number;
  requesterNickname: string;
  requesterAvatarUrl: string;
  friendshipId?: number;
  conversationId?: number;
  conversationName?: string;
  actionStatus?: 'idle' | 'processing' | 'accepted' | 'rejected' | 'error';
}
 
const friends = ref<UserSearchResultDto[]>([]); // 好友列表现在也是这个类型
const friendsLoading = ref<boolean>(false);
const groups = ref<GroupDetailsDto[]>([]);
const groupsLoading = ref<boolean>(false);
const isSendingMessage = ref<boolean>(false);
const hasPendingRequests = computed(() => authStore.hasPendingFriendRequests);
const selectedItem = ref<UserSearchResultDto | GroupDetailsDto | null>(null);
type SelectedItemType = 'friend' | 'group' | 'new_friends' | 'add_friend' | null;
const selectedItemType = ref<SelectedItemType>(null);
 
const searchQuery = ref(''); // 重命名: searchUsername -> searchQuery
const searchType = ref<'user' | 'group'>('user'); // 新增: 搜索类型
const searchResult = ref<UserSearchResultDto | GroupDetailsDto | null>(null);
type SearchStatus = 'idle' | 'searching' | 'found' | 'not_found' | 'error';
const searchStatus = ref<SearchStatus>('idle');
type GroupRequestStatus = 'idle' | 'sending' | 'sent' | 'error' | 'joined';
const groupRequestStatus = ref<GroupRequestStatus>('idle');

// (新增) 用于处理好友请求的状态
type FriendRequestStatus = 'idle' | 'sending' | 'sent' | 'error';
const friendRequestStatus = ref<FriendRequestStatus>('idle');
const requestMessage = ref<string>('');
const isRequestError = ref<boolean>(false);

const pendingRequests = ref<PendingRequest[]>([]);
type PendingRequestStatus = 'idle' | 'loading' | 'loaded' | 'error';
const pendingRequestsStatus = ref<PendingRequestStatus>('idle');

const showFriendOptions = ref(false);
const showDeleteConfirm = ref(false);
const isDeletingFriend = ref(false);

const closeOptionsOnClickOutside = (event: MouseEvent) => {
  const optionsBtn = document.querySelector('.options-btn');
  const dropdown = document.querySelector('.options-dropdown');
  
  if (optionsBtn && !optionsBtn.contains(event.target as Node) && 
      dropdown && !dropdown.contains(event.target as Node)) {
    showFriendOptions.value = false;
  }
};

const toggleFriendOptions = () => {
  showFriendOptions.value = !showFriendOptions.value;
  
  if (showFriendOptions.value) {
    // 添加全局点击监听
    nextTick(() => {
      document.addEventListener('click', closeOptionsOnClickOutside);
    });
  } else {
    document.removeEventListener('click', closeOptionsOnClickOutside);
  }
};

const confirmDeleteFriend = () => {
  showFriendOptions.value = false;
  showDeleteConfirm.value = true;
  document.removeEventListener('click', closeOptionsOnClickOutside);
};

const deleteFriend = async () => {
  if (!selectedItem.value || selectedItemType.value !== 'friend') return;
  
  isDeletingFriend.value = true;
  const friend = selectedItem.value as UserSearchResultDto;
  
  try {
    // 调用后端API删除好友
    await apiClient.delete(`/api/friendships/${friend.id}`);
    
    // 从好友列表中移除
    friends.value = friends.value.filter(f => f.id !== friend.id);
    
    // 清理本地存储的会话记录
    const cachedConversations = localStorage.getItem('conversations');
    if (cachedConversations) {
      const conversations: ConversationSummary[] = JSON.parse(cachedConversations);
      const conversationId = await apiClient.get(`/api/friendships/${friend.id}/delete`).then(res => res.data);
      const updatedConversations = conversations.filter(conv => 
        !(conv.type === 'PRIVATE' && conv.conversationId === conversationId)
      );
      localStorage.setItem('conversations', JSON.stringify(updatedConversations));
    }
    
    // 关闭对话框并重置选中项
    showDeleteConfirm.value = false;
    selectedItem.value = null;
    selectedItemType.value = null;
    
  } catch (error: any) {
    console.error('删除好友失败:', error);
    alert(error.response?.data || '删除好友失败，请稍后重试');
  } finally {
    isDeletingFriend.value = false;
  }
};

// (新增) 计算属性，用于动态改变按钮文本
const friendRequestButtonText = computed(() => {
  switch (friendRequestStatus.value) {
    case 'sending': return '发送中...';
    case 'sent': return '请求已发送';
    case 'error': return '添加好友'; // 允许重试
    default: return '添加好友';
  }
});

const groupRequestButtonText = computed(() => {
  switch (groupRequestStatus.value) {
    case 'sending': return '申请中...';
    case 'sent': return '申请已发送';
    case 'joined': return '已加入';
    case 'error': return '申请加入'; // 允许重试
    default: return '申请加入';
  }
});

// (新增) 监听搜索结果的变化，当搜索结果更新时，重置好友请求的状态
watch(searchResult, () => {
  friendRequestStatus.value = 'idle';
  groupRequestStatus.value = 'idle';
  requestMessage.value = '';
  isRequestError.value = false;
});

// --- 计算属性 ---
const groupedFriends = computed(() => {
  const groups: Record<string, UserSearchResultDto[]> = {};
  const sortedFriends = [...friends.value].sort((a, b) => {
    // 按 nickname 排序
    const pinyinA = pinyin(a.nickname, { style: pinyin.STYLE_NORMAL }).join('');
    const pinyinB = pinyin(b.nickname, { style: pinyin.STYLE_NORMAL }).join('');
    return pinyinA.localeCompare(pinyinB);
  });
  
  sortedFriends.forEach(friend => {
    const firstLetter = pinyin(friend.nickname, {
      style: pinyin.STYLE_FIRST_LETTER,
    })[0][0].toUpperCase();
 
    if (!groups[firstLetter]) {
      groups[firstLetter] = [];
    }
    groups[firstLetter].push(friend);
  });
  return groups;
});
 
// --- 方法 ---
 
async function fetchFriends() {
  friendsLoading.value = true;
  try {
    // 调用后端的 /api/friendships/friends 接口
    const response = await apiClient.get<UserSearchResultDto[]>('/api/friendships/friends');
    friends.value = response.data; // 将获取到的真实数据赋值给 friends
  } catch (error) {
    console.error('Failed to fetch friends list:', error);
    // 可以在这里添加错误提示，例如使用一个弹窗通知用户
  } finally {
    friendsLoading.value = false;
  }
}

async function fetchGroups() {
  groupsLoading.value = true;
  try {
    const response = await apiClient.get<GroupDetailsDto[]>('/api/friendships/groups');
    groups.value = response.data;
  } catch (error) {
    console.error('Failed to fetch groups list:', error);
  } finally {
    groupsLoading.value = false;
  }
}

// (新增) 重置搜索状态，在切换视图时调用
const resetSearchState = () => {
    searchQuery.value = '';
    searchResult.value = null;
    searchStatus.value = 'idle';
    searchType.value = 'user'; // 重置为默认选项
};
 
const fetchData = () => {
  setTimeout(() => {
    fetchFriends();
    fetchGroups();
  }, 500);
};
 
const selectItem = (item: UserSearchResultDto | GroupDetailsDto, type: 'friend' | 'group')  => {
  // ... (方法内部逻辑无变化)
  resetSearchState();
  selectedItem.value = item;
  selectedItemType.value = type;
};

 
const sendMessage = async () => {
  // Guard clause to ensure an item is selected and it's either a friend or a group
  if (!selectedItem.value || (selectedItemType.value !== 'friend' && selectedItemType.value !== 'group')) {
    return;
  }
  
  isSendingMessage.value = true;
  
  try {
    let conversation: ConversationSummary | null = null;

    // --- Logic Branching ---
    if (selectedItemType.value === 'friend') {
      // Logic for private chat: find or create the conversation
      const friend = selectedItem.value as UserSearchResultDto;
      const response = await apiClient.post<ConversationSummary>(
        '/api/home/private',
        friend.id,
        { headers: { 'Content-Type': 'application/json' } }
      );
      conversation = response.data;

    } else { // selectedItemType.value === 'group'
      // Logic for group chat: get the existing conversation summary
      const group = selectedItem.value as GroupDetailsDto;
      const response = await apiClient.get<ConversationSummary>(`/api/home/conversations/${group.conversationId}`);
      conversation = response.data;
    }
    // --- End of Logic Branching ---

    if (!conversation) {
        throw new Error("Could not retrieve conversation details.");
    }

    // (步骤2: 核心修改) 在导航前调用状态检查API
    const statusResponse = await apiClient.get<{ status: string }>(`/api/conversations/${conversation.conversationId}/status`);
    const status = statusResponse.data.status;

    if (status !== 'OK') {
      // 如果状态不是OK，则给出提示并终止导航
      switch (status) {
        case 'NOT_A_MEMBER':
          alert('您已不在该群聊中。');
          break;
        case 'BLOCKED_FROM_GROUP':
          alert('您已被该群聊拉黑。');
          break;
        case 'FRIENDSHIP_TERMINATED':
          alert('您与该用户已不是好友关系。');
          break;
        default:
          alert('当前无法发起聊天。');
      }
      return; // 终止函数执行
    }

    // --- Common Logic for both chat types ---
    // Update the conversations cache in localStorage
    const cachedConversationsRaw = localStorage.getItem('conversations');
    const conversations: ConversationSummary[] = cachedConversationsRaw ? JSON.parse(cachedConversationsRaw) : [];
    const existingIndex = conversations.findIndex(c => c.conversationId === conversation!.conversationId);
    
    if (existingIndex > -1) {
      conversations[existingIndex] = conversation;
    } else {
      conversations.unshift(conversation);
    }
    localStorage.setItem('conversations', JSON.stringify(conversations));

    // Set the ID in sessionStorage so ChatView can auto-select it upon loading
    sessionStorage.setItem('selectedConversationId', conversation.conversationId.toString());
    
    // Navigate to the chat page
    router.push('/chat');

  } catch (error) {
    console.error('Failed to start or find conversation:', error);
    alert('发起聊天失败，请稍后重试。');
  } finally {
    isSendingMessage.value = false;
  }
};
 
async function handleSearch() {
  if (!searchQuery.value.trim()) {
    return;
  }

  searchStatus.value = 'searching';
  searchResult.value = null;

  try {
    let endpoint = '';
    if (searchType.value === 'user') {
      endpoint = `/api/friendships/search?query=${encodeURIComponent(searchQuery.value)}`;
    } else {
      endpoint = `/api/conversations/${encodeURIComponent(searchQuery.value)}`;
    }

    // 2. 使用 apiClient 发起请求
    const response = await apiClient.get(endpoint);
    const data = response.data;

    // 3. 处理响应数据
    if (searchType.value === 'user') {
      if (data && data.length > 0) {
        searchResult.value = data[0];
        searchStatus.value = 'found';
      } else {
        searchStatus.value = 'not_found';
      }
    } else {
      if (data) {
        searchResult.value = data;
        searchStatus.value = 'found';
      } else {
        searchStatus.value = 'not_found';
      }
    }
  } catch (error: any) {
    console.error('Search failed:', error);
    // Axios错误对象通常在 error.response 中包含后端返回信息
    if (error.response && (error.response.status === 404 || error.response.status === 400)) {
        searchStatus.value = 'not_found';
    } else {
        searchStatus.value = 'error';
    }
  }
}
 
// (核心修改) 实现 handleSearchResultAction 方法
async function handleSearchResultAction(result: UserSearchResultDto | GroupDetailsDto) {
  // 类型守卫：只处理用户搜索结果
  if ('username' in result) {
    friendRequestStatus.value = 'sending';
    requestMessage.value = '';
    isRequestError.value = false;

    try {
      // 调用后端的 /api/friendships/request 接口
      // 请求体直接是目标用户的 username 字符串
      const response = await apiClient.post<string>(
        '/api/friendships/request',
        result.username, // 直接发送字符串
        {
          // 明确告知后端请求体是纯文本
          headers: { 'Content-Type': 'text/plain' }
        }
      );

      // 请求成功
      friendRequestStatus.value = 'sent';
      requestMessage.value = response.data || '好友请求已成功发送！';
      isRequestError.value = false;

    } catch (error: any) {
      // 请求失败
      friendRequestStatus.value = 'error';
      isRequestError.value = true;
      if (error.response) {
        // 显示后端返回的错误信息，例如“A friendship or pending request already exists.”
        requestMessage.value = error.response.data || '发送请求失败，请重试。';
      } else {
        requestMessage.value = '网络错误，请稍后重试。';
      }
    }
  } else {
    groupRequestStatus.value = 'sending';
    try {
      // 调用后端的 /api/conversations/{conversationId}/join 接口
      // conversationId 就是群聊的 uuid
      const response = await apiClient.post<string>(`/api/conversations/${result.uuid}/join`);
      
      // 根据后端返回的消息判断状态
      // 假设直接加入成功会返回特定消息
      if (response.data.includes("成功加入")) {
          groupRequestStatus.value = 'joined';
      } else {
          groupRequestStatus.value = 'sent';
      }
      requestMessage.value = response.data;

    } catch (error: any) {
      groupRequestStatus.value = 'error';
      isRequestError.value = true;
      requestMessage.value = error.response?.data || '申请失败，请稍后重试。';
    }
  }
}

async function fetchPendingRequests() {
  pendingRequestsStatus.value = 'loading';
  try {
    const response = await apiClient.get<PendingRequest[]>('/api/user/pending-requests');
    // 为每个请求添加前端UI状态
    pendingRequests.value = response.data.map(req => ({ ...req, actionStatus: 'idle' }));
    pendingRequestsStatus.value = 'loaded';

    // 更新全局通知状态
    authStore.setPendingFriendRequests(pendingRequests.value.length > 0);
  } catch (error) {
    console.error('Failed to fetch pending requests:', error);
    pendingRequestsStatus.value = 'error';
  }
}

const selectSpecialItem = (type: 'new_friends' | 'add_friend') => {
  if (selectedItemType.value !== type) {
    resetSearchState();
  }
  selectedItem.value = null;
  selectedItemType.value = type;

  // 当选中 "新的朋友" 时，获取请求列表
  if (type === 'new_friends') {
    fetchPendingRequests();
  }
};

// (核心新增) 处理好友请求
async function handlePendingRequest(request: PendingRequest, action: 'ACCEPT' | 'REJECT') {
  if (request.actionStatus !== 'idle' && request.actionStatus !== 'error') return;
  request.actionStatus = 'processing';

  try {
    // 构建与后端 handleRequest DTO 匹配的载荷
    const payload = {
      requestType: request.requestType,
      action: action,
      friendshipId: request.friendshipId,
      conversationId: request.conversationId,
      requesterId: request.requesterId
    };

    await apiClient.post('/api/user/handle-request', payload);

    // 成功后更新UI状态
    request.actionStatus = action === 'ACCEPT' ? 'accepted' : 'rejected';

    // 检查是否还有待处理的请求
    const hasRemainingRequests = pendingRequests.value.some(
      req => req.actionStatus === 'idle' || req.actionStatus === 'error'
    );
    
    // 更新全局通知状态
    authStore.setPendingFriendRequests(hasRemainingRequests);

    fetchFriends();

  } catch (error) {
    console.error('Failed to handle request:', error);
    request.actionStatus = 'error'; // 标记为错误状态，可以添加UI提示
    // 可以在这里添加一个短暂的消息提示用户操作失败
  }
}
 
// --- 生命周期钩子 ---
onMounted(() => {
  fetchData();
});

onUnmounted(() => {
  document.removeEventListener('click', closeOptionsOnClickOutside);
});
</script>

<style scoped>
.more-options {
  position: relative;
  margin-left: auto;
}

.options-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 5px 15px;
  color: var(--text-light);
  border-radius: 4px;
}

.options-btn:hover {
  background-color: #f0f0f0;
}

.options-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  background-color: white;
  border-radius: 6px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  z-index: 100;
  min-width: 120px;
  overflow: hidden;
}

.dropdown-item {
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
}

.dropdown-item:hover {
  background-color: #f5f5f5;
  color: #e74c3c;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.confirm-modal {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  width: 350px;
  max-width: 90%;
}

.confirm-modal h3 {
  margin-top: 0;
  color: #333;
}

.confirm-modal p {
  margin: 15px 0;
  color: #666;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.cancel-btn, .confirm-btn {
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.cancel-btn {
  background-color: #f0f0f0;
  border: 1px solid #ddd;
  color: #333;
}

.confirm-btn {
  background-color: #e74c3c;
  border: 1px solid #e74c3c;
  color: white;
}

.confirm-btn:disabled {
  background-color: #ffa494;
  border-color: #ffa494;
  cursor: not-allowed;
}

.confirm-btn:hover:not(:disabled) {
  background-color: #c0392b;
  border-color: #c0392b;
}
.request-status-message {
  margin-top: 15px;
  padding: 10px;
  border-radius: 6px;
  text-align: center;
  font-size: 14px;
  border: 1px solid transparent;
  color: #155724;
  background-color: #d4edda;
  border-color: #c3e6cb;
}
.error-message {
  color: #721c24;
  background-color: #f8d7da;
  border-color: #f5c6cb;
}
.action-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}
:root {
  --sidebar-bg: #E7E7E7;
  --main-bg: #F5F5F5;
  --hover-bg: #D9D9D9;
  --active-bg: #C9C9C9;
  --text-color: #000;
  --text-light: #888;
  --primary-color: #07C160;
  --border-color: #E0E0E0;
}

.contacts-view {
  display: flex;
  height: 100vh;
  background-color: var(--main-bg);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* --- 左侧边栏 --- */
.contacts-sidebar {
  width: 260px;
  flex-shrink: 0;
  background-color: var(--sidebar-bg);
  border-right: 1px solid var(--border-color);
  overflow-y: auto;
  padding-bottom: 20px;
}

.sidebar-header {
  padding: 18px;
  border-bottom: 1px solid var(--border-color);
}

.manage-btn {
  width: 100%;
  padding: 8px;
  background-color: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.manage-btn:hover {
  background-color: #f7f7f7;
}
.sidebar-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 8px;
  height: 8px;
  background-color: #ff3b30;
  border-radius: 50%;
  border: 1px solid #e7e7e7;
}

.sidebar-item {
  display: flex;
  align-items: center;
  padding: 10px 18px;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.sidebar-item:hover {
  background-color: var(--hover-bg);
}

.sidebar-item.active {
  background-color: var(--active-bg);
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  margin-right: 12px;
  object-fit: cover;
}

.avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  margin-right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.new-friends-icon {
  background-color: #FA9D3B;
}
.new-friends-icon svg {
  width: 24px;
  height: 24px;
  color: white;
}

.list-section .list-title {
  padding: 8px 18px;
  font-size: 13px;
  color: var(--text-light);
  background-color: #E7E7E7;
}

.list-section .letter-group {
    position: sticky;
    top: 0;
    z-index: 1;
}

/* --- 右侧详情 --- */
.contact-details {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.details-content {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  padding: 40px;
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-light);
  font-size: 16px;
}

/* 好友详情 */
.friend-details {
  justify-content: flex-start;
}

.friend-info {
  display: flex;
  align-items: center;
  margin-bottom: 40px;
}

.large-avatar {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  margin-right: 20px;
}

.info-text h2 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
}

.info-text p {
  margin: 0;
  color: var(--text-light);
}

.more-options {
  margin-left: auto;
}

.options-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 5px 15px;
  color: var(--text-light);
}

/* 群聊详情 */
.group-details {
  padding: 20px 40px;
}

.group-header {
    text-align: center;
    margin-bottom: 20px;
    flex-shrink: 0;
}

.group-header h3 {
    margin: 0 0 5px;
}

.group-header p {
    margin: 0;
    color: var(--text-light);
    font-size: 14px;
}

.group-members-grid {
  flex-grow: 1;
  overflow-y: auto; /* 关键：让成员列表可以滚动 */
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 20px;
  padding: 10px;
  align-content: flex-start;
}

.member-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.member-item .avatar {
  width: 64px;
  height: 64px;
  margin-right: 0;
  margin-bottom: 8px;
}
.member-item span {
    font-size: 13px;
    width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 通用操作栏 */
.action-bar {
  flex-shrink: 0; /* 关键：防止操作栏被压缩 */
  padding-top: 20px;
  text-align: center;
  display:flex;
  justify-content: center;
  align-items: flex-end; /* 确保按钮在底部 */
}
/* 当好友详情时，需要更大的空间推到底部 */
.friend-details .action-bar {
    flex-grow: 1;
}

.send-message-btn {
  background-color: var(--primary-color);
  color: rgb(45, 62, 216);
  border: none;
  padding: 12px 60px;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.send-message-btn:hover {
  background-color: #06A853;
}

/* (新增) 为请求状态消息添加样式 */
.request-status-message {
  margin-top: 15px;
  padding: 10px;
  border-radius: 6px;
  text-align: center;
}
.success-message {
  color: #155724;
  background-color: #d4edda;
  border: 1px solid #c3e6cb;
}
.error-message {
  color: #721c24;
  background-color: #f8d7da;
  border: 1px solid #f5c6cb;
}
.action-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}
</style>
