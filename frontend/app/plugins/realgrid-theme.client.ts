/**
 * RealGrid Theme Dynamic Loader Plugin
 *
 * 공식 realgrid-dark.css를 다크모드일 때만 동적으로 로드/언로드
 * - useHead composable을 사용하여 Nuxt 공식 방식으로 처리
 * - 테마 스토어의 isDark 상태에 따라 자동으로 CSS 토글
 *
 * @see https://nuxt.com/docs/api/composables/use-head
 */

import { useThemeStore } from '~/stores/theme'

export default defineNuxtPlugin(() => {
  const themeStore = useThemeStore()

  // 다크모드일 때만 공식 RealGrid 다크테마 CSS 로드
  useHead({
    link: computed(() =>
      themeStore.isDark
        ? [{
            rel: 'stylesheet',
            href: '/css/realgrid-dark.css',
            id: 'realgrid-dark-theme',
          }]
        : [],
    ),
  })
})
