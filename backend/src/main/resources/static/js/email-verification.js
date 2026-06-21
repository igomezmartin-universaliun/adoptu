let seconds = 10;
const countdownEl = document.getElementById('countdown');
const interval = setInterval(() => {
    seconds--;
    if (countdownEl) countdownEl.textContent = seconds;
    if (seconds <= 0) {
        clearInterval(interval);
        window.location.href = '/';
    }
}, 1000);
