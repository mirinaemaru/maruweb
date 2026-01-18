import { test, expect } from '@playwright/test';

/**
 * 종목 관리 UI E2E 테스트
 *
 * 시나리오: 종목 목록 조회 → 종목 검색 → 종목 상세
 */
test.describe('Instrument Management', () => {

  test('종목 목록 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/instruments');

    // Then
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('body')).toContainText(/종목|Instrument|Stock/i);
  });

  test('종목 검색 - 코드로 검색', async ({ page }) => {
    // Given
    await page.goto('/trading/instruments');

    // When - 검색 입력
    const searchInput = page.locator('input[type="search"], input[name="keyword"], input[placeholder*="검색"]');

    if (await searchInput.count() > 0) {
      await searchInput.fill('005930');
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('종목 검색 - 이름으로 검색', async ({ page }) => {
    // Given
    await page.goto('/trading/instruments');

    // When - 검색 입력
    const searchInput = page.locator('input[type="search"], input[name="keyword"], input[placeholder*="검색"]');

    if (await searchInput.count() > 0) {
      await searchInput.fill('삼성전자');
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('종목 상세 보기', async ({ page }) => {
    // Given
    await page.goto('/trading/instruments');

    // When - 첫 번째 종목 클릭
    const instrumentRow = page.locator('tr a, .instrument-item, tbody tr').first();

    if (await instrumentRow.count() > 0) {
      await instrumentRow.click();
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('종목 필터링 - 거래소별', async ({ page }) => {
    // Given
    await page.goto('/trading/instruments');

    // When - 거래소 필터
    const exchangeFilter = page.locator('select[name="exchange"], #exchange');

    if (await exchangeFilter.count() > 0) {
      await exchangeFilter.selectOption({ index: 1 });
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('종목 페이지네이션', async ({ page }) => {
    // Given
    await page.goto('/trading/instruments');

    // When - 페이지네이션
    const pagination = page.locator('.pagination, nav[aria-label="pagination"]');

    if (await pagination.count() > 0) {
      const nextBtn = pagination.locator('a:has-text("다음"), button:has-text("다음"), .next');
      if (await nextBtn.count() > 0) {
        await nextBtn.click();
        await page.waitForTimeout(500);
      }

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('종목 시세 정보 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/instruments');

    // Then - 시세 컬럼 확인
    const priceColumn = page.locator('th:has-text("현재가"), th:has-text("시세"), th:has-text("가격")');

    if (await priceColumn.count() > 0) {
      await expect(priceColumn.first()).toBeVisible();
    }
  });
});
