<script setup lang="ts">
import Drawer from 'primevue/drawer'
import type { DrawerProps } from 'primevue/drawer'

interface Props extends /* @vue-ignore */ DrawerProps {
  /** 표시 여부 */
  visible?: boolean
  /** 위치 */
  position?: 'left' | 'right' | 'top' | 'bottom'
  /** 헤더 텍스트 */
  header?: string
  /** 모달 모드 */
  modal?: boolean
  /** 닫기 버튼 표시 */
  dismissable?: boolean
  /** 닫기 아이콘 표시 */
  showCloseIcon?: boolean
  /** 전체 화면 */
  fullScreen?: boolean
  /** 기본 스타일 */
  baseZIndex?: number
}

withDefaults(defineProps<Props>(), {
  position: 'right',
  modal: true,
  dismissable: true,
  showCloseIcon: true,
})

defineEmits<{
  'update:visible': [value: boolean]
}>()
</script>

<template>
  <Drawer
    :visible="visible"
    :position="position"
    :header="header"
    :modal="modal"
    :dismissable="dismissable"
    :show-close-icon="showCloseIcon"
    :full-screen="fullScreen"
    :base-z-index="baseZIndex"
    v-bind="$attrs"
    @update:visible="$emit('update:visible', $event)"
  >
    <template
      v-if="$slots.header"
      #header
    >
      <slot name="header" />
    </template>
    <slot />
    <template
      v-if="$slots.footer"
      #footer
    >
      <slot name="footer" />
    </template>
  </Drawer>
</template>
