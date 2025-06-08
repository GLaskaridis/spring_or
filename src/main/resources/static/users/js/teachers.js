// Utility function to decode JWT
function parseJwt(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        return JSON.parse(jsonPayload);
    } catch (e) {
        console.error('Error parsing token:', e);
        return null;
    }
}

function logout(){
            //  const token = localStorage.getItem('token');
    localStorage.setItem('token', null) ;
    window.location.href = '/login';
}

// Check if token is expired
function isTokenExpired(token) {
    if (!token) return true;
    
    const decodedToken = parseJwt(token);
    if (!decodedToken) return true;
    
    // Check if token has an expiration date
    if (!decodedToken.exp) return false;
    
    // Compare expiration timestamp with current time
    const expirationTime = decodedToken.exp * 1000; // Convert to milliseconds
    const currentTime = Date.now();
    
    const isExpired = currentTime > expirationTime;
    console.log('Token expiration check:', {
        expirationTime: new Date(expirationTime).toLocaleString(),
        currentTime: new Date(currentTime).toLocaleString(),
        isExpired: isExpired
    });
    
    return isExpired;
}

document.addEventListener('DOMContentLoaded', function() {
    // Setup teacher tab functionality
    setupTeachersTab();
    
    // Check if token exists and is valid
    const token = localStorage.getItem('token');
    console.log('Token from localStorage:', token ? 'Token exists' : 'No token found');
    
    if (token) {
        // Parse the token to check its contents
        const decodedToken = parseJwt(token);
        console.log('Decoded token:', decodedToken);
        
        // Check token expiration
        const expired = isTokenExpired(token);
        console.log('Is token expired?', expired);
        
        if (expired) {
            console.log('Token is expired, redirecting to login...');
            localStorage.removeItem('token'); // Clear expired token
            window.location.href = '/login';
            return;
        }
    } else {
        console.log('No token found, redirecting to login...');
        window.location.href = '/login';
        return;
    }
    
    // Display username if available
    const username = localStorage.getItem('username');
    if (username) {
        document.getElementById('username-display').textContent = username;
    }
    
    // Initialize edit teacher form submission
    const saveEditTeacherBtn = document.getElementById('save-edit-teacher-btn');
    if (saveEditTeacherBtn) {
        saveEditTeacherBtn.addEventListener('click', saveTeacherChanges);
    }
    
    // Initialize delete teacher button
    const confirmDeleteBtn = document.getElementById('confirm-delete-teacher-btn');
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener('click', deleteTeacher);
    }
});

function setupTeachersTab() {
    // Set up buttons
    const addTeacherBtn = document.getElementById('add-teacher-btn');
    if (addTeacherBtn) {
        addTeacherBtn.addEventListener('click', function() {
            const addTeacherModal = new bootstrap.Modal(document.getElementById('add-teacher-modal'));
            addTeacherModal.show();
        });
    }
    
    const addFirstTeacherBtn = document.getElementById('add-first-teacher-btn');
    if (addFirstTeacherBtn) {
        addFirstTeacherBtn.addEventListener('click', function() {
            const addTeacherModal = new bootstrap.Modal(document.getElementById('add-teacher-modal'));
            addTeacherModal.show();
        });
    }
    
    // Load teachers when tab is shown
    const teachersTabLink = document.querySelector('a[href="#teachers-content"]');
    if (teachersTabLink) {
        teachersTabLink.addEventListener('shown.bs.tab', function() {
            console.log('Teachers tab shown, loading data...');
            loadTeachers();
        });
    }
    
    // Initial load if teachers tab is active
    if (document.querySelector('#teachers-content.active')) {
        console.log('Teachers tab is active on page load, loading data...');
        loadTeachers();
    }
}

function loadTeachers() {
    console.log('Loading teachers data...');
    const token = localStorage.getItem('token');
    console.log('Using token for API call:', token ? 'Token available' : 'No token');
    
    // Get DOM elements
    const loadingEl = document.getElementById('teachers-loading');
    const errorEl = document.getElementById('teachers-error');
    const emptyEl = document.getElementById('teachers-empty');
    const tableContainerEl = document.getElementById('teachers-table-container');
    
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
    
    // Fetch teachers data
    console.log('Fetching from API...');
    console.log('Request headers:', { 'Authorization': 'Bearer ' + token });
    
    fetch('/api/teachers', {
        headers: {
            'Authorization': 'Bearer ' + token
        }
    })
    .then(response => {
        console.log('API response received:', response.status);
        if (!response.ok) {
            // Log response details for debugging
            console.error('API response error:', {
                status: response.status,
                statusText: response.statusText
            });
            
            return response.text().then(text => {
                try {
                    // Try to parse as JSON first
                    const json = JSON.parse(text);
                    console.error('API error details:', json);
                    throw new Error('Failed to load teachers: ' + response.status + ' - ' + (json.message || json.error || response.statusText));
                } catch (e) {
                    // If it's not JSON, log as text
                    console.error('API error response:', text);
                    throw new Error('Failed to load teachers: ' + response.status + ' - ' + response.statusText);
                }
            });
        }
        return response.json();
    })
    .then(data => {
        console.log('Teachers data received:', data);
        loadingEl.style.display = 'none';
        
        if (!data || data.length === 0) {
            console.log('No teachers found, showing empty state');
            emptyEl.style.display = 'flex';
        } else {
            console.log('Displaying teachers table with', data.length, 'records');
            tableContainerEl.style.display = 'block';
            populateTeachersTable(data);
        }
    })
    .catch(error => {
        console.error('Error loading teachers:', error);
        loadingEl.style.display = 'none';
        errorEl.style.display = 'block';
        const errorMsgEl = document.getElementById('teachers-error-message');
        if (errorMsgEl) {
            errorMsgEl.textContent = error.message || 'Unknown error occurred';
        }
    });
}

function populateTeachersTable(teachers) {
    console.log('Populating teachers table...');
    const tableBody = document.getElementById('teachers-table-body');
    if (!tableBody) {
        console.error('Teachers table body element not found!');
        return;
    }
    
    tableBody.innerHTML = '';
    
    teachers.forEach((teacher, index) => {
        console.log('Adding teacher to table:', teacher.username);
        const row = document.createElement('tr');
        row.innerHTML = `
            <th scope="row">${index + 1}</th>
            <td>${teacher.fullName || 'N/A'}</td>
            <td>${teacher.username || 'N/A'}</td>
            <td>${teacher.teacherType || 'N/A'}</td>
            <td>${teacher.teacherRank || 'N/A'}</td>
            <td>${teacher.email || 'N/A'}</td>
            <td>
                <span class="badge ${teacher.active ? 'bg-success' : 'bg-danger'}">
                    ${teacher.active ? 'Ενεργός' : 'Ανενεργός'}
                </span>
            </td>
            <td>
                <button class="btn btn-sm btn-outline-primary edit-teacher-btn" data-id="${teacher.id}">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger delete-teacher-btn" data-id="${teacher.id}" data-name="${teacher.fullName}">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        `;
        tableBody.appendChild(row);
    });
    
    // Add event listeners to edit/delete buttons
    document.querySelectorAll('.edit-teacher-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const teacherId = this.getAttribute('data-id');
            console.log('Edit button clicked for teacher ID:', teacherId);
            openEditTeacherModal(teacherId);
        });
    });
    
    document.querySelectorAll('.delete-teacher-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const teacherId = this.getAttribute('data-id');
            const teacherName = this.getAttribute('data-name');
            console.log('Delete button clicked for teacher:', teacherName);
            openDeleteTeacherModal(teacherId, teacherName);
        });
    });
    
    console.log('Teachers table populated successfully');
}

function openEditTeacherModal(teacherId) {
    const token = localStorage.getItem('token');
    
    // Show loading indicator in the modal
    const modalBody = document.querySelector('#edit-teacher-modal .modal-body');
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
        
        const form = document.getElementById('edit-teacher-form');
        if (form) {
            form.style.display = 'none';
        }
        modalBody.prepend(loadingSpinner);
    }
    
    // Show the modal
    const editModal = new bootstrap.Modal(document.getElementById('edit-teacher-modal'));
    editModal.show();
    
    // Fetch teacher details
    fetch(`/api/teachers/${teacherId}`, {
        headers: {
            'Authorization': 'Bearer ' + token
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to load teacher details');
        }
        return response.json();
    })
    .then(teacher => {
        // Remove loading indicator
        const loadingSpinner = document.getElementById('edit-modal-loading');
        if (loadingSpinner) {
            loadingSpinner.remove();
        }
        
        // Show the form
        const form = document.getElementById('edit-teacher-form');
        if (form) {
            form.style.display = 'block';
        }
        
        // Populate the form
        document.getElementById('edit-teacher-id').value = teacher.id;
        document.getElementById('edit-full-name').value = teacher.fullName || '';
        document.getElementById('edit-username').value = teacher.username || '';
        document.getElementById('edit-email').value = teacher.email || '';
        document.getElementById('edit-teacher-type').value = teacher.teacherType || '';
        document.getElementById('edit-teacher-rank').value = teacher.teacherRank || '';
        document.getElementById('edit-active').checked = teacher.active;
    })
    .catch(error => {
        console.error('Error loading teacher details:', error);
        alert('Error loading teacher details: ' + error.message);
        
        // Close the modal on error
        editModal.hide();
    });
}

function saveTeacherChanges() {
    // Show saving indicator
    const saveBtn = document.getElementById('save-edit-teacher-btn');
    const originalText = saveBtn.textContent;
    saveBtn.disabled = true;
    saveBtn.innerHTML = `
        <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
        Αποθήκευση...
    `;
    
    // Get form data
    const teacherId = document.getElementById('edit-teacher-id').value;
    const fullName = document.getElementById('edit-full-name').value;
    const username = document.getElementById('edit-username').value;
    const email = document.getElementById('edit-email').value;
    const teacherType = document.getElementById('edit-teacher-type').value;
    const teacherRank = document.getElementById('edit-teacher-rank').value;
    const active = document.getElementById('edit-active').checked;
    
    // Create data object
    const teacherData = {
        id: teacherId,
        fullName: fullName,
        username: username,
        email: email,
        teacherType: teacherType,
        teacherRank: teacherRank,
        active: active
    };
    
    console.log('Saving teacher data:', teacherData);
    
    // Send update request
    const token = localStorage.getItem('token');
    fetch(`/api/teachers/${teacherId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify(teacherData)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to update teacher: ' + response.status);
        }
        return response.json();
    })
    .then(updatedTeacher => {
        console.log('Teacher updated successfully:', updatedTeacher);
        
        // Close the modal
        const editModal = bootstrap.Modal.getInstance(document.getElementById('edit-teacher-modal'));
        if (editModal) {
            editModal.hide();
        }
        
        // Show success message
        showToast('Επιτυχία', 'Τα στοιχεία του διδάσκοντα ενημερώθηκαν επιτυχώς.');
        
        // Reload teachers data
        loadTeachers();
    })
    .catch(error => {
        console.error('Error updating teacher:', error);
        alert('Error updating teacher: ' + error.message);
    })
    .finally(() => {
        // Reset button state
        saveBtn.disabled = false;
        saveBtn.textContent = originalText;
    });
}

function openDeleteTeacherModal(teacherId, teacherName) {
    document.getElementById('delete-teacher-id').value = teacherId;
    document.getElementById('delete-teacher-name').textContent = teacherName || 'this teacher';
    
    const deleteModal = new bootstrap.Modal(document.getElementById('delete-teacher-modal'));
    deleteModal.show();
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

function deleteTeacher() {
    // Show deleting indicator
    const deleteBtn = document.getElementById('confirm-delete-teacher-btn');
    const originalText = deleteBtn.textContent;
    deleteBtn.disabled = true;
    deleteBtn.innerHTML = `
        <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
        Διαγραφή...
    `;
    
    // Get teacher ID
    const teacherId = document.getElementById('delete-teacher-id').value;
    
    console.log('Deleting teacher with ID:', teacherId);
    
    // Send delete request
    const token = localStorage.getItem('token');
    fetch(`/api/teachers/${teacherId}`, {
        method: 'DELETE',
        headers: {
            'Authorization': 'Bearer ' + token
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to delete teacher: ' + response.status);
        }
        return response.json();
    })
    .then(() => {
        console.log('Teacher deleted successfully');
        
        // Close the modal
        const deleteModal = bootstrap.Modal.getInstance(document.getElementById('delete-teacher-modal'));
        if (deleteModal) {
            deleteModal.hide();
        }
        
        // Show success message
        showToast('Επιτυχία', 'Ο διδάσκοντας διαγράφηκε επιτυχώς.');
        
        // Reload teachers data
        loadTeachers();
    })
    .catch(error => {
        console.error('Error deleting teacher:', error);
        alert('Error deleting teacher: ' + error.message);
    })
    .finally(() => {
        // Reset button state
        deleteBtn.disabled = false;
        deleteBtn.textContent = originalText;
    });
}

// Make sure these functions are defined to avoid errors
if (!window.loadTeachers) {
    window.loadTeachers = loadTeachers;
}