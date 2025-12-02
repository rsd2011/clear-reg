// https://nuxt.com/docs/api/configuration/nuxt-config
import Aura from '@primeuix/themes/aura'

export default defineNuxtConfig({
  // SSR 비활성화 - CSR(Client Side Rendering) 전용 SPA 모드
  // PrimeVue Hydration 경고 완전 제거
  ssr: false,

  // 테마 FOUC(Flash of Unstyled Content) 방지를 위한 inline script
  app: {
    head: {
      script: [
        {
          innerHTML: `
            (function() {
              try {
                var themeName = localStorage.getItem('enterman-theme-name') || 'linear-dark';
                var themeMode = localStorage.getItem('enterman-theme-mode') || 'system';
                var isDark = themeMode === 'dark' ||
                  (themeMode === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);
                var themeClass = themeName === 'koscom-light' ? 'theme-koscom-light' : 'theme-linear-dark';
                document.documentElement.classList.add(themeClass);
                if (isDark) document.documentElement.classList.add('app-dark');
              } catch (e) {}
            })();
          `,
          type: 'text/javascript',
        },
      ],
    },
  },

  modules: [
    '@nuxt/eslint',
    '@nuxt/content',
    '@nuxt/fonts',
    '@nuxt/icon',
    '@nuxt/image',
    '@nuxt/scripts',
    '@nuxt/test-utils/module',
    // '@nuxt/hints', // SSR 비활성화로 hydration 체크 불필요
    '@nuxtjs/tailwindcss',
    '@primevue/nuxt-module',
    '@pinia/nuxt',
  ],

  // 컴포넌트 자동 import 설정
  components: [
    // Base 컴포넌트 - 카테고리별 프리픽스
    { path: '~/components/base/form', prefix: 'Form' },
    { path: '~/components/base/action', prefix: 'Action' },
    { path: '~/components/base/data', prefix: 'Data' },
    { path: '~/components/base/panel', prefix: 'Panel' },
    { path: '~/components/base/overlay', prefix: 'Overlay' },
    { path: '~/components/base/menu', prefix: 'Menu' },
    { path: '~/components/base/feedback', prefix: 'Feedback' },
    // Composite 컴포넌트 - 프리픽스 없음
    { path: '~/components/composite', pathPrefix: false },
    // Common 컴포넌트 - 프리픽스 없음
    { path: '~/components/common', pathPrefix: false },
  ],
  devtools: { enabled: true },

  css: [
    '~/assets/css/main.css',
    'realgrid/dist/realgrid-style.css', // RealGrid 기본 스타일
    '~/assets/css/realgrid-theme.css', // 테마 색상 덮어쓰기
  ],
  compatibilityDate: '2025-07-15',

  // 개발 서버 설정
  devServer: {
    port: 3000, // 개발 서버 포트 고정
  },

  // Vite 서버 설정
  vite: {
    server: {
      strictPort: true, // 포트 사용 중이면 에러 발생 (다른 포트로 자동 이동 안 함)
      allowedHosts: ['.rsd-toy.com'], // rsd-toy.com의 모든 서브도메인 허용
    },
  },

  // TypeScript 강화 설정
  typescript: {
    strict: true,
    typeCheck: 'build',
  },

  // ESLint Stylistic 설정
  eslint: {
    config: {
      stylistic: {
        indent: 2,
        quotes: 'single',
        semi: false,
      },
    },
  },

  primevue: {
    options: {
      theme: {
        // ⚠️ 빌드 타임 프리셋 설정 (Aura 기반)
        // 실제 테마 전환은 main.css의 HTML 클래스 기반 CSS 변수 스코핑으로 처리
        // (.theme-linear-dark, .theme-koscom-light)
        preset: Aura,
        options: {
          // 다크모드 전환용 CSS 클래스 셀렉터
          darkModeSelector: '.app-dark',
          cssLayer: {
            // CSS 레이어 순서: Tailwind Base → PrimeVue → Tailwind Utilities
            name: 'primevue',
            order: 'tailwind-base, primevue, tailwind-utilities',
          },
        },
      },
    },
  },
})
