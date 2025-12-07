<script setup lang="ts">
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import type { TabsProps } from 'primevue/tabs'

interface TabItem {
  value: string
  label: string
  icon?: string
  disabled?: boolean
}

interface Props extends /* @vue-ignore */ Omit<TabsProps, 'value'> {
  /** 활성화된 탭 값 */
  value?: string
  /** 탭 아이템 */
  items?: TabItem[]
  /** 스크롤 가능 */
  scrollable?: boolean
  /** 🚀 탭 지연 렌더링 (활성 탭만 렌더링) */
  lazy?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  items: () => [],
  value: '',
  lazy: true, // 🚀 기본값: 지연 렌더링 활성화
})

defineEmits<{
  'update:value': [value: string | number]
}>()
</script>

<template>
  <Tabs
    :value="props.value"
    :scrollable="props.scrollable"
    v-bind="$attrs"
    @update:value="$emit('update:value', $event)"
  >
    <TabList>
      <Tab
        v-for="item in props.items"
        :key="item.value"
        :value="item.value"
        :disabled="item.disabled"
      >
        <i
          v-if="item.icon"
          :class="item.icon"
          class="mr-2"
        />
        {{ item.label }}
      </Tab>
      <slot name="tabs" />
    </TabList>
    <TabPanels>
      <TabPanel
        v-for="item in props.items"
        :key="item.value"
        :value="item.value"
        :lazy="props.lazy"
      >
        <slot :name="item.value" />
      </TabPanel>
      <slot name="panels" />
    </TabPanels>
  </Tabs>
</template>
