<script setup lang="ts">
import Button from 'primevue/button'
import type { ButtonProps } from 'primevue/button'

interface Props extends /* @vue-ignore */ ButtonProps {
  /** 버튼 라벨 */
  label?: string
  /** 버튼 아이콘 (PrimeIcons 클래스명) */
  icon?: string
  /** 아이콘 위치 */
  iconPos?: 'left' | 'right' | 'top' | 'bottom'
  /** 버튼 심각도 */
  severity?: 'secondary' | 'success' | 'info' | 'warn' | 'danger' | 'help' | 'contrast'
  /** 텍스트만 표시 (배경 없음) */
  text?: boolean
  /** 아웃라인 스타일 */
  outlined?: boolean
  /** 둥근 모서리 */
  rounded?: boolean
  /** 로딩 상태 */
  loading?: boolean
  /** 비활성화 */
  disabled?: boolean
  /** 버튼 크기 */
  size?: 'small' | 'large'
  /** 링크 스타일 */
  link?: boolean
}

withDefaults(defineProps<Props>(), {
  iconPos: 'left',
})

defineEmits<{
  click: [event: MouseEvent]
}>()
</script>

<template>
  <Button
    :label="label"
    :icon="icon"
    :icon-pos="iconPos"
    :severity="severity"
    :text="text"
    :outlined="outlined"
    :rounded="rounded"
    :loading="loading"
    :disabled="disabled"
    :size="size"
    :link="link"
    v-bind="$attrs"
    @click="$emit('click', $event)"
  >
    <template
      v-if="$slots.default"
      #default
    >
      <slot />
    </template>
    <template
      v-if="$slots.icon"
      #icon
    >
      <slot name="icon" />
    </template>
    <template
      v-if="$slots.loadingicon"
      #loadingicon
    >
      <slot name="loadingicon" />
    </template>
  </Button>
</template>
