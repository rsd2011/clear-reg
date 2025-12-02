<script setup lang="ts">
/**
 * Empty 공통 컴포넌트
 *
 * 데이터가 없을 때 표시하는 범용 컴포넌트
 */

interface Props {
  /** 아이콘 */
  icon?: string
  /** 제목 */
  title?: string
  /** 설명 */
  description?: string
  /** 액션 버튼 라벨 */
  actionLabel?: string
}

withDefaults(defineProps<Props>(), {
  icon: 'pi pi-inbox',
  title: '데이터 없음',
  description: '표시할 데이터가 없습니다',
})

defineEmits<{
  action: []
}>()
</script>

<template>
  <div class="flex flex-col items-center justify-center gap-4 p-8 text-center">
    <i
      :class="icon"
      class="text-6xl text-gray-400"
    />

    <div class="flex flex-col gap-2">
      <h3 class="text-xl font-semibold text-gray-700">
        {{ title }}
      </h3>
      <p class="text-gray-500">
        {{ description }}
      </p>
    </div>

    <ActionButton
      v-if="actionLabel"
      :label="actionLabel"
      @click="$emit('action')"
    />

    <slot />
  </div>
</template>
