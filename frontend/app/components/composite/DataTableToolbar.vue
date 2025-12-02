<script setup lang="ts">
/**
 * DataTableToolbar 복합 컴포넌트
 *
 * PanelToolbar + 검색/필터/액션 버튼들의 조합
 */

interface Props {
  /** 제목 */
  title?: string
  /** 검색어 */
  searchQuery?: string
  /** 검색 플레이스홀더 */
  searchPlaceholder?: string
  /** 새로고침 버튼 표시 */
  showRefresh?: boolean
  /** 추가 버튼 표시 */
  showAdd?: boolean
  /** 추가 버튼 라벨 */
  addLabel?: string
}

withDefaults(defineProps<Props>(), {
  searchPlaceholder: '검색...',
  addLabel: '추가',
})

defineEmits<{
  'update:searchQuery': [value: string]
  'refresh': []
  'add': []
}>()
</script>

<template>
  <PanelToolbar>
    <template #start>
      <h3
        v-if="title"
        class="text-xl font-semibold"
      >
        {{ title }}
      </h3>
      <slot name="start" />
    </template>

    <template #center>
      <slot name="center" />
    </template>

    <template #end>
      <div class="flex gap-2">
        <FormInput
          v-if="searchQuery !== undefined"
          :model-value="searchQuery"
          :placeholder="searchPlaceholder"
          @update:model-value="$emit('update:searchQuery', $event)"
        />

        <ActionButton
          v-if="showRefresh"
          icon="pi pi-refresh"
          text
          @click="$emit('refresh')"
        />

        <ActionButton
          v-if="showAdd"
          :label="addLabel"
          icon="pi pi-plus"
          @click="$emit('add')"
        />

        <slot name="end" />
      </div>
    </template>
  </PanelToolbar>
</template>
