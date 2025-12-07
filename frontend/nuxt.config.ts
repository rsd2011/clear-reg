// https://nuxt.com/docs/api/configuration/nuxt-config
import Aura from '@primeuix/themes/aura'

export default defineNuxtConfig({
  // SSR 비활성화 - CSR(Client Side Rendering) 전용 SPA 모드
  // PrimeVue Hydration 경고 완전 제거

  modules: [
    '@nuxt/eslint',
    // '@nuxt/content', // 미사용 - 필요 시 content.config.ts와 함께 활성화
    '@nuxt/fonts',
    // '@nuxt/icon', // 미사용 - PrimeIcons (pi pi-*) 사용 중
    // '@nuxt/image', // 미사용 - 필요 시 활성화
    // '@nuxt/scripts', // 미사용 - 필요 시 활성화
    '@nuxt/test-utils/module',
    // '@nuxt/hints', // SSR 비활성화로 hydration 체크 불필요
    '@nuxtjs/tailwindcss',
    '@primevue/nuxt-module',
    '@pinia/nuxt',
  ],
  ssr: false,

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
    // RealGrid 컴포넌트 - 프리픽스 없음
    { path: '~/components/realgrid', pathPrefix: false },
  ],
  devtools: { enabled: true },

  // 테마 FOUC(Flash of Unstyled Content) 방지를 위한 inline script
  // 🆕 하이브리드 방식: CSS light-dark()가 시스템 기본 처리, JS는 저장된 선택만 처리
  app: {
    head: {
      script: [
        {
          innerHTML: `
            (function() {
              try {
                // 🆕 하이브리드 FOUC 방지:
                // 1. CSS light-dark()가 시스템 기본 테마 즉시 처리 (FOUC 완전 방지)
                // 2. 저장된 사용자 선택이 있으면 클래스로 오버라이드
                var themeName = null;
                var themeMode = null;

                // 저장된 테마 확인 (pinia-plugin-persistedstate)
                var stored = localStorage.getItem('app-theme');
                if (stored) {
                  var parsed = JSON.parse(stored);
                  themeName = parsed.themeName;
                  themeMode = parsed.themeMode;
                } else {
                  // 레거시 폴백
                  themeName = localStorage.getItem('app-theme-name');
                  themeMode = localStorage.getItem('app-theme-mode');
                }

                // 저장된 테마가 없으면 기본값 사용
                if (!themeName) {
                  themeName = 'linear-dark';
                  themeMode = 'system';
                }

                // 테마 클래스 매핑
                var themeClasses = {
                  'linear-dark': 'theme-linear-dark',
                  'github-dark': 'theme-github-dark',
                  'figma-dark': 'theme-figma-dark',
                  'slack-aubergine': 'theme-slack-aubergine',
                  'koscom-light': 'theme-koscom-light',
                  'notion-light': 'theme-notion-light'
                };

                var themeClass = themeClasses[themeName];
                if (themeClass) {
                  document.documentElement.classList.add(themeClass);
                }

                // 다크/라이트 모드 처리
                // - 'system': CSS light-dark()에 위임 (클래스 추가 안함)
                // - 'dark': app-dark 클래스로 다크모드 강제
                // - 'light': app-light 클래스로 라이트모드 강제
                if (themeMode === 'dark') {
                  document.documentElement.classList.add('app-dark');
                } else if (themeMode === 'light') {
                  document.documentElement.classList.add('app-light');
                } else if (themeMode === 'system') {
                  // 시스템 모드: 현재 시스템 설정에 따라 클래스 추가
                  // CSS light-dark()도 있지만, PrimeVue 컴포넌트는 클래스 기반이므로 필요
                  if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
                    document.documentElement.classList.add('app-dark');
                  }
                }
              } catch (e) {}
            })();
          `,
          type: 'text/javascript',
        },
      ],
    },
  },

  css: [
    '~/assets/css/main.css',
    'realgrid/dist/realgrid-style.css', // RealGrid 기본 스타일 (라이트모드)
    // RealGrid 다크테마: main.css에서 .app-dark 스코프로 핵심 스타일 적용
  ],

  // 개발 서버 설정
  devServer: {
    port: 3000, // 개발 서버 포트 고정
  },
  compatibilityDate: '2025-07-15',

  // Vite 설정 (서버 + 빌드 최적화)
  vite: {
    server: {
      strictPort: true, // 포트 사용 중이면 에러 발생 (다른 포트로 자동 이동 안 함)
      allowedHosts: ['.rsd-toy.com'], // rsd-toy.com의 모든 서브도메인 허용
    },
    // 🚀 빌드 최적화: 무거운 라이브러리 청크 분리
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            // RealGrid + JSZip (Excel 내보내기) - ~8MB
            'vendor-realgrid': ['realgrid', 'jszip'],
            // DockView (도킹 레이아웃) - ~15MB
            'vendor-dockview': ['dockview-core', 'dockview-vue'],
            // PrimeVue 코어 - ~19MB
            'vendor-primevue': ['primevue'],
          },
        },
      },
    },
    // 의존성 사전 번들링 최적화
    optimizeDeps: {
      include: ['realgrid', 'jszip', 'dockview-core', 'dockview-vue', 'primevue'],
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
