document.addEventListener('DOMContentLoaded', function() {
    // Setup courses tab functionality
    setupCoursesTab();
    
    // Check if token exists
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = '/login';
        return;
    }
    
    // Initialize edit course form submission
    const saveEditCourseBtn = document.getElementById('save-edit-course-btn');
    if (saveEditCourseBtn) {
        saveEditCourseBtn.addEventListener('click', saveCourseChanges);
    }
    
    // Initialize add course form submission
    const saveAddCourseBtn = document.getElementById('save-add-course-btn');
    if (saveAddCourseBtn) {
        saveAddCourseBtn.addEventListener('click', saveNewCourse);
    }
    
    // Initialize delete course button
    const confirmDeleteBtn = document.getElementById('confirm-delete-course-btn');
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener('click', deleteCourse);
    }
    
    // Initialize teaching hours buttons for add form
    const addTeachingHourBtnNew = document.getElementById('add-teaching-hour-btn-new');
    if (addTeachingHourBtnNew) {
        addTeachingHourBtnNew.addEventListener('click', function() {
            addTeachingHourRow('add-teaching-hours-container');
        });
    }
    
    // Initialize teaching hours buttons for edit form
    const addTeachingHourBtn = document.getElementById('add-teaching-hour-btn');
    if (addTeachingHourBtn) {
        addTeachingHourBtn.addEventListener('click', function() {
            addTeachingHourRow('edit-teaching-hours-container');
        });
    }
    
    // Add event listeners for removing teaching hours
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('remove-teaching-hour-btn') || 
            e.target.closest('.remove-teaching-hour-btn')) {
            const button = e.target.classList.contains('remove-teaching-hour-btn') ? 
                          e.target : e.target.closest('.remove-teaching-hour-btn');
            const row = button.closest('.teaching-hour-row');
            if (row) {
                row.remove();
            }
        }
    });
});

function setupCoursesTab() {
    // Set up buttons
    const addCourseBtn = document.getElementById('add-course-btn');
    if (addCourseBtn) {
        addCourseBtn.addEventListener('click', function() {
            resetAddCourseForm();
            const addCourseModal = new bootstrap.Modal(document.getElementById('add-course-modal'));
            addCourseModal.show();
        });
    }
    
    const addFirstCourseBtn = document.getElementById('add-first-course-btn');
    if (addFirstCourseBtn) {
        addFirstCourseBtn.addEventListener('click', function() {
            resetAddCourseForm();
            const addCourseModal = new bootstrap.Modal(document.getElementById('add-course-modal'));
            addCourseModal.show();
        });
    }
    
    // Load courses when tab is shown
    const coursesTabLink = document.querySelector('a[href="#courses-content"]');
    if (coursesTabLink) {
        coursesTabLink.addEventListener('shown.bs.tab', function() {
            console.log('Courses tab shown, loading data...');
            loadCourses();
        });
    }
    
    // Initial load if courses tab is active
    if (document.querySelector('#courses-content.active')) {
        console.log('Courses tab is active on page load, loading data...');
        loadCourses();
    }
}

function loadCourses() {
    console.log('Loading courses data...');
    const token = localStorage.getItem('token');
    
    // Get DOM elements
    const loadingEl = document.getElementById('courses-loading');
    const errorEl = document.getElementById('courses-error');
    const emptyEl = document.getElementById('courses-empty');
    const tableContainerEl = document.getElementById('courses-table-container');
    
    // Make sure elements exist before manipulating them
    if (!loadingEl || !errorEl || !emptyEl || !tableContainerEl) {
        console.error('Required DOM elements not found!', {
            loadingEl, 
            errorEl, 
            emptyEl, 
            tableContainerEl
        });
        return;
    }
    
    // Show loading, hide others
    loadingEl.style.display = 'block';
    errorEl.style.display = 'none';
    emptyEl.style.display = 'none';
    tableContainerEl.style.display = 'none';
    
    // Fetch courses data
    console.log('Fetching from API...');
    fetch('/api/courses', {
        headers: {
            'Authorization': 'Bearer ' + token
        }
    })
    .then(response => {
        console.log('API response received:', response.status);
        if (!response.ok) {
            throw new Error('Failed to load courses: ' + response.status);
        }
        return response.json();
    })
    .then(data => {
        console.log('Courses data received:', data);
        loadingEl.style.display = 'none';
        
        if (!data || data.length === 0) {
            console.log('No courses found, showing empty state');
            emptyEl.style.display = 'flex';
        } else {
            console.log('Displaying courses table with', data.length, 'records');
            tableContainerEl.style.display = 'block';
            populateCoursesTable(data);
        }
    })
    .catch(error => {
        console.error('Error loading courses:', error);
        loadingEl.style.display = 'none';
        errorEl.style.display = 'block';
        const errorMsgEl = document.getElementById('courses-error-message');
        if (errorMsgEl) {
            errorMsgEl.textContent = error.message || 'Unknown error occurred';
        }
    });
}

function populateCoursesTable(courses) {
    console.log('Populating courses table...');
    const tableBody = document.getElementById('courses-table-body');
    if (!tableBody) {
        console.error('Courses table body element not found!');
        return;
    }
    
    tableBody.innerHTML = '';
    
    courses.forEach((course, index) => {
        console.log('Adding course to table:', course.code);
        const row = document.createElement('tr');
        row.innerHTML = `
            <th scope="row">${index + 1}</th>
            <td>${course.code || 'N/A'}</td>
            <td>${course.name || 'N/A'}</td>
            <td>${formatCourseType(course.type) || 'N/A'}</td>
            <td>${course.year || 'N/A'}</td>
            <td>${course.semester || 'N/A'}</td>
            <td>${course.capacity || 'N/A'}</td>
            <td>
                <span class="badge ${course.active ? 'bg-success' : 'bg-danger'}">
                    ${course.active ? 'Ενεργό' : 'Ανενεργό'}
                </span>
            </td>
            <td>
                <button class="btn btn-sm btn-outline-primary edit-course-btn" data-id="${course.id}">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger delete-course-btn" data-id="${course.id}" data-name="${course.name}">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        `;
        tableBody.appendChild(row);
    });
    
    // Add event listeners to edit/delete buttons
    document.querySelectorAll('.edit-course-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const courseId = this.getAttribute('data-id');
            console.log('Edit button clicked for course ID:', courseId);
            openEditCourseModal(courseId);
        });
    });
    
    document.querySelectorAll('.delete-course-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const courseId = this.getAttribute('data-id');
            const courseName = this.getAttribute('data-name');
            console.log('Delete button clicked for course:', courseName);
            openDeleteCourseModal(courseId, courseName);
        });
    });
    
    console.log('Courses table populated successfully');
}

function formatCourseType(type) {
    if (!type) return 'N/A';
    
    switch(type) {
        case 'BASIC': return 'Βασικό';
        case 'ELECTIVE': return 'Επιλογής';
        default: return type;
    }
}

function addTeachingHourRow(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;
    
    const row = document.createElement('div');
    row.className = 'teaching-hour-row mb-2';
    row.innerHTML = `
        <div class="row">
            <div class="col-6">
                <select class="form-select teaching-hour-component">
                    <option value="THEORY">Θεωρία</option>
                    <option value="LABORATORY">Εργαστήριο</option>
                </select>
            </div>
            <div class="col-5">
                <input type="number" class="form-control teaching-hour-hours" placeholder="Ώρες" min="1" required>
            </div>
            <div class="col-1">
                <button type="button" class="btn btn-sm btn-outline-danger remove-teaching-hour-btn">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        </div>
    `;
    
    container.appendChild(row);
}

function openEditCourseModal(courseId) {
    const token = localStorage.getItem('token');
    
    // Show loading indicator in the modal
    const modalBody = document.querySelector('#edit-course-modal .modal-body');
    if (modalBody) {
        const loadingSpinner = document.createElement('div');
        loadingSpinner.id = 'edit-modal-loading';
        loadingSpinner.className = 'text-center py-3';
        loadingSpinner.innerHTML = `
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <p class="mt-2">Φόρτωση δεδομένων...</p>
        `;
        
        const form = document.getElementById('edit-course-form');
        if (form) {
            form.style.display = 'none';
        }
        modalBody.prepend(loadingSpinner);
    }
    
    // Show the modal
    const editModal = new bootstrap.Modal(document.getElementById('edit-course-modal'));
    editModal.show();
    
    // Fetch course details
    fetch(`/api/courses/${courseId}`, {
        headers: {
            'Authorization': 'Bearer ' + token
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to load course details');
        }
        return response.json();
    })
    .then(course => {
        // Remove loading indicator
        const loadingSpinner = document.getElementById('edit-modal-loading');
        if (loadingSpinner) {
            loadingSpinner.remove();
        }
        
        // Show the form
        const form = document.getElementById('edit-course-form');
        if (form) {
            form.style.display = 'block';
        }
        
        // Populate the form
        document.getElementById('edit-course-id').value = course.id;
        document.getElementById('edit-course-name').value = course.name || '';
        document.getElementById('edit-course-code').value = course.code || '';
        document.getElementById('edit-course-type').value = course.type || '';
        document.getElementById('edit-course-year').value = course.year || '';
        document.getElementById('edit-course-semester').value = course.semester || '';
        document.getElementById('edit-course-capacity').value = course.capacity || '';
        document.getElementById('edit-course-active').checked = course.active;
        
        // Clear teaching hours container
        const teachingHoursContainer = document.getElementById('edit-teaching-hours-container');
        if (teachingHoursContainer) {
            teachingHoursContainer.innerHTML = '';
            
            // Add teaching hours
            if (course.teachingHours && course.teachingHours.length > 0) {
                course.teachingHours.forEach(hour => {
                    const row = document.createElement('div');
                    row.className = 'teaching-hour-row mb-2';
                    row.innerHTML = `
                        <div class="row">
                            <div class="col-6">
                                <select class="form-select teaching-hour-component">
                                    <option value="THEORY" ${hour.component === 'THEORY' ? 'selected' : ''}>Θεωρία</option>
                                    <option value="LABORATORY" ${hour.component === 'LABORATORY' ? 'selected' : ''}>Εργαστήριο</option>
                                </select>
                            </div>
                            <div class="col-5">
                                <input type="number" class="form-control teaching-hour-hours" placeholder="Ώρες" min="1" value="${hour.hours || ''}" required>
                            </div>
                            <div class="col-1">
                                <button type="button" class="btn btn-sm btn-outline-danger remove-teaching-hour-btn">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </div>
                        </div>
                    `;
                    teachingHoursContainer.appendChild(row);
                });
            } else {
                // Add at least one empty row if no teaching hours exist
                addTeachingHourRow('edit-teaching-hours-container');
            }
        }
    })
    .catch(error => {
        console.error('Error loading course details:', error);
        alert('Error loading course details: ' + error.message);
        
        // Close the modal on error
        editModal.hide();
    });
}

function saveCourseChanges() {
    // Show saving indicator
    const saveBtn = document.getElementById('save-edit-course-btn');
    const originalText = saveBtn.textContent;
    saveBtn.disabled = true;
    saveBtn.innerHTML = `
        <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
        Αποθήκευση...
    `;
    
    // Get form data
    const courseId = document.getElementById('edit-course-id').value;
    const name = document.getElementById('edit-course-name').value;
    const code = document.getElementById('edit-course-code').value;
    const type = document.getElementById('edit-course-type').value;
    const year = parseInt(document.getElementById('edit-course-year').value);
    const semester = parseInt(document.getElementById('edit-course-semester').value);
    const capacity = parseInt(document.getElementById('edit-course-capacity').value);
    const active = document.getElementById('edit-course-active').checked;
    
    // Get teaching hours
    const teachingHours = [];
    document.querySelectorAll('#edit-teaching-hours-container .teaching-hour-row').forEach(row => {
        const component = row.querySelector('.teaching-hour-component').value;
        const hours = parseInt(row.querySelector('.teaching-hour-hours').value);
        
        if (component && hours && !isNaN(hours)) {
            teachingHours.push({
                component: component,
                hours: hours
            });
        }
    });
    
    // Create data object
    const courseData = {
        id: courseId,
        name: name,
        code: code,
        type: type,
        year: year,
        semester: semester,
        capacity: capacity,
        active: active,
        teachingHours: teachingHours
    };
    
    console.log('Saving course data:', courseData);
    
    // Send update request
    const token = localStorage.getItem('token');
    fetch(`/api/courses/${courseId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify(courseData)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to update course: ' + response.status);
        }
        return response.json();
    })
    .then(updatedCourse => {
        console.log('Course updated successfully:', updatedCourse);
        
        // Close the modal
        const editModal = bootstrap.Modal.getInstance(document.getElementById('edit-course-modal'));
        if (editModal) {
            editModal.hide();
        }
        
        // Show success message
        showToast('Επιτυχία', 'Τα στοιχεία του μαθήματος ενημερώθηκαν επιτυχώς.');
        
        // Reload courses data
        loadCourses();
    })
    .catch(error => {
        console.error('Error updating course:', error);
        alert('Error updating course: ' + error.message);
    })
    .finally(() => {
        // Reset button state
        saveBtn.disabled = false;
        saveBtn.textContent = originalText;
    });
}

function resetAddCourseForm() {
    const form = document.getElementById('add-course-form');
    if (form) {
        form.reset();
    }
    
    // Clear and add one teaching hour row
    const container = document.getElementById('add-teaching-hours-container');
    if (container) {
        container.innerHTML = '';
        addTeachingHourRow('add-teaching-hours-container');
    }
}

function saveNewCourse() {
    // Show saving indicator
    const saveBtn = document.getElementById('save-add-course-btn');
    const originalText = saveBtn.textContent;
    saveBtn.disabled = true;
    saveBtn.innerHTML = `
        <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
        Αποθήκευση...
    `;
    
    // Get form data
    const name = document.getElementById('add-course-name').value;
    const code = document.getElementById('add-course-code').value;
    const type = document.getElementById('add-course-type').value;
    const year = parseInt(document.getElementById('add-course-year').value);
    const semester = parseInt(document.getElementById('add-course-semester').value);
    const capacity = parseInt(document.getElementById('add-course-capacity').value);
    const active = document.getElementById('add-course-active').checked;
    
    // Get teaching hours
    const teachingHours = [];
    document.querySelectorAll('#add-teaching-hours-container .teaching-hour-row').forEach(row => {
        const component = row.querySelector('.teaching-hour-component').value;
        const hours = parseInt(row.querySelector('.teaching-hour-hours').value);
        
        if (component && hours && !isNaN(hours)) {
            teachingHours.push({
                component: component,
                hours: hours
            });
        }
    });
    
    // Create data object
    const courseData = {
        name: name,
        code: code,
        type: type,
        year: year,
        semester: semester,
        capacity: capacity,
        active: active,
        teachingHours: teachingHours
    };
    
    console.log('Creating new course:', courseData);
    
    // Send create request
    const token = localStorage.getItem('token');
    fetch('/api/courses', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify(courseData)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to create course: ' + response.status);
        }
        return response.json();
    })
    .then(newCourse => {
        console.log('Course created successfully:', newCourse);
        
        // Close the modal
        const addModal = bootstrap.Modal.getInstance(document.getElementById('add-course-modal'));
        if (addModal) {
            addModal.hide();
        }
        
        // Show success message
        showToast('Επιτυχία', 'Το μάθημα δημιουργήθηκε επιτυχώς.');
        
        // Reload courses data
        loadCourses();
    })
    .catch(error => {
        console.error('Error creating course:', error);
        alert('Error creating course: ' + error.message);
    })
    .finally(() => {
        // Reset button state
        saveBtn.disabled = false;
        saveBtn.textContent = originalText;
    });
}

function openDeleteCourseModal(courseId, courseName) {
    document.getElementById('delete-course-id').value = courseId;
    document.getElementById('delete-course-name').textContent = courseName || 'this course';
    
    const deleteModal = new bootstrap.Modal(document.getElementById('delete-course-modal'));
    deleteModal.show();
}

function deleteCourse() {
    // Show deleting indicator
    const deleteBtn = document.getElementById('confirm-delete-course-btn');
    const originalText = deleteBtn.textContent;
    deleteBtn.disabled = true;
    deleteBtn.innerHTML = `
        <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
        Διαγραφή...
    `;
    
    // Get course ID
    const courseId = document.getElementById('delete-course-id').value;
    
    console.log('Deleting course with ID:', courseId);
    
    // Send delete request
    const token = localStorage.getItem('token');
    fetch(`/api/courses/${courseId}`, {
        method: 'DELETE',
        headers: {
            'Authorization': 'Bearer ' + token
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to delete course: ' + response.status);
        }
        return response.json();
    })
    .then(() => {
        console.log('Course deleted successfully');
        
        // Close the modal
        const deleteModal = bootstrap.Modal.getInstance(document.getElementById('delete-course-modal'));
        if (deleteModal) {
            deleteModal.hide();
        }
        
        // Show success message
        showToast('Επιτυχία', 'Το μάθημα διαγράφηκε επιτυχώς.');
        
        // Reload courses data
        loadCourses();
    })
    .catch(error => {
        console.error('Error deleting course:', error);
        alert('Error deleting course: ' + error.message);
    })
    .finally(() => {
        // Reset button state
        deleteBtn.disabled = false;
        deleteBtn.textContent = originalText;
    });
}

// Utility function to show toast messages
function showToast(title, message) {
    // Check if toast container exists, if not create it
    let toastContainer = document.getElementById('toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toast-container';
        toastContainer.className = 'position-fixed top-0 end-0 p-3';
        toastContainer.style.zIndex = '1050';
        document.body.appendChild(toastContainer);
    }
    
    // Create toast element
    const toastId = 'toast-' + Date.now();
    const toastEl = document.createElement('div');
    toastEl.id = toastId;
    toastEl.className = 'toast';
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');
    
    toastEl.innerHTML = `
        <div class="toast-header">
            <strong class="me-auto">${title}</strong>
            <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
        <div class="toast-body">
            ${message}
        </div>
    `;
    
    toastContainer.appendChild(toastEl);
    
    // Initialize and show the toast
    const toast = new bootstrap.Toast(toastEl, {
        autohide: true,
        delay: 3000
    });
    toast.show();
    
    // Remove toast after it's hidden
    toastEl.addEventListener('hidden.bs.toast', function() {
        toastEl.remove();
    });
}

// Make sure these functions are defined to avoid errors
if (!window.loadCourses) {
    window.loadCourses = loadCourses;
}