<template>
  <div class="management-panel mute-management-content">
    <div class="column member-list-column">
      <h3 class="column-title">选择成员禁言</h3>
      <div class="search-bar">
        <input type="text" v-model="memberSearch" placeholder="搜索成员" />
      </div>
      <ul class="user-list">
        <li v-for="member in filteredMembers" :key="member.id" class="user-item">
          <input
            type="checkbox"
            :id="'mute-member-' + member.id"
            v-model="selectedMembers"
            :value="member.id"
          />
          <label :for="'mute-member-' + member.id">
            <img :src="member.avatarUrl" class="avatar" />
            <span class="nickname">{{ member.nickname }}</span>
          </label>
        </li>
      </ul>
      <div class="mute-action-panel">
        <select v-model="muteDuration">
          <option value="600">10分钟</option>
          <option value="3600">1小时</option>
          <option value="86400">1天</option>
          <option value="604800">7天</option>
        </select>
        <button @click="applyMute" :disabled="selectedMembers.length === 0" class="btn mute-btn">禁言</button>
      </div>
    </div>
    <div class="column muted-list-column">
      <h3 class="column-title">当前已禁言</h3>
      <ul class="user-list">
        <!-- (核心修改) 绑定到新的 countdowns 计算属性 -->
        <li v-for="mutedUser in countdowns" :key="mutedUser.userId" class="user-item">
          <img :src="mutedUser.avatarUrl" class="avatar" />
          <div class="muted-info">
            <span class="nickname">{{ mutedUser.nickname }}</span>
            <span class="mute-time">剩余: {{ mutedUser.timeLeft }}</span>
          </div>
          <button @click="unmuteUser(mutedUser.userId)" class="btn unmute-btn">解除</button>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import apiClient from '@/api/apiClient';
import type { ConversationSummary, GroupUserInfo, MutedUserDto } from '@/types/api';

const props = defineProps<{
  conversation: ConversationSummary;
}>();

const allMembers = ref<GroupUserInfo[]>([]);
const mutedList = ref<MutedUserDto[]>([]); // (核心修改) 类型现在是 MutedUserDto
const memberSearch = ref('');
const selectedMembers = ref<number[]>([]);
const muteDuration = ref(600); // Default 10 minutes in seconds

// (核心新增) 用于实时更新倒计时的定时器
let countdownInterval: number | null = null;
const currentTime = ref(new Date());

const fetchAllData = async () => {
  try {
    const membersResponse = await apiClient.get<GroupUserInfo[]>(`/api/conversations/${props.conversation.uuid}/members`);
    allMembers.value = membersResponse.data;
    
    // (核心修改) 获取包含 mutedUntil 的数据
    const mutedResponse = await apiClient.get<MutedUserDto[]>(`/api/conversations/${props.conversation.conversationId}/mutes`);
    mutedList.value = mutedResponse.data;
  } catch (err) {
    console.error('Failed to fetch data', err);
  }
};

const filteredMembers = computed(() => {
  return allMembers.value.filter(member =>
    member.nickname.toLowerCase().includes(memberSearch.value.toLowerCase()) &&
    !mutedList.value.some(muted => muted.userId === member.id)
  );
});

// (核心新增) 计算属性，用于动态生成倒计时文本
const countdowns = computed(() => {
  // 依赖 currentTime.value 来触发每秒的重新计算
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const _ = currentTime.value; 

  return mutedList.value.map(user => {
    const mutedUntilDate = new Date(user.mutedUntil);
    const now = new Date();
    const secondsLeft = Math.round((mutedUntilDate.getTime() - now.getTime()) / 1000);

    if (secondsLeft <= 0) {
      return { ...user, timeLeft: '已到期' };
    }
    return { ...user, timeLeft: formatDuration(secondsLeft) };
  });
});

const applyMute = async () => {
  try {
    await apiClient.post(`/api/conversations/${props.conversation.conversationId}/mute`, {
      targetUserIds: selectedMembers.value,
      durationSeconds: muteDuration.value
    });
    selectedMembers.value = [];
    fetchAllData();
  } catch (err) {
    alert('禁言失败。');
    console.error(err);
  }
};

const unmuteUser = async (userId: number) => {
  try {
    await apiClient.delete(`/api/conversations/${props.conversation.conversationId}/mute/${userId}`);
    fetchAllData();
  } catch (err) {
    alert('解除禁言失败。');
    console.error(err);
  }
};

const formatDuration = (seconds: number): string => {
  if (seconds < 60) return `${seconds} 秒`;
  const days = Math.floor(seconds / 86400);
  seconds %= 86400;
  const hours = Math.floor(seconds / 3600);
  seconds %= 3600;
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  
  let result = '';
  if (days > 0) result += `${days}天 `;
  if (hours > 0) result += `${hours}小时 `;
  if (minutes > 0) result += `${minutes}分 `;
  if (remainingSeconds > 0) result += `${remainingSeconds}秒`;
  
  return result.trim();
};

onMounted(() => {
  fetchAllData();
  // (核心新增) 启动定时器
  countdownInterval = window.setInterval(() => {
    currentTime.value = new Date();
  }, 1000);
});

onUnmounted(() => {
  // (核心新增) 组件销毁时清除定时器，防止内存泄漏
  if (countdownInterval) {
    clearInterval(countdownInterval);
  }
});
</script>

<style scoped lang="scss">
.mute-management-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  height: 100%;
}
.column {
  display: flex;
  flex-direction: column;
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 15px;
}
.column-title {
  font-size: 16px;
  margin-top: 0;
  margin-bottom: 15px;
}
.search-bar input {
  width: 90%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  margin-bottom: 15px;
}
.user-list {
  list-style: none;
  padding: 0;
  margin: 0;
  flex-grow: 1;
  overflow-y: auto;
}
.user-item {
  display: flex;
  align-items: center;
  padding: 8px;
  label {
    display: flex;
    align-items: center;
    cursor: pointer;
  }
  input[type="checkbox"] {
    margin-right: 12px;
  }
  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    margin-right: 12px;
  }
  .nickname {
    font-weight: 500;
  }
  .muted-info {
    flex-grow: 1;
    display: flex;
    flex-direction: column;
  }
  .mute-time {
    font-size: 12px;
    color: #888;
  }
}
.mute-action-panel {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #eee;
  select {
    padding: 8px;
    border-radius: 6px;
    border: 1px solid #ddd;
  }
}
.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.mute-btn {
  background-color: #f0ad4e;
  color: white;
  &:disabled {
    background-color: #ccc;
    cursor: not-allowed;
  }
}
.unmute-btn {
  background-color: #5bc0de;
  color: white;
  margin-left: auto;
}
</style>
