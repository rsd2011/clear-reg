import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

/**
 * Pinia Persistedstate 플러그인
 * - 선언적 상태 영속화
 * - localStorage/sessionStorage 자동 동기화
 * - SSR hydration 자동 처리
 *
 * 사용법: 스토어에서 persist 옵션 추가
 * ```ts
 * export const useMyStore = defineStore('my-store', {
 *   state: () => ({ ... }),
 *   persist: true, // 또는 상세 옵션
 * })
 * ```
 */
export default defineNuxtPlugin(({ $pinia }) => {
  $pinia.use(piniaPluginPersistedstate)
})
