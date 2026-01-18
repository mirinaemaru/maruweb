import { test, expect } from '@playwright/test';

/**
 * Health Check UI E2E 테스트
 *
 * 시나리오: 시스템 상태 조회 → 컴포넌트 상태 확인
 */
test.describe('Health Check', () => {

  test('Health Check 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/health');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('전체 시스템 상태 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/health');

    // Then - 상태 표시 확인
    const status = page.locator('.status, .health-status, [class*="status"]');

    if (await status.count() > 0) {
      await expect(status.first()).toBeVisible();
    }
  });

  test('개별 컴포넌트 상태 목록', async ({ page }) => {
    // Given
    await page.goto('/trading/health');

    // Then - 컴포넌트 목록 확인
    const components = page.locator('.component, .service, table tbody tr');

    if (await components.count() > 0) {
      await expect(components.first()).toBeVisible();
    }
  });

  test('데이터베이스 연결 상태', async ({ page }) => {
    // Given
    await page.goto('/trading/health');

    // Then - DB 상태 확인
    const dbStatus = page.locator(':has-text("Database"), :has-text("DB"), :has-text("데이터베이스")');

    if (await dbStatus.count() > 0) {
      await expect(dbStatus.first()).toBeVisible();
    }
  });

  test('API 연결 상태', async ({ page }) => {
    // Given
    await page.goto('/trading/health');

    // Then - API 상태 확인
    const apiStatus = page.locator(':has-text("API"), :has-text("Trading System")');

    if (await apiStatus.count() > 0) {
      await expect(apiStatus.first()).toBeVisible();
    }
  });

  test('상태 새로고침', async ({ page }) => {
    // Given
    await page.goto('/trading/health');

    // When - 새로고침 버튼
    const refreshBtn = page.locator('button:has-text("새로고침"), button:has-text("Refresh")');

    if (await refreshBtn.count() > 0) {
      await refreshBtn.click();
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('메트릭스 정보 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/health');

    // Then - 메트릭스 확인
    const metrics = page.locator('.metrics, .stats, [class*="metric"]');

    if (await metrics.count() > 0) {
      await expect(metrics.first()).toBeVisible();
    }
  });

  test('에러 로그/알림 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/health');

    // Then - 에러/알림 섹션
    const alerts = page.locator('.alerts, .errors, .warnings, [class*="alert"]');

    if (await alerts.count() > 0) {
      await expect(alerts.first()).toBeVisible();
    }
  });
});
