/**
 * Symbol Search Component
 * 종목 검색 공통 컴포넌트
 *
 * Usage:
 * 1. Include CSS: <link rel="stylesheet" th:href="@{/css/symbol-search.css}">
 * 2. Include this JS: <script th:src="@{/js/symbol-search.js}"></script>
 * 3. Include modal fragment: <div th:replace="~{fragments/symbol-search :: modal}"></div>
 * 4. Open modal: SymbolSearch.open('targetInputId', { multiSelect: true })
 *
 * Options:
 * - multiSelect: boolean (default: true) - Allow multiple symbol selection
 * - defaultMarket: string (default: 'KOSPI') - Default market filter
 * - apiUrl: string (default: '/trading/instruments/api/search') - Search API endpoint
 * - onConfirm: function(symbols) - Callback when symbols are confirmed
 */

const SymbolSearch = (function() {
    'use strict';

    // State
    let selectedSymbols = [];
    let targetInputId = null;
    let options = {};

    // Default options
    const defaults = {
        multiSelect: true,
        defaultMarket: 'KOSPI',
        apiUrl: '/trading/instruments/api/search',
        onConfirm: null
    };

    // DOM Elements (cached after init)
    let modal, marketFilter, searchKeyword, selectedContainer, listContainer;

    /**
     * Initialize the component
     */
    function init() {
        modal = document.getElementById('symbolSearchModal');
        if (!modal) {
            console.warn('SymbolSearch: Modal element not found. Make sure to include the fragment.');
            return false;
        }

        marketFilter = document.getElementById('symbolSearchMarket');
        searchKeyword = document.getElementById('symbolSearchKeyword');
        selectedContainer = document.getElementById('symbolSearchSelected');
        listContainer = document.getElementById('symbolSearchList');

        // Event listeners
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                close();
            }
        });

        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && modal.classList.contains('active')) {
                close();
            }
        });

        return true;
    }

    /**
     * Open the search modal
     * @param {string} inputId - Target input element ID
     * @param {object} opts - Options
     */
    function open(inputId, opts) {
        if (!modal && !init()) {
            console.error('SymbolSearch: Failed to initialize');
            return;
        }

        targetInputId = inputId;
        options = Object.assign({}, defaults, opts);

        // Initialize selected symbols from target input
        const targetInput = document.getElementById(targetInputId);
        if (targetInput && targetInput.value) {
            const symbols = targetInput.value.split(',').map(s => s.trim()).filter(s => s);
            selectedSymbols = symbols.map(s => ({ symbol: s, name: s }));
        } else {
            selectedSymbols = [];
        }

        // Set default market
        if (marketFilter) {
            marketFilter.value = options.defaultMarket;
        }

        // Clear search keyword
        if (searchKeyword) {
            searchKeyword.value = '';
        }

        // Update UI
        updateSelectedDisplay();

        // Show modal
        modal.classList.add('active');

        // Auto search with default market
        search();

        // Focus search input
        if (searchKeyword) {
            setTimeout(() => searchKeyword.focus(), 100);
        }
    }

    /**
     * Close the modal
     */
    function close() {
        if (modal) {
            modal.classList.remove('active');
        }
        targetInputId = null;
    }

    /**
     * Search instruments
     */
    function search() {
        if (!listContainer) return;

        const market = marketFilter ? marketFilter.value : '';
        const keyword = searchKeyword ? searchKeyword.value : '';

        // Show loading
        listContainer.innerHTML = '<div class="symbol-search-loading">종목을 검색 중입니다...</div>';

        // Build URL
        const params = new URLSearchParams();
        if (market) params.append('market', market);
        if (keyword) params.append('search', keyword);

        const url = options.apiUrl + '?' + params.toString();

        fetch(url)
            .then(response => response.json())
            .then(data => {
                if (data.error) {
                    listContainer.innerHTML = '<div class="symbol-search-empty">⚠️<p>' + data.error + '</p></div>';
                    return;
                }

                const items = data.items || [];
                if (items.length === 0) {
                    listContainer.innerHTML = '<div class="symbol-search-empty">🔍<p>검색 결과가 없습니다</p></div>';
                    return;
                }

                renderList(items);
            })
            .catch(error => {
                console.error('SymbolSearch error:', error);
                listContainer.innerHTML = '<div class="symbol-search-empty">⚠️<p>검색 중 오류가 발생했습니다</p></div>';
            });
    }

    /**
     * Render instrument list
     * @param {array} items - Instrument items
     */
    function renderList(items) {
        let html = '';
        items.forEach(item => {
            const name = item.nameKr || item.name || item.symbol;
            const isSelected = selectedSymbols.some(s => s.symbol === item.symbol);

            html += '<div class="symbol-search-item' + (isSelected ? ' selected' : '') + '" ';
            html += 'data-symbol="' + item.symbol + '" data-name="' + escapeHtml(name) + '" ';
            html += 'onclick="SymbolSearch.toggle(this)">';
            html += '<input type="checkbox" ' + (isSelected ? 'checked' : '') + ' ';
            html += 'onclick="event.stopPropagation(); SymbolSearch.toggle(this.parentElement)">';
            html += '<div class="info">';
            html += '<div class="name">' + escapeHtml(name) + '</div>';
            html += '<div class="code">' + item.symbol + '</div>';
            html += '</div>';
            html += '<span class="market">' + (item.market || 'KR') + '</span>';
            html += '</div>';
        });
        listContainer.innerHTML = html;
    }

    /**
     * Toggle item selection
     * @param {HTMLElement} element - Item element
     */
    function toggle(element) {
        const symbol = element.getAttribute('data-symbol');
        const name = element.getAttribute('data-name');
        const checkbox = element.querySelector('input[type="checkbox"]');

        const index = selectedSymbols.findIndex(s => s.symbol === symbol);

        if (index > -1) {
            // Remove
            selectedSymbols.splice(index, 1);
            element.classList.remove('selected');
            if (checkbox) checkbox.checked = false;
        } else {
            // Add
            if (!options.multiSelect) {
                // Single select mode - clear previous
                selectedSymbols = [];
                document.querySelectorAll('.symbol-search-item.selected').forEach(el => {
                    el.classList.remove('selected');
                    const cb = el.querySelector('input[type="checkbox"]');
                    if (cb) cb.checked = false;
                });
            }
            selectedSymbols.push({ symbol: symbol, name: name });
            element.classList.add('selected');
            if (checkbox) checkbox.checked = true;
        }

        updateSelectedDisplay();
    }

    /**
     * Remove selected symbol
     * @param {string} symbol - Symbol to remove
     */
    function remove(symbol) {
        const index = selectedSymbols.findIndex(s => s.symbol === symbol);
        if (index > -1) {
            selectedSymbols.splice(index, 1);
            updateSelectedDisplay();

            // Update list item if visible
            const listItem = document.querySelector('.symbol-search-item[data-symbol="' + symbol + '"]');
            if (listItem) {
                listItem.classList.remove('selected');
                const checkbox = listItem.querySelector('input[type="checkbox"]');
                if (checkbox) checkbox.checked = false;
            }
        }
    }

    /**
     * Update selected symbols display
     */
    function updateSelectedDisplay() {
        if (!selectedContainer) return;

        if (selectedSymbols.length === 0) {
            selectedContainer.innerHTML = '<span class="symbol-search-no-selection">선택된 종목이 없습니다</span>';
            return;
        }

        let html = '';
        selectedSymbols.forEach(item => {
            html += '<span class="symbol-search-tag">';
            html += escapeHtml(item.symbol) + ' (' + escapeHtml(item.name) + ')';
            html += '<span class="remove" onclick="event.stopPropagation(); SymbolSearch.remove(\'' + item.symbol + '\')">&times;</span>';
            html += '</span>';
        });
        selectedContainer.innerHTML = html;
    }

    /**
     * Confirm selection
     */
    function confirm() {
        const symbols = selectedSymbols.map(s => s.symbol).join(', ');

        // Update target input
        if (targetInputId) {
            const targetInput = document.getElementById(targetInputId);
            if (targetInput) {
                targetInput.value = symbols;
                // Trigger change event
                targetInput.dispatchEvent(new Event('change', { bubbles: true }));
            }
        }

        // Callback
        if (typeof options.onConfirm === 'function') {
            options.onConfirm(selectedSymbols);
        }

        close();
    }

    /**
     * Get selected symbols
     * @returns {array} Selected symbols
     */
    function getSelected() {
        return selectedSymbols.slice();
    }

    /**
     * Escape HTML to prevent XSS
     * @param {string} str - String to escape
     * @returns {string} Escaped string
     */
    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    // Initialize on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Public API
    return {
        open: open,
        close: close,
        search: search,
        toggle: toggle,
        remove: remove,
        confirm: confirm,
        getSelected: getSelected
    };
})();
