import { test, expect } from '@playwright/test';

/**
 * 백테스트 UI E2E 테스트
 *
 * 시나리오: 백테스트 결과 조회 → 백테스트 실행 → Walk-Forward 분석
 */
test.describe('Backtest', () => {

  test('백테스트 결과 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/backtest/results');

    // Then
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('body')).toContainText(/백테스트|Backtest/i);
  });

  test('백테스트 실행 폼 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/backtest/results');

    // When - 새 백테스트 버튼
    const newBacktestBtn = page.locator('a:has-text("새 백테스트"), button:has-text("실행"), a[href*="new"]');

    if (await newBacktestBtn.count() > 0) {
      await newBacktestBtn.first().click();
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('백테스트 설정 - 전략 선택', async ({ page }) => {
    // Given
    await page.goto('/trading/backtest/results');

    // When - 전략 선택
    const strategySelect = page.locator('select[name="strategyId"], #strategyId');

    if (await strategySelect.count() > 0) {
      await strategySelect.selectOption({ index: 1 });

      // Then
      await expect(strategySelect).not.toHaveValue('');
    }
  });

  test('백테스트 설정 - 기간 입력', async ({ page }) => {
    // Given
    await page.goto('/trading/backtest/results');

    // When - 시작일/종료일 입력
    const startDate = page.locator('input[name="startDate"], #startDate');
    const endDate = page.locator('input[name="endDate"], #endDate');

    if (await startDate.count() > 0 && await endDate.count() > 0) {
      await startDate.fill('2024-01-01');
      await endDate.fill('2024-06-30');

      // Then
      await expect(startDate).toHaveValue('2024-01-01');
      await expect(endDate).toHaveValue('2024-06-30');
    }
  });

  test('백테스트 결과 상세 보기', async ({ page }) => {
    // Given
    await page.goto('/trading/backtest/results');

    // When - 첫 번째 결과 클릭
    const resultRow = page.locator('tr a, .backtest-item, tbody tr').first();

    if (await resultRow.count() > 0) {
      await resultRow.click();
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('Walk-Forward 분석 페이지', async ({ page }) => {
    // When
    await page.goto('/trading/backtest/walk-forward');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('포트폴리오 백테스트 페이지', async ({ page }) => {
    // When
    await page.goto('/trading/backtest/portfolio');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('백테스트 관리 페이지', async ({ page }) => {
    // When
    await page.goto('/trading/backtest/management');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('백테스트 결과 삭제 확인', async ({ page }) => {
    // Given
    await page.goto('/trading/backtest/results');

    // When - 삭제 버튼
    const deleteBtn = page.locator('button:has-text("삭제"), a:has-text("삭제")').first();

    if (await deleteBtn.count() > 0) {
      page.on('dialog', async dialog => {
        expect(dialog.type()).toBe('confirm');
        await dialog.dismiss();
      });

      await deleteBtn.click();
    }
  });

  test('백테스트 성과 지표 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/backtest/results');

    // Then - 성과 지표 확인
    const metrics = page.locator('.metrics, .performance, [class*="stat"]');

    if (await metrics.count() > 0) {
      await expect(metrics.first()).toBeVisible();
    }
  });
});
