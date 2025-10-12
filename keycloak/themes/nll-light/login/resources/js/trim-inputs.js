// Trim whitespace from username and password fields on form submission
document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('kc-form-login');
    
    if (loginForm) {
        loginForm.addEventListener('submit', function(event) {
            // Trim username field
            const usernameField = document.getElementById('username');
            if (usernameField && usernameField.value) {
                usernameField.value = usernameField.value.trim();
            }
            
            // Trim password field
            const passwordField = document.getElementById('password');
            if (passwordField && passwordField.value) {
                passwordField.value = passwordField.value.trim();
            }
        });
        
        // Also trim on blur (when user leaves the field)
        const usernameField = document.getElementById('username');
        if (usernameField) {
            usernameField.addEventListener('blur', function() {
                this.value = this.value.trim();
            });
        }
    }
});
