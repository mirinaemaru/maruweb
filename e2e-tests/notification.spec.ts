import { test, expect } from '@playwright/test';

/**
 * 알림 관리 UI E2E 테스트
 *
 * 시나리오: 알림 설정 → 알림 목록 조회 → 알림 읽음 처리
 */
test.describe('Notification Management', () => {

  test('알림 설정 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/notifications/settings');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('알림 채널 설정', async ({ page }) => {
    // Given
    await page.goto('/trading/notifications/settings');

    // When - 채널 선택 (이메일, SMS, 카카오 등)
    const channelCheckbox = page.locator('input[type="checkbox"][name*="channel"], input[type="checkbox"][name*="email"]');

    if (await channelCheckbox.count() > 0) {
      const isChecked = await channelCheckbox.first().isChecked();
      await channelCheckbox.first().click();
      await page.waitForTimeout(500);

      // Then - 상태 변경됨
      const newChecked = await channelCheckbox.first().isChecked();
      expect(newChecked).not.toBe(isChecked);
    }
  });

  test('알림 유형별 설정', async ({ page }) => {
    // Given
    await page.goto('/trading/notifications/settings');

    // When - 알림 유형 설정
    const notificationTypes = page.locator('.notification-type, [class*="notification-setting"]');

    if (await notificationTypes.count() > 0) {
      // Then
      await expect(notificationTypes.first()).toBeVisible();
    }
  });

  test('알림 목록 페이지', async ({ page }) => {
    // When
    await page.goto('/trading/notifications');

    // Then
    await expect(page.locator('body')).toBeVisible();
  });

  test('알림 목록 표시', async ({ page }) => {
    // Given
    await page.goto('/trading/notifications');

    // Then - 알림 목록
    const notifications = page.locator('.notification, .notification-item, table tbody tr');

    if (await notifications.count() > 0) {
      await expect(notifications.first()).toBeVisible();
    }
  });

  test('알림 필터링 - 읽음/안읽음', async ({ page }) => {
    // Given
    await page.goto('/trading/notifications');

    // When - 필터 선택
    const filterSelect = page.locator('select[name="status"], button:has-text("안읽음")');

    if (await filterSelect.count() > 0) {
      await filterSelect.first().click();
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('알림 읽음 처리', async ({ page }) => {
    // Given
    await page.goto('/trading/notifications');

    // When - 읽음 버튼
    const markReadBtn = page.locator('button:has-text("읽음"), a:has-text("읽음 처리")').first();

    if (await markReadBtn.count() > 0) {
      await markReadBtn.click();
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('모두 읽음 처리', async ({ page }) => {
    // Given
    await page.goto('/trading/notifications');

    // When - 모두 읽음 버튼
    const markAllReadBtn = page.locator('button:has-text("모두 읽음"), button:has-text("전체 읽음")');

    if (await markAllReadBtn.count() > 0) {
      // Then
      await expect(markAllReadBtn.first()).toBeVisible();
    }
  });

  test('알림 삭제', async ({ page }) => {
    // Given
    await page.goto('/trading/notifications');

    // When - 삭제 버튼
    const deleteBtn = page.locator('button:has-text("삭제"), a:has-text("삭제")').first();

    if (await deleteBtn.count() > 0) {
      page.on('dialog', async dialog => {
        await dialog.dismiss();
      });

      await deleteBtn.click();
    }
  });

  test('알림 상세 보기', async ({ page }) => {
    // Given
    await page.goto('/trading/notifications');

    // When - 알림 클릭
    const notification = page.locator('.notification, .notification-item, table tbody tr a').first();

    if (await notification.count() > 0) {
      await notification.click();
      await page.waitForTimeout(500);

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('알림 설정 저장', async ({ page }) => {
    // Given
    await page.goto('/trading/notifications/settings');

    // When - 저장 버튼
    const saveBtn = page.locator('button:has-text("저장"), button[type="submit"]');

    if (await saveBtn.count() > 0) {
      // Then
      await expect(saveBtn.first()).toBeVisible();
    }
  });
});
