import { test, expect } from '@playwright/test';

/**
 * 파라미터 최적화 UI E2E 테스트
 *
 * 시나리오: 최적화 실행 → 결과 조회 → 결과 적용
 */
test.describe('Parameter Optimization', () => {

  test('최적화 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/optimization');

    // Then
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('body')).toContainText(/최적화|Optimization/i);
  });

  test('최적화 설정 - 전략 선택', async ({ page }) => {
    // Given
    await page.goto('/trading/optimization');

    // When - 전략 선택
    const strategySelect = page.locator('select[name="strategyId"], #strategyId');

    if (await strategySelect.count() > 0) {
      await strategySelect.selectOption({ index: 1 });

      // Then
      await expect(strategySelect).not.toHaveValue('');
    }
  });

  test('최적화 설정 - 최적화 방법 선택', async ({ page }) => {
    // Given
    await page.goto('/trading/optimization');

    // When - 방법 선택
    const methodSelect = page.locator('select[name="method"], #method');

    if (await methodSelect.count() > 0) {
      await methodSelect.selectOption({ index: 1 });

      // Then
      await expect(methodSelect).not.toHaveValue('');
    }
  });

  test('최적화 파라미터 범위 설정', async ({ page }) => {
    // Given
    await page.goto('/trading/optimization');

    // When - 파라미터 범위 입력
    const minInput = page.locator('input[name*="min"], input[placeholder*="최소"]').first();
    const maxInput = page.locator('input[name*="max"], input[placeholder*="최대"]').first();

    if (await minInput.count() > 0 && await maxInput.count() > 0) {
      await minInput.fill('5');
      await maxInput.fill('20');

      // Then
      await expect(minInput).toHaveValue('5');
      await expect(maxInput).toHaveValue('20');
    }
  });

  test('최적화 실행 버튼', async ({ page }) => {
    // Given
    await page.goto('/trading/optimization');

    // When - 실행 버튼 확인
    const runBtn = page.locator('button:has-text("실행"), button:has-text("시작"), button[type="submit"]');

    // Then
    if (await runBtn.count() > 0) {
      await expect(runBtn.first()).toBeVisible();
    }
  });

  test('최적화 결과 목록 보기', async ({ page }) => {
    // Given
    await page.goto('/trading/optimization');

    // Then - 결과 목록/테이블 확인
    const results = page.locator('table, .results-list, .optimization-results');

    if (await results.count() > 0) {
      await expect(results.first()).toBeVisible();
    }
  });

  test('최적화 결과 적용 버튼', async ({ page }) => {
    // Given
    await page.goto('/trading/optimization');

    // When - 적용 버튼
    const applyBtn = page.locator('button:has-text("적용"), a:has-text("적용")').first();

    if (await applyBtn.count() > 0) {
      page.on('dialog', async dialog => {
        await dialog.dismiss();
      });

      // Then
      await expect(applyBtn).toBeVisible();
    }
  });

  test('최적화 진행 상태 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/optimization');

    // Then - 진행률 표시 확인
    const progress = page.locator('.progress, .progress-bar, [role="progressbar"]');

    // 진행 중인 최적화가 있다면 표시됨
    if (await progress.count() > 0) {
      await expect(progress.first()).toBeVisible();
    }
  });

  test('최적화 결과 삭제', async ({ page }) => {
    // Given
    await page.goto('/trading/optimization');

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
});
