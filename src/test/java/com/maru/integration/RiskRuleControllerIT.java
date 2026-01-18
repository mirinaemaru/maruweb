package com.maru.integration;

import com.maru.todo.TodoApplication;
import com.maru.trading.service.TradingApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("RiskRuleController 통합테스트")
class RiskRuleControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    @BeforeEach
    void setUp() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        when(tradingApiService.getHealthStatus()).thenReturn(healthStatus);
    }

    // ========== 리스크 룰 목록 ==========

    @Test
    @DisplayName("리스크 룰 목록 조회 - 성공")
    void listRiskRules_Success() throws Exception {
        // Given
        List<Map<String, Object>> accounts = new ArrayList<>();
        accounts.add(createAccountMock("account-1", "테스트계좌"));
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", accounts);

        List<Map<String, Object>> rules = new ArrayList<>();
        rules.add(createRiskRuleMock("rule-1", "ACCOUNT", "account-1"));
        Map<String, Object> rulesData = new HashMap<>();
        rulesData.put("rules", rules);

        when(tradingApiService.getAccounts()).thenReturn(accountsData);
        when(tradingApiService.getRiskRulesForAccount("account-1")).thenReturn(rulesData);

        // When & Then
        mockMvc.perform(get("/trading/risk-rules"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-rules/list"))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attributeExists("rules"))
                .andExpect(model().attribute("apiConnected", true));
    }

    @Test
    @DisplayName("리스크 룰 목록 조회 - 계좌 선택")
    void listRiskRules_WithAccountId() throws Exception {
        // Given
        List<Map<String, Object>> accounts = new ArrayList<>();
        accounts.add(createAccountMock("account-1", "테스트계좌"));
        Map<String, Object> accountsData = new HashMap<>();
        accountsData.put("items", accounts);

        Map<String, Object> rulesData = new HashMap<>();
        rulesData.put("rules", Collections.emptyList());

        when(tradingApiService.getAccounts()).thenReturn(accountsData);
        when(tradingApiService.getRiskRulesForAccount("account-1")).thenReturn(rulesData);

        // When & Then
        mockMvc.perform(get("/trading/risk-rules")
                        .param("accountId", "account-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-rules/list"))
                .andExpect(model().attribute("selectedAccountId", "account-1"));
    }

    @Test
    @DisplayName("리스크 룰 목록 조회 - API 오류")
    void listRiskRules_ApiError() throws Exception {
        // Given
        when(tradingApiService.getAccounts()).thenThrow(new RuntimeException("API 연결 실패"));

        // When & Then
        mockMvc.perform(get("/trading/risk-rules"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-rules/list"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("apiConnected", false));
    }

    // ========== 전역 리스크 룰 ==========

    @Test
    @DisplayName("전역 리스크 룰 수정 페이지 조회")
    void editGlobalRiskRule_Page() throws Exception {
        // When & Then
        mockMvc.perform(get("/trading/risk-rules/global/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-rules/edit"))
                .andExpect(model().attribute("scope", "GLOBAL"));
    }

    @Test
    @DisplayName("전역 리스크 룰 저장 - 성공")
    void saveGlobalRiskRule_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        when(tradingApiService.updateGlobalRiskRule(anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/risk-rules/global")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("maxPositionValuePerSymbol", "10000000")
                        .param("maxOpenOrders", "10")
                        .param("maxOrdersPerMinute", "5")
                        .param("dailyLossLimit", "500000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/risk-rules"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("전역 리스크 룰 저장 - 오류")
    void saveGlobalRiskRule_Error() throws Exception {
        // Given
        doThrow(new RuntimeException("저장 실패")).when(tradingApiService).updateGlobalRiskRule(anyMap());

        // When & Then
        mockMvc.perform(post("/trading/risk-rules/global")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("maxPositionValuePerSymbol", "10000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/risk-rules/global/edit"))
                .andExpect(flash().attributeExists("error"));
    }

    // ========== 계좌별 리스크 룰 ==========

    @Test
    @DisplayName("계좌별 리스크 룰 수정 페이지 조회")
    void editAccountRiskRule_Page() throws Exception {
        // Given
        Map<String, Object> account = createAccountMock("account-1", "테스트계좌");
        when(tradingApiService.getAccount("account-1")).thenReturn(account);

        // When & Then
        mockMvc.perform(get("/trading/risk-rules/account/account-1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-rules/edit"))
                .andExpect(model().attribute("scope", "ACCOUNT"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @DisplayName("계좌별 리스크 룰 저장 - 성공")
    void saveAccountRiskRule_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        when(tradingApiService.updateAccountRiskRule(eq("account-1"), anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/risk-rules/account/account-1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("maxPositionValuePerSymbol", "5000000")
                        .param("maxOpenOrders", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/risk-rules?accountId=account-1"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("계좌별 리스크 룰 저장 - 오류")
    void saveAccountRiskRule_Error() throws Exception {
        // Given
        doThrow(new RuntimeException("저장 실패"))
                .when(tradingApiService).updateAccountRiskRule(eq("account-1"), anyMap());

        // When & Then
        mockMvc.perform(post("/trading/risk-rules/account/account-1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("maxOpenOrders", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/risk-rules/account/account-1/edit"))
                .andExpect(flash().attributeExists("error"));
    }

    // ========== 종목별 리스크 룰 ==========

    @Test
    @DisplayName("종목별 리스크 룰 수정 페이지 조회")
    void editSymbolRiskRule_Page() throws Exception {
        // Given
        Map<String, Object> account = createAccountMock("account-1", "테스트계좌");
        when(tradingApiService.getAccount("account-1")).thenReturn(account);

        // When & Then
        mockMvc.perform(get("/trading/risk-rules/account/account-1/symbol/edit")
                        .param("symbol", "005930"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-rules/edit"))
                .andExpect(model().attribute("scope", "SYMBOL"))
                .andExpect(model().attribute("symbol", "005930"));
    }

    @Test
    @DisplayName("종목별 리스크 룰 저장 - 성공")
    void saveSymbolRiskRule_Success() throws Exception {
        // Given
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        when(tradingApiService.updateSymbolRiskRule(eq("account-1"), eq("005930"), anyMap())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/trading/risk-rules/account/account-1/symbol")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("symbol", "005930")
                        .param("maxPositionValuePerSymbol", "3000000")
                        .param("dailyLossLimit", "100000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/risk-rules?accountId=account-1"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("종목별 리스크 룰 저장 - 오류")
    void saveSymbolRiskRule_Error() throws Exception {
        // Given
        doThrow(new RuntimeException("저장 실패"))
                .when(tradingApiService).updateSymbolRiskRule(eq("account-1"), eq("005930"), anyMap());

        // When & Then
        mockMvc.perform(post("/trading/risk-rules/account/account-1/symbol")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("symbol", "005930")
                        .param("maxPositionValuePerSymbol", "3000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/risk-rules/account/account-1/symbol/edit?symbol=005930"))
                .andExpect(flash().attributeExists("error"));
    }

    // ========== 리스크 룰 삭제 ==========

    @Test
    @DisplayName("리스크 룰 삭제 - 성공")
    void deleteRiskRule_Success() throws Exception {
        // Given
        doNothing().when(tradingApiService).deleteRiskRule("rule-1");

        // When & Then
        mockMvc.perform(post("/trading/risk-rules/rule-1/delete")
                        .param("accountId", "account-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/risk-rules?accountId=account-1"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("리스크 룰 삭제 - 오류")
    void deleteRiskRule_Error() throws Exception {
        // Given
        doThrow(new RuntimeException("삭제 실패")).when(tradingApiService).deleteRiskRule("rule-1");

        // When & Then
        mockMvc.perform(post("/trading/risk-rules/rule-1/delete")
                        .param("accountId", "account-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/risk-rules?accountId=account-1"))
                .andExpect(flash().attributeExists("error"));
    }

    // ========== Helper Methods ==========

    private Map<String, Object> createAccountMock(String accountId, String alias) {
        Map<String, Object> account = new HashMap<>();
        account.put("accountId", accountId);
        account.put("alias", alias);
        account.put("cano", "12345678");  // 계좌번호 (템플릿에서 사용)
        account.put("broker", "키움증권");
        account.put("status", "ACTIVE");
        return account;
    }

    private Map<String, Object> createRiskRuleMock(String ruleId, String scope, String accountId) {
        Map<String, Object> rule = new HashMap<>();
        rule.put("ruleId", ruleId);
        rule.put("riskRuleId", ruleId);  // 템플릿에서 riskRuleId로 접근
        rule.put("scope", scope);
        rule.put("accountId", accountId);
        rule.put("symbol", "");  // null 대신 빈 문자열 사용 (SpEL 접근 호환성)
        rule.put("maxPositionValuePerSymbol", new BigDecimal("10000000"));
        rule.put("maxOpenOrders", 10);
        rule.put("maxOrdersPerMinute", 5);
        rule.put("dailyLossLimit", new BigDecimal("500000"));
        rule.put("consecutiveOrderFailuresLimit", 3);
        rule.put("createdAt", "2024-01-01T10:00:00");
        return rule;
    }
}
