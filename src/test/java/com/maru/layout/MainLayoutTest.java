package com.maru.layout;

import com.maru.calendar.service.CalendarEventService;
import com.maru.calendar.service.GoogleOAuthService;
import com.maru.dashboard.controller.DashboardController;
import com.maru.dday.service.DDayService;
import com.maru.shortcut.service.ShortcutService;
import com.maru.todo.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for main.html layout template.
 * Verifies that the layout structure, navigation, and common elements are rendered correctly.
 * Uses @WebMvcTest with actual Thymeleaf rendering via DashboardController.
 */
@WebMvcTest
@ContextConfiguration(classes = LayoutTestConfig.class)
@Import(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Main Layout 템플릿 테스트")
class MainLayoutTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoService todoService;

    @MockBean
    private CalendarEventService calendarEventService;

    @MockBean
    private ShortcutService shortcutService;

    @MockBean
    private DDayService ddayService;

    @MockBean
    private GoogleOAuthService googleOAuthService;

    private String renderedHtml;

    @BeforeEach
    void setUp() throws Exception {
        // Setup mocks
        when(todoService.getTodosByStatus(anyString())).thenReturn(Collections.emptyList());
        when(calendarEventService.getEventsForDay(any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(calendarEventService.getEventsForMonth(anyInt(), anyInt())).thenReturn(Collections.emptyList());
        when(shortcutService.getAllCategoriesWithShortcuts()).thenReturn(Collections.emptyList());
        when(ddayService.getUpcomingDDays()).thenReturn(Collections.emptyList());

        // Render the page once and reuse for all tests
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();
        renderedHtml = result.getResponse().getContentAsString();
    }

    // ==================== Layout Structure Tests ====================

    @Nested
    @DisplayName("레이아웃 구조 테스트")
    class LayoutStructureTests {

        @Test
        @DisplayName("필수 HTML 구조가 포함되어야 함")
        void shouldContainRequiredHtmlStructure() {
            assertThat(renderedHtml).contains("<!DOCTYPE html>");
            assertThat(renderedHtml).contains("<html");
            assertThat(renderedHtml).contains("</html>");
            assertThat(renderedHtml).contains("<head>");
            assertThat(renderedHtml).contains("</head>");
            assertThat(renderedHtml).contains("<body>");
            assertThat(renderedHtml).contains("</body>");
        }

        @Test
        @DisplayName("메타 태그가 포함되어야 함")
        void shouldContainMetaTags() {
            assertThat(renderedHtml).contains("charset=\"UTF-8\"");
            assertThat(renderedHtml).contains("viewport");
            assertThat(renderedHtml).contains("width=device-width");
            assertThat(renderedHtml).contains("initial-scale=1.0");
        }

        @Test
        @DisplayName("CSS 스타일시트가 포함되어야 함")
        void shouldContainStylesheet() {
            assertThat(renderedHtml).contains("/css/style.css");
        }

        @Test
        @DisplayName("favicon이 포함되어야 함")
        void shouldContainFavicon() {
            assertThat(renderedHtml).contains("/images/favicon.png");
        }

        @Test
        @DisplayName("app-container 클래스가 포함되어야 함")
        void shouldContainAppContainer() {
            assertThat(renderedHtml).contains("app-container");
        }
    }

    // ==================== Sidebar Tests ====================

    @Nested
    @DisplayName("사이드바 테스트")
    class SidebarTests {

        @Test
        @DisplayName("Maru Web 헤더가 포함되어야 함")
        void shouldContainHeader() {
            assertThat(renderedHtml).contains("Maru Web");
            assertThat(renderedHtml).contains("Management System");
        }

        @Test
        @DisplayName("테마 토글 버튼이 포함되어야 함")
        void shouldContainThemeToggle() {
            assertThat(renderedHtml).contains("themeToggle");
            assertThat(renderedHtml).contains("Toggle theme");
            assertThat(renderedHtml).contains("theme-toggle");
        }

        @Test
        @DisplayName("저작권 정보가 포함되어야 함")
        void shouldContainFooter() {
            // Template uses &copy; entity which renders as ©
            assertThat(renderedHtml).containsAnyOf("© 2026 Maru Web", "&copy; 2026 Maru Web");
        }

        @Test
        @DisplayName("사이드바 토글 버튼이 포함되어야 함")
        void shouldContainSidebarToggle() {
            assertThat(renderedHtml).contains("sidebar-toggle");
            assertThat(renderedHtml).contains("Show menu");
        }

        @Test
        @DisplayName("sidebar 클래스가 포함되어야 함")
        void shouldContainSidebarClass() {
            assertThat(renderedHtml).contains("class=\"sidebar\"");
        }
    }

    // ==================== Navigation Menu Tests ====================

    @Nested
    @DisplayName("네비게이션 메뉴 테스트")
    class NavigationMenuTests {

        @Test
        @DisplayName("Dashboard 링크가 포함되어야 함")
        void shouldContainDashboardLink() {
            assertThat(renderedHtml).contains("Dashboard");
            assertThat(renderedHtml).contains("🏠");
        }

        @Test
        @DisplayName("Kanban 링크가 포함되어야 함")
        void shouldContainKanbanLink() {
            assertThat(renderedHtml).contains("Kanban");
            assertThat(renderedHtml).contains("📋");
            assertThat(renderedHtml).contains("/kanban");
        }

        @Test
        @DisplayName("Todo 링크가 포함되어야 함")
        void shouldContainTodoLink() {
            assertThat(renderedHtml).contains("Todo");
            assertThat(renderedHtml).contains("📝");
            assertThat(renderedHtml).contains("/todos");
        }

        @Test
        @DisplayName("Calendar 링크가 포함되어야 함")
        void shouldContainCalendarLink() {
            assertThat(renderedHtml).contains("Calendar");
            assertThat(renderedHtml).contains("📅");
            assertThat(renderedHtml).contains("/calendar");
        }

        @Test
        @DisplayName("D-Day 링크가 포함되어야 함")
        void shouldContainDDayLink() {
            assertThat(renderedHtml).contains("D-Day");
            assertThat(renderedHtml).contains("⏰");
            assertThat(renderedHtml).contains("/dday");
        }

        @Test
        @DisplayName("Notes 링크가 포함되어야 함")
        void shouldContainNotesLink() {
            assertThat(renderedHtml).contains("Notes");
            assertThat(renderedHtml).contains("📒");
            assertThat(renderedHtml).contains("/notes");
        }

        @Test
        @DisplayName("Habits 링크가 포함되어야 함")
        void shouldContainHabitsLink() {
            assertThat(renderedHtml).contains("Habits");
            assertThat(renderedHtml).contains("✅");
            assertThat(renderedHtml).contains("/habits");
        }

        @Test
        @DisplayName("Shortcuts 링크가 포함되어야 함")
        void shouldContainShortcutsLink() {
            assertThat(renderedHtml).contains("Shortcuts");
            assertThat(renderedHtml).contains("🔗");
            assertThat(renderedHtml).contains("/shortcuts");
        }

        @Test
        @DisplayName("Trading 메뉴가 포함되어야 함")
        void shouldContainTradingMenu() {
            assertThat(renderedHtml).contains("Trading");
            assertThat(renderedHtml).contains("📊");
            assertThat(renderedHtml).contains("/trading/dashboard");
        }
    }

    // ==================== Trading Submenu Tests ====================

    @Nested
    @DisplayName("Trading 서브메뉴 테스트")
    class TradingSubmenuTests {

        @Test
        @DisplayName("계좌관리 링크가 포함되어야 함")
        void shouldContainAccountsLink() {
            assertThat(renderedHtml).contains("계좌관리");
            assertThat(renderedHtml).contains("/trading/accounts");
        }

        @Test
        @DisplayName("전략 관리 링크가 포함되어야 함")
        void shouldContainStrategiesLink() {
            assertThat(renderedHtml).contains("전략 관리");
            assertThat(renderedHtml).contains("/trading/strategies");
        }

        @Test
        @DisplayName("주문조회 링크가 포함되어야 함")
        void shouldContainOrdersLink() {
            assertThat(renderedHtml).contains("주문조회");
            assertThat(renderedHtml).contains("/trading/orders");
        }

        @Test
        @DisplayName("Kill Switch 링크가 포함되어야 함")
        void shouldContainKillSwitchLink() {
            assertThat(renderedHtml).contains("Kill Switch");
            assertThat(renderedHtml).contains("/trading/kill-switch");
        }

        @Test
        @DisplayName("백테스팅 결과 링크가 포함되어야 함")
        void shouldContainBacktestLink() {
            assertThat(renderedHtml).contains("백테스팅 결과");
            assertThat(renderedHtml).contains("/trading/backtests");
        }

        @Test
        @DisplayName("Health Check 링크가 포함되어야 함")
        void shouldContainHealthCheckLink() {
            assertThat(renderedHtml).contains("Health Check");
            assertThat(renderedHtml).contains("/trading/health-check");
        }

        @Test
        @DisplayName("서브메뉴 헤더가 포함되어야 함")
        void shouldContainSubmenuHeaders() {
            // Check for submenu headers - & may be rendered as &amp; or as &
            assertThat(renderedHtml).containsAnyOf("대시보드 &amp; 계좌", "대시보드 & 계좌");
            assertThat(renderedHtml).containsAnyOf("거래 &amp; 실행", "거래 & 실행");
            assertThat(renderedHtml).containsAnyOf("리스크 &amp; 성과", "리스크 & 성과");
        }

        @Test
        @DisplayName("has-submenu 클래스가 포함되어야 함")
        void shouldContainHasSubmenuClass() {
            assertThat(renderedHtml).contains("has-submenu");
            assertThat(renderedHtml).contains("submenu");
        }
    }

    // ==================== JavaScript Tests ====================

    @Nested
    @DisplayName("JavaScript 기능 테스트")
    class JavaScriptTests {

        @Test
        @DisplayName("테마 토글 스크립트가 포함되어야 함")
        void shouldContainThemeToggleScript() {
            assertThat(renderedHtml).contains("getInitialTheme");
            assertThat(renderedHtml).contains("applyTheme");
            assertThat(renderedHtml).contains("toggleTheme");
            assertThat(renderedHtml).contains("localStorage.setItem('theme'");
        }

        @Test
        @DisplayName("최근 메뉴 스크립트가 포함되어야 함")
        void shouldContainRecentMenuScript() {
            assertThat(renderedHtml).contains("recentMenus");
            assertThat(renderedHtml).contains("getCookie");
            assertThat(renderedHtml).contains("setCookie");
            assertThat(renderedHtml).contains("displayRecentMenus");
        }

        @Test
        @DisplayName("서브메뉴 토글 스크립트가 포함되어야 함")
        void shouldContainSubmenuToggleScript() {
            assertThat(renderedHtml).contains("has-submenu");
            assertThat(renderedHtml).contains("classList.toggle");
        }

        @Test
        @DisplayName("다크모드 지원 코드가 포함되어야 함")
        void shouldSupportDarkMode() {
            assertThat(renderedHtml).contains("data-theme");
            assertThat(renderedHtml).contains("dark");
            assertThat(renderedHtml).contains("prefers-color-scheme");
        }
    }

    // ==================== External Libraries Tests ====================

    @Nested
    @DisplayName("외부 라이브러리 테스트")
    class ExternalLibrariesTests {

        @Test
        @DisplayName("SockJS 라이브러리가 포함되어야 함")
        void shouldContainSockJs() {
            assertThat(renderedHtml).contains("sockjs");
        }

        @Test
        @DisplayName("STOMP 라이브러리가 포함되어야 함")
        void shouldContainStomp() {
            assertThat(renderedHtml).contains("stomp");
        }
    }

    // ==================== Main Content Area Tests ====================

    @Nested
    @DisplayName("메인 컨텐츠 영역 테스트")
    class MainContentTests {

        @Test
        @DisplayName("메인 컨텐츠 영역이 포함되어야 함")
        void shouldContainMainContentArea() {
            assertThat(renderedHtml).contains("main-content");
        }

        @Test
        @DisplayName("최근 메뉴 리스트 영역이 포함되어야 함")
        void shouldContainRecentMenuList() {
            assertThat(renderedHtml).contains("recentMenuList");
        }
    }

    // ==================== Menu Map Tests ====================

    @Nested
    @DisplayName("메뉴 맵 테스트")
    class MenuMapTests {

        @Test
        @DisplayName("메뉴 맵에 Dashboard 경로가 정의되어야 함")
        void shouldContainDashboardPath() {
            assertThat(renderedHtml).contains("'/': { name: 'Dashboard'");
        }

        @Test
        @DisplayName("메뉴 맵에 Kanban 경로가 정의되어야 함")
        void shouldContainKanbanPath() {
            assertThat(renderedHtml).contains("'/kanban': { name: 'Kanban'");
        }

        @Test
        @DisplayName("메뉴 맵에 Todo 경로가 정의되어야 함")
        void shouldContainTodoPath() {
            assertThat(renderedHtml).contains("'/todos': { name: 'Todo'");
        }

        @Test
        @DisplayName("메뉴 맵에 Calendar 경로가 정의되어야 함")
        void shouldContainCalendarPath() {
            assertThat(renderedHtml).contains("'/calendar': { name: 'Calendar'");
        }

        @Test
        @DisplayName("메뉴 맵에 Trading 경로가 정의되어야 함")
        void shouldContainTradingPath() {
            assertThat(renderedHtml).contains("'/trading/dashboard': { name: 'Trading'");
        }
    }
}
