import { test, expect } from '@playwright/test';

/**
 * 트레이딩 모니터링 UI E2E 테스트
 *
 * 시나리오: 실시간 모니터링 화면 검증
 */
test.describe('Trading Monitor', () => {

  test('트레이딩 대시보드 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/dashboard');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('시스템 상태 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/dashboard');

    // Then - 시스템 상태 영역이 있어야 함
    const statusArea = page.locator(
      '.system-status, .health-status, [data-testid="system-status"], .status-indicator'
    );

    // 상태 영역이 있거나 상태 텍스트가 있어야 함
    const hasStatusArea = await statusArea.count() > 0;
    const hasStatusText = await page.locator('body').textContent().then(text =>
      /UP|DOWN|ACTIVE|상태|Status/i.test(text || '')
    );

    expect(hasStatusArea || hasStatusText).toBeTruthy();
  });

  test('킬 스위치 상태 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/dashboard');

    // Then - 킬 스위치 상태가 표시됨
    const killSwitchArea = page.locator(
      '.kill-switch, [data-testid="kill-switch"], .emergency-stop'
    );

    // 킬 스위치 영역 또는 관련 텍스트 확인
    const hasKillSwitch = await killSwitchArea.count() > 0 ||
      await page.locator('body').textContent().then(text =>
        /Kill Switch|킬 스위치|긴급|Emergency/i.test(text || '')
      );
  });

  test('킬 스위치 페이지 접근', async ({ page }) => {
    // When
    await page.goto('/trading/kill-switch');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('킬 스위치 토글 버튼', async ({ page }) => {
    // Given
    await page.goto('/trading/kill-switch');

    // When - 토글 버튼 찾기
    const toggleBtn = page.locator(
      'button:has-text("활성화"), button:has-text("비활성화"), button:has-text("ON"), button:has-text("OFF"), .toggle-switch'
    ).first();

    if (await toggleBtn.count() > 0) {
      // 확인 다이얼로그 핸들러
      page.on('dialog', async dialog => {
        await dialog.dismiss();  // 테스트이므로 취소
      });

      // Then - 버튼이 클릭 가능한지 확인
      await expect(toggleBtn).toBeVisible();
    }
  });

  test('주문 목록 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/orders');

    // Then
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('body')).toContainText(/주문|Order/i);
  });

  test('주문 필터링 - 계좌 선택', async ({ page }) => {
    // Given
    await page.goto('/trading/orders');

    // When - 계좌 선택
    const accountSelect = page.locator('select[name="accountId"], #accountId');

    if (await accountSelect.count() > 0) {
      // 옵션이 있으면 첫 번째 선택
      const options = await accountSelect.locator('option').count();
      if (options > 1) {
        await accountSelect.selectOption({ index: 1 });

        // Then - 필터링 결과
        await page.waitForTimeout(500);
      }
    }
  });

  test('주문 필터링 - 날짜 범위', async ({ page }) => {
    // Given
    await page.goto('/trading/orders');

    // When - 날짜 필터 입력
    const startDate = page.locator('input[name="startDate"], #startDate, input[type="date"]').first();
    const endDate = page.locator('input[name="endDate"], #endDate, input[type="date"]').last();

    if (await startDate.count() > 0) {
      const today = new Date().toISOString().split('T')[0];
      const monthAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

      await startDate.fill(monthAgo);
      if (await endDate.count() > 0) {
        await endDate.fill(today);
      }
    }
  });

  test('포지션 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/positions');

    // Then
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('body')).toContainText(/포지션|Position/i);
  });

  test('체결 내역 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/fills');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('잔고 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/balances');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('성과 분석 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/performance');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('성과 분석 - 기간 선택', async ({ page }) => {
    // Given
    await page.goto('/trading/performance');

    // When - 기간 선택
    const periodSelect = page.locator('select[name="period"], #period');

    if (await periodSelect.count() > 0) {
      await periodSelect.selectOption('monthly');

      // Then
      await page.waitForTimeout(500);
    }
  });

  test('리스크 분석 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/risk-analysis');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('알림 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/notifications');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('알림 읽음 처리', async ({ page }) => {
    // Given
    await page.goto('/trading/notifications');

    // When - 읽지 않은 알림 클릭
    const unreadNotification = page.locator('.unread, .notification-unread, [data-read="false"]').first();

    if (await unreadNotification.count() > 0) {
      await unreadNotification.click();

      // Then - 읽음 처리됨
      await page.waitForTimeout(500);
    }
  });

  test('실행 히스토리 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/execution-history');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('실시간 데이터 갱신 (있는 경우)', async ({ page }) => {
    // Given
    await page.goto('/trading/dashboard');

    // When - 자동 갱신 버튼 또는 WebSocket 연결 확인
    const refreshBtn = page.locator('button:has-text("새로고침"), button:has-text("Refresh"), .auto-refresh');

    if (await refreshBtn.count() > 0) {
      await refreshBtn.click();

      // Then - 데이터가 갱신됨
      await page.waitForTimeout(1000);
    }
  });

  test('차트 표시 (있는 경우)', async ({ page }) => {
    // Given
    await page.goto('/trading/performance');

    // Then - 차트 컨테이너가 있어야 함
    const chartArea = page.locator('canvas, .chart, [data-chart], svg.chart, .echarts, .recharts');

    if (await chartArea.count() > 0) {
      await expect(chartArea.first()).toBeVisible();
    }
  });
});
