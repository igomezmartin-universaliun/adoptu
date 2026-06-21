(async () => {
    try {
        const user = await api.me();
        if (user.authenticated === false) { location.href = '/login'; return; }
        if (!user.activeRoles?.includes('TEMPORAL_HOME') && !user.activeRoles?.includes('ADMIN')) { location.href = '/'; return; }
    } catch (e) { location.href = '/login'; return; }
})();

async function loadRequests() {
    try {
        const output = await fetch('/api/users/temporal-home/requests');
        if (!output.ok) throw new Error('Failed to load requests');
        const requests = await output.json();
        const container = document.getElementById('requests-container');
        if (!requests.length) { container.innerHTML = '<p>No requests yet.</p>'; return; }
        container.innerHTML = requests.map(r => '<div class="request-card"><p><strong>'+r.rescuerName+'</strong> wants help with '+(r.petName || 'a pet')+'</p><p>'+r.message+'</p><button class="btn btn-small" onclick="blockRescuer('+r.rescuerId+')">Block Rescuer</button></div>').join('');
    } catch (err) { console.error(err); }
}
loadRequests();

window.blockRescuer = async (rescuerId) => {
    if (!confirm('Block this rescuer from sending you more requests?')) return;
    try {
        const output = await fetch('/api/temporal-homes/block', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ rescuerId })
        });
        const result = await output.json();
        alert(result.blocked ? 'Rescuer blocked!' : 'Already blocked');
        loadRequests();
    } catch (err) { alert(err.message); }
};