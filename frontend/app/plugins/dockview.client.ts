import { defineComponent, h } from 'vue'
import type { IDockviewPanelProps } from 'dockview-vue'

// DockView 패널 props 확장 타입 (id 속성 추가)
interface ExtendedDockviewPanelProps extends IDockviewPanelProps {
  id?: string
}

// DockView 패널 컴포넌트 정의
const PanelComponent = defineComponent({
  name: 'PanelComponent',
  props: {
    params: Object as () => ExtendedDockviewPanelProps,
  },
  setup(props) {
    return () => h('div', { class: 'dv-panel-content' }, [
      h('h2', {}, `Panel ${props.params?.params?.title || props.params?.id}`),
      h('p', {}, '테마 전환을 테스트해보세요!'),
      h('p', { style: 'margin-top: 1rem; font-size: 0.875rem; opacity: 0.7' },
        `ID: ${props.params?.id}`),
    ])
  },
})

export default defineNuxtPlugin((nuxtApp) => {
  // DockView 패널 컴포넌트를 전역으로 등록
  nuxtApp.vueApp.component('panelComponent', PanelComponent)
})
