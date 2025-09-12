<template>
  <!-- 模态框遮罩层 -->
  <div class="modal-overlay" @click.self="handleCancel">
    <!-- 模态框内容 -->
    <div class="modal-content">
      <!-- 步骤一：选择联系人 -->
      <template v-if="step === 1">
        <!-- 左侧联系人选择区域 -->
        <div class="contact-list-panel">
          <div class="search-container">
            <input type="text" placeholder="搜索" v-model="searchTerm" />
          </div>
          <div class="contact-scroll-area">
            <!-- (核心新增) 单人建群选项 -->
            <div class="special-option" @click="createSinglePersonGroup">
              <div class="icon-wrapper">
                <i class="mdi mdi-account"></i>
              </div>
              <span>单人群聊</span>
            </div>

            <div v-for="(group, key) in sortedContacts" :key="key">
              <h3 class="group-letter">{{ key }}</h3>
              <ul>
                <li v-for="contact in group" :key="contact.id">
                  <label>
                    <input type="checkbox" :value="contact.id" v-model="selectedContactIds" />
                    <img :src="contact.avatarUrl" class="avatar" />
                    <span class="nickname">{{ contact.nickname }}</span>
                  </label>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 右侧已选和操作区域 -->
        <div class="selection-panel">
          <div class="selection-header">
            <h2>选择联系人</h2>
            <p>{{ selectedContactIds.length }}/{{ contacts.length }}</p>
          </div>
          <div class="selected-contacts-area">
             <div v-if="selectedContacts.length === 0" class="empty-selection">
               请从左侧选择联系人<br>或创建单人群聊
             </div>
             <ul v-else>
               <li v-for="contact in selectedContacts" :key="contact.id">
                  <img :src="contact.avatarUrl" class="avatar" />
                  <span class="nickname">{{ contact.nickname }}</span>
               </li>
             </ul>
          </div>
          <div class="actions">
            <!-- (核心修改) 这个按钮现在进入下一步 -->
            <button class="btn-confirm" @click="goToNextStep" :disabled="selectedContactIds.length === 0">
              下一步
            </button>
            <button class="btn-cancel" @click="handleCancel">取消</button>
          </div>
        </div>
      </template>

      <!-- (核心新增) 步骤二：填写群聊信息 -->
      <template v-if="step === 2">
        <div class="group-details-panel">
          <div class="panel-header">
            <button class="back-btn" @click="goToPreviousStep">&lt; 返回</button>
            <h2>设置群聊信息</h2>
          </div>
          <div class="form-content">
            <!-- (核心新增) 头像预览区域 -->
            <div class="form-group avatar-preview-group">
              <label>群聊头像</label>
              <canvas ref="avatarCanvas" width="100" height="100" class="avatar-canvas"></canvas>
            </div>
            <div class="form-group">
              <label for="group-name">群聊名称</label>
              <input id="group-name" type="text" v-model="groupName" placeholder="给你的群聊起个名字吧">
            </div>
            <div class="form-group">
              <label for="group-description">群聊描述</label>
              <textarea id="group-description" v-model="groupDescription" placeholder="介绍一下你的群聊是做什么的..."></textarea>
            </div>
            <div class="form-group">
              <label>群聊标签 (可多选)</label>
              <!-- (核心修改) 标签容器现在会动态渲染从API获取的数据 -->
              <div v-if="tagsLoading" class="loading-message">正在加载标签...</div>
              <div v-else class="tags-container">
                <button
                  v-for="tag in availableTags"
                  :key="tag"
                  class="tag-btn"
                  :class="{ active: selectedTags.includes(tag) }"
                  @click="toggleTag(tag)"
                >
                  {{ tag }}
                </button>
              </div>
            </div>
          </div>
          <div class="actions">
            <button class="btn-confirm" @click="handleCreateGroup" :disabled="isLoading || !groupName.trim()">
              {{ isLoading ? '创建中...' : '完成创建' }}
            </button>
            <button class="btn-cancel" @click="handleCancel" :disabled="isLoading">取消</button>
          </div>
        </div>
      </template>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import pinying from 'pinyin';
import apiClient from '@/api/apiClient';
import { useAuthStore } from '@/stores/auth'; 
const authStore = useAuthStore();

// --- 类型定义 ---
interface Contact {
  id: number;
  username: string;
  nickname: string;
  avatarUrl: string;
}

// --- 组件通信 ---
const emit = defineEmits(['close', 'group-created']);

// --- 响应式状态 ---
const step = ref(1); // (新增) 控制当前步骤
const contacts = ref<Contact[]>([]);
const selectedContactIds = ref<number[]>([]);
const searchTerm = ref('');
const isLoading = ref(false);

// (新增) 第二步的表单数据
const groupName = ref('');
const groupDescription = ref('');
const availableTags = ref<string[]>([]);
const tagsLoading = ref<boolean>(false);
const selectedTags = ref<string[]>([]);
const avatarCanvas = ref<HTMLCanvasElement | null>(null);

const fetchContacts = async () => {
  isLoading.value = true;
  try {
    // 调用与 ContactsView 相同的接口
    const response = await apiClient.get<Contact[]>('/api/friendships/friends');
    
    // 假设后端直接返回一个 User[] 数组
    contacts.value = response.data;
  } catch (err: any) {
    console.error('Failed to fetch contacts:', err);
    err.value = err.message || '无法加载联系人列表。';
  } finally {
    isLoading.value = false;
  }
};

const fetchTags = async () => {
  tagsLoading.value = true;
  try {
    const response = await apiClient.get<string[]>('/api/discover/tags');
    availableTags.value = response.data;
  } catch (error) {
    console.error('获取标签列表失败:', error);
    // 可以在这里给用户一个提示
  } finally {
    tagsLoading.value = false;
  }
};


// --- 生命周期函数 ---
onMounted(() => {
  // 在组件挂载后获取联系人列表
  fetchContacts(); 
  fetchTags();
});

const createSinglePersonGroup = () => {
  // 1. 确保没有选中任何其他联系人
  selectedContactIds.value = [];
  // 2. 直接进入下一步
  goToNextStep();
};

const goToNextStep = () => {
  step.value = 2;
};
const goToPreviousStep = () => {
  step.value = 1;
};

const toggleTag = (tag: string) => {
  const index = selectedTags.value.indexOf(tag);
  if (index > -1) {
    selectedTags.value.splice(index, 1); // 如果已选，则取消
  } else {
    selectedTags.value.push(tag); // 如果未选，则添加
  }
};

// 根据搜索词过滤联系人
const filteredContacts = computed(() => {
  if (!searchTerm.value) {
    return contacts.value;
  }
  return contacts.value.filter(c => 
    c.nickname.toLowerCase().includes(searchTerm.value.toLowerCase())
  );
});

// 对过滤后的联系人进行排序和分组
const sortedContacts = computed(() => {
  // 定义一个分组对象
  const grouped: Record<string, Contact[]> = {};

  filteredContacts.value.forEach(contact => {
    // 获取昵称的拼音首字母
    const firstLetter = pinying(contact.nickname, {
      style: 'first_letter',
    })[0][0].toUpperCase();
    
    // 如果该字母的分组不存在，则创建
    if (!grouped[firstLetter]) {
      grouped[firstLetter] = [];
    }
    
    // 将联系人添加到对应分组
    grouped[firstLetter].push(contact);
  });
  
  // 对分组的键（字母）进行排序
  const sortedKeys = Object.keys(grouped).sort();
  
  // 创建一个排序后的新分组对象
  const sortedGrouped: Record<string, Contact[]> = {};
  for (const key of sortedKeys) {
    sortedGrouped[key] = grouped[key];
  }
  
  return sortedGrouped;
});

// 获取已选中的联系人完整对象
const selectedContacts = computed(() => {
  return contacts.value.filter(c => selectedContactIds.value.includes(c.id));
});


// --- 方法 ---

const handleCancel = () => {
  if (!isLoading.value) {
    emit('close');
  }
};

const drawAvatarCanvas = () => {
  const canvas = avatarCanvas.value;
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  const size = 100; // Canvas 尺寸
  ctx.fillStyle = '#e0e0e0'; // 默认背景色
  ctx.fillRect(0, 0, size, size);

  // 只取最多前9个成员用于绘制
  const membersToDraw = selectedContacts.value.slice(0, 9);
  const count = membersToDraw.length;
  if (count === 0) {
    // 单人聊天：绘制当前用户的头像
    const currentUser = authStore.userInfo;
    if (currentUser) {
      const img = new Image();
      img.crossOrigin = 'Anonymous';
      img.onload = () => ctx.drawImage(img, 0, 0, size, size);
      img.src = currentUser.avatarUrl;
    }
    return;
  }

  const promises = membersToDraw.map(contact => {
    return new Promise<HTMLImageElement>((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'Anonymous'; // 允许跨域加载图片
      img.onload = () => resolve(img);
      img.onerror = reject;
      img.src = contact.avatarUrl;
    });
  });

  Promise.all(promises).then(images => {
    // 根据成员数量决定布局
    let positions: {x: number, y: number, w: number, h: number}[] = [];
    const gap = 2; // 图片间距

    if (count === 1) {
      positions = [{ x: 0, y: 0, w: size, h: size }];
    } else if (count === 2) {
    const w = size / 2 - gap / 2;
      positions = [{ x: 0, y: 0, w, h: size }, { x: w + gap, y: 0, w, h: size }];
    } else if (count <= 4) {
      const w = size / 2 - gap / 2;
    positions = [
      { x: 0, y: 0, w, h: w }, { x: w + gap, y: 0, w, h: w },
      { x: 0, y: w + gap, w, h: w }, { x: w + gap, y: w + gap, w, h: w }
    ];
    } else { // 5 to 9 members
      const w = size / 3 - (gap * 2) / 3;
      for (let i = 0; i < count; i++) {
        const row = Math.floor(i / 3);
        const col = i % 3;
        positions.push({ x: col * (w + gap), y: row * (w + gap), w, h: w });
      }
    }
    
    images.forEach((img, index) => {
      const pos = positions[index];
      if (pos) {
        ctx.drawImage(img, pos.x, pos.y, pos.w, pos.h);
      }
    });
  }).catch(err => console.error("Error drawing avatars:", err));
};

// (核心新增) 当步骤切换到2时，或者选中的联系人变化时，重新绘制 canvas
watch([step, selectedContacts], () => {
  if (step.value === 2) {
    drawAvatarCanvas();
  }
}, { immediate: true });

const handleCreateGroup = async () => {
  if (!groupName.value.trim() || isLoading.value) return;
  
  isLoading.value = true;
  
  try {
    const requestBody = {
      name: groupName.value,
      description: groupDescription.value,
      tags: selectedTags.value,
      memberIds: selectedContactIds.value,
    };
    
    // 调用后端API, 注意这里的URL需要配置代理或写全路径
    const response = await apiClient.post('/api/home/conversations', requestBody);
    
    console.log('群聊创建成功:', response.data);
    emit('group-created', response.data); // 将新群聊信息传给父组件
    emit('close');

  } catch (error) {
    console.error('创建群聊失败:', error);
    alert('创建群聊失败，请稍后重试。');
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped lang="scss">
.special-option {
  display: flex;
  align-items: center;
  padding: 10px 5px;
  cursor: pointer;
  border-radius: 4px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 10px;

  &:hover {
    background-color: #f0f0f0;
  }

  .icon-wrapper {
    width: 36px;
    height: 36px;
    border-radius: 4px;
    margin-right: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #ffab00; // 一个醒目的颜色
    color: white;
    font-size: 22px;
  }

  span {
    font-size: 14px;
    font-weight: 500;
  }
}

.empty-selection {
  color: #aaa;
  text-align: center;
  margin-top: 40px;
  line-height: 1.5;
}
.avatar-preview-group {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-canvas {
  border-radius: 6px;
  border: 1px solid #ccc;
}
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal-content {
  width: 680px;
  height: 520px;
  background-color: #f5f5f5;
  border-radius: 4px;
  display: flex;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.contact-list-panel {
  width: 250px;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  padding: 12px;
}
.search-container {
  padding: 8px 0;
  input {
    width: 100%;
    height: 30px;
    border: 1px solid #e0e0e0;
    border-radius: 4px;
    padding: 0 8px;
    box-sizing: border-box;
  }
}
.contact-scroll-area {
  flex-grow: 1;
  overflow-y: auto;
  margin-top: 10px;
}
.group-letter {
  margin: 10px 0 5px 5px;
  font-size: 14px;
  color: #888;
}
ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
li label {
  display: flex;
  align-items: center;
  padding: 8px 5px;
  cursor: pointer;
  border-radius: 4px;
  &:hover {
    background-color: #f0f0f0;
  }
  input[type="checkbox"] {
    margin-right: 15px;
    width: 16px;
    height: 16px;
  }
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  margin-right: 10px;
}
.nickname {
  font-size: 14px;
}
.selection-panel {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
}
.selection-header {
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 12px;
  
  h2 {
    margin: 0 0 4px 0;
    font-size: 18px;
    font-weight: 500;
  }
  p {
    margin: 0;
    font-size: 14px;
    color: #888;
  }
}
.selected-contacts-area {
  flex-grow: 1;
  overflow-y: auto;
  padding: 10px 0;

  .empty-selection {
    color: #aaa;
    text-align: center;
    margin-top: 40px;
  }
  
   li {
    display: flex;
    align-items: center;
    padding: 6px 0;
  }
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;

  button {
    padding: 8px 24px;
    border: 1px solid #ccc;
    border-radius: 4px;
    cursor: pointer;
    background-color: #f0f0f0;
    
    &:hover {
      background-color: #e0e0e0;
    }
    &:disabled {
      cursor: not-allowed;
      opacity: 0.6;
    }
  }

  .btn-confirm {
    background-color: #4CAF50;
    color: white;
    border-color: #4CAF50;

    &:hover:not(:disabled) {
      background-color: #45a049;
    }
  }
}
.group-details-panel {
  width: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px 30px;
}
.panel-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  position: relative;
  text-align: center;
  
  .back-btn {
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    background: none;
    border: none;
    cursor: pointer;
    font-size: 14px;
    color: #888;
    &:hover { color: #333; }
  }

  h2 {
    flex-grow: 1;
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
}

.form-content {
  flex-grow: 1;
  overflow-y: auto;
}

.form-group {
  margin-bottom: 20px;
  
  label {
    display: block;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 500;
    color: #333;
  }

  input[type="text"], textarea {
    width: 100%;
    padding: 10px;
    border: 1px solid #e0e0e0;
    border-radius: 4px;
    font-size: 14px;
    box-sizing: border-box;
    &:focus {
      outline: none;
      border-color: #4CAF50;
    }
  }

  textarea {
    height: 100px;
    resize: vertical;
  }
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-btn {
  padding: 6px 12px;
  border: 1px solid #ccc;
  border-radius: 15px;
  background-color: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;

  &.active {
    background-color: #d4edda;
    border-color: #4CAF50;
    color: #155724;
    font-weight: 500;
  }
  &:hover:not(.active) {
    border-color: #aaa;
  }
}
</style>
