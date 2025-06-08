let teachersDataTable;

document.addEventListener('DOMContentLoaded', function () {
    // Check if token exists
    const token = localStorage.getItem('token');
    const username = localStorage.getItem('username');

    if (!token) {
        // Redirect to login page if no token
        window.location.href = '/login';
        return;
    }

    // Display username
    if (username) {
        document.getElementById('username-display').textContent = username;
    }

    // Toggle sidebar functionality
    const sidebar = document.getElementById('sidebar');
    const content = document.getElementById('content');
    const topbar = document.getElementById('topbar');
    const toggleBtn = document.getElementById('toggle-sidebar');

    toggleBtn.addEventListener('click', function () {
        alert("helo");
        sidebar.classList.toggle('sidebar-collapsed');
        content.classList.toggle('content-collapsed');
        topbar.classList.toggle('topbar-collapsed');
    });

    // Fetch user profile data
    fetchUserProfile(token);
});

async function fetchUserProfile(token) {
    try {
        const response = await fetch('/users/api/profile', {
            headers: {
                'Authorization': 'Bearer ' + token
            }
        });

        if (response.ok) {
            const userData = await response.json();
            displayProfileData(userData);
        } else if (response.status === 401) {
            // Token expired or invalid
            localStorage.removeItem('token');
            localStorage.removeItem('username');
            window.location.href = '/login?session=expired';
        }
    } catch (error) {
        console.error('Error fetching user profile:', error);
    }
}

function displayProfileData(userData) {
    // Update profile section
    document.getElementById('profile-name').textContent = userData.fullName || 'Ν/Α';
    document.getElementById('profile-role').textContent = userData.teacherType || 'Χρήστης';

    // Update form fields
    document.getElementById('fullName').value = userData.fullName || '';
    document.getElementById('username').value = userData.username || '';
    document.getElementById('email').value = userData.email || '';
    document.getElementById('teacherType').value = userData.teacherType || '';
    document.getElementById('teacherRank').value = userData.teacherRank || '';
}

function logout() {
    // Clear the token
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    // Redirect to login page
    window.location.href = '/login?logout=success';
}