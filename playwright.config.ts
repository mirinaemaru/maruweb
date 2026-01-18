import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright 설정 파일
 * maruweb UI E2E 테스트용
 *
 * 사용법:
 * 1. npm install
 * 2. npx playwright install chromium
 * 3. ./run-maruweb.sh (개발 서버 시작)
 * 4. npm run test:e2e
 */
export default defineConfig({
  // 테스트 디렉토리
  testDir: './e2e-tests',

  // 테스트 파일 패턴
  testMatch: '**/*.spec.ts',

  // 전체 테스트 타임아웃 (30초)
  timeout: 30000,

  // 각 expect의 타임아웃
  expect: {
    timeout: 5000
  },

  // 병렬 실행 설정
  fullyParallel: true,

  // 실패 시 재시도 횟수
  retries: process.env.CI ? 2 : 0,

  // 병렬 워커 수
  workers: process.env.CI ? 1 : undefined,

  // 리포터 설정
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['list']
  ],

  // 공통 설정
  use: {
    // maruweb 서버 URL
    baseURL: 'http://localhost:8090',

    // 헤드리스 모드 (CI에서는 headless, 로컬에서는 선택 가능)
    headless: true,

    // 스크린샷 설정 - 실패시에만
    screenshot: 'only-on-failure',

    // 비디오 녹화 - 실패시에만
    video: 'retain-on-failure',

    // 트레이스 수집 - 재시도시에만
    trace: 'on-first-retry',

    // 뷰포트 크기
    viewport: { width: 1280, height: 720 },

    // 액션 타임아웃
    actionTimeout: 10000,

    // 네비게이션 타임아웃
    navigationTimeout: 15000,
  },

  // 프로젝트 (브라우저별 설정)
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    // 필요시 다른 브라우저 추가
    // {
    //   name: 'firefox',
    //   use: { ...devices['Desktop Firefox'] },
    // },
    // {
    //   name: 'webkit',
    //   use: { ...devices['Desktop Safari'] },
    // },
  ],

  // 웹 서버 설정 (테스트 실행 전 서버 시작)
  webServer: {
    command: './run-maruweb.sh',
    url: 'http://localhost:8090',
    reuseExistingServer: true,
    timeout: 120000,  // 서버 시작 대기 시간 (2분)
  },

  // 출력 디렉토리
  outputDir: 'test-results/',
});
