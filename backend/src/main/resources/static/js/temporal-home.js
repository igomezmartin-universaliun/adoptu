async function blockRescuerAndRedirect(thId, rId) {
    try {
        const res = await fetch('/api/temporal-homes/block/'+thId+'?rescuer='+rId);
        const data = await res.json();
        if (data.blocked) {
            document.body.innerHTML = '<h1>Rescuer blocked!</h1><p>You will no longer receive requests from this rescuer.</p><a href="/">Go to Home</a>';
        } else {
            document.body.innerHTML = '<h1>This rescuer was already blocked.</h1><a href="/">Go to Home</a>';
        }
    } catch(e) { document.body.innerHTML = '<h1>Error blocking rescuer</h1>'; }
}
