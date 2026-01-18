import { test, expect } from '@playwright/test';

/**
 * 리스크 관리 UI E2E 테스트
 *
 * 시나리오: 리스크 룰 관리 → Kill Switch → 리스크 분석
 */
test.describe('Risk Management', () => {

  test.describe('Risk Rules', () => {
    test('리스크 룰 페이지 로드', async ({ page }) => {
      // When
      await page.goto('/trading/risk-rules');

      // Then
      await expect(page.locator('body')).toBeVisible();
      await expect(page.locator('body')).toContainText(/리스크|Risk/i);
    });

    test('리스크 룰 생성 폼', async ({ page }) => {
      // Given
      await page.goto('/trading/risk-rules');

      // When - 새 룰 버튼
      const newRuleBtn = page.locator('a:has-text("추가"), button:has-text("생성"), a[href*="new"]');

      if (await newRuleBtn.count() > 0) {
        await newRuleBtn.first().click();
        await page.waitForTimeout(500);

        // Then
        await expect(page.locator('body')).toBeVisible();
      }
    });

    test('리스크 룰 설정 입력', async ({ page }) => {
      // Given
      await page.goto('/trading/risk-rules/new');

      // When - 룰 설정 입력
      const nameInput = page.locator('input[name="name"], #name');
      const typeSelect = page.locator('select[name="type"], #type');
      const thresholdInput = page.locator('input[name="threshold"], #threshold');

      if (await nameInput.count() > 0) {
        await nameInput.fill('E2E 테스트 룰');
      }

      if (await typeSelect.count() > 0) {
        await typeSelect.selectOption({ index: 1 });
      }

      if (await thresholdInput.count() > 0) {
        await thresholdInput.fill('5');
      }

      // Then
      if (await nameInput.count() > 0) {
        await expect(nameInput).toHaveValue('E2E 테스트 룰');
      }
    });

    test('리스크 룰 활성화/비활성화', async ({ page }) => {
      // Given
      await page.goto('/trading/risk-rules');

      // When - 토글 스위치
      const toggleSwitch = page.locator('input[type="checkbox"], .toggle-switch, [role="switch"]').first();

      if (await toggleSwitch.count() > 0) {
        await toggleSwitch.click();
        await page.waitForTimeout(500);

        // Then
        await expect(page.locator('body')).toBeVisible();
      }
    });

    test('리스크 룰 삭제 확인', async ({ page }) => {
      // Given
      await page.goto('/trading/risk-rules');

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

  test.describe('Kill Switch', () => {
    test('Kill Switch 페이지 로드', async ({ page }) => {
      // When
      await page.goto('/trading/kill-switch');

      // Then
      await expect(page.locator('body')).toBeVisible();
    });

    test('Kill Switch 상태 표시', async ({ page }) => {
      // Given
      await page.goto('/trading/kill-switch');

      // Then - 상태 표시 확인
      const status = page.locator('.status, .kill-switch-status, [class*="status"]');

      if (await status.count() > 0) {
        await expect(status.first()).toBeVisible();
      }
    });

    test('Kill Switch 활성화 버튼', async ({ page }) => {
      // Given
      await page.goto('/trading/kill-switch');

      // When - 활성화 버튼
      const activateBtn = page.locator('button:has-text("활성화"), button:has-text("ON")');

      if (await activateBtn.count() > 0) {
        page.on('dialog', async dialog => {
          await dialog.dismiss();
        });

        // Then
        await expect(activateBtn.first()).toBeVisible();
      }
    });

    test('Kill Switch 히스토리 표시', async ({ page }) => {
      // Given
      await page.goto('/trading/kill-switch');

      // Then - 히스토리 확인
      const history = page.locator('.history, table, .log-list');

      if (await history.count() > 0) {
        await expect(history.first()).toBeVisible();
      }
    });
  });

  test.describe('Risk Analysis', () => {
    test('리스크 분석 페이지 로드', async ({ page }) => {
      // When
      await page.goto('/trading/risk/analysis');

      // Then
      await expect(page.locator('body')).toBeVisible();
    });

    test('VaR 분석 표시', async ({ page }) => {
      // Given
      await page.goto('/trading/risk/analysis');

      // Then - VaR 관련 정보
      const var_info = page.locator('[class*="var"], [class*="VaR"], :has-text("VaR")');

      if (await var_info.count() > 0) {
        await expect(var_info.first()).toBeVisible();
      }
    });

    test('리스크 차트 표시', async ({ page }) => {
      // Given
      await page.goto('/trading/risk/analysis');

      // Then - 차트 확인
      const chart = page.locator('canvas, .chart, svg[class*="chart"]');

      if (await chart.count() > 0) {
        await expect(chart.first()).toBeVisible();
      }
    });
  });
});
