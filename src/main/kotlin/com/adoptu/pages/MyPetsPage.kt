package com.adoptu.pages

import kotlinx.html.*

fun HTML.myPetsPage() {
    commonHead("My Pets - Adopt-U")
    body {
        header {
            a("/") { classes = setOf("logo"); +"Adopt-U" }
            nav {
                a("/pets") { attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
                a("/my-pets") { attributes["data-i18n"] = "myPets"; +"My Pets" }
                a("#") { id = "logout-link"; attributes["data-i18n"] = "logout"; +"Logout" }
                languageDropdown()
            }
        }
        main {
            h1 { attributes["data-i18n"] = "myPets"; +"My Pets" }
            div { id = "message"; +"" }
            div { id = "form-container"; style = "display:none"
                h2 { id = "form-title"; attributes["data-i18n"] = "addPet"; +"Add Pet" }
                form { id = "pet-form"
                    input(InputType.hidden) { id = "pet-id" }
                    label { htmlFor = "name"; attributes["data-i18n"] = "name"; +"Name *" }; input(InputType.text) { id = "name"; required = true }
                    label { htmlFor = "type"; attributes["data-i18n"] = "type"; +"Type *" }; select { id = "type"; required = true
                        option { value = "DOG"; +"Dog" }
                        option { value = "CAT"; +"Cat" }
                        option { value = "BIRD"; +"Bird" }
                        option { value = "FISH"; +"Fish" }
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
                    label { htmlFor = "adoptionFee"; attributes["data-i18n"] = "adoptionFee"; +"Adoption Fee ($)" }; input(InputType.number) { id = "adoptionFee"; step = "0.01"; value = "0" }
                    div { classes = setOf("checkbox-group")
                        input(InputType.checkBox) { id = "isUrgent" }; label { htmlFor = "isUrgent"; attributes["data-i18n"] = "urgentAdoption"; +"Urgent adoption needed" }
                    }
                    label { attributes["data-i18n"] = "photos"; +"Photos" }
                    div(classes = "image-upload-form") {
                        input(InputType.file) { id = "pet-images"; accept = "image/*"; multiple = true }
                    }
                    div(classes = "form-actions") {
                        button(classes = "btn", type = ButtonType.submit) { attributes["data-i18n"] = "save"; +"Save" }
                        button(classes = "btn btn-secondary", type = ButtonType.button) { id = "cancel-btn"; attributes["data-i18n"] = "cancel"; +"Cancel" }
                    }
                }
            }
            button(classes = "btn") { id = "add-btn"; attributes["data-i18n"] = "addPet"; +"Add Pet" }
            div { id = "pets"; classes = setOf("pet-grid"); style = "margin-top: 2rem;"; +"" }
        }
        commonScripts()
        script { unsafe { raw("""
const emoji = { DOG: '🐕', CAT: '🐱', BIRD: '🐦', FISH: '🐟' };
const params = new URLSearchParams(location.search);
let user;
async function load() {
    user = await api.me();
    if (user.authenticated === false || !['RESCUER','ADMIN'].includes(user.role)) { location.href = '/'; return; }
    let pets = await api.getPets(params.get('filter'));
    if (user.role !== 'ADMIN') pets = pets.filter(p => p.rescuerId === user.id);
    const container = document.getElementById('pets');
    container.innerHTML = pets.length ? pets.map(p => '<div class="pet-card"><div class="pet-card-placeholder">'+(emoji[p.type]||'🐾')+'</div><div class="pet-card-body"><span class="pet-type">'+p.type+'</span><span class="pet-sex '+(p.sex === 'MALE' ? 'male' : 'female')+'">'+p.sex+'</span>'+(p.size ? '<span class="pet-size">'+p.size+'</span>' : '')+'<h3>'+p.name+(p.isUrgent ? ' ⚠️' : '')+'</h3><p class="pet-info"><span class="pet-age">'+p.breed+' • '+p.ageYears+t('years')+' '+p.ageMonths+t('months')+' • '+p.weight+' kg</span><span class="pet-rescue-date">'+(p.rescueDate ? ' '+t('rescued')+': '+new Date(p.rescueDate).toLocaleDateString() : '')+'</span></p><p>'+p.status+'</p><div class="pet-card-actions"><a href="/pet/'+p.id+'" class="btn">'+t('viewDetails')+'</a><button class="btn btn-secondary" onclick="edit('+p.id+')">'+t('edit')+'</button><button class="btn btn-secondary" onclick="del('+p.id+')">'+t('delete')+'</button></div></div></div>').join('') : '<p>'+t('noPets')+'</p>';
    const editId = params.get('edit');
    if (editId) { 
    const pet = await api.getPet(editId); 
    fillForm(pet);
    document.getElementById('form-title').textContent = 'Edit Pet'; 
    document.getElementById('form-container').style.display = 'block'; }
}
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
    document.getElementById('isUrgent').checked = pet.isUrgent || false;
}
window.edit = async (id) => { const pet = await api.getPet(id); fillForm(pet); document.getElementById('form-title').textContent = 'Edit Pet'; document.getElementById('form-container').style.display = 'block'; };
window.del = async (id) => { if (!confirm('Delete this pet?')) return; await api.deletePet(id); load(); };
document.getElementById('add-btn').onclick = () => { document.getElementById('pet-form').reset(); document.getElementById('pet-id').value = ''; document.getElementById('form-title').textContent = 'Add Pet'; document.getElementById('form-container').style.display = 'block'; };
document.getElementById('cancel-btn').onclick = () => { document.getElementById('form-container').style.display = 'none'; history.replaceState({}, '', '/my-pets'); };
['weight', 'ageYears', 'ageMonths'].forEach(id => {
    const el = document.getElementById(id);
    el.addEventListener('input', () => { if (parseFloat(el.value) < 0) el.value = 0; });
    el.addEventListener('blur', () => { if (parseFloat(el.value) < 0) el.value = 0; if (id === 'ageMonths' && parseInt(el.value) > 11) el.value = 11; });
});
document.getElementById('pet-form').onsubmit = async (e) => {
    e.preventDefault(); 
    const msg = document.getElementById('message'); 
    const id = document.getElementById('pet-id').value; 
    const rescueDateVal = document.getElementById('rescueDate').value;
    const weight = parseFloat(document.getElementById('weight').value) || 0;
    const ageYears = parseInt(document.getElementById('ageYears').value) || 0;
    const ageMonths = parseInt(document.getElementById('ageMonths').value) || 0;
    if (weight < 0) { msg.className = 'message error'; msg.textContent = 'Weight must be zero or positive'; return; }
    if (ageYears < 0) { msg.className = 'message error'; msg.textContent = 'Age (years) must be zero or positive'; return; }
    if (ageMonths < 0 || ageMonths > 11) { msg.className = 'message error'; msg.textContent = 'Age (months) must be between 0 and 11'; return; }
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
        
        const imageFiles = document.getElementById('pet-images').files;
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
document.getElementById('logout-link').onclick = async (e) => { e.preventDefault(); await api.logout(); location.reload(); };
""".trimIndent()) } }
    }
}
