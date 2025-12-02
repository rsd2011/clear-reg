<script setup lang="ts">
import SpeedDial from 'primevue/speeddial'
import type { SpeedDialProps } from 'primevue/speeddial'

interface MenuItem {
  label?: string
  icon?: string
  command?: () => void
  url?: string
  to?: string
  disabled?: boolean
}

interface Props extends /* @vue-ignore */ Omit<SpeedDialProps, 'model'> {
  /** 메뉴 아이템 */
  model?: MenuItem[]
  /** 방향 */
  direction?: 'up' | 'down' | 'left' | 'right' | 'up-left' | 'up-right' | 'down-left' | 'down-right'
  /** 마스크 표시 */
  mask?: boolean
  /** 표시 여부 */
  visible?: boolean
  /** 버튼 클래스 */
  buttonClass?: string
  /** 마스크 클래스 */
  maskClass?: string
  /** 표시 아이콘 */
  showIcon?: string
  /** 숨김 아이콘 */
  hideIcon?: string
  /** 회전 애니메이션 */
  rotateAnimation?: boolean
}

withDefaults(defineProps<Props>(), {
  model: () => [],
  direction: 'up',
  rotateAnimation: true,
})

defineEmits<{
  'update:visible': [value: boolean]
  'show': []
  'hide': []
}>()
</script>

<template>
  <SpeedDial
    :model="model"
    :direction="direction"
    :mask="mask"
    :visible="visible"
    :button-class="buttonClass"
    :mask-class="maskClass"
    :show-icon="showIcon"
    :hide-icon="hideIcon"
    :rotate-animation="rotateAnimation"
    v-bind="$attrs"
    @update:visible="$emit('update:visible', $event)"
    @show="$emit('show')"
    @hide="$emit('hide')"
  >
    <template
      v-if="$slots.item"
      #item="slotProps"
    >
      <slot
        name="item"
        v-bind="slotProps"
      />
    </template>
  </SpeedDial>
</template>
