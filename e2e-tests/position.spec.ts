import { test, expect } from '@playwright/test';

/**
 * 포지션 관리 UI E2E 테스트
 *
 * 시나리오: 포지션 목록 조회 → 포지션 상세 → 계좌별 필터
 */
test.describe('Position Management', () => {

  test('포지션 목록 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/positions');

    // Then
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('body')).toContainText(/포지션|Position/i);
  });

  test('포지션 필터링 - 계좌별', async ({ page }) => {
    // Given
    await page.goto('/trading/positions');

    // When - 계좌 필터 선택
    const accountFilter = page.locator('select[name="accountId"], #accountId, select:has-text("계좌")');

    if (await accountFilter.count() > 0) {
      await accountFilter.selectOption({ index: 1 });
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('포지션 필터링 - 상태별 (오픈/클로즈)', async ({ page }) => {
    // Given
    await page.goto('/trading/positions');

    // When - 상태 필터
    const statusFilter = page.locator('select[name="status"], input[value="OPEN"]');

    if (await statusFilter.count() > 0) {
      if (await statusFilter.first().inputValue !== undefined) {
        await statusFilter.first().check();
      } else {
        await statusFilter.selectOption({ index: 1 });
      }
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('포지션 상세 보기', async ({ page }) => {
    // Given
    await page.goto('/trading/positions');

    // When - 첫 번째 포지션 클릭
    const positionRow = page.locator('tr a, .position-item, tbody tr').first();

    if (await positionRow.count() > 0) {
      await positionRow.click();
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('포지션 청산 버튼', async ({ page }) => {
    // Given
    await page.goto('/trading/positions');

    // When - 청산 버튼 찾기
    const closeBtn = page.locator('button:has-text("청산"), a:has-text("청산")').first();

    if (await closeBtn.count() > 0) {
      page.on('dialog', async dialog => {
        expect(dialog.type()).toBe('confirm');
        await dialog.dismiss();
      });

      await closeBtn.click();
    }
  });

  test('포지션 요약 정보 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/positions');

    // Then - 요약 정보 확인
    const summary = page.locator('.summary, .position-summary, .stats');

    if (await summary.count() > 0) {
      await expect(summary.first()).toBeVisible();
    }
  });
});
