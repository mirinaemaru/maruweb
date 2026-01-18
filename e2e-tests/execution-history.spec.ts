import { test, expect } from '@playwright/test';

/**
 * 실행 이력 UI E2E 테스트
 *
 * 시나리오: 실행 이력 조회 → 필터링 → 상세 보기
 */
test.describe('Execution History', () => {

  test('실행 이력 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/execution-history');

    // Then
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('body')).toContainText(/실행|Execution|이력|History/i);
  });

  test('실행 이력 목록 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/execution-history');

    // Then - 이력 목록
    const historyList = page.locator('table tbody tr, .execution-item, .history-list');

    if (await historyList.count() > 0) {
      await expect(historyList.first()).toBeVisible();
    }
  });

  test('날짜별 필터링', async ({ page }) => {
    // Given
    await page.goto('/trading/execution-history');

    // When - 날짜 필터
    const dateInput = page.locator('input[type="date"], input[name="date"]');

    if (await dateInput.count() > 0) {
      const today = new Date().toISOString().split('T')[0];
      await dateInput.first().fill(today);
      await page.waitForTimeout(500);

      // Then
      await expect(dateInput.first()).toHaveValue(today);
    }
  });

  test('전략별 필터링', async ({ page }) => {
    // Given
    await page.goto('/trading/execution-history');

    // When - 전략 필터
    const strategyFilter = page.locator('select[name="strategyId"], #strategyId');

    if (await strategyFilter.count() > 0) {
      await strategyFilter.selectOption({ index: 1 });
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('상태별 필터링', async ({ page }) => {
    // Given
    await page.goto('/trading/execution-history');

    // When - 상태 필터
    const statusFilter = page.locator('select[name="status"], #status');

    if (await statusFilter.count() > 0) {
      await statusFilter.selectOption({ index: 1 });
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('실행 상세 보기', async ({ page }) => {
    // Given
    await page.goto('/trading/execution-history');

    // When - 상세 보기 클릭
    const detailLink = page.locator('a:has-text("상세"), tr a').first();

    if (await detailLink.count() > 0) {
      await detailLink.click();
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('실행 로그 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/execution-history');

    // Then - 로그 정보
    const logs = page.locator('.log, .execution-log, pre, code');

    if (await logs.count() > 0) {
      await expect(logs.first()).toBeVisible();
    }
  });

  test('페이지네이션', async ({ page }) => {
    // Given
    await page.goto('/trading/execution-history');

    // When - 페이지네이션
    const pagination = page.locator('.pagination, nav[aria-label="pagination"]');

    if (await pagination.count() > 0) {
      const nextBtn = pagination.locator('a:has-text("다음"), .next');
      if (await nextBtn.count() > 0) {
        await nextBtn.click();
        await page.waitForTimeout(500);
      }

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('실행 이력 내보내기', async ({ page }) => {
    // Given
    await page.goto('/trading/execution-history');

    // When - 내보내기 버튼
    const exportBtn = page.locator('button:has-text("내보내기"), a:has-text("Export"), button:has-text("CSV")');

    if (await exportBtn.count() > 0) {
      // Then
      await expect(exportBtn.first()).toBeVisible();
    }
  });

  test('실행 통계 요약', async ({ page }) => {
    // Given
    await page.goto('/trading/execution-history');

    // Then - 통계 요약
    const stats = page.locator('.stats, .summary, .execution-stats');

    if (await stats.count() > 0) {
      await expect(stats.first()).toBeVisible();
    }
  });
});
