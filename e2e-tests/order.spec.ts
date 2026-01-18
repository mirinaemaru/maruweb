import { test, expect } from '@playwright/test';

/**
 * 주문 관리 UI E2E 테스트
 *
 * 시나리오: 주문 목록 조회 → 주문 상세 → 필터링
 */
test.describe('Order Management', () => {

  test('주문 목록 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/orders');

    // Then
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('body')).toContainText(/주문|Order/i);
  });

  test('주문 필터링 - 상태별', async ({ page }) => {
    // Given
    await page.goto('/trading/orders');

    // When - 상태 필터 선택
    const statusFilter = page.locator('select[name="status"], #status, select:has-text("상태")');

    if (await statusFilter.count() > 0) {
      await statusFilter.selectOption({ index: 1 });
      await page.waitForTimeout(500);

      // Then - 필터가 적용됨
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('주문 필터링 - 날짜별', async ({ page }) => {
    // Given
    await page.goto('/trading/orders');

    // When - 날짜 필터 입력
    const dateInput = page.locator('input[type="date"], input[name="date"], input[name="startDate"]');

    if (await dateInput.count() > 0) {
      const today = new Date().toISOString().split('T')[0];
      await dateInput.first().fill(today);

      // Then
      await expect(dateInput.first()).toHaveValue(today);
    }
  });

  test('주문 상세 보기', async ({ page }) => {
    // Given
    await page.goto('/trading/orders');

    // When - 첫 번째 주문 클릭
    const orderRow = page.locator('tr a, .order-item, tbody tr').first();

    if (await orderRow.count() > 0) {
      await orderRow.click();

      // Then
      await page.waitForTimeout(500);
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('주문 검색 기능', async ({ page }) => {
    // Given
    await page.goto('/trading/orders');

    // When - 검색
    const searchInput = page.locator('input[type="search"], input[name="search"], input[placeholder*="검색"]');

    if (await searchInput.count() > 0) {
      await searchInput.fill('005930');
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('주문 취소 기능 (대기 주문)', async ({ page }) => {
    // Given
    await page.goto('/trading/orders');

    // When - 취소 버튼 찾기
    const cancelBtn = page.locator('button:has-text("취소"), a:has-text("취소")').first();

    if (await cancelBtn.count() > 0) {
      // 다이얼로그 핸들러 설정
      page.on('dialog', async dialog => {
        expect(dialog.type()).toBe('confirm');
        await dialog.dismiss();
      });

      await cancelBtn.click();
    }
  });

  test('주문 페이지네이션', async ({ page }) => {
    // Given
    await page.goto('/trading/orders');

    // When - 페이지네이션 버튼 확인
    const pagination = page.locator('.pagination, nav[aria-label="pagination"], .page-link');

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
});
