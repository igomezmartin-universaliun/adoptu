let editingId = null;

function escapeHtml(s) {
    if (!s) return '';
    return s.toString().replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

async function loadShelters() {
    const country = document.getElementById('filter-country').value;
    const state = document.getElementById('filter-state').value;
    const container = document.getElementById('shelters');
    const stateSelect = document.getElementById('filter-state');
    
    if (!country) {
        container.innerHTML = '<p>' + t('selectCountryToFilter') + '</p>';
        return;
    }
    
    // Load states for filter
    stateSelect.innerHTML = '<option value="">' + t('allStates') + '</option>';
    try {
        const statesRes = await fetch('/api/shelters/countries/' + encodeURIComponent(country) + '/states');
        if (statesRes.ok) {
            const data = await statesRes.json();
            if (data.states && data.states.length > 0) {
                data.states.forEach(state => {
                    const option = document.createElement('option');
                    option.value = state;
                    option.textContent = state;
                    stateSelect.appendChild(option);
                });
            }
        }
    } catch (e) {
        console.error('Error loading states:', e);
    }
    
    const params = new URLSearchParams();
    params.append('country', country);
    if (state) params.append('state', state);
    
    try {
        const output = await fetch('/api/shelters?' + params.toString());
        if (!output.ok) throw new Error('Failed to load shelters');
        const shelters = await output.json();
        
        if (!shelters || shelters.length === 0) {
            container.innerHTML = '<p>' + t('noSheltersFound') + '</p>';
            return;
        }
        
        container.innerHTML = '<table class="admin-table"><thead><tr><th>' + t('name') + '</th><th>' + t('location') + '</th><th>' + t('contact') + '</th><th>' + t('actions') + '</th></tr></thead><tbody>' +
            shelters.map(s => '<tr>' +
                '<td><strong>' + escapeHtml(s.name) + '</strong></td>' +
                '<td>' + escapeHtml(s.city || '') + ', ' + escapeHtml(s.state || '') + ', ' + escapeHtml(tCountry(s.country) || '') + '</td>' +
                '<td>' + escapeHtml(s.phone || s.email || '-') + '</td>' +
                '<td><button class="btn btn-secondary" onclick="editShelter(' + s.id + ')">' + t('edit') + '</button> <button class="btn btn-danger" onclick="deleteShelter(' + s.id + ')">' + t('delete') + '</button></td>' +
                '</tr>'
            ).join('') + '</tbody></table>';
    } catch (err) {
        showMessage(t('errorLoadingShelters'), 'error');
    }
}

function showMessage(msg, type) {
    const div = document.getElementById('message');
    div.textContent = msg;
    div.className = type === 'error' ? 'error-message' : 'success-message';
    div.style.display = 'block';
}

function getFormData() {
    return {
        name: document.getElementById('name').value,
        country: document.getElementById('country').value,
        state: document.getElementById('state').value || null,
        city: document.getElementById('city').value,
        address: document.getElementById('address').value,
        zip: document.getElementById('zip').value || null,
        phone: document.getElementById('phone').value || null,
        email: document.getElementById('email').value || null,
        website: document.getElementById('website').value || null,
        fiscalId: document.getElementById('fiscalId').value || null,
        bankName: document.getElementById('bankName').value || null,
        accountHolderName: document.getElementById('accountHolderName').value || null,
        accountNumber: document.getElementById('accountNumber').value || null,
        iban: document.getElementById('iban').value || null,
        swiftBic: document.getElementById('swiftBic').value || null,
        currency: document.getElementById('currency').value,
        description: document.getElementById('description').value || null
    };
}

function clearForm() {
    document.getElementById('shelter-form').reset();
    document.getElementById('shelter-id').value = '';
    document.getElementById('submit-btn').textContent = t('addShelter');
    document.getElementById('cancel-btn').style.display = 'none';
    editingId = null;
}

function cancelEdit() {
    clearForm();
}

async function editShelter(id) {
    try {
        const output = await fetch('/api/shelters/' + id);
        if (!output.ok) throw new Error('Failed to load shelter');
        const shelter = await output.json();
        
        editingId = id;
        document.getElementById('shelter-id').value = id;
        document.getElementById('name').value = shelter.name;
        document.getElementById('country').value = shelter.country;
        document.getElementById('state').value = shelter.state || '';
        document.getElementById('city').value = shelter.city;
        document.getElementById('address').value = shelter.address;
        document.getElementById('zip').value = shelter.zip || '';
        document.getElementById('phone').value = shelter.phone || '';
        document.getElementById('email').value = shelter.email || '';
        document.getElementById('website').value = shelter.website || '';
        document.getElementById('fiscalId').value = shelter.fiscalId || '';
        document.getElementById('bankName').value = shelter.bankName || '';
        document.getElementById('accountHolderName').value = shelter.accountHolderName || '';
        document.getElementById('accountNumber').value = shelter.accountNumber || '';
        document.getElementById('iban').value = shelter.iban || '';
        document.getElementById('swiftBic').value = shelter.swiftBic || '';
        document.getElementById('currency').value = shelter.currency || 'USD';
        document.getElementById('description').value = shelter.description || '';
        
        document.getElementById('submit-btn').textContent = t('updateShelter');
        document.getElementById('cancel-btn').style.display = 'inline-block';
        
        document.getElementById('shelter-form').scrollIntoView({ behavior: 'smooth' });
    } catch (err) {
        showMessage(t('errorLoadingShelter'), 'error');
    }
}

async function deleteShelter(id) {
    if (!confirm(t('confirmDeleteShelter'))) return;
    
    try {
        const output = await fetch('/api/admin/shelters/' + id, { method: 'DELETE' });
        if (!output.ok) throw new Error('Failed to delete shelter');
        showMessage(t('shelterDeleted'), 'success');
        loadShelters();
    } catch (err) {
        showMessage(t('errorDeletingShelter'), 'error');
    }
}

document.getElementById('shelter-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = getFormData();
    const id = document.getElementById('shelter-id').value;
    const method = id ? 'PUT' : 'POST';
    const url = id ? '/api/admin/shelters/' + id : '/api/admin/shelters';
    
    try {
        const output = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        if (!output.ok) {
            const error = await output.json();
            throw new Error(error.message || 'Failed to save shelter');
        }
        
        showMessage(id ? t('shelterUpdated') : t('shelterAdded'), 'success');
        clearForm();
        loadShelters();
    } catch (err) {
        showMessage(err.message || t('errorSavingShelter'), 'error');
    }
});

// Initial load
document.addEventListener('DOMContentLoaded', () => {
    const stateSelect = document.getElementById('filter-state');
    stateSelect.innerHTML = '<option value="">' + t('allStates') + '</option>';
    stateSelect.disabled = true;
});