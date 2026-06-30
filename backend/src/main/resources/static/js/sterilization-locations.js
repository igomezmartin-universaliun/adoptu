let editingId = null;

function escapeHtml(s) {
    if (!s) return '';
    return s.toString().replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

async function loadCountries() {
    const res = await fetch('/api/sterilization-locations/countries');
    const data = await res.json();
    const select = document.getElementById('form-country');
    select.innerHTML = '<option value="">' + t('selectCountry') + '</option>' + 
        data.countries.map(c => '<option value="'+c+'">'+c+'</option>').join('');
}

async function loadLocations() {
    const res = await fetch('/api/admin/sterilization-locations');
    const locations = await res.json();
    const container = document.getElementById('locations-container');
    if (locations.length === 0) {
        container.innerHTML = '<p>'+t('noLocationsFound')+'</p>';
        return;
    }
    container.innerHTML = '<div class="location-list">' + locations.map(loc => `
        <div class="location-card card-bg">
            <h3>${escapeHtml(loc.name)}</h3>
            <p class="location-address">${escapeHtml(loc.address)}, ${escapeHtml(loc.city)}${loc.state ? ', ' + escapeHtml(loc.state) : ''}, ${escapeHtml(loc.country)}</p>
            ${loc.phone ? `<p class="location-phone"><strong>${t('phone')}:</strong> ${escapeHtml(loc.phone)}</p>` : ''}
            ${loc.email ? `<p class="location-email"><strong>${t('email')}:</strong> ${escapeHtml(loc.email)}</p>` : ''}
            ${loc.website ? `<p class="location-website"><a href="${escapeHtml(loc.website)}" target="_blank">${t('website')}</a></p>` : ''}
            ${loc.description ? `<p class="location-description">${escapeHtml(loc.description)}</p>` : ''}
            <div class="pet-card-actions">
                <button class="btn" onclick="editLocation(${loc.id})">${t('edit')}</button>
                <button class="btn btn-danger" onclick="deleteLocation(${loc.id})">${t('delete')}</button>
            </div>
        </div>
    `).join('') + '</div>';
}

function showForm() {
    editingId = null;
    document.getElementById('location-form').reset();
    document.getElementById('form-modal').style.display = 'flex';
    loadCountries();
}

function hideForm() {
    document.getElementById('form-modal').style.display = 'none';
    editingId = null;
}

async function editLocation(id) {
    editingId = id;
    const res = await fetch('/api/sterilization-locations/'+id);
    const loc = await res.json();
    await loadCountries();
    document.getElementById('location-form').name.value = loc.name;
    document.getElementById('form-country').value = loc.country;
    document.getElementById('location-form').state.value = loc.state || '';
    document.getElementById('location-form').city.value = loc.city;
    document.getElementById('location-form').address.value = loc.address;
    document.getElementById('location-form').zip.value = loc.zip || '';
    document.getElementById('location-form').phone.value = loc.phone || '';
    document.getElementById('location-form').email.value = loc.email || '';
    document.getElementById('location-form').website.value = loc.website || '';
    document.getElementById('location-form').description.value = loc.description || '';
    document.getElementById('form-modal').style.display = 'flex';
}

async function deleteLocation(id) {
    if (!confirm(t('confirmDelete'))) return;
    await fetch('/api/admin/sterilization-locations/'+id, { method: 'DELETE' });
    loadLocations();
}

document.getElementById('location-form').onsubmit = async (e) => {
    e.preventDefault();
    const form = e.target;
    const data = {
        name: form.name.value,
        country: form.country.value,
        state: form.state.value || null,
        city: form.city.value,
        address: form.address.value,
        zip: form.zip.value || null,
        phone: form.phone.value || null,
        email: form.email.value || null,
        website: form.website.value || null,
        description: form.description.value || null
    };
    const method = editingId ? 'PUT' : 'POST';
    const url = editingId ? '/api/admin/sterilization-locations/'+editingId : '/api/admin/sterilization-locations';
    const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    if (res.ok) {
        hideForm();
        loadLocations();
    } else {
        document.getElementById('message').innerHTML = '<div class="message error">'+t('errorSaving')+'</div>';
    }
};

loadCountries();
loadLocations();