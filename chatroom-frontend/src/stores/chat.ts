import { defineStore } from 'pinia';
import type { ConversationSummary } from '@/types/api';
import apiClient from '@/api/apiClient';
import { parseFileContent, isImageFile } from '@/utils/fileUtils'; 
import type { MessageDto } from '@/types/api';

export const useChatStore = defineStore('chat', {
  // 1. State: 存储所有会话列表
  state: () => ({
    conversations: [] as ConversationSummary[],
  }),

  // 2. Getters: 计算派生状态，例如未读总数
  getters: {
    /**
     * 计算所有会话的未读消息总数。
     * 这是一个计算属性，当 conversations 数组变化时，它会自动重新计算。
     */
    totalUnreadCount(state): number {
      return state.conversations.reduce((sum, convo) => sum + (convo.unreadCount || 0), 0);
    },
  },

  // 3. Actions: 定义修改 state 的方法
  actions: {
    /**
     * 从后端获取数据后，设置整个会话列表。
     */
    setConversations(conversations: ConversationSummary[]) {
      this.conversations = conversations;
      this.sortConversations();
    },
    
    /**
     * 当收到一个已删除/隐藏会话的消息时，获取其信息并添加回列表。
     */
    addOrUpdateConversation(conversation: ConversationSummary) {
        const index = this.conversations.findIndex(c => c.conversationId === conversation.conversationId);
        if (index !== -1) {
            this.conversations[index] = conversation;
        } else {
            this.conversations.push(conversation);
        }
        this.sortConversations();
    },

    updateConversationSummary(message: MessageDto) {
      const index = this.conversations.findIndex(c => c.conversationId === message.conversationId);
      if (index !== -1) {
        const convo = this.conversations[index];
        convo.lastMessageTimestamp = message.timestamp;

        // 根据消息类型格式化摘要内容
        switch (message.messageType) {
          case 'file':
            try {
              const fileInfo = parseFileContent(message.content);
              if (isImageFile(fileInfo.name)) {
                convo.lastMessageContent = '[图片]';
              } else {
                convo.lastMessageContent = `[文件] ${fileInfo.name}`;
              }
            } catch (e) {
              convo.lastMessageContent = '[文件]';
            }
            break;
          
          // emoji和text现在统一处理
          case 'text':
          case 'emoji':
          default:
            convo.lastMessageContent = message.content;
            break;
        }
        
        this.sortConversations();
      }
    },

    /**
     * 为指定会话增加未读计数。
     */
    incrementUnreadCount(conversationId: number) {
      const index = this.conversations.findIndex(c => c.conversationId === conversationId);
      if (index !== -1) {
        this.conversations[index].unreadCount = (this.conversations[index].unreadCount || 0) + 1;
      }
    },

    /**
     * 清除指定会话的未读计数，并通知后端。
     */
    async clearUnreadCount(conversationId: number) {
      const index = this.conversations.findIndex(c => c.conversationId === conversationId);
      if (index !== -1 && this.conversations[index].unreadCount > 0) {
        this.conversations[index].unreadCount = 0;
        try {
          // 同时通知后端，将该会话标记为已读
          await apiClient.post(`/api/conversations/${conversationId}/read`);
        } catch (error) {
          console.error('Failed to mark conversation as read on backend:', error);
          // 可以在此添加错误处理，例如回滚未读状态
        }
      }
    },
    
    /**
     * 清理会话列表，用于退出登录等场景。
     */
    clearChatState() {
      this.conversations = [];
    },

    /**
     * 对会话列表进行排序（置顶 > 最新消息时间）。
     */
    sortConversations() {
      this.conversations.sort((a, b) => {
        if (a.isPinned !== b.isPinned) {
          return a.isPinned ? -1 : 1;
        }
        const timeA = a.lastMessageTimestamp ? new Date(a.lastMessageTimestamp).getTime() : 0;
        const timeB = b.lastMessageTimestamp ? new Date(b.lastMessageTimestamp).getTime() : 0;
        return timeB - timeA;
      });
    },
  },
});