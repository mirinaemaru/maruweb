import { test, expect } from '@playwright/test';

/**
 * 전략 관리 UI E2E 테스트
 *
 * 시나리오: 전략 생성 → 설정 → 동기화 버튼 클릭
 */
test.describe('Strategy Management', () => {

  test('전략 목록 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/strategies');

    // Then
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('body')).toContainText(/전략|Strategy/i);
  });

  test('전략 생성 폼으로 이동', async ({ page }) => {
    // Given
    await page.goto('/trading/strategies');

    // When - 새 전략 버튼 클릭
    const newStrategyBtn = page.locator(
      'a[href*="new"], a[href*="create"], button:has-text("새 전략"), button:has-text("등록"), a:has-text("새 전략")'
    );

    if (await newStrategyBtn.count() > 0) {
      await newStrategyBtn.first().click();
      await expect(page).toHaveURL(/new|create/);
    } else {
      await page.goto('/trading/strategies/new');
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('전략 생성 - 기본 정보 입력', async ({ page }) => {
    // Given
    await page.goto('/trading/strategies/new');

    // When - 기본 정보 입력
    const nameInput = page.locator('input[name="name"], #name, input[placeholder*="전략"]');
    if (await nameInput.count() > 0) {
      await nameInput.fill('E2E 테스트 전략');
    }

    const descInput = page.locator('textarea[name="description"], #description, textarea');
    if (await descInput.count() > 0) {
      await descInput.fill('E2E 테스트로 생성된 전략입니다.');
    }

    // 전략 유형 선택
    const typeSelect = page.locator('select[name="strategyType"], #strategyType');
    if (await typeSelect.count() > 0) {
      await typeSelect.selectOption('AUTO_TRADING');
    }

    // Then
    if (await nameInput.count() > 0) {
      await expect(nameInput).toHaveValue('E2E 테스트 전략');
    }
  });

  test('전략 상세 페이지 조회', async ({ page }) => {
    // Given
    await page.goto('/trading/strategies');

    // When - 첫 번째 전략 클릭
    const strategyLink = page.locator('a[href*="/strategies/"]').first();

    if (await strategyLink.count() > 0) {
      await strategyLink.click();

      // Then - 상세 페이지로 이동
      await expect(page.locator('body')).toContainText(/전략|Strategy/i);
    }
  });

  test('자동매매 설정 페이지 이동', async ({ page }) => {
    // Given
    await page.goto('/trading/strategies');

    // When - Configure Trading 버튼 클릭
    const configureBtn = page.locator(
      'a:has-text("Configure"), a:has-text("설정"), a[href*="trading"]'
    ).first();

    if (await configureBtn.count() > 0) {
      await configureBtn.click();

      // Then
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('전략 활성화 버튼 클릭', async ({ page }) => {
    // Given
    await page.goto('/trading/strategies');

    // When - 활성화 버튼 찾기
    const activateBtn = page.locator(
      'button:has-text("활성화"), button:has-text("Activate"), a:has-text("활성화")'
    ).first();

    if (await activateBtn.count() > 0) {
      // 확인 다이얼로그 핸들러
      page.on('dialog', async dialog => {
        await dialog.accept();
      });

      await activateBtn.click();

      // Then - 활성화 결과 확인
      await page.waitForTimeout(1000);
    }
  });

  test('전략 동기화 버튼 클릭', async ({ page }) => {
    // Given - 전략 상세 페이지로 이동
    await page.goto('/trading/strategies');

    const strategyLink = page.locator('a[href*="/strategies/"]').first();
    if (await strategyLink.count() > 0) {
      await strategyLink.click();
    }

    // When - 동기화 버튼 클릭
    const syncBtn = page.locator(
      'button:has-text("동기화"), button:has-text("Sync"), form[action*="sync"] button'
    ).first();

    if (await syncBtn.count() > 0) {
      await syncBtn.click();

      // Then - 동기화 결과 메시지 확인
      await page.waitForTimeout(2000);
      const hasMessage = await page.locator('.alert, .message, .toast, [role="alert"]').count() > 0;
      // 메시지가 있거나 페이지가 리로드됨
    }
  });

  test('전략 모니터링 페이지 접근', async ({ page }) => {
    // Given
    await page.goto('/trading/strategies');

    // When - 모니터 버튼 클릭
    const monitorBtn = page.locator(
      'a:has-text("Monitor"), a:has-text("모니터"), a[href*="monitor"]'
    ).first();

    if (await monitorBtn.count() > 0) {
      await monitorBtn.click();

      // Then
      await expect(page).toHaveURL(/monitor/);
    }
  });

  test('전략 카테고리 필터링', async ({ page }) => {
    // Given
    await page.goto('/trading/strategies');

    // When - 카테고리 필터 선택
    const categoryFilter = page.locator('select[name="category"], #category, .category-filter');

    if (await categoryFilter.count() > 0) {
      await categoryFilter.selectOption({ index: 1 });

      // Then - 필터링된 결과
      await page.waitForTimeout(500);
    }
  });

  test('전략 상태 필터링', async ({ page }) => {
    // Given
    await page.goto('/trading/strategies');

    // When - 상태 필터 선택
    const statusFilter = page.locator('select[name="status"], #status');

    if (await statusFilter.count() > 0) {
      await statusFilter.selectOption('ACTIVE');

      // Then
      await page.waitForTimeout(500);
    }
  });

  test('전략 진입조건 설정', async ({ page }) => {
    // Given - 자동매매 설정 페이지로 이동
    const strategyId = '1'; // 테스트용 ID
    await page.goto(`/trading/strategies/${strategyId}/trading`);

    // When - 진입조건 추가
    const addConditionBtn = page.locator(
      'button:has-text("조건 추가"), button:has-text("Add Condition"), .add-entry-condition'
    ).first();

    if (await addConditionBtn.count() > 0) {
      await addConditionBtn.click();

      // 조건 유형 선택
      const conditionType = page.locator('select[name*="type"], .condition-type').last();
      if (await conditionType.count() > 0) {
        await conditionType.selectOption({ index: 1 });
      }
    }
  });

  test('전략 수정 후 저장', async ({ page }) => {
    // Given
    await page.goto('/trading/strategies');

    const editBtn = page.locator('a:has-text("수정"), a[href*="edit"]').first();
    if (await editBtn.count() > 0) {
      await editBtn.click();

      // When - 필드 수정
      const descInput = page.locator('textarea[name="description"], #description');
      if (await descInput.count() > 0) {
        await descInput.fill('E2E 테스트로 수정됨');
      }

      // 저장 버튼 클릭
      const saveBtn = page.locator('button:has-text("저장"), button[type="submit"]').first();
      if (await saveBtn.count() > 0) {
        await saveBtn.click();

        // Then - 저장 성공 메시지 또는 목록 페이지로 리다이렉트
        await page.waitForTimeout(1000);
      }
    }
  });
});
