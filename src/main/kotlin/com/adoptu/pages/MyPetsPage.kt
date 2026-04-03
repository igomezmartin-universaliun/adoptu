package com.adoptu.pages

import kotlinx.html.*

fun HTML.myPetsPage() {
    commonHead("My Pets - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav() }
        }
        main {
            h1 { attributes["data-i18n"] = "myPets"; +"My Pets" }
            div { id = "message"; +"" }
            div { id = "adoption-requests-section"; style = "margin-bottom: 2rem;"
                h2 { attributes["data-i18n"] = "adoptionRequests"; +"Adoption Requests" }
                div { id = "adoption-requests"; +"" }
            }
            div { id = "form-container"; style = "display:none"
                h2 { id = "form-title"; attributes["data-i18n"] = "addPet"; +"Add Pet" }
                form { id = "pet-form"
                    input(InputType.hidden) { id = "pet-id" }
                    label { htmlFor = "name"; attributes["data-i18n"] = "name"; +"Name *" }; input(InputType.text) { id = "name"; required = true }
                    label { htmlFor = "type"; attributes["data-i18n"] = "type"; +"Type *" }; select { id = "type"; required = true
                        option { value = "DOG"; attributes["data-i18n"] = "dog"; +"Dog" }
                        option { value = "CAT"; attributes["data-i18n"] = "cat"; +"Cat" }
                        option { value = "BIRD"; attributes["data-i18n"] = "bird"; +"Bird" }
                        option { value = "FISH"; attributes["data-i18n"] = "fish"; +"Fish" }
                    }
                    label { htmlFor = "breed"; attributes["data-i18n"] = "breed"; +"Breed" }; input(InputType.text) { id = "breed" }
                    label { htmlFor = "description"; attributes["data-i18n"] = "description"; +"Description" }; textArea { id = "description" }
                    label { htmlFor = "weight"; attributes["data-i18n"] = "weight"; +"Weight (kg)" }; input(InputType.number) { id = "weight"; step = "0.01"; value = "0"; this.min = "0" }
                    label { htmlFor = "ageYears"; attributes["data-i18n"] = "ageYears"; +"Age (years)" }; input(InputType.number) { id = "ageYears"; value = "0"; this.min = "0" }
                    label { htmlFor = "ageMonths"; attributes["data-i18n"] = "ageMonths"; +"Age (months)" }; input(InputType.number) { id = "ageMonths"; value = "0"; this.min = "0"; this.max = "11" }
                    label { htmlFor = "sex"; attributes["data-i18n"] = "sex"; +"Sex" }; select { id = "sex"
                        option { value = "MALE"; attributes["data-i18n"] = "male"; +"Male" }
                        option { value = "FEMALE"; attributes["data-i18n"] = "female"; +"Female" }
                    }
                    label { htmlFor = "color"; attributes["data-i18n"] = "color"; +"Color" }; input(InputType.text) { id = "color" }
                    label { htmlFor = "size"; attributes["data-i18n"] = "size"; +"Size" }; select { id = "size"
                        option { value = ""; attributes["data-i18n"] = "selectSize"; +"Select size" }
                        option { value = "SMALL"; attributes["data-i18n"] = "small"; +"Small" }
                        option { value = "MEDIUM"; attributes["data-i18n"] = "medium"; +"Medium" }
                        option { value = "LARGE"; attributes["data-i18n"] = "large"; +"Large" }
                    }
                    label { htmlFor = "temperament"; attributes["data-i18n"] = "temperament"; +"Temperament" }; input(InputType.text) { id = "temperament" }
                    label { htmlFor = "energyLevel"; attributes["data-i18n"] = "energyLevel"; +"Energy Level" }; select { id = "energyLevel"
                        option { value = ""; attributes["data-i18n"] = "selectEnergy"; +"Select energy" }
                        option { value = "LOW"; attributes["data-i18n"] = "low"; +"Low" }
                        option { value = "MEDIUM"; +"Medium" }
                        option { value = "HIGH"; attributes["data-i18n"] = "high"; +"High" }
                    }
                    label { attributes["data-i18n"] = "medical"; +"Medical" }
                    div { classes = setOf("checkbox-group")
                        input(InputType.checkBox) { id = "isSterilized" }; label { htmlFor = "isSterilized"; attributes["data-i18n"] = "sterilized"; +"Sterilized" }
                        input(InputType.checkBox) { id = "isMicrochipped" }; label { htmlFor = "isMicrochipped"; attributes["data-i18n"] = "microchipped"; +"Microchipped" }
                    }
                    label { htmlFor = "microchipId"; attributes["data-i18n"] = "microchipId"; +"Microchip ID" }; input(InputType.text) { id = "microchipId" }
                    label { htmlFor = "vaccinations"; attributes["data-i18n"] = "vaccinations"; +"Vaccinations" }; textArea { id = "vaccinations" }
                    label { attributes["data-i18n"] = "compatibility"; +"Compatibility" }
                    div { classes = setOf("checkbox-group")
                        input(InputType.checkBox) { id = "isGoodWithKids"; checked = true }; label { htmlFor = "isGoodWithKids"; attributes["data-i18n"] = "goodWithKids"; +"Good with kids" }
                        input(InputType.checkBox) { id = "isGoodWithDogs"; checked = true }; label { htmlFor = "isGoodWithDogs"; attributes["data-i18n"] = "goodWithDogs"; +"Good with dogs" }
                        input(InputType.checkBox) { id = "isGoodWithCats"; checked = true }; label { htmlFor = "isGoodWithCats"; attributes["data-i18n"] = "goodWithCats"; +"Good with cats" }
                        input(InputType.checkBox) { id = "isHouseTrained" }; label { htmlFor = "isHouseTrained"; attributes["data-i18n"] = "houseTrained"; +"House trained" }
                    }
                    label { htmlFor = "rescueLocation"; attributes["data-i18n"] = "rescueLocation"; +"Rescue Location" }; input(InputType.text) { id = "rescueLocation" }
                    label { htmlFor = "rescueDate"; attributes["data-i18n"] = "rescueDate"; +"Rescue Date" }; input(InputType.date) { id = "rescueDate" }
                    label { htmlFor = "specialNeeds"; attributes["data-i18n"] = "specialNeeds"; +"Special Needs" }; textArea { id = "specialNeeds" }
                    label { htmlFor = "adoptionFee"; attributes["data-i18n"] = "adoptionFee"; +"Adoption Fee" }; 
                    div(classes = "fee-input-group") { style = "display: flex; gap: 0.5rem;"
                        input(InputType.number) { id = "adoptionFee"; step = "0.01"; value = "0"; style = "flex: 3;"; this.min = "0" }
                        select { id = "currency"; style = "flex: 1;"
                            option { value = "USD"; +"$ USD" }
                            option { value = "EUR"; +"€ EUR" }
                            option { value = "GBP"; +"£ GBP" }
                            option { value = "CAD"; +"$ CAD" }
                            option { value = "AUD"; +"$ AUD" }
                        }
                    }
                    div { classes = setOf("checkbox-group")
                        input(InputType.checkBox) { id = "isUrgent" }; label { htmlFor = "isUrgent"; attributes["data-i18n"] = "urgentAdoption"; +"Urgent adoption needed" }
                    }
                    label { attributes["data-i18n"] = "photos"; +"Photos (max 12)" }
                    div(classes = "storage-dropzone") {
                        id = "storage-dropzone"
                        div { classes = setOf("dropzone-content"); +"Drop images here or click to browse" }
                        input(InputType.file) { id = "pet-images"; accept = "storage/*"; multiple = true; classes = setOf("file-input") }
                    }
                    div { id = "storage-previews"; classes = setOf("storage-previews") }
                    div(classes = "form-actions") {
                        button(classes = "btn", type = ButtonType.submit) { attributes["data-i18n"] = "save"; +"Save" }
                        button(classes = "btn btn-secondary", type = ButtonType.button) { id = "cancel-btn"; attributes["data-i18n"] = "cancel"; +"Cancel" }
                    }
                }
            }
            button(classes = "btn") { id = "add-btn"; attributes["data-i18n"] = "addPet"; +"Add Pet" }
            div { id = "pets"; classes = setOf("pet-grid"); style = "margin-top: 2rem;"; +"" }
        }
        footer()
        style { unsafe { raw("""
            .storage-dropzone { border: 2px dashed #ccc; border-radius: 8px; padding: 2rem; text-align: center; cursor: pointer; transition: border-color 0.3s; }
            .storage-dropzone:hover, .storage-dropzone.dragover { border-color: #4a90d9; background: #f8f9fa; }
            .storage-dropzone .file-input { display: none; }
            .dropzone-content { color: #666; }
            .storage-previews { display: flex; flex-wrap: wrap; gap: 0.5rem; margin-top: 1rem; }
            .preview-item { position: relative; width: 80px; height: 80px; border-radius: 4px; overflow: hidden; }
            .preview-item img { width: 100%; height: 100%; object-fit: cover; }
            .preview-item.primary { border: 3px solid #4a90d9; }
            .preview-item .primary-badge { position: absolute; top: 2px; left: 2px; background: #4a90d9; color: white; border-radius: 50%; width: 20px; height: 20px; font-size: 14px; display: flex; align-items: center; justify-content: center; }
            .preview-item .primary-btn { position: absolute; top: 2px; left: 2px; background: rgba(0,0,0,0.6); color: white; border: none; border-radius: 50%; width: 20px; height: 20px; cursor: pointer; font-size: 14px; line-height: 1; opacity: 0; transition: opacity 0.2s; }
            .preview-item:hover .primary-btn { opacity: 1; }
            .preview-item button { position: absolute; top: 2px; right: 2px; background: rgba(0,0,0,0.6); color: white; border: none; border-radius: 50%; width: 20px; height: 20px; cursor: pointer; font-size: 14px; line-height: 1; }
            .adoption-request-card { border: 1px solid #ddd; border-radius: 8px; padding: 1rem; margin-bottom: 1rem; background: #1b5e20; color: white; display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 1rem; }
            .adoption-request-card .ar-pet { font-weight: bold; font-size: 1.1rem; margin-bottom: 0.5rem; }
            .adoption-request-card .ar-status { display: inline-block; padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.875rem; font-weight: bold; margin-bottom: 0.5rem; }
            .adoption-request-card .status-PENDING { background: #ffc107; color: #000; }
            .adoption-request-card .status-APPROVED { background: #4caf50; color: #fff; }
            .adoption-request-card .status-REJECTED { background: #f44336; color: #fff; }
            .adoption-request-card .ar-message { margin: 0.5rem 0; color: #e0e0e0; grid-column: 1 / -1; }
            .adoption-request-card .ar-date { font-size: 0.875rem; color: #b0b0b0; }
            .adoption-request-card .ar-actions { margin-top: 0.5rem; display: flex; gap: 0.5rem; grid-column: 1 / -1; }
        """) } }
        commonScripts()
        script { unsafe { raw("""
const emoji = { DOG: '🐕', CAT: '🐱', BIRD: '🐦', FISH: '🐟' };
const params = new URLSearchParams(location.search);
let user;
let existingImages = [];
async function load() {
    user = await api.me();
    if (user.authenticated === false || !user.activeRoles?.includes('RESCUER') && !user.activeRoles?.includes('ADMIN')) { location.href = '/'; return; }
    let pets = await api.getPets(params.get('filter'));
    if (!user.activeRoles?.includes('ADMIN')) pets = pets.filter(p => p.rescuerId === user.id);
    const container = document.getElementById('pets');
    container.innerHTML = pets.length ? pets.map(p => {
        const primaryImage = p.images && p.images.length > 0 ? p.images.find(img => img.isPrimary) || p.images[0] : null;
        const imageHtml = primaryImage 
            ? '<img src="'+primaryImage.imageUrl+'" alt="'+p.name+'">' 
            : '<div class="pet-card-placeholder">'+(emoji[p.type]||'🐾')+'</div>';
        return '<div class="pet-card">'+imageHtml+'<div class="pet-card-body"><span class="pet-type">'+t(p.type.toLowerCase())+'</span><span class="pet-sex '+(p.sex === 'MALE' ? 'male' : 'female')+'">'+t(p.sex.toLowerCase())+'</span>'+(p.size ? '<span class="pet-size">'+p.size+'</span>' : '')+'<div class="pet-name"><h3>'+p.name+(p.isUrgent ? ' ⚠️' : '')+'</h3>'+(p.breed ? '<span class="pet-breed">'+p.breed+'</span>' : '')+'</div><p class="pet-info"><span class="pet-age">'+p.ageYears+t('years')+' '+p.ageMonths+t('months')+' • '+p.weight+' kg</span><span class="pet-rescue-date">'+(p.rescueDate ? ' '+t('rescued')+': '+new Date(p.rescueDate).toLocaleDateString() : '')+'</span></p><p>'+p.status+'</p><div class="pet-card-actions"><a href="/pet/'+p.id+'" class="btn">'+t('viewDetails')+'</a><button class="btn btn-secondary" onclick="edit('+p.id+')">'+t('edit')+'</button><button class="btn btn-secondary" onclick="del('+p.id+')">'+t('delete')+'</button></div></div></div>';
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
    container.innerHTML = allRequests.map(r => '<div class="adoption-request-card"><div class="ar-pet">'+(emoji[r.petType]||'🐾')+' '+r.petName+'</div><div class="ar-status status-'+r.status.toLowerCase()+'">'+r.status+'</div><div class="ar-message">'+(r.message || 'No message')+'</div><div class="ar-date">'+new Date(r.createdAt).toLocaleDateString()+'</div>'+(r.status === 'PENDING' ? '<div class="ar-actions"><button class="btn btn-secondary" onclick="approveRequest('+r.id+')">Approve</button><button class="btn btn-secondary" onclick="rejectRequest('+r.id+')">Reject</button></div>' : '')+'</div>').join('');
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
""".trimIndent()) } }
    }
}
