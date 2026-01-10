package com.maru.trading.controller;

import com.maru.trading.controller.TestConfig;
import com.maru.trading.service.TradingApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(RiskRuleController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RiskRuleController 단위 테스트")
class RiskRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingApiService tradingApiService;

    private Map<String, Object> createMockAccountsResponse() {
        Map<String, Object> accounts = new HashMap<>();
        Map<String, Object> account1 = new HashMap<>();
        account1.put("accountId", "test-account-001");
        account1.put("alias", "Test Account");
        account1.put("cano", "12345678");
        account1.put("broker", "KIS");
        account1.put("environment", "PAPER");
        account1.put("status", "ACTIVE");
        accounts.put("items", Arrays.asList(account1));
        return accounts;
    }

    private Map<String, Object> createMockRulesResponse() {
        Map<String, Object> rules = new HashMap<>();
        Map<String, Object> rule1 = new HashMap<>();
        rule1.put("riskRuleId", "rule-001");
        rule1.put("scope", "GLOBAL");
        rule1.put("maxPositionValue", 5000000);
        rule1.put("maxPositionValuePerSymbol", 1000000);
        rule1.put("accountId", null);
        rule1.put("symbol", null);
        rule1.put("maxOpenOrders", 10);
        rule1.put("dailyLossLimit", 100000);
        rule1.put("maxOrdersPerMinute", 10);
        rule1.put("consecutiveOrderFailuresLimit", 5);
        rule1.put("enabled", true);
        rules.put("rules", Arrays.asList(rule1));
        return rules;
    }

    @Test
    @DisplayName("리스크 룰 목록 페이지 - 성공")
    void list_Success() throws Exception {
        when(tradingApiService.getAccounts()).thenReturn(createMockAccountsResponse());
        when(tradingApiService.getRiskRulesForAccount(anyString())).thenReturn(createMockRulesResponse());

        mockMvc.perform(get("/trading/risk-rules")
                        .param("accountId", "test-account-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-rules/list"))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attributeExists("rules"))
                .andExpect(model().attribute("apiConnected", true));
    }

    @Test
    @DisplayName("리스크 룰 목록 - API 오류 시")
    void list_ApiError() throws Exception {
        when(tradingApiService.getAccounts()).thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(get("/trading/risk-rules"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-rules/list"))
                .andExpect(model().attribute("apiConnected", false))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("전역 리스크 룰 수정 페이지")
    void editGlobal_Success() throws Exception {
        mockMvc.perform(get("/trading/risk-rules/global/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-rules/edit"))
                .andExpect(model().attribute("scope", "GLOBAL"))
                .andExpect(model().attribute("pageTitle", "전역 리스크 룰 설정"));
    }

    @Test
    @DisplayName("계좌별 리스크 룰 수정 페이지")
    void editAccount_Success() throws Exception {
        Map<String, Object> account = new HashMap<>();
        account.put("accountId", "test-account-001");
        when(tradingApiService.getAccount("test-account-001")).thenReturn(account);

        mockMvc.perform(get("/trading/risk-rules/account/test-account-001/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-rules/edit"))
                .andExpect(model().attribute("scope", "ACCOUNT"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @DisplayName("종목별 리스크 룰 수정 페이지")
    void editSymbol_Success() throws Exception {
        Map<String, Object> account = new HashMap<>();
        account.put("accountId", "test-account-001");
        when(tradingApiService.getAccount("test-account-001")).thenReturn(account);

        mockMvc.perform(get("/trading/risk-rules/account/test-account-001/symbol/edit")
                        .param("symbol", "005930"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/risk-rules/edit"))
                .andExpect(model().attribute("scope", "SYMBOL"))
                .andExpect(model().attribute("symbol", "005930"));
    }

    @Test
    @DisplayName("전역 리스크 룰 저장 - 성공")
    void saveGlobal_Success() throws Exception {
        Map<String, Object> ruleResponse = new HashMap<>();
        ruleResponse.put("riskRuleId", "rule-001");
        when(tradingApiService.updateGlobalRiskRule(any())).thenReturn(ruleResponse);

        mockMvc.perform(post("/trading/risk-rules/global")
                        .param("maxPositionValuePerSymbol", "5000000")
                        .param("maxOpenOrders", "10")
                        .param("dailyLossLimit", "100000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/risk-rules"))
                .andExpect(flash().attributeExists("message"));
    }
}
