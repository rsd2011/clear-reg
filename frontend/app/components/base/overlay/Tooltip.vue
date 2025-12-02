<script setup lang="ts">
/**
 * Tooltip 래핑 컴포넌트
 *
 * PrimeVue Tooltip은 디렉티브로 사용되므로,
 * 이 컴포넌트는 Tooltip을 래핑하여 사용할 수 있도록 합니다.
 *
 * 사용 예:
 * <OverlayTooltip value="툴팁 내용">
 *   <ActionButton label="버튼" />
 * </OverlayTooltip>
 */
import Tooltip from 'primevue/tooltip'

interface Props {
  /** 툴팁 내용 */
  value?: string
  /** 위치 */
  position?: 'top' | 'bottom' | 'left' | 'right'
  /** 이벤트 타입 */
  event?: 'hover' | 'focus' | 'both'
  /** 비활성화 */
  disabled?: boolean
  /** 표시 지연 (ms) */
  showDelay?: number
  /** 숨김 지연 (ms) */
  hideDelay?: number
}

const props = withDefaults(defineProps<Props>(), {
  position: 'top',
  event: 'hover',
  showDelay: 0,
  hideDelay: 0,
})

// Tooltip directive 설정
const vTooltip = Tooltip
</script>

<template>
  <span
    v-tooltip="{
      value: props.value,
      pt: {
        root: { class: `p-tooltip-${props.position}` },
      },
      showDelay: props.showDelay,
      hideDelay: props.hideDelay,
      disabled: props.disabled,
    }"
  >
    <slot />
  </span>
</template>
