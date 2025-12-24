// Calendar JavaScript Functions

function showAddEventModal() {
    const modal = document.getElementById('addEventModal');
    modal.style.display = 'block';

    // Set default time to now + 1 hour
    const now = new Date();
    const startTime = new Date(now.getTime() + 60 * 60 * 1000); // 1 hour from now
    const endTime = new Date(now.getTime() + 2 * 60 * 60 * 1000); // 2 hours from now

    document.getElementById('startDateTime').value = formatDateTimeLocal(startTime);
    document.getElementById('endDateTime').value = formatDateTimeLocal(endTime);
}

function showAddEventModalWithDate(dateStr) {
    const modal = document.getElementById('addEventModal');
    modal.style.display = 'block';

    // Set start date/time to the clicked date at 9 AM
    const date = new Date(dateStr + 'T09:00:00');
    const startTime = new Date(date);
    const endTime = new Date(date.getTime() + 60 * 60 * 1000); // 1 hour later

    document.getElementById('startDateTime').value = formatDateTimeLocal(startTime);
    document.getElementById('endDateTime').value = formatDateTimeLocal(endTime);
}

function closeAddEventModal() {
    const modal = document.getElementById('addEventModal');
    modal.style.display = 'none';
}

function showEventDetail(eventId) {
    // Navigate to event detail page
    window.location.href = '/calendar/events/' + eventId;
}

// Format date for datetime-local input
function formatDateTimeLocal(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('addEventModal');
    if (event.target == modal) {
        modal.style.display = 'none';
    }
}

// Handle all-day checkbox
document.addEventListener('DOMContentLoaded', function() {
    const allDayCheckbox = document.getElementById('allDayCheckbox');
    const startInput = document.getElementById('startDateTime');
    const endInput = document.getElementById('endDateTime');

    if (allDayCheckbox) {
        allDayCheckbox.addEventListener('change', function() {
            if (this.checked) {
                // Convert to date-only inputs
                const startDate = startInput.value.split('T')[0];
                const endDate = endInput.value.split('T')[0];

                startInput.type = 'date';
                endInput.type = 'date';

                if (startDate) startInput.value = startDate;
                if (endDate) endInput.value = endDate;
            } else {
                // Convert back to datetime inputs
                const startDate = startInput.value;
                const endDate = endInput.value;

                startInput.type = 'datetime-local';
                endInput.type = 'datetime-local';

                if (startDate) startInput.value = startDate + 'T09:00';
                if (endDate) endInput.value = endDate + 'T10:00';
            }
        });
    }
});
