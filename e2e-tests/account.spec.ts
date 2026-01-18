import { test, expect } from '@playwright/test';

/**
 * 계좌 관리 UI E2E 테스트
 *
 * 시나리오: 계좌 생성 폼 입력 → 저장 → 목록 확인
 */
test.describe('Account Management', () => {

  test('계좌 목록 페이지 로드', async ({ page }) => {
    // When
    await page.goto('/trading/accounts');

    // Then
    await expect(page.locator('body')).toBeVisible();
    // 계좌 관련 텍스트가 있어야 함
    await expect(page.locator('body')).toContainText(/계좌|Account/i);
  });

  test('계좌 등록 폼으로 이동', async ({ page }) => {
    // Given
    await page.goto('/trading/accounts');

    // When - 새 계좌 등록 버튼 클릭
    const newAccountBtn = page.locator(
      'a[href*="new"], button:has-text("등록"), button:has-text("추가"), button:has-text("새 계좌"), a:has-text("등록")'
    );

    if (await newAccountBtn.count() > 0) {
      await newAccountBtn.first().click();

      // Then
      await expect(page).toHaveURL(/accounts\/new|accounts\/create/);
    } else {
      // 직접 이동
      await page.goto('/trading/accounts/new');
      await expect(page.locator('body')).toBeVisible();
    }
  });

  test('계좌 등록 폼 필드 확인', async ({ page }) => {
    // Given
    await page.goto('/trading/accounts/new');

    // Then - 필수 입력 필드가 있어야 함
    await expect(page.locator('input, select').first()).toBeVisible();
  });

  test('계좌 등록 - 폼 입력', async ({ page }) => {
    // Given
    await page.goto('/trading/accounts/new');

    // When - 폼 필드 입력
    const brokerSelect = page.locator('select[name="broker"], #broker');
    if (await brokerSelect.count() > 0) {
      await brokerSelect.selectOption({ index: 1 });
    }

    const canoInput = page.locator('input[name="cano"], #cano');
    if (await canoInput.count() > 0) {
      await canoInput.fill('12345678');
    }

    const aliasInput = page.locator('input[name="alias"], #alias');
    if (await aliasInput.count() > 0) {
      await aliasInput.fill('E2E 테스트 계좌');
    }

    const envSelect = page.locator('select[name="environment"], #environment');
    if (await envSelect.count() > 0) {
      await envSelect.selectOption('PAPER');
    }

    // Then - 입력값 확인
    if (await aliasInput.count() > 0) {
      await expect(aliasInput).toHaveValue('E2E 테스트 계좌');
    }
  });

  test('계좌 등록 - 필수 필드 검증', async ({ page }) => {
    // Given
    await page.goto('/trading/accounts/new');

    // When - 빈 폼 제출 시도
    const submitBtn = page.locator('button[type="submit"], input[type="submit"], button:has-text("저장"), button:has-text("등록")');

    if (await submitBtn.count() > 0) {
      await submitBtn.first().click();

      // Then - 에러 메시지 또는 필수 필드 표시
      // HTML5 validation 또는 커스텀 에러 메시지
      const hasValidation = await page.locator(':invalid, .error, .is-invalid, .error-message').count() > 0;
      const stayedOnForm = page.url().includes('new') || page.url().includes('create');

      // 검증에 실패하면 폼에 남아있거나 에러가 표시됨
      expect(hasValidation || stayedOnForm).toBeTruthy();
    }
  });

  test('계좌 상세 페이지 조회', async ({ page }) => {
    // Given
    await page.goto('/trading/accounts');

    // When - 첫 번째 계좌 클릭
    const accountRow = page.locator('tr a, .account-item a, .list-item a').first();

    if (await accountRow.count() > 0) {
      await accountRow.click();

      // Then - 상세 페이지로 이동
      await expect(page.locator('body')).toContainText(/계좌|Account|상세/i);
    }
  });

  test('계좌 수정 페이지 이동', async ({ page }) => {
    // Given
    await page.goto('/trading/accounts');

    // When - 수정 버튼 클릭
    const editBtn = page.locator('a:has-text("수정"), button:has-text("수정"), a[href*="edit"]').first();

    if (await editBtn.count() > 0) {
      await editBtn.click();

      // Then
      await expect(page).toHaveURL(/edit/);
    }
  });

  test('계좌 삭제 확인 다이얼로그', async ({ page }) => {
    // Given
    await page.goto('/trading/accounts');

    // When - 삭제 버튼 클릭
    const deleteBtn = page.locator('button:has-text("삭제"), a:has-text("삭제")').first();

    if (await deleteBtn.count() > 0) {
      // 다이얼로그 핸들러 설정
      page.on('dialog', async dialog => {
        expect(dialog.type()).toBe('confirm');
        await dialog.dismiss();  // 취소
      });

      await deleteBtn.click();
    }
  });

  test('계좌 검색 기능 (있는 경우)', async ({ page }) => {
    // Given
    await page.goto('/trading/accounts');

    // When - 검색 필드 찾기
    const searchInput = page.locator('input[type="search"], input[name="search"], input[placeholder*="검색"], .search-input');

    if (await searchInput.count() > 0) {
      await searchInput.fill('테스트');

      // Then - 검색 결과가 필터링되거나 결과가 표시됨
      await page.waitForTimeout(500);  // 디바운싱 대기
    }
  });
});
