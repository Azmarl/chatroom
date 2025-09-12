<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="left-panel">
        <div class="search-bar">
          <input type="text" placeholder="搜索" v-model="searchTerm">
        </div>
        <div class="list-container">
          <div v-for="convo in filteredConversations" :key="convo.conversationId" class="list-item">
            <label>
              <input type="checkbox" :value="convo.conversationId" v-model="selectedConversationIds">
              <img :src="convo.avatarUrl" class="avatar">
              <span>{{ convo.name }}</span>
            </label>
          </div>
        </div>
      </div>
      <div class="right-panel">
        <h4>发送给</h4>
        <div class="selected-list">
          <div v-if="selectedConversations.length === 0" class="empty-text">
            请从左侧选择
          </div>
          <div v-for="convo in selectedConversations" :key="convo.conversationId" class="selected-item">
            <img :src="convo.avatarUrl" class="avatar">
            <span>{{ convo.name }}</span>
            <button @click="deselectConversation(convo.conversationId)" class="remove-btn">&times;</button>
          </div>
        </div>
        <div class="forward-preview">
          <p>{{ messageToForward?.content }}</p>
        </div>
        <textarea v-model="attachedMessage" placeholder="给朋友留言"></textarea>
        <div class="actions">
          <button @click="$emit('close')" class="btn-cancel">取消</button>
          <button @click="handleForward" class="btn-confirm" :disabled="selectedConversationIds.length === 0 || isForwarding">
            {{ isForwarding ? '发送中...' : '发送' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import type { ConversationSummary, MessageDto } from '@/types/api';

const props = defineProps<{
  conversations: ConversationSummary[];
  messageToForward: MessageDto | null;
}>();

const emit = defineEmits(['close', 'forward']);

const searchTerm = ref('');
const selectedConversationIds = ref<number[]>([]);
const attachedMessage = ref('');
const isForwarding = ref(false);

const filteredConversations = computed(() => {
  if (!searchTerm.value) return props.conversations;
  return props.conversations.filter(c => c.name.toLowerCase().includes(searchTerm.value.toLowerCase()));
});

const selectedConversations = computed(() => {
  return props.conversations.filter(c => selectedConversationIds.value.includes(c.conversationId));
});

const deselectConversation = (id: number) => {
  selectedConversationIds.value = selectedConversationIds.value.filter(convoId => convoId !== id);
};

const handleForward = () => {
  isForwarding.value = true;
  emit('forward', {
    targetConversationIds: selectedConversationIds.value,
    attachedMessage: attachedMessage.value,
  });
  // 实际的API调用在父组件中处理，这里只发出事件
};
</script>

<style scoped lang="scss">
/* ... (此处省略详细样式，您可以根据截图进行美化) ... */
.modal-content { display: flex; width: 650px; height: 500px; }
.left-panel { width: 250px; border-right: 1px solid #eee; display: flex; flex-direction: column; }
.right-panel { flex-grow: 1; padding: 20px; display: flex; flex-direction: column; }
/* ... */
</style>
