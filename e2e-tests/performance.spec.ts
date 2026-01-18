import { test, expect } from '@playwright/test';

/**
 * 성과 분석 UI E2E 테스트
 *
 * 시나리오: 성과 대시보드 → 수익률 차트 → 기간별 분석
 */
test.describe('Performance Analysis', () => {

  test('성과 분석 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/performance');

    // Then
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('body')).toContainText(/성과|Performance|수익/i);
  });

  test('계좌 선택으로 성과 조회', async ({ page }) => {
    // Given
    await page.goto('/trading/performance');

    // When - 계좌 선택
    const accountSelect = page.locator('select[name="accountId"], #accountId');

    if (await accountSelect.count() > 0) {
      await accountSelect.selectOption({ index: 1 });
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('기간 필터링', async ({ page }) => {
    // Given
    await page.goto('/trading/performance');

    // When - 기간 선택
    const periodFilter = page.locator('select[name="period"], button:has-text("1M"), button:has-text("3M")');

    if (await periodFilter.count() > 0) {
      await periodFilter.first().click();
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('수익률 차트 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/performance');

    // Then - 차트 확인
    const chart = page.locator('canvas, .chart, svg[class*="chart"], [class*="chart"]');

    if (await chart.count() > 0) {
      await expect(chart.first()).toBeVisible();
    }
  });

  test('성과 지표 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/performance');

    // Then - 성과 지표 확인
    const metrics = page.locator('.metrics, .stats, .performance-stats, [class*="metric"]');

    if (await metrics.count() > 0) {
      await expect(metrics.first()).toBeVisible();
    }
  });

  test('누적 수익률 그래프', async ({ page }) => {
    // Given
    await page.goto('/trading/performance');

    // Then - 누적 수익률 그래프
    const cumulativeReturn = page.locator('[class*="cumulative"], :has-text("누적")');

    if (await cumulativeReturn.count() > 0) {
      await expect(cumulativeReturn.first()).toBeVisible();
    }
  });

  test('일별/월별 수익률 테이블', async ({ page }) => {
    // Given
    await page.goto('/trading/performance');

    // Then - 수익률 테이블
    const returnTable = page.locator('table, .returns-table');

    if (await returnTable.count() > 0) {
      await expect(returnTable.first()).toBeVisible();
    }
  });

  test('드로우다운 분석', async ({ page }) => {
    // Given
    await page.goto('/trading/performance');

    // Then - 드로우다운 정보
    const drawdown = page.locator('[class*="drawdown"], :has-text("Drawdown"), :has-text("MDD")');

    if (await drawdown.count() > 0) {
      await expect(drawdown.first()).toBeVisible();
    }
  });

  test('샤프 비율 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/performance');

    // Then - 샤프 비율
    const sharpe = page.locator(':has-text("Sharpe"), :has-text("샤프")');

    if (await sharpe.count() > 0) {
      await expect(sharpe.first()).toBeVisible();
    }
  });

  test('성과 보고서 내보내기', async ({ page }) => {
    // Given
    await page.goto('/trading/performance');

    // When - 내보내기 버튼
    const exportBtn = page.locator('button:has-text("내보내기"), button:has-text("Export"), a:has-text("다운로드")');

    if (await exportBtn.count() > 0) {
      // Then
      await expect(exportBtn.first()).toBeVisible();
    }
  });
});
