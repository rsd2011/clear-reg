<script setup lang="ts">
/**
 * StatusBadge 복합 컴포넌트
 *
 * FeedbackBadge + 아이콘 조합으로 상태 표시 배지
 */

type Status = 'active' | 'inactive' | 'pending' | 'success' | 'error' | 'warning'

interface Props {
  /** 상태 */
  status: Status
  /** 커스텀 라벨 (기본값은 상태에 따라 자동 설정) */
  label?: string
}

const props = defineProps<Props>()

const statusConfig: Record<Status, { severity: string, icon: string, defaultLabel: string }> = {
  active: { severity: 'success', icon: 'pi pi-check-circle', defaultLabel: '활성' },
  inactive: { severity: 'secondary', icon: 'pi pi-circle', defaultLabel: '비활성' },
  pending: { severity: 'warn', icon: 'pi pi-clock', defaultLabel: '대기중' },
  success: { severity: 'success', icon: 'pi pi-check', defaultLabel: '성공' },
  error: { severity: 'danger', icon: 'pi pi-times-circle', defaultLabel: '오류' },
  warning: { severity: 'warn', icon: 'pi pi-exclamation-triangle', defaultLabel: '경고' },
}

const config = computed(() => statusConfig[props.status])
const displayLabel = computed(() => props.label || config.value.defaultLabel)
</script>

<template>
  <FeedbackBadge
    :value="displayLabel"
    :severity="config.severity as any"
  >
    <i
      :class="config.icon"
      class="mr-1"
    />
    {{ displayLabel }}
  </FeedbackBadge>
</template>
