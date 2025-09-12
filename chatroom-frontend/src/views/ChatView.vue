<template>
  <div class="chat-view">
    <!-- 左侧聊天列表面板 -->
    <div class="chat-list-panel">
      
      <!-- 搜索和添加好友/群聊的头部 -->
      <div class="list-header">
        <div class="search-bar">
          <i class="mdi mdi-magnify"></i>
          <input type="text" placeholder="搜索">
        </div>
        <button class="add-chat-btn" @click="openCreateGroupModal">
          <i class="mdi mdi-plus"></i>
        </button>
      </div>

      <!-- 聊天会话列表 -->
      <div class="chat-list">
        <!-- 加载状态 -->
        <div v-if="isLoading" class="loading-placeholder">
          <p>正在加载聊天列表...</p>
        </div>
        <!-- 列表为空 -->
        <div v-else-if="conversations.length === 0" class="empty-placeholder">
          <p>暂无会话，<br>开始新的聊天吧！</p>
        </div>
        <!-- 渲染列表 -->
        <div 
          v-else
          v-for="convo in conversations" 
          :key="convo.conversationId" 
          class="chat-item"
          @click="selectChat(convo)"
          :class="{ active: selectedChat?.conversationId === convo.conversationId }"
          @contextmenu.prevent="showConversationContextMenu(convo, $event)"
        >
          <!-- (核心修改) 头像容器 -->
          <div class="avatar-container">
            <img :src="convo.avatarUrl" class="avatar">
            <!-- (核心修改) 未读消息红点，使用 v-if 控制显示 -->
            <span v-if="convo.unreadCount > 0" class="unread-badge">{{ convo.unreadCount }}</span>
          </div>
          <div class="chat-info">
            <div class="info-top">
              <span class="name">{{ convo.name }}</span>
              <span class="time" v-if="convo.lastMessageTimestamp">
                {{ formatTimestamp(convo.lastMessageTimestamp) }}
              </span>
              <span class="time" v-else></span>
            </div>
            <p class="last-message">
              {{ convo.lastMessageContent || ' ' }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧聊天对话面板 -->
    <div class="conversation-panel" :class="{ 'sidebar-open': isDetailsSidebarVisible }">
      <!-- 当有聊天被选中时显示 -->
      <div v-if="selectedChat" class="chat-window">
        <!-- 聊天头部，显示对方昵称或群聊名称 -->
        <div class="chat-header">
          <span class="chat-title">{{ selectedChat.name }}</span>
          <button @click="toggleDetailsSidebar" class="details-btn">
            <i class="mdi mdi-dots-horizontal"></i>
          </button>
        </div>

        <!-- 聊天消息区域 -->
        <div class="chat-messages" ref="messagesContainer">
          <!-- 加载历史消息的提示 -->
              <div v-if="isMessagesLoading" class="loading-messages">
                <p>正在加载消息...</p>
              </div>
          <!-- 消息列表循环 -->
          <!-- 使用 <template> 来循环，以便可以渲染多个根节点 (时间分割线和消息) -->
          <template v-else v-for="(msg, index) in messages" :key="msg.id">
            <!-- 时间分割线 -->
            <div v-if="shouldShowTimestamp(index)" class="time-divider">
              <span>{{ formatDetailedTimestamp(msg.timestamp) }}</span>
            </div>

            <!-- (核心修改) 撤回消息的提示 -->
            <div v-if="msg.recalled" class="recalled-message-tip">
              <span>{{ msg.sender.id === authStore.userInfo?.id ? '你' : msg.sender.nickname }} 撤回了一条消息</span>
            </div>

            <div 
              v-else
              class="message-wrapper"
              :class="{ 'sent': msg.sender.id === authStore.userInfo?.id, 'received': msg.sender.id !== authStore.userInfo?.id }"
              @contextmenu.prevent="showMessageContextMenu(msg, $event)"
            >
              <!-- 对方头像 (只在接收到的消息时显示) -->
              <img 
                v-if="msg.sender.id !== authStore.userInfo?.id"
                :src="msg.sender.avatarUrl" 
                class="message-avatar" 
                :alt="msg.sender.nickname"
              />
              
              <div class="message-content">
                <!-- 发送者昵称 (只在接收到的消息时显示) -->
                <div v-if="msg.sender.id !== authStore.userInfo?.id" class="sender-name">
                  {{ msg.sender.nickname}}
                </div>
                <!-- 消息气泡 -->
                <div class="message-bubble">
                  <!-- (核心新增) 引用消息预览 -->
                  <div v-if="msg.repliedMessage" class="reply-quote">
                    <div class="reply-sender">{{ msg.repliedMessage.senderNickname }}:</div>
                    <div class="reply-content">{{ msg.repliedMessage.content }}</div>
                  </div>
                  <!-- 消息正文 -->
                  <span class="main-content">{{ msg.content }}</span>
                </div>
              </div>

              <!-- 我的头像 (只在我发送的消息时显示) -->
              <img 
                v-if="msg.sender.id === authStore.userInfo?.id" 
                :src="msg.sender.avatarUrl" 
                class="message-avatar" 
                :alt="msg.sender.nickname"
              />
            </div>
          </template>

          <!-- (核心新增) 撤回后可重新编辑的提示 -->
          <div v-if="recalledMessageForEdit" class="recalled-edit-tip">
            <span>你撤回了一条消息</span>
            <button @click="reEditRecalledMessage" class="re-edit-btn">重新编辑</button>
          </div>
        </div>

        <!-- 消息发送区域 -->
        <div class="chat-input-area">

          <EmojiPicker 
            :visible="isEmojiPickerVisible" 
            @select="handleEmojiSelect"
          />

          <div v-if="quotedMessage" class="quote-preview">
            <div class="quote-content">
              <strong>回复 {{ quotedMessage.sender.nickname }}:</strong>
              <p>{{ quotedMessage.content }}</p>
            </div>
            <button @click="cancelQuote" class="cancel-quote-btn">&times;</button>
          </div>

          <!-- 工具栏 -->
          <div class="toolbar">
            <button @click.stop="toggleEmojiPicker" class="tool-btn" title="表情">
              <i class="mdi mdi-emoticon-outline"></i>
            </button>
            <button class="tool-btn" title="发送文件"><i class="mdi mdi-folder-outline"></i></button>
            <button class="tool-btn" title="截图"><i class="mdi mdi-scissors-cutting"></i></button>
            <button class="tool-btn" title="聊天记录"><i class="mdi mdi-message-text-outline"></i></button>
          </div>

          <!-- 文本输入框 -->
          <textarea
                v-model="newMessage"
                placeholder="输入消息..."
                ref="textareaRef"
                @keyup.enter.prevent.exact="sendMessage"
              ></textarea>

          <!-- 发送按钮区域 -->
          <div class="send-action">
            <button
              @click="sendMessage"
              :disabled="isSending || !newMessage.trim()"
              class="send-btn"
            >
              {{ isSending ? '发送中...' : '发送' }}
            </button>
          </div>
        </div>
      </div>
      
      <!-- 当没有聊天被选中时显示 -->
      <div v-else class="placeholder">
        <p>开启你的聊天吧</p>
      </div>
    </div>

    <CreateGroupChatModal 
      v-if="isModalVisible" 
      @close="closeCreateGroupModal"
      @group-created="handleGroupCreated"
    />
    <ContextMenu
      :visible="contextMenu.visible"
      :x="contextMenu.x"
      :y="contextMenu.y"
      :options="contextMenu.options"
      @close="hideContextMenu"
      @select="handleMenuAction"
    />
    <ForwardModal
      v-if="forwardModal.visible"
      :conversations="conversations"
      :message-to-forward="forwardModal.message"
      @close="closeForwardModal"
      @forward="executeForward"
    />
    <ConfirmModal
      :visible="deleteModal.visible"
      title="删除聊天"
      message="删除后，将清空该聊天的消息记录，确定删除吗？"
      @confirm="confirmDelete"
      @cancel="cancelDelete"
    />
    <ChatDetailsSidebar 
      :visible="isDetailsSidebarVisible"
      :chat="selectedChat"
      @close="closeDetailsSidebar"
      @chat-updated="handleChatUpdate"
      @conversation-left="handleConversationLeft"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, onUnmounted, nextTick } from 'vue';
import CreateGroupChatModal from '../components/CreateGroupChatModal.vue';
import { webSocketService } from '../service/WebSocketService';
import ContextMenu from '../components/ContextMenu.vue'; 
import ConfirmModal from '../components/ConfirmModal.vue'; 
import ForwardModal from '../components/ForwardModal.vue';
import EmojiPicker from '../components/EmojiPicker.vue';
import apiClient from '@/api/apiClient';
import type { MessageDto, ConversationSummary } from '@/types/api';
import { useAuthStore } from '@/stores/auth'; 
import ChatDetailsSidebar from '../components/ChatDetailsSidebar.vue';
import { useChatStore } from '@/stores/chat'; // 1. 引入 chatStore
import { storeToRefs } from 'pinia'; // 2. 引入 storeToRefs 以保持响应性

const authStore = useAuthStore(); 
const chatStore = useChatStore();
const { conversations } = storeToRefs(chatStore);
const selectedChat = ref<ConversationSummary | null>(null);
const messages = ref<MessageDto[]>([]); 
const messagesContainer = ref<HTMLElement | null>(null);
const textareaRef = ref<HTMLTextAreaElement | null>(null);

const isLoading = ref<boolean>(false);
const isMessagesLoading = ref(false);
const isModalVisible = ref(false);
const isEmojiPickerVisible = ref(false);
const newMessage = ref('');
const isSending = ref(false);

const recalledMessageForEdit = ref<MessageDto | null>(null);
const isDetailsSidebarVisible = ref(false);
// (新增) 用于跟踪正在获取的隐藏会话ID，防止重复请求
const fetchingConversationIds = ref<Set<number>>(new Set());
// (核心新增) 切换/关闭侧边栏的方法
const toggleDetailsSidebar = () => {
  isDetailsSidebarVisible.value = !isDetailsSidebarVisible.value;
};
const closeDetailsSidebar = () => {
  isDetailsSidebarVisible.value = false;
};

/**
 * (核心新增) 切换 Emoji 选择器的显示/隐藏
 */
const toggleEmojiPicker = () => {
  isEmojiPickerVisible.value = !isEmojiPickerVisible.value;
};

const handleConversationLeft = (conversationId) => {
  // 从聊天列表中移除该群聊
  conversations.value = conversations.value.filter(c => c.conversationId !== conversationId);
  // 如果当前选中的是这个群聊，清空选中状态
  if (selectedChat.value?.conversationId === conversationId) {
    selectedChat.value = null;
  }

  webSocketService.unsubscribe(conversationId);
};

const handleChatUpdate = (updatedChat: ConversationSummary) => {
  // 1. 在 conversations 数组中找到需要更新的会话
  const index = conversations.value.findIndex(c => c.conversationId === updatedChat.conversationId);
  
  if (index !== -1) {
    // 2. 用新的对象替换旧的对象，触发响应式更新
    conversations.value[index] = updatedChat;

    // 3. 如果当前选中的就是这个聊天，也更新 selectedChat
    if (selectedChat.value?.conversationId === updatedChat.conversationId) {
        selectedChat.value = updatedChat;
    }

    // 4. (重要) 更新后重新排序列表，确保置顶的聊天能显示在最上面
    sortConversations();
  }
};

/**
 * (核心新增) 关闭 Emoji 选择器
 */
const closeEmojiPicker = () => {
  if (isEmojiPickerVisible.value) {
    isEmojiPickerVisible.value = false;
  }
};

/**
 * (核心新增) 处理从 EmojiPicker 组件传来的 select 事件
 * @param emoji - 用户点击的表情字符
 */
const handleEmojiSelect = (emoji: string) => {
  const textarea = textareaRef.value;
  if (!textarea) return;

  // 获取当前光标位置
  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;

  // 将表情插入到光标位置
  newMessage.value = 
    newMessage.value.substring(0, start) + 
    emoji + 
    newMessage.value.substring(end);

  // 更新光标位置到插入表情之后
  // 使用 nextTick 确保 DOM 更新后再移动光标
  nextTick(() => {
    textarea.selectionStart = textarea.selectionEnd = start + emoji.length;
    textarea.focus();
  });
};
/**
 * (核心新增) 检查是否应该显示时间分割线
 * @param index - 当前消息在数组中的索引
 */
const shouldShowTimestamp = (index: number): boolean => {
  // 第一条消息，总是显示时间
  if (index === 0) {
    return true;
  }

  const currentMessage = messages.value[index];
  const previousMessage = messages.value[index - 1];
  
  const currentTime = new Date(currentMessage.timestamp).getTime();
  const previousTime = new Date(previousMessage.timestamp).getTime();

  // 5分钟的毫秒数 = 5 * 60 * 1000 = 300000
  const fiveMinutes = 300000;

  // 如果两条消息的时间差大于5分钟，则显示时间
  return (currentTime - previousTime) > fiveMinutes;
};

/**
 * (核心新增) 格式化消息列表中的详细时间戳
 * @param timestamp - ISO 格式的时间字符串
 */
const formatDetailedTimestamp = (timestamp: string): string => {
  if (!timestamp) return '';
  const messageDate = new Date(timestamp);
  const now = new Date();
  
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);

  const hours = messageDate.getHours().toString().padStart(2, '0');
  const minutes = messageDate.getMinutes().toString().padStart(2, '0');
  const timeStr = `${hours}:${minutes}`;

  // 如果是今天
  if (messageDate >= today) {
    return timeStr;
  }
  // 如果是昨天
  if (messageDate >= yesterday) {
    return `昨天 ${timeStr}`;
  }
  // 如果是今年，但比昨天更早
  if (messageDate.getFullYear() === now.getFullYear()) {
    const month = (messageDate.getMonth() + 1).toString();
    const day = messageDate.getDate().toString();
    return `${month}月${day}日 ${timeStr}`;
  }
  // 如果是往年
  const year = messageDate.getFullYear();
  const month = (messageDate.getMonth() + 1).toString();
  const day = messageDate.getDate().toString();
  return `${year}年${month}月${day}日 ${timeStr}`;
};

// (核心修改) 将 contextMenu 用于两种不同场景
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  target: null as ConversationSummary | MessageDto | null,
  type: 'conversation' as 'conversation' | 'message', // 标记菜单类型
  options: [] as any[],
});

/**
 * (核心新增) 监听 messages 数组的变化
 * 每当消息列表被更新（无论是加载历史记录还是接收新消息），
 * 都会自动执行滚动到底部的操作。这是唯一的滚动触发点。
 */
watch(messages, () => {
  scrollToBottom('auto'); // 默认立即滚动
}, { deep: true });

watch(selectedChat, (newChat) => {
  if (newChat) {
    // 职责单一化：只负责获取历史消息
    fetchMessages(newChat.conversationId);
  } else {
    // 如果没有选中聊天（例如删除了当前聊天），则清空消息列表
    messages.value = [];
  }
}, { deep: true });

const updateLocalStorageConversation = (updatedConvo: ConversationSummary) => {
  const cachedConversations = JSON.parse(localStorage.getItem('conversations') || '[]');
  const index = cachedConversations.findIndex((c: ConversationSummary) => c.conversationId === updatedConvo.conversationId);
  
  if (index !== -1) {
    cachedConversations[index] = updatedConvo;
    localStorage.setItem('conversations', JSON.stringify(cachedConversations));
  }
};

/**
 * (核心新增) 辅助函数，用于更新左侧会话列表的最后一条消息
 */
const updateConversationSummary = (newMessage: MessageDto) => {
  const conversationIndex = conversations.value.findIndex(c => c.conversationId === newMessage.conversationId);
  if (conversationIndex !== -1) {
    conversations.value[conversationIndex].lastMessageContent = newMessage.content;
    conversations.value[conversationIndex].lastMessageTimestamp = newMessage.timestamp;
    
    // 更新本地存储
    updateLocalStorageConversation(conversations.value[conversationIndex]);
    
    // 将更新后的会话移到顶部（如果需要）
    sortConversations();
  }
};

// (核心新增) 删除确认弹窗状态
const deleteModal = reactive({
  visible: false,
  targetId: null as number | null,
});

// (新增) 转发和引用状态
const forwardModal = reactive({
  visible: false,
  message: null as MessageDto | null,
});
const quotedMessage = ref<MessageDto | null>(null);

// --- 方法 ---

const hideContextMenu = () => {
  contextMenu.visible = false;
};

// (核心修改) 重命名旧的右键菜单方法
const showConversationContextMenu = (convo: ConversationSummary, event: MouseEvent) => {
  contextMenu.target = convo;
  contextMenu.type = 'conversation';
  contextMenu.x = event.clientX;
  contextMenu.y = event.clientY;
  contextMenu.options = [
    { label: convo.isPinned ? '取消置顶' : '置顶聊天', action: 'pin' },
    { label: convo.areNotificationsMuted ? '允许消息通知' : '消息免打扰', action: 'mute' },
    { label: '删除', action: 'delete', danger: true },
  ];
  contextMenu.visible = true;
};

// (新增) 显示消息右键菜单的方法
const showMessageContextMenu = (msg: MessageDto, event: MouseEvent) => {
  contextMenu.target = msg;
  contextMenu.type = 'message';
  contextMenu.x = event.clientX;
  contextMenu.y = event.clientY;
  contextMenu.options = [
    { label: '复制', action: 'copy' },
    { label: '转发', action: 'forward' },
    { label: '引用', action: 'quote' },
  ];
  // (核心修改) 只有自己发送的消息才能撤回
  if (msg.sender.id === authStore.userInfo?.id) {
    // 假设2分钟内可撤回
    const twoMinutes = 2 * 60 * 1000;
    if (new Date().getTime() - new Date(msg.timestamp).getTime() < twoMinutes) {
      contextMenu.options.push({ label: '撤回', action: 'recall', danger: true });
    }
  }
  contextMenu.visible = true;
};

// (核心新增) 撤回消息的方法
const recallMessage = async (msg: MessageDto) => {
  try {
    await apiClient.delete(`/api/conversations/${msg.conversationId}/messages/${msg.id}`);
    // API 成功后，不需要立即在前端做任何事，等待WebSocket通知
    // 存储这条消息以便“重新编辑”
    recalledMessageForEdit.value = msg;
  } catch (error: any) {
    console.error('Failed to recall message:', error);
    alert(error.response?.data || '撤回失败，可能已超过2分钟。');
  }
};

// (核心新增) 重新编辑撤回的消息
const reEditRecalledMessage = () => {
  if (recalledMessageForEdit.value) {
    newMessage.value = recalledMessageForEdit.value.content;
    recalledMessageForEdit.value = null; // 清除提示
    textareaRef.value?.focus(); // 聚焦到输入框
  }
};

const handleMenuAction = async (action: string) => {
  const target = contextMenu.target;
  if (!target) return;

  // 根据菜单类型分发操作
  if (contextMenu.type === 'conversation') {
    handleConversationMenuAction(action, target as ConversationSummary);
  } else {
    handleMessageMenuAction(action, target as MessageDto);
  }
};

const handleConversationMenuAction = async (action: string, target: ConversationSummary) => {
  if (!target) return;

  try {
    switch (action) {
      case 'pin':
        await apiClient.post(`/api/conversations/${target.conversationId}/pin`);
        target.isPinned = !target.isPinned;
        // (可选) 置顶后重新排序列表
        sortConversations();
        break;
      case 'mute':
        await apiClient.post(`/api/conversations/${target.conversationId}/mute`);
        target.areNotificationsMuted = !target.areNotificationsMuted;
        break;
      case 'delete':
        deleteModal.targetId = target.conversationId;
        deleteModal.visible = true;
        break;
    }
  } catch (error) {
    console.error(`Action '${action}' failed:`, error);
    alert('操作失败，请稍后重试。');
  }
};

const handleMessageMenuAction = async (action: string, msg: MessageDto) => {
  switch (action) {
    case 'copy':
      navigator.clipboard.writeText(msg.content).catch(err => console.error('Copy failed:', err));
      break;
    case 'forward':
      forwardModal.message = msg;
      forwardModal.visible = true;
      break;
    case 'quote':
      quotedMessage.value = msg;
      break;
    case 'recall':
      await recallMessage(msg);
      break;
  }
};

const closeForwardModal = () => { forwardModal.visible = false; };

const executeForward = async (payload: { targetConversationIds: number[], attachedMessage: string }) => {
  try {
    await apiClient.post('/api/messages/forward', {
      originalMessageId: forwardModal.message!.id,
      ...payload
    });
    alert('转发成功！');
  } catch (error) {
    console.error('Forward failed:', error);
    alert('转发失败。');
  } finally {
    closeForwardModal();
  }
};

const cancelQuote = () => { quotedMessage.value = null; };

const confirmDelete = async () => {
  const id = deleteModal.targetId;
  if (!id) return;
  try {
    await apiClient.delete(`/api/home/conversations/${id}`);
    // 从前端列表中移除
    conversations.value = conversations.value.filter(c => c.conversationId !== id);
    localStorage.setItem('conversations', JSON.stringify(conversations.value));
    if (selectedChat.value?.conversationId === id) {
      selectedChat.value = null;
    }
  } catch (error) {
    console.error('Delete failed:', error);
    alert('删除失败，请稍后重试。');
  } finally {
    cancelDelete();
  }
};

const cancelDelete = () => {
  deleteModal.visible = false;
  deleteModal.targetId = null;
};

const sortConversations = () => {
  conversations.value.sort((a, b) => {
    // 置顶的排在前面
    if (a.isPinned !== b.isPinned) {
      return a.isPinned ? -1 : 1;
    }
    
    // 处理时间为null的情况 - 将null时间视为很早的时间
    const timeA = a.lastMessageTimestamp ? new Date(a.lastMessageTimestamp).getTime() : 0;
    const timeB = b.lastMessageTimestamp ? new Date(b.lastMessageTimestamp).getTime() : 0;
    
    // 然后按最后消息时间降序
    return timeB - timeA;
  });
};

const fetchMessages = async (conversationId: number) => {
  if (isMessagesLoading.value) return;
  isMessagesLoading.value = true;
  messages.value = []; // 清空旧消息

  try {
    const response = await apiClient.get<MessageDto[]>(`/api/conversations/${conversationId}/messages`);
    messages.value = response.data;
  } catch (error) {
    console.error(`Failed to fetch messages for conversation ${conversationId}:`, error);
    alert('历史消息加载失败。');
  } finally {
    isMessagesLoading.value = false;
  }
};

const scrollToBottom = (behavior: 'auto' | 'smooth' = 'auto') => {
  // 使用 nextTick 确保在DOM更新后的下一帧执行滚动
  nextTick(() => {
    const container = messagesContainer.value;
    if (container) {
      container.scrollTo({
        top: container.scrollHeight,
        behavior: behavior,
      });
    }
  });
};

const sendMessage = async () => {
  if (!selectedChat.value || !newMessage.value.trim() || isSending.value) {
    return;
  }

  isSending.value = true;
  const contentToSend = newMessage.value;
  newMessage.value = '';
  
  // 1. 乐观更新UI：立即将消息添加到本地列表
  // 注意：这只是一个临时消息，没有真实的ID和时间戳
  const tempMessage: MessageDto = {
    id: Date.now(), // 临时ID
    conversationId: selectedChat.value.conversationId,
    content: contentToSend,
    timestamp: new Date().toISOString(),
    sender: { // 使用当前登录用户的信息
      id: authStore.userInfo!.id,
      nickname: authStore.userInfo!.nickname,
      avatarUrl: authStore.userInfo!.avatarUrl,
    },
    // (核心修改) 乐观更新时也带上引用信息
    repliedMessage: quotedMessage.value ? {
      messageId: quotedMessage.value.id,
      senderNickname: quotedMessage.value.sender.nickname,
      content: quotedMessage.value.content,
    } : undefined,
  };
  
  try {
    // (核心修改) 创建一个与后端 MessageRequest DTO 结构完全匹配的对象
    const requestBody = {
      content: contentToSend,
      replyToMessageId: quotedMessage.value ? quotedMessage.value.id : null,
    };

    // 调用后端API，并发送结构完整的 requestBody
    const response = await apiClient.post<MessageDto>(
      `/api/conversations/${selectedChat.value.conversationId}/messages`,
      requestBody 
    );
    
    // 用后端返回的真实消息替换掉我们的临时消息 (无变化)
    const realMessage = response.data;
    const tempMessageIndex = messages.value.findIndex(m => m.id === tempMessage.id);
    if (tempMessageIndex !== -1) {
      messages.value[tempMessageIndex] = realMessage;
    }

    scrollToBottom('smooth');

    const newConversationsStr = localStorage.getItem('conversations');
    const newConversations = newConversationsStr
      ? (JSON.parse(newConversationsStr) as ConversationSummary[])
      : [];
    const conversationIndex = newConversations.findIndex(
      (convo: ConversationSummary) => selectedChat.value && convo.conversationId === selectedChat.value.conversationId
    );
    if (conversationIndex !== -1) {
      newConversations[conversationIndex].lastMessageContent = realMessage.content;
      newConversations[conversationIndex].lastMessageTimestamp = new Date().toISOString();
      localStorage.setItem('conversations', JSON.stringify(newConversations));
    }

    quotedMessage.value = null;

  } catch (error) {
    console.error('Failed to send message:', error);

    // (核心修改) 发送失败时，调用状态检查API
    if (selectedChat.value) {
      try {
        const response = await apiClient.get<{ status: string }>(
          `/api/conversations/${selectedChat.value.conversationId}/status`
        );
        const status = response.data.status;

        // 根据返回的状态给出明确提示
        switch (status) {
          case 'MUTED':
            alert('您已被禁言，无法发送消息。');
            break;
          case 'NOT_A_MEMBER':
            alert('您已不是该群成员，无法发送消息。');
            // (可选) 可以在此处将该聊天从列表中移除
            break;
          case 'BLOCKED_FROM_GROUP':
            alert('您已被该群聊拉黑，无法发送消息。');
            break;
          case 'FRIENDSHIP_TERMINATED':
            alert('对方已将您从好友列表中删除，消息无法发送。');
            break;
          default:
            alert('消息发送失败，请检查网络或稍后重试。');
        }
      } catch (statusError) {
        console.error('Failed to check conversation status:', statusError);
        alert('消息发送失败，请稍后重试。');
      }
    }
    
  } finally {
    isSending.value = false;
    textareaRef.value?.focus();
  }
};

const formatTimestamp = (timestamp: string): string => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);

  // 如果是今天，只显示时间
  if (date.toDateString() === today.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  // 如果是昨天
  if (date.toDateString() === yesterday.toDateString()) {
    return '昨天';
  }
  // 更早则显示日期
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
};

const fetchConversations = async () => {
  isLoading.value = true;
  try {
    const response = await apiClient.get<ConversationSummary[]>('/api/home/conversations');
    // 调用 action 来设置会话列表
    chatStore.setConversations(response.data);
    subscribeToAllConversations();
  } catch (error) {
    console.error('Failed to fetch conversations:', error);
  } finally {
    isLoading.value = false;
  }
};

const selectChat = async (chat: ConversationSummary) => {
  selectedChat.value = chat;
  
  // 如果该会话有未读消息，立即清零
  if (chat.unreadCount > 0) {
    chatStore.clearUnreadCount(chat.conversationId);
  }
};
 
// 新增处理群聊邀请的方法
const handleGroupInvitation = async (event: CustomEvent) => {
  const invitation = event.detail;
  console.log('处理群聊邀请:', invitation);
  
  // 刷新会话列表
  await fetchConversations();
  
  // 查找新加入的群聊并订阅消息
  const newGroupChat = conversations.value.find(
    conv => conv.type === 'GROUP' && conv.uuid === invitation.groupUuid
  );
  
  if (newGroupChat) {
    console.log('New group chat found, selecting and subscribing:', newGroupChat.name);
    // Subscribe to the new group chat's messages
    webSocketService.subscribe(newGroupChat.conversationId, (newMessage: MessageDto) => {
      // You can reuse your existing logic for handling new messages
      if (selectedChat.value?.conversationId === newMessage.conversationId) {
        if (!messages.value.some(m => m.id === newMessage.id)) {
          messages.value.push(newMessage);
        }
      }
      updateConversationSummary(newMessage);
    });
    
    // Automatically select the new chat for the user
    selectChat(newGroupChat);
  } else {
    console.warn('Could not find the newly joined group chat in the list after fetching.');
  }
};

const openCreateGroupModal = () => { isModalVisible.value = true; };
const closeCreateGroupModal = () => { isModalVisible.value = false; };

const handleGroupCreated = (newGroup: ConversationSummary) => {
  // 当新群聊创建成功后，不只是在前端添加，而是重新获取整个列表以保证数据同步
  console.log('新群聊已创建，刷新列表:', newGroup);
  fetchConversations();
  closeCreateGroupModal();
};

const handleIncomingMessage = (newMessage: MessageDto) => {
  const conversationId = newMessage.conversationId;

  // 更新当前打开的聊天窗口（不变）
  if (selectedChat.value?.conversationId === conversationId) {
    if (!messages.value.some(m => m.id === newMessage.id)) {
      messages.value.push(newMessage);
    }
  }

  // 检查会话是否存在于 store 中
  const conversationExists = chatStore.conversations.some(c => c.conversationId === conversationId);

  if (conversationExists) {
    // 调用 action 更新最后一条消息
    chatStore.updateConversationSummary(conversationId, newMessage.content, newMessage.timestamp);
    // 如果聊天未打开，调用 action 增加未读数
    if (selectedChat.value?.conversationId !== conversationId) {
      chatStore.incrementUnreadCount(conversationId);
    }
  } else {
    // 如果会话不存在 (刚被删除)，则获取并添加回 store
    fetchAndReAddConversation(newMessage);
  }
};

const fetchAndReAddConversation = async (newMessage: MessageDto) => {
  const conversationId = newMessage.conversationId;
  fetchingConversationIds.value.add(conversationId);

  try {
    // 使用我们之前创建的API来获取单个会话的摘要信息
    const response = await apiClient.get<ConversationSummary>(`/api/home/conversations/${conversationId}`);
    const newConversation = response.data;

    // 将新会话信息与触发它的新消息同步
    newConversation.lastMessageContent = newMessage.content;
    newConversation.lastMessageTimestamp = newMessage.timestamp;
    newConversation.unreadCount = 1; // 既然是新消息触发的，未读数至少为1

    // 将其添加回会话列表
    conversations.value.push(newConversation);
    
    // 重新排序，使其根据最新消息时间显示在顶部
    sortConversations();
    
    // 更新本地存储
    chatStore.addOrUpdateConversation(newConversation);
  } catch (error) {
    console.error(`无法获取会话 ${conversationId} 的信息:`, error);
  } finally {
    // 无论成功与否，都要从请求集合中移除ID，以便下次可以重新请求
    fetchingConversationIds.value.delete(conversationId);
  }
};

const handleIncomingRecall = (notification: { messageId: number }) => {
  console.log('Received global recall notification:', notification);
  const { messageId } = notification;
  
  // 仅当被撤回的消息存在于当前打开的聊天窗口时，才需要更新UI
  const messageToRecall = messages.value.find(m => m.id === messageId);
  if (messageToRecall) {
    messageToRecall.recalled = true;
    console.log(`Message ${messageId} has been marked as recalled.`);
  }
};

const subscribeToAllConversations = () => {
  console.log(`准备订阅 ${chatStore.conversations.length} 个会话...`);
  // 直接从 store 获取 conversations
  chatStore.conversations.forEach(convo => {
    webSocketService.subscribe(convo.conversationId, handleIncomingMessage);
    webSocketService.subscribeToRecalls(convo.conversationId, handleIncomingRecall);
  });
};

// --- 生命周期钩子 ---
onMounted(() => {
  // 1. 尝试从缓存加载数据
  webSocketService.connect();
  window.addEventListener('click', closeEmojiPicker);
  // 监听全局的群聊邀请事件
  window.addEventListener('group-invitation-received', handleGroupInvitation as unknown as EventListener);
  fetchConversations();
});

onUnmounted(() => {
  // webSocketService.disconnect();
  window.removeEventListener('click', closeEmojiPicker);
  // window.removeEventListener('group-invitation-received', handleGroupInvitation as unknown as EventListener);
});
</script>

<style scoped lang="scss">
.recalled-message-tip, .recalled-edit-tip {
  text-align: center;
  margin: 10px 0;
  color: #999;
  font-size: 12px;
}

.re-edit-btn {
  color: #576b95; /* 微信风格的蓝色链接 */
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  margin-left: 5px;
  &:hover {
    text-decoration: underline;
  }
}
/* (核心新增) 时间分割线样式 */
.time-divider {
  text-align: center;
  margin: 10px 0 20px; /* 上下边距 */

  span {
    background-color: #dadada; /* 微信风格的灰色背景 */
    color: #fff;
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 4px;
  }
}
.quote-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #f0f0f0;
  padding: 8px 13px;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
  .quote-content p { margin: 2px 0 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 300px; }
  .cancel-quote-btn { background: none; border: none; font-size: 18px; cursor: pointer; color: #999; }
}
.message-wrapper {
  display: flex;
  margin-bottom: 15px;
  align-items: flex-start;

  &.sent {
    justify-content: flex-end; // 我发送的消息靠右
    .message-content {
      align-items: flex-end;
    }
    .message-bubble {
      background-color: #95ec69; // 微信绿色气泡
    }
  }

  &.received {
    justify-content: flex-start; // 接收的消息靠左
    .message-content {
      align-items: flex-start;
    }
    .message-bubble {
      background-color: #ffffff; // 白色气泡
    }
  }
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  flex-shrink: 0;
}

.message-content {
  display: flex;
  flex-direction: column;
  margin: 0 10px;
  max-width: 65%; // 限制消息最大宽度
}

.sender-name {
  font-size: 12px;
  color: #888;
  margin-bottom: 4px;
}

.message-bubble {
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.5;
  overflow-wrap: break-word; /* 优先使用这个标准属性，处理长单词 */
  word-break: break-word; // 保持换行和空格
  display: inline-block;
  text-align: left;
}
// .main-content{
//   height: 20px;
// }
.reply-quote {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 8px;
  margin: -4px -6px 8px -6px; /* 调整内边距使其看起来更融合 */
  border-radius: 4px;
  border-left: 3px solid rgba(0, 0, 0, 0.2);
  font-size: 13px;
  
  .reply-sender {
    font-weight: 500;
    color: #555;
  }
  
  .reply-content {
    color: #777;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-top: 2px;
  }
}
.chat-input-area {
  height: 180px; // 增加高度以容纳工具栏
  border-top: 1px solid #e7e7e7;
  flex-shrink: 0;
  padding: 0 20px; // 调整内边距
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
  position: relative;

  .toolbar {
    display: flex;
    align-items: center;
    padding: 10px 0;
    
    .tool-btn {
      background: none;
      border: none;
      cursor: pointer;
      font-size: 20px;
      color: #666;
      padding: 5px;
      margin-right: 12px;
      
      &:hover {
        color: #000;
      }
    }
  }

  textarea {
    min-height: 13px;
    flex-grow: 1;
    border: none;
    resize: none;
    background: transparent;
    font-size: 16px;
    font-family: inherit;
    padding: 5px 0;
    
    &:focus {
      outline: none;
    }
  }
  
  .send-action {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    padding: 10px 0;

    .send-hint {
        font-size: 12px;
        color: #aaa;
        margin-right: 15px;
    }

    .send-btn {
      padding: 6px 24px;
      border: 1px solid #e0e0e0;
      background-color: #fff;
      color: #000;
      border-radius: 4px;
      cursor: pointer;
      transition: background-color 0.2s;
      
      &:hover:not(:disabled) {
        background-color: #07C160;
        color: #fff;
        border-color: #07C160;
      }
      &:disabled {
        background-color: #f5f5f5;
        color: #aaa;
        cursor: not-allowed;
      }
    }
  }
}
.avatar-container {
  position: relative;
  margin-right: 12px;
}
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
}
/* (核心新增) 未读消息红点的完整样式，参考微信风格 */
.unread-badge {
  position: absolute;
  top: -5px;      /* 向上偏移，使其部分超出头像 */
  right: -5px;     /* 向右偏移 */
  background-color: #fa3e3e; /* 微信红点颜色 */
  color: white;
  border-radius: 50%; /* 保证是圆形 */
  padding: 2px 6px;   /* 内边距，让数字不贴边 */
  font-size: 10px;
  font-weight: bold;
  line-height: 1;     /* 确保数字垂直居中 */
  min-width: 18px;    /* 即使是1位数，也保持最小宽度，使其更圆 */
  text-align: center;
  box-sizing: border-box; /* 让 padding 和 border 不会增加元素的总宽度 */
  border: 1px solid #e6e6e6; /* (可选) 添加一个与背景色相同的边框，使其看起来更分离 */
}

/* (新增) 加载和空状态的占位符样式 */
.loading-placeholder, .empty-placeholder {
  padding: 40px 20px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.chat-view {
  display: flex;
  width: 100%;
  height: 100%;
  background-color: #f5f5f5; /* 微信PC版的淡灰色背景 */
  position: relative; // 父容器需要相对定位
  overflow: hidden; // 防止布局移动时出现滚动条
}

// 左侧面板
.chat-list-panel {
  width: 260px;
  flex-shrink: 0;
  background-color: #e6e6e6; // 左栏稍深的背景
  border-right: 1px solid #dcdcdc;
  display: flex;
  flex-direction: column;
}

.list-header {
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-bar {
  flex-grow: 1;
  position: relative;
  
  i {
    position: absolute;
    left: 8px;
    top: 50%;
    transform: translateY(-50%);
    color: #aaa;
    font-size: 16px;
  }

  input {
    width: 100%;
    height: 32px;
    border-radius: 4px;
    border: none;
    background-color: #dddbdb;
    padding-left: 30px;
    box-sizing: border-box;
    &:focus {
      outline: none;
      background-color: #fff;
    }
  }
}

.add-chat-btn {
  width: 32px;
  height: 32px;
  border: none;
  background-color: #dddbdb;
  border-radius: 4px;
  cursor: pointer;
  font-size: 20px;
  color: #555;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background-color: #d1d1d1;
  }
}

.chat-list {
  flex-grow: 1;
  overflow-y: auto; // 当列表过长时显示滚动条
}

.chat-item {
  display: flex;
  padding: 12px;
  cursor: pointer;
  
  &:hover {
    background-color: #dcdcdc;
  }

  &.active {
    background-color: #c9c9c9;
  }

  .avatar {
    width: 40px;
    height: 40px;
    border-radius: 4px;
    margin-right: 12px;
  }

  .chat-info {
    flex-grow: 1;
    overflow: hidden;

    .info-top {
      display: flex;
      justify-content: space-between;
      .name {
        font-weight: 500;
        color: #000;
      }
      .time {
        font-size: 12px;
        color: #aaa;
      }
    }

    .last-message {
      font-size: 12px;
      color: #999;
      margin: 4px 0 0;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}

// 右侧面板
.conversation-panel {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  width: 100%;

  &.sidebar-open {
    // (核心新增) 当侧边栏打开时，主聊天区的宽度减少
    width: calc(100% - 280px); 
  }

  .placeholder {
    margin: auto;
    color: #999;
    text-align: center;
  }

  .chat-window {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  .chat-header {
    height: 56px;
    line-height: 56px;
    padding: 0 20px;
    font-size: 16px;
    font-weight: bold;
    border-bottom: 1px solid #e7e7e7;
    flex-shrink: 0;
    display: flex;
    justify-content: space-between; 
    align-items: center;

    .chat-title {
      font-size: 16px;
      font-weight: bold;
    }

    .details-btn {
      background: none;
      border: none;
      cursor: pointer;
      font-size: 24px;
      color: #666;
      padding: 5px;
      border-radius: 50%;
      
      &:hover {
        background-color: #e0e0e0;
        color: #000;
      }
    }
  }

  .chat-messages {
    flex-grow: 1;
    padding: 20px;
    overflow-y: auto;
  }
  
  .chat-input-area {
    height: 160px;
    border-top: 1px solid #e7e7e7;
    flex-shrink: 0;
    padding: 10px 20px;
    display: flex;
    flex-direction: column;

    textarea {
      flex-grow: 1;
      border: none;
      resize: none;
      background: transparent;
      font-size: 16px;
      &:focus {
        outline: none;
      }
    }
    
    button {
      align-self: flex-end;
      padding: 6px 20px;
      border: 1px solid #e7e7e7;
      background-color: #f5f5f5;
      cursor: pointer;
      &:hover {
        background-color: #dcdcdc;
      }
    }
  }
}
</style>