import { defineNuxtPlugin } from '#app'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import { useThemeStore } from '~/stores/theme'

export default defineNuxtPlugin((nuxtApp) => {
  // PrimeVue 서비스 등록
  nuxtApp.vueApp.use(ToastService)
  nuxtApp.vueApp.use(ConfirmationService)

  // 테마 초기화 (DOM 렌더링 전에 실행하여 FOUC 방지)
  nuxtApp.hook('app:beforeMount', () => {
    const themeStore = useThemeStore()
    themeStore.init()
  })
})
