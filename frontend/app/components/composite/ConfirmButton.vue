<script setup lang="ts">
/**
 * ConfirmButton 복합 컴포넌트
 *
 * ActionButton + OverlayConfirm 조합으로 확인 다이얼로그가 있는 버튼
 */
import { useConfirm } from 'primevue/useconfirm'

interface Props {
  /** 버튼 라벨 */
  label?: string
  /** 버튼 아이콘 */
  icon?: string
  /** 확인 메시지 */
  message?: string
  /** 확인 헤더 */
  header?: string
  /** 확인 버튼 라벨 */
  acceptLabel?: string
  /** 취소 버튼 라벨 */
  rejectLabel?: string
  /** 심각도 */
  severity?: 'secondary' | 'success' | 'info' | 'warn' | 'danger' | 'contrast'
  /** 버튼 크기 */
  size?: 'small' | 'large'
  /** 비활성화 */
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  message: '정말 실행하시겠습니까?',
  header: '확인',
  acceptLabel: '확인',
  rejectLabel: '취소',
})

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()

const confirm = useConfirm()

function handleClick() {
  confirm.require({
    message: props.message,
    header: props.header,
    acceptLabel: props.acceptLabel,
    rejectLabel: props.rejectLabel,
    accept: () => emit('confirm'),
    reject: () => emit('cancel'),
  })
}
</script>

<template>
  <ActionButton
    :label="label"
    :icon="icon"
    :severity="severity"
    :size="size"
    :disabled="disabled"
    v-bind="$attrs"
    @click="handleClick"
  />
</template>
