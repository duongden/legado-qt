<template>
  <div 
    class="translate-toggle" 
    @click="toggleTranslate" 
    :class="{ active: isTranslateMode }"
    :title="isTranslateMode ? 'Đang dịch → Bấm để tắt' : 'Đang gốc → Bấm để dịch'"
  >
    <div class="toggle-content">
      <span class="lang-icon">{{ isTranslateMode ? '🇻🇳' : '🇨🇳' }}</span>
      <span class="lang-text">{{ isTranslateMode ? 'VI' : 'CN' }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useBookStore } from '@/store'

const store = useBookStore()
const isTranslateMode = computed(() => store.isTranslateMode)

const toggleTranslate = () => {
  store.toggleTranslateMode()
}
</script>

<style scoped>
.translate-toggle {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  z-index: 9999;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  font-family: system-ui, -apple-system, sans-serif;
}

.translate-toggle.active {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  box-shadow: 0 4px 15px rgba(17, 153, 142, 0.4);
}

.translate-toggle:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.translate-toggle.active:hover {
  box-shadow: 0 6px 20px rgba(17, 153, 142, 0.5);
}

.translate-toggle:active {
  transform: translateY(0) scale(0.98);
}

.toggle-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.lang-icon {
  font-size: 20px;
  line-height: 1;
}

.lang-text {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}
</style>
