// Shortcut Page JavaScript - Tab Layout

// Active category data
let activeCategory = {
    id: null,
    name: '',
    description: '',
    order: 0
};

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    // Check for tab parameter in URL
    const urlParams = new URLSearchParams(window.location.search);
    const tabParam = urlParams.get('tab');

    if (tabParam) {
        // Select the tab from URL parameter
        const targetTab = document.querySelector('.category-tab[data-category-id="' + tabParam + '"]');
        if (targetTab) {
            selectCategory(parseInt(tabParam));
            return;
        }
    }

    // Select first tab if no URL parameter or tab not found
    const firstTab = document.querySelector('.category-tab');
    if (firstTab) {
        const categoryId = firstTab.getAttribute('data-category-id');
        selectCategory(parseInt(categoryId));
    }
});

// ========== Tab Selection ==========
function selectCategory(categoryId) {
    // Update active category data
    const tab = document.querySelector('.category-tab[data-category-id="' + categoryId + '"]');
    if (tab) {
        activeCategory.id = categoryId;
        activeCategory.name = tab.getAttribute('data-category-name');
        activeCategory.description = tab.getAttribute('data-category-description') || '';
        activeCategory.order = parseInt(tab.getAttribute('data-category-order')) || 0;
    }

    // Update tab styles
    document.querySelectorAll('.category-tab').forEach(t => t.classList.remove('active'));
    if (tab) tab.classList.add('active');

    // Update panel visibility
    document.querySelectorAll('.shortcuts-panel').forEach(p => p.classList.remove('active'));
    const panel = document.getElementById('panel-' + categoryId);
    if (panel) panel.classList.add('active');

    // Show category actions and update delete form
    const actionsBar = document.getElementById('activeCategoryActions');
    actionsBar.style.display = 'flex';

    const deleteForm = document.getElementById('deleteCategoryForm');
    deleteForm.action = '/shortcuts/categories/' + categoryId + '/delete';
}

// ========== Active Category Actions ==========
function showAddShortcutModalForActive() {
    if (!activeCategory.id) return;
    showAddShortcutModal(activeCategory.id, activeCategory.name);
}

function showEditCategoryModalForActive() {
    if (!activeCategory.id) return;
    showEditCategoryModal(activeCategory.id, activeCategory.name, activeCategory.description, activeCategory.order);
}

// ========== Add Category Modal ==========
function showAddCategoryModal() {
    document.getElementById('addCategoryModal').style.display = 'flex';
}

function closeAddCategoryModal() {
    document.getElementById('addCategoryModal').style.display = 'none';
}

// ========== Edit Category Modal ==========
function showEditCategoryModal(id, name, description, order) {
    document.getElementById('editCategoryForm').action = '/shortcuts/categories/' + id;
    document.getElementById('editCategoryName').value = name;
    document.getElementById('editCategoryDescription').value = description || '';
    document.getElementById('editCategoryOrder').value = order || 0;
    document.getElementById('editCategoryModal').style.display = 'flex';
}

function closeEditCategoryModal() {
    document.getElementById('editCategoryModal').style.display = 'none';
}

// ========== Add Shortcut Modal ==========
function showAddShortcutModal(categoryId, categoryName) {
    document.getElementById('addShortcutCategoryId').value = categoryId;
    document.getElementById('addShortcutCategoryName').textContent = 'to Category: ' + categoryName;
    document.getElementById('addShortcutModal').style.display = 'flex';
}

function closeAddShortcutModal() {
    document.getElementById('addShortcutModal').style.display = 'none';
}

// ========== Edit Shortcut Modal ==========
function showEditShortcutModal(id, name, url, description, order, categoryId) {
    document.getElementById('editShortcutForm').action = '/shortcuts/items/' + id;
    document.getElementById('editShortcutName').value = name;
    document.getElementById('editShortcutUrl').value = url;
    document.getElementById('editShortcutDescription').value = description || '';
    document.getElementById('editShortcutOrder').value = order || 0;
    document.getElementById('editShortcutCategoryId').value = categoryId;
    document.getElementById('editShortcutModal').style.display = 'flex';
}

function closeEditShortcutModal() {
    document.getElementById('editShortcutModal').style.display = 'none';
}

// ========== Close modals when clicking outside ==========
window.onclick = function(event) {
    if (event.target.classList.contains('modal')) {
        event.target.style.display = 'none';
    }
}

// ========== Close modals with Escape key ==========
document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        closeAddCategoryModal();
        closeEditCategoryModal();
        closeAddShortcutModal();
        closeEditShortcutModal();
    }
});
