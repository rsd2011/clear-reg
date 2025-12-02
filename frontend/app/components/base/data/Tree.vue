<script setup lang="ts">
import Tree from 'primevue/tree'
import type { TreeProps } from 'primevue/tree'
import type { TreeNode } from 'primevue/treenode'

// TreeSelectionKeys 타입 정의 (PrimeVue 내부 타입)
type TreeSelectionKeys = Record<string, boolean>

interface Props extends /* @vue-ignore */ Omit<TreeProps, 'selectionKeys'> {
  /** 트리 노드 데이터 */
  value?: TreeNode[]
  /** 선택된 키 */
  selectionKeys?: TreeSelectionKeys | null
  /** 선택 모드 */
  selectionMode?: 'single' | 'multiple' | 'checkbox'
  /** 확장된 키 */
  expandedKeys?: Record<string, boolean>
  /** 필터 사용 */
  filter?: boolean
  /** 필터 모드 */
  filterMode?: 'lenient' | 'strict'
  /** 필터 placeholder */
  filterPlaceholder?: string
  /** 로딩 상태 */
  loading?: boolean
  /** 로딩 모드 */
  loadingMode?: 'mask' | 'icon'
}

const props = withDefaults(defineProps<Props>(), {
  value: () => [],
  filterMode: 'lenient',
})

defineEmits<{
  'update:selectionKeys': [value: TreeSelectionKeys | null]
  'update:expandedKeys': [value: Record<string, boolean>]
  'node-select': [node: TreeNode]
  'node-unselect': [node: TreeNode]
  'node-expand': [node: TreeNode]
  'node-collapse': [node: TreeNode]
}>()
</script>

<template>
  <Tree
    :value="props.value"
    :selection-keys="props.selectionKeys ?? undefined"
    :selection-mode="props.selectionMode"
    :expanded-keys="props.expandedKeys"
    :filter="props.filter"
    :filter-mode="props.filterMode"
    :filter-placeholder="props.filterPlaceholder"
    :loading="props.loading"
    :loading-mode="props.loadingMode"
    v-bind="$attrs"
    @update:selection-keys="$emit('update:selectionKeys', $event)"
    @update:expanded-keys="$emit('update:expandedKeys', $event)"
    @node-select="$emit('node-select', $event)"
    @node-unselect="$emit('node-unselect', $event)"
    @node-expand="$emit('node-expand', $event)"
    @node-collapse="$emit('node-collapse', $event)"
  >
    <template
      v-if="$slots.default"
      #default="slotProps"
    >
      <slot v-bind="slotProps" />
    </template>
  </Tree>
</template>
