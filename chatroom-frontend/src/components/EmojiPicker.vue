<template>
  <div v-if="visible" class="emoji-picker" @click.stop>
    <div class="emoji-content">
      <div class="emoji-category">
        <div class="category-title">所有表情</div>
        <div class="emoji-grid">
          <span
            v-for="emoji in emojis"
            :key="emoji"
            class="emoji-item"
            @click="selectEmoji(emoji)"
          >
            {{ emoji }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

defineProps<{
  visible: boolean;
}>();

const emit = defineEmits(['select']);

// A comprehensive list of default emojis
const emojis = ref([
  '😀', '😁', '😂', '🤣', '😃', '😄', '😅', '😆', '😉', '😊',
  '😋', '😎', '😍', '😘', '🥰', '😗', '😙', '😚', '🙂', '🤗',
  '🤩', '🤔', '🤨', '😐', '😑', '😶', '🙄', '😏', '😣', '😥',
  '😮', '🤐', '😯', '😪', '😫', '🥱', '😴', '😌', '😛', '😜',
  '😝', '🤤', '😒', '😓', '😔', '😕', '🙃', '🤑', '😲', '🙁',
  '😖', '😞', '😟', '😤', '😢', '😭', '😦', '😧', '😨', '😩',
  '🤯', '😬', '😰', '😱', '🥵', '🥶', '😳', '🤪', '😵', '🥴',
  '😠', '😡', '🤬', '😷', '🤒', '🤕', '🤢', '🤮', '🤧', '😇',
  '🥳', '🥺', '🤥', '🤫', '🤭', '🧐', '🤓', '😈', '👿',
  '🤡', '👹', '👺', '💀', '👻', '👽', '🤖', '💩', '😺', '😸',
  '😹', '😻', '😼', '😽', '🙀', '😿', '😾', '🙈', '🙉', '🙊',
  '💋', '💌', '💘', '💝', '💖', '💗', '💓', '💞', '💕', '💟',
  '❣️', '💔', '❤️', '🧡', '💛', '💚', '💙', '💜', '🤎', '🖤',
  '🤍', '💯', '💢', '💥', '💫', '💦', '💨', '🕳️', '💣', '💬',
  '👁️‍🗨️', '🗨️', '🗯️', '💭', '💤', '👋', '🤚', '🖐️', '✋', '🖖',
  '👌', '🤌', '🤏', '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉',
  '👆', '🖕', '👇', '☝️', '👍', '👎', '✊', '👊', '🤛', '🤜',
  '👏', '🙌', '👐', '🤲', '🤝', '🙏', '✍️', '💅', '🤳', '💪',
  '👀', '👁️', '👅', '👄', '👶', '🧒', '👦', '👧', '🧑', '👱',
  '👨', '👩', '🧓', '👴', '👵', '👮', '🕵️', '💂', '👷', '🤴',
  '👸', '👳', '👲', '🧕', '🤵', '👰', '🤰', '🤱', '👼', '🎅',
  '🤶', '🦸', '🦹', '🧙', '🧚', '🧛', '🧜', '🧝', '🧞', '🧟',
  '💆', '💇', '🚶', '🧍', '🧎', '🏃', '💃', '🕺', '🕴️', '🧗',
  '🏇', '⛷️', '🏂', '🏌️', '🏄', '🚣', '🏊', '⛹️', '🏋️', '🚴',
  '🚵', '🤸', '🤼', '🤽', '🤾', '🤹', '🧘', '🛀', '🛌', '❤️‍🔥',
  '❤️‍🩹', '😊', '🔥', '✨', '🎉', '🎁', '🎄', '🎃', '🎈', '🎊'
]);

const selectEmoji = (emoji: string) => {
  emit('select', emoji);
};
</script>

<style scoped>
.emoji-picker {
  position: absolute;
  bottom: 100%; /* Position it above the input area */
  left: 0;
  width: 380px;
  height: 280px;
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
}

.emoji-content {
  flex-grow: 1;
  overflow-y: auto;
  padding: 10px;
}

/* Custom scrollbar for a better look */
.emoji-content::-webkit-scrollbar {
  width: 6px;
}
.emoji-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}
.emoji-content::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 3px;
}
.emoji-content::-webkit-scrollbar-thumb:hover {
  background: #aaa;
}

.category-title {
  font-size: 13px;
  font-weight: 500;
  color: #888;
  margin-bottom: 10px;
  padding-left: 5px;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(32px, 1fr));
  gap: 8px;
}

.emoji-item {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  cursor: pointer;
  border-radius: 6px;
  padding: 4px;
  transition: background-color 0.2s;
}

.emoji-item:hover {
  background-color: #f0f0f0;
}
</style>
