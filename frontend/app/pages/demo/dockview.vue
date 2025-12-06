<script setup lang="ts">
import { computed } from 'vue'
import { DockviewVue } from 'dockview-vue'
import { themeLight, themeAbyss } from 'dockview-core'
import type { DockviewReadyEvent } from 'dockview-core'
import { useThemeStore } from '~/stores/theme'

definePageMeta({
  layout: false,
})

const themeStore = useThemeStore()

// Dockview 테마 (다크/라이트에 따라 공식 테마 객체 반환)
const dockviewTheme = computed(() =>
  themeStore.isDark ? themeAbyss : themeLight,
)

function handleDockViewReady(event: DockviewReadyEvent) {
  const api = event.api

  // 샘플 패널 추가
  api.addPanel({
    id: 'panel1',
    component: 'panelComponent',
    params: { title: '1' },
  })

  api.addPanel({
    id: 'panel2',
    component: 'panelComponent',
    params: { title: '2' },
    position: { referencePanel: 'panel1', direction: 'right' },
  })

  api.addPanel({
    id: 'panel3',
    component: 'panelComponent',
    params: { title: '3' },
    position: { referencePanel: 'panel1', direction: 'below' },
  })
}
</script>

<template>
  <div class="dockview-demo">
    <!-- 테스트 컨트롤 패널 -->
    <div class="test-controls">
      <div class="control-group">
        <span>테마:</span>
        <button
          class="btn"
          :class="{ active: themeStore.themeName === 'linear-dark' }"
          @click="themeStore.setTheme('linear-dark')"
        >
          Linear Dark
        </button>
        <button
          class="btn"
          :class="{ active: themeStore.themeName === 'koscom-light' }"
          @click="themeStore.setTheme('koscom-light')"
        >
          Koscom Light
        </button>
      </div>

      <div class="control-group">
        <span>다크모드:</span>
        <button
          class="btn"
          @click="themeStore.toggleDarkMode()"
        >
          {{ themeStore.isDark ? '🌙 Dark' : '☀️ Light' }}
        </button>
      </div>

      <div class="status">
        현재: {{ themeStore.themeName }} / {{ themeStore.isDark ? 'Dark' : 'Light' }}
      </div>
    </div>

    <DockviewVue
      class="dockview-container"
      :theme="dockviewTheme"
      @ready="handleDockViewReady"
    />
  </div>
</template>

<style scoped>
.dockview-demo {
  width: 100vw;
  height: 100vh;
  background: var(--p-surface-0);
  display: flex;
  flex-direction: column;
}

/* 공식 테마는 HTML에서 상속됨 - plugins/dockview-theme.client.ts */
.dockview-container {
  flex: 1;
  min-height: 0;
}

.test-controls {
  background: var(--p-surface-100);
  padding: 1rem;
  display: flex;
  gap: 2rem;
  align-items: center;
  border-bottom: 1px solid var(--p-surface-200);
  flex-shrink: 0;
}

.control-group {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.btn {
  padding: 0.5rem 1rem;
  border: 1px solid var(--p-surface-300);
  border-radius: var(--p-border-radius);
  background: var(--p-surface-0);
  color: var(--p-surface-900);
  cursor: pointer;
  transition: all 0.2s;
}

.btn:hover {
  background: var(--p-primary-500);
  color: white;
}

.btn.active {
  background: var(--p-primary-500);
  color: white;
  font-weight: bold;
}

.status {
  margin-left: auto;
  font-weight: bold;
  color: var(--p-primary-500);
}

/* 패널 콘텐츠 스타일은 plugins/dockview.client.ts에서 정의된 전역 컴포넌트에 적용 */
:deep(.dv-panel-content) {
  padding: 1rem;
  color: var(--p-surface-900);
}

.app-dark :deep(.dv-panel-content) {
  color: var(--p-surface-0);
}
</style>
