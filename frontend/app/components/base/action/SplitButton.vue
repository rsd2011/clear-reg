<script setup lang="ts">
import SplitButton from 'primevue/splitbutton'
import type { SplitButtonProps } from 'primevue/splitbutton'

interface MenuItem {
  label?: string
  icon?: string
  command?: () => void
  url?: string
  to?: string
  separator?: boolean
  disabled?: boolean
}

interface Props extends /* @vue-ignore */ Omit<SplitButtonProps, 'model'> {
  /** 버튼 라벨 */
  label?: string
  /** 아이콘 */
  icon?: string
  /** 메뉴 아이템 */
  model?: MenuItem[]
  /** 심각도 */
  severity?: 'secondary' | 'success' | 'info' | 'warn' | 'danger' | 'contrast'
  /** 크기 */
  size?: 'small' | 'large'
  /** 텍스트 버튼 */
  text?: boolean
  /** 외곽선 버튼 */
  outlined?: boolean
  /** 둥근 모서리 */
  rounded?: boolean
  /** 비활성화 */
  disabled?: boolean
}

withDefaults(defineProps<Props>(), {
  model: () => [],
})

defineEmits<{
  click: [event: Event]
}>()
</script>

<template>
  <SplitButton
    :label="label"
    :icon="icon"
    :model="model"
    :severity="severity"
    :size="size"
    :text="text"
    :outlined="outlined"
    :rounded="rounded"
    :disabled="disabled"
    v-bind="$attrs"
    @click="$emit('click', $event)"
  />
</template>
