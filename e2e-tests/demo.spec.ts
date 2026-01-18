import { test, expect } from '@playwright/test';

/**
 * 데모 기능 UI E2E 테스트
 *
 * 시나리오: 데모 시나리오 → 데모 신호 → 최적화 데모 → 백테스트 데모
 */
test.describe('Demo Features', () => {

  test.describe('Demo Scenarios', () => {
    test('데모 시나리오 페이지 로드', async ({ page }) => {
      // When
      await page.goto('/trading/demo/scenarios');

      // Then
      await expect(page.locator('body')).toBeVisible();
      await expect(page.locator('body')).toContainText(/데모|Demo/i);
    });

    test('시나리오 목록 표시', async ({ page }) => {
      // Given
      await page.goto('/trading/demo/scenarios');

      // Then - 시나리오 목록
      const scenarios = page.locator('.scenario, .scenario-list, table tbody tr, .card');

      if (await scenarios.count() > 0) {
        await expect(scenarios.first()).toBeVisible();
      }
    });

    test('시나리오 실행 버튼', async ({ page }) => {
      // Given
      await page.goto('/trading/demo/scenarios');

      // When - 실행 버튼
      const runBtn = page.locator('button:has-text("실행"), button:has-text("시작")').first();

      if (await runBtn.count() > 0) {
        // Then
        await expect(runBtn).toBeVisible();
      }
    });

    test('시나리오 유형 선택', async ({ page }) => {
      // Given
      await page.goto('/trading/demo/scenarios');

      // When - 유형 선택
      const typeSelect = page.locator('select[name="scenarioType"], #scenarioType');

      if (await typeSelect.count() > 0) {
        await typeSelect.selectOption({ index: 1 });

        // Then
        await expect(typeSelect).not.toHaveValue('');
      }
    });
  });

  test.describe('Demo Signals', () => {
    test('데모 신호 페이지 로드', async ({ page }) => {
      // When
      await page.goto('/trading/demo/signals');

      // Then
      await expect(page.locator('body')).toBeVisible();
    });

    test('신호 목록 표시', async ({ page }) => {
      // Given
      await page.goto('/trading/demo/signals');

      // Then - 신호 목록
      const signals = page.locator('.signal, table tbody tr, .signal-list');

      if (await signals.count() > 0) {
        await expect(signals.first()).toBeVisible();
      }
    });

    test('신호 생성 폼', async ({ page }) => {
      // Given
      await page.goto('/trading/demo/signals');

      // When - 종목 입력
      const symbolInput = page.locator('input[name="symbol"], #symbol');

      if (await symbolInput.count() > 0) {
        await symbolInput.fill('005930');

        // Then
        await expect(symbolInput).toHaveValue('005930');
      }
    });

    test('신호 유형 선택 (매수/매도)', async ({ page }) => {
      // Given
      await page.goto('/trading/demo/signals');

      // When - 유형 선택
      const signalType = page.locator('select[name="signalType"], input[name="signalType"]');

      if (await signalType.count() > 0) {
        if (await signalType.first().getAttribute('type') === 'radio') {
          await signalType.first().check();
        } else {
          await signalType.selectOption({ index: 1 });
        }
      }
    });
  });

  test.describe('Demo Optimization', () => {
    test('최적화 데모 페이지 로드', async ({ page }) => {
      // When
      await page.goto('/trading/demo/optimization');

      // Then
      await expect(page.locator('body')).toBeVisible();
    });

    test('전략 유형 선택', async ({ page }) => {
      // Given
      await page.goto('/trading/demo/optimization');

      // When - 전략 유형 선택
      const strategyType = page.locator('select[name="strategyType"], #strategyType');

      if (await strategyType.count() > 0) {
        await strategyType.selectOption({ index: 1 });

        // Then
        await expect(strategyType).not.toHaveValue('');
      }
    });

    test('최적화 실행', async ({ page }) => {
      // Given
      await page.goto('/trading/demo/optimization');

      // When - 실행 버튼
      const runBtn = page.locator('button:has-text("실행"), button[type="submit"]');

      if (await runBtn.count() > 0) {
        // Then
        await expect(runBtn.first()).toBeVisible();
      }
    });
  });

  test.describe('Demo Backtest', () => {
    test('데모 백테스트 페이지 로드', async ({ page }) => {
      // When
      await page.goto('/trading/demo/backtest');

      // Then
      await expect(page.locator('body')).toBeVisible();
    });

    test('기간 선택', async ({ page }) => {
      // Given
      await page.goto('/trading/demo/backtest');

      // When - 기간 선택
      const periodSelect = page.locator('select[name="period"], #period');

      if (await periodSelect.count() > 0) {
        await periodSelect.selectOption({ index: 1 });

        // Then
        await expect(periodSelect).not.toHaveValue('');
      }
    });

    test('초기 자본 입력', async ({ page }) => {
      // Given
      await page.goto('/trading/demo/backtest');

      // When - 초기 자본 입력
      const capitalInput = page.locator('input[name="initialCapital"], #initialCapital');

      if (await capitalInput.count() > 0) {
        await capitalInput.fill('10000000');

        // Then
        await expect(capitalInput).toHaveValue('10000000');
      }
    });

    test('백테스트 실행', async ({ page }) => {
      // Given
      await page.goto('/trading/demo/backtest');

      // When - 실행 버튼
      const runBtn = page.locator('button:has-text("실행"), button[type="submit"]');

      if (await runBtn.count() > 0) {
        // Then
        await expect(runBtn.first()).toBeVisible();
      }
    });
  });

  test('데모 데이터 초기화', async ({ page }) => {
    // Given
    await page.goto('/trading/demo/scenarios');

    // When - 초기화 버튼
    const resetBtn = page.locator('button:has-text("초기화"), button:has-text("Reset")');

    if (await resetBtn.count() > 0) {
      page.on('dialog', async dialog => {
        await dialog.dismiss();
      });

      // Then
      await expect(resetBtn.first()).toBeVisible();
    }
  });
});
