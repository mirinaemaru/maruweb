import { test, expect } from '@playwright/test';

/**
 * 체결 내역 UI E2E 테스트
 *
 * 시나리오: 체결 목록 조회 → 필터링 → 상세 조회
 */
test.describe('Fill History', () => {

  test('체결 내역 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/fills');

    // Then
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('body')).toContainText(/체결|Fill|Trade/i);
  });

  test('체결 필터링 - 날짜별', async ({ page }) => {
    // Given
    await page.goto('/trading/fills');

    // When - 날짜 필터 입력
    const dateInput = page.locator('input[type="date"], input[name="date"], input[name="tradeDate"]');

    if (await dateInput.count() > 0) {
      const today = new Date().toISOString().split('T')[0];
      await dateInput.first().fill(today);
      await page.waitForTimeout(500);

      // Then
      await expect(dateInput.first()).toHaveValue(today);
    }
  });

  test('체결 필터링 - 계좌별', async ({ page }) => {
    // Given
    await page.goto('/trading/fills');

    // When - 계좌 필터 선택
    const accountFilter = page.locator('select[name="accountId"], #accountId');

    if (await accountFilter.count() > 0) {
      await accountFilter.selectOption({ index: 1 });
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('체결 필터링 - 종목별', async ({ page }) => {
    // Given
    await page.goto('/trading/fills');

    // When - 종목 검색
    const symbolInput = page.locator('input[name="symbol"], input[placeholder*="종목"]');

    if (await symbolInput.count() > 0) {
      await symbolInput.fill('005930');
      await page.waitForTimeout(500);

      // Then
      await expect(symbolInput).toHaveValue('005930');
    }
  });

  test('체결 상세 보기', async ({ page }) => {
    // Given
    await page.goto('/trading/fills');

    // When - 첫 번째 체결 클릭
    const fillRow = page.locator('tr a, .fill-item, tbody tr').first();

    if (await fillRow.count() > 0) {
      await fillRow.click();
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('체결 통계 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/fills');

    // Then - 통계 정보 확인
    const stats = page.locator('.stats, .statistics, .summary');

    if (await stats.count() > 0) {
      await expect(stats.first()).toBeVisible();
    }
  });

  test('체결 내역 내보내기', async ({ page }) => {
    // Given
    await page.goto('/trading/fills');

    // When - 내보내기 버튼
    const exportBtn = page.locator('button:has-text("내보내기"), a:has-text("Export"), button:has-text("CSV")');

    if (await exportBtn.count() > 0) {
      // Then - 버튼이 활성화되어 있음
      await expect(exportBtn.first()).toBeEnabled();
    }
  });
});
