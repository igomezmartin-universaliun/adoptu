const emoji = { DOG: '🐕', CAT: '🐱', BIRD: '🐦', FISH: '🐟' };
const params = new URLSearchParams(location.search);
let user;
let existingImages = [];

function escapeHtml(s) {
    if (!s) return '';
    return s.toString().replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

async function load() {
    user = await api.me();
    if (user.authenticated === false || !user.activeRoles?.includes('RESCUER') && !user.activeRoles?.includes('ADMIN')) { location.href = '/'; return; }
    let pets = await api.getPets(params.get('filter'));
    if (!user.activeRoles?.includes('ADMIN')) pets = pets.filter(p => p.rescuerId === user.id);
    const container = document.getElementById('pets');
    container.innerHTML = pets.length ? pets.map(p => {
        const primaryImage = p.images && p.images.length > 0 ? p.images.find(img => img.isPrimary) || p.images[0] : null;
        const imageHtml = primaryImage 
            ? '<img src="'+primaryImage.imageUrl+'" alt="'+escapeHtml(p.name)+'">' 
            : '<div class="pet-card-placeholder">'+(emoji[p.type]||'🐾')+'</div>';
        return '<div class="pet-card">'+imageHtml+'<div class="pet-card-body"><span class="pet-type">'+t(p.type.toLowerCase())+'</span><span class="pet-sex '+(p.sex === 'MALE' ? 'male' : 'female')+'">'+t(p.sex.toLowerCase())+'</span>'+(p.size ? '<span class="pet-size">'+escapeHtml(p.size)+'</span>' : '')+'<div class="pet-name"><h3>'+escapeHtml(p.name)+(p.isUrgent ? ' ⚠️' : '')+'</h3>'+(p.breed ? '<span class="pet-breed">'+escapeHtml(p.breed)+'</span>' : '')+'</div><p class="pet-info"><span class="pet-age">'+p.ageYears+t('years')+' '+p.ageMonths+t('months')+' • '+p.weight+' kg</span><span class="pet-rescue-date">'+(p.rescueDate ? ' '+t('rescued')+': '+new Date(p.rescueDate).toLocaleDateString() : '')+'</span></p><p>'+escapeHtml(p.status)+'</p><div class="pet-card-actions"><a href="/pet/'+p.id+'" class="btn">'+t('viewDetails')+'</a><button class="btn btn-secondary" onclick="edit('+p.id+')">'+t('edit')+'</button><button class="btn btn-secondary" onclick="del('+p.id+')">'+t('delete')+'</button></div></div></div>';
    }).join('') : '<p>'+t('noPets')+'</p>';
    const editId = params.get('edit');
    if (editId) { 
    const pet = await api.getPet(editId); 
    fillForm(pet);
    document.getElementById('form-title').textContent = 'Edit Pet'; 
    document.getElementById('form-container').style.display = 'block'; }
    
    await loadAdoptionRequests(pets);
}
async function loadAdoptionRequests(pets) {
    const container = document.getElementById('adoption-requests');
    let allRequests = [];
    for (const pet of pets) {
        try {
            const requests = await api.getAdoptionRequests(pet.id);
            if (requests.length > 0) {
                allRequests = allRequests.concat(requests.map(r => ({...r, petName: pet.name, petType: pet.type})));
            }
        } catch (e) {}
    }
    if (allRequests.length === 0) {
        container.innerHTML = '<p>No adoption requests</p>';
        return;
    }
    container.innerHTML = allRequests.map(r => '<div class="adoption-request-card"><div class="ar-pet">'+(emoji[r.petType]||'🐾')+' '+escapeHtml(r.petName)+'</div><div class="ar-status status-'+r.status.toLowerCase()+'">'+escapeHtml(r.status)+'</div><div class="ar-message">'+(r.message ? escapeHtml(r.message) : 'No message')+'</div><div class="ar-date">'+new Date(r.createdAt).toLocaleDateString()+'</div>'+(r.status === 'PENDING' ? '<div class="ar-actions"><button class="btn btn-secondary" onclick="approveRequest('+r.id+')">Approve</button><button class="btn btn-secondary" onclick="rejectRequest('+r.id+')">Reject</button></div>' : '')+'</div>').join('');
}
window.approveRequest = async (requestId) => { try { await api.updateAdoptionRequest(requestId, 'APPROVED'); load(); } catch (err) { alert(err.message); } };
window.rejectRequest = async (requestId) => { try { await api.updateAdoptionRequest(requestId, 'REJECTED'); load(); } catch (err) { alert(err.message); } };
function fillForm(pet) {
    document.getElementById('pet-id').value = pet.id; 
    document.getElementById('name').value = pet.name || ''; 
    document.getElementById('type').value = pet.type || 'DOG'; 
    document.getElementById('breed').value = pet.breed || ''; 
    document.getElementById('description').value = pet.description || ''; 
    document.getElementById('weight').value = pet.weight || 0; 
    document.getElementById('ageYears').value = pet.ageYears || 0; 
    document.getElementById('ageMonths').value = pet.ageMonths || 0; 
    document.getElementById('sex').value = pet.sex || 'MALE';
    document.getElementById('color').value = pet.color || '';
    document.getElementById('size').value = pet.size || '';
    document.getElementById('temperament').value = pet.temperament || '';
    document.getElementById('energyLevel').value = pet.energyLevel || '';
    document.getElementById('isSterilized').checked = pet.isSterilized || false;
    document.getElementById('isMicrochipped').checked = pet.isMicrochipped || false;
    document.getElementById('microchipId').value = pet.microchipId || '';
    document.getElementById('vaccinations').value = pet.vaccinations || '';
    document.getElementById('isGoodWithKids').checked = pet.isGoodWithKids !== false;
    document.getElementById('isGoodWithDogs').checked = pet.isGoodWithDogs !== false;
    document.getElementById('isGoodWithCats').checked = pet.isGoodWithCats !== false;
    document.getElementById('isHouseTrained').checked = pet.isHouseTrained || false;
    document.getElementById('rescueLocation').value = pet.rescueLocation || '';
    if (pet.rescueDate) {
        const d = new Date(pet.rescueDate);
        document.getElementById('rescueDate').value = d.toISOString().split('T')[0];
    }
    document.getElementById('specialNeeds').value = pet.specialNeeds || '';
    document.getElementById('adoptionFee').value = pet.adoptionFee || 0;
    document.getElementById('currency').value = pet.currency || 'USD';
    document.getElementById('isUrgent').checked = pet.isUrgent || false;
    
    existingImages = pet.images || [];
    updatePreviews();
}
window.edit = async (id) => { const pet = await api.getPet(id); fillForm(pet); document.getElementById('form-title').textContent = 'Edit Pet'; document.getElementById('form-container').style.display = 'block'; };
window.del = async (id) => { if (!confirm('Delete this pet?')) return; await api.deletePet(id); load(); };
const addBtn = document.getElementById('add-btn');
if (addBtn) {
    addBtn.onclick = () => { document.getElementById('pet-form').reset(); document.getElementById('pet-id').value = ''; document.getElementById('currency').value = 'USD'; selectedFiles = []; existingImages = []; updatePreviews(); document.getElementById('form-title').textContent = 'Add Pet'; document.getElementById('form-container').style.display = 'block'; };
}
const cancelBtn = document.getElementById('cancel-btn');
if (cancelBtn) {
    cancelBtn.onclick = () => { document.getElementById('form-container').style.display = 'none'; history.replaceState({}, '', '/my-pets'); };
}
['weight', 'ageYears', 'ageMonths'].forEach(id => {
    const el = document.getElementById(id);
    el.addEventListener('input', () => { if (parseFloat(el.value) < 0) el.value = 0; });
    el.addEventListener('blur', () => { if (parseFloat(el.value) < 0) el.value = 0; if (id === 'ageMonths' && parseInt(el.value) > 11) el.value = 11; });
});
['adoptionFee'].forEach(id => {
    const el = document.getElementById(id);
    el.addEventListener('input', () => { if (parseFloat(el.value) < 0) el.value = 0; });
    el.addEventListener('blur', () => { if (parseFloat(el.value) < 0) el.value = 0; });
});
const dropzone = document.getElementById('storage-dropzone');
const fileInput = document.getElementById('pet-images');
const previewContainer = document.getElementById('storage-previews');
let selectedFiles = [];
dropzone.addEventListener('click', () => fileInput.click());
dropzone.addEventListener('dragover', (e) => { e.preventDefault(); dropzone.classList.add('dragover'); });
dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));
dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('dragover');
    handleFiles(e.dataTransfer.files);
});
fileInput.addEventListener('change', () => handleFiles(fileInput.files));
function handleFiles(files) {
    const remaining = 12 - selectedFiles.length;
    if (remaining <= 0) { alert('Maximum 12 photos allowed'); return; }
    const filesToAdd = Array.from(files).slice(0, remaining);
    selectedFiles = [...selectedFiles, ...filesToAdd];
    updatePreviews();
    const dataTransfer = new DataTransfer();
    selectedFiles.forEach(f => dataTransfer.items.add(f));
    fileInput.files = dataTransfer.files;
}
function updatePreviews() {
    const existingHtml = existingImages.map((img, index) => 
        '<div class="preview-item'+(img.isPrimary ? ' primary' : '')+'"><img src="'+img.imageUrl+'">'+(img.isPrimary ? '<span class="primary-badge">★</span>' : '<button type="button" class="primary-btn" onclick="setPrimaryImage('+index+')" title="Set as primary">☆</button>')+'<button type="button" onclick="removeExistingImage('+index+')">×</button></div>'
    ).join('');
    const newFilesHtml = selectedFiles.map((file, index) => 
        '<div class="preview-item"><img src="'+URL.createObjectURL(file)+'"><button type="button" onclick="removePreview('+index+')">×</button></div>'
    ).join('');
    previewContainer.innerHTML = existingHtml + newFilesHtml;
}
window.setPrimaryImage = async (index) => {
    const img = existingImages[index];
    const petId = document.getElementById('pet-id').value;
    try {
        await api.setPrimaryImage(petId, img.id);
        existingImages.forEach((img, i) => img.isPrimary = (i === index));
        updatePreviews();
    } catch (err) {
        alert('Failed to set primary storage: ' + err.message);
    }
};
window.removeExistingImage = async (index) => { 
    const img = existingImages[index];
    const petId = document.getElementById('pet-id').value;
    if (!confirm('Delete this storage?')) return;
    try {
        await api.removeImage(petId, img.id);
        existingImages.splice(index, 1);
        updatePreviews();
    } catch (err) {
        alert('Failed to delete storage: ' + err.message);
    }
};
window.removePreview = (index) => { selectedFiles.splice(index, 1); updatePreviews(); const dataTransfer = new DataTransfer(); selectedFiles.forEach(f => dataTransfer.items.add(f)); fileInput.files = dataTransfer.files; };
document.getElementById('pet-form').onsubmit = async (e) => {
    e.preventDefault(); 
    const msg = document.getElementById('message'); 
    const id = document.getElementById('pet-id').value; 
    const rescueDateVal = document.getElementById('rescueDate').value;
    const weight = parseFloat(document.getElementById('weight').value) || 0;
    const ageYears = parseInt(document.getElementById('ageYears').value) || 0;
    const ageMonths = parseInt(document.getElementById('ageMonths').value) || 0;
    const adoptionFee = parseFloat(document.getElementById('adoptionFee').value) || 0;
    if (weight < 0) { msg.className = 'message error'; msg.textContent = 'Weight must be zero or positive'; return; }
    if (ageYears < 0) { msg.className = 'message error'; msg.textContent = 'Age (years) must be zero or positive'; return; }
    if (ageMonths < 0 || ageMonths > 11) { msg.className = 'message error'; msg.textContent = 'Age (months) must be between 0 and 11'; return; }
    if (adoptionFee < 0) { msg.className = 'message error'; msg.textContent = 'Adoption fee must be zero or positive'; return; }
    const data = {
        name: document.getElementById('name').value,
        type: document.getElementById('type').value,
        breed: document.getElementById('breed').value || null,
        description: document.getElementById('description').value,
        weight: weight,
        ageYears: ageYears,
        ageMonths: ageMonths,
        sex: document.getElementById('sex').value,
        color: document.getElementById('color').value || null,
        size: document.getElementById('size').value || null,
        temperament: document.getElementById('temperament').value || null,
        energyLevel: document.getElementById('energyLevel').value || null,
        isSterilized: document.getElementById('isSterilized').checked,
        isMicrochipped: document.getElementById('isMicrochipped').checked,
        microchipId: document.getElementById('microchipId').value || null,
        vaccinations: document.getElementById('vaccinations').value || null,
        isGoodWithKids: document.getElementById('isGoodWithKids').checked,
        isGoodWithDogs: document.getElementById('isGoodWithDogs').checked,
        isGoodWithCats: document.getElementById('isGoodWithCats').checked,
        isHouseTrained: document.getElementById('isHouseTrained').checked,
        rescueLocation: document.getElementById('rescueLocation').value || null,
        rescueDate: rescueDateVal ? new Date(rescueDateVal).getTime() : null,
        specialNeeds: document.getElementById('specialNeeds').value || null,
        adoptionFee: parseFloat(document.getElementById('adoptionFee').value) || 0,
        currency: document.getElementById('currency').value,
        isUrgent: document.getElementById('isUrgent').checked
    }; 
    try { 
        let petId = id;
        if (id) {
            await api.updatePet(id, data); 
        } else {
            const pet = await api.createPet(data);
            petId = pet.id;
        }
        
        const imageFiles = selectedFiles;
        if (imageFiles.length > 0) {
            for (let i = 0; i < imageFiles.length; i++) {
                await api.addImage(petId, imageFiles[i], i === 0);
            }
        }
        
        msg.className = 'message success'; 
        msg.textContent = 'Saved!'; 
        document.getElementById('form-container').style.display = 'none'; 
        load(); 
    } catch (err) { 
        msg.className = 'message error'; 
        msg.textContent = err.message; 
    } 
};
load();
const logoutLink = document.getElementById('logout-link');
if (logoutLink) {
    logoutLink.onclick = async (e) => { e.preventDefault(); await api.logout(); window.location.href = '/'; };
}