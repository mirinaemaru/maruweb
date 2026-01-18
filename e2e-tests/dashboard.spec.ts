import { test, expect } from '@playwright/test';

/**
 * 대시보드 UI E2E 테스트
 *
 * 시나리오: 대시보드 로드, 메뉴 네비게이션
 */
test.describe('Dashboard', () => {

  test('메인 대시보드 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/dashboard');

    // Then
    await expect(page).toHaveTitle(/maruweb|대시보드|Dashboard/i);
    await expect(page.locator('body')).toBeVisible();
  });

  test('사이드바 메뉴가 표시됨', async ({ page }) => {
    // Given
    await page.goto('/dashboard');

    // Then - 사이드바 또는 네비게이션 메뉴가 있어야 함
    const sidebar = page.locator('.sidebar, .nav, nav, .menu');
    await expect(sidebar.first()).toBeVisible();
  });

  test('Trading 메뉴로 이동', async ({ page }) => {
    // Given
    await page.goto('/dashboard');

    // When - Trading 관련 링크 클릭
    const tradingLink = page.locator('a[href*="trading"], a:has-text("Trading"), a:has-text("트레이딩")');
    if (await tradingLink.count() > 0) {
      await tradingLink.first().click();

      // Then
      await expect(page).toHaveURL(/trading/);
    }
  });

  test('Todo 메뉴로 이동', async ({ page }) => {
    // Given
    await page.goto('/dashboard');

    // When - Todo 관련 링크 클릭
    const todoLink = page.locator('a[href*="todo"], a:has-text("Todo"), a:has-text("할 일")');
    if (await todoLink.count() > 0) {
      await todoLink.first().click();

      // Then
      await expect(page).toHaveURL(/todo/);
    }
  });

  test('Calendar 메뉴로 이동', async ({ page }) => {
    // Given
    await page.goto('/dashboard');

    // When
    const calendarLink = page.locator('a[href*="calendar"], a:has-text("Calendar"), a:has-text("캘린더")');
    if (await calendarLink.count() > 0) {
      await calendarLink.first().click();

      // Then
      await expect(page).toHaveURL(/calendar/);
    }
  });

  test('Kanban 메뉴로 이동', async ({ page }) => {
    // Given
    await page.goto('/dashboard');

    // When
    const kanbanLink = page.locator('a[href*="kanban"], a:has-text("Kanban"), a:has-text("칸반")');
    if (await kanbanLink.count() > 0) {
      await kanbanLink.first().click();

      // Then
      await expect(page).toHaveURL(/kanban/);
    }
  });

  test('다크모드 토글 (있는 경우)', async ({ page }) => {
    // Given
    await page.goto('/dashboard');

    // When - 다크모드 토글 버튼 찾기
    const darkModeToggle = page.locator(
      'button[data-theme-toggle], .theme-toggle, .dark-mode-toggle, #darkModeToggle'
    );

    if (await darkModeToggle.count() > 0) {
      const initialTheme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));

      await darkModeToggle.click();

      // Then - 테마가 변경되어야 함
      const newTheme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
      expect(newTheme).not.toBe(initialTheme);
    }
  });

  test('반응형 레이아웃 - 모바일 뷰', async ({ page }) => {
    // Given - 모바일 뷰포트
    await page.setViewportSize({ width: 375, height: 667 });

    // When
    await page.goto('/dashboard');

    // Then - 페이지가 정상 로드됨
    await expect(page.locator('body')).toBeVisible();
  });

  test('반응형 레이아웃 - 태블릿 뷰', async ({ page }) => {
    // Given - 태블릿 뷰포트
    await page.setViewportSize({ width: 768, height: 1024 });

    // When
    await page.goto('/dashboard');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });
});
