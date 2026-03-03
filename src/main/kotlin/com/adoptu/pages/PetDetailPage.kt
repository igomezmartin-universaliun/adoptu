package com.adoptu.pages

import kotlinx.html.*

fun HTML.petDetailPage() {
    commonHead("Pet Details - Adopt-U")
    body {
        header {
            a("/") { classes = setOf("logo"); +"Adopt-U" }
            nav {
                a("/pets") { attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
                a("/login") { id = "login-link"; attributes["data-i18n"] = "login"; +"Login" }
                a("/register") { id = "register-link"; attributes["data-i18n"] = "register"; +"Register" }
                a("/my-pets") { id = "my-pets-link"; style = "display:none"; attributes["data-i18n"] = "myPets"; +"My Pets" }
                a("/admin") { id = "admin-link"; style = "display:none"; attributes["data-i18n"] = "admin"; +"Admin" }
                a("#") { id = "logout-link"; style = "display:none"; attributes["data-i18n"] = "logout"; +"Logout" }
                languageDropdown()
            }
        }
        main {
            div { id = "pet-detail"; classes = setOf("pet-detail"); +"" }
            div { id = "message"; +"" }
        }
        commonScripts()
        script { unsafe { raw("""
const emoji = { DOG: '🐕', CAT: '🐱', BIRD: '🐦', FISH: '🐟' };
const id = location.pathname.split('/').pop() || new URLSearchParams(location.search).get('id');
let user;
let currentPet;
async function load() {
    if (!id) { location.href = '/pets'; return; }
    try { user = await api.me(); } catch { user = { authenticated: false }; }
    currentPet = await api.getPet(id);
    const container = document.getElementById('pet-detail');
    let isOwner = (user.role === 'RESCUER' || user.role === 'ADMIN') && currentPet.rescuerId === user.id;
    let adoptForm = currentPet.status === 'AVAILABLE' && user.role === 'ADOPTER' ? '<form id="adopt-form"><label for="msg">Message (optional)</label><textarea id="msg" name="message"></textarea><button type="submit" class="btn">Request Adoption</button></form>' : '';
    let editBtn = isOwner ? '<a href="/my-pets?edit='+currentPet.id+'" class="btn">Edit Pet</a>' : '';
    
    let primaryImage = currentPet.images && currentPet.images.length > 0 
        ? currentPet.images.find(img => img.isPrimary) || currentPet.images[0] 
        : null;
    
    let details = '<div class="pet-detail-header">';
    
    if (primaryImage) {
        details += '<img src="'+primaryImage.imageUrl+'" alt="'+currentPet.name+'" class="pet-main-image">';
    } else {
        details += '<div class="pet-detail-placeholder">'+(emoji[currentPet.type]||'🐾')+'</div>';
    }
    
    details += '<span class="pet-type">'+currentPet.type+'</span><h1>'+currentPet.name+'</h1>';
    if (currentPet.breed) details += '<p class="pet-breed">'+currentPet.breed+'</p>';
    details += '<p><strong>Weight:</strong> '+currentPet.weight+' kg | <strong>Age:</strong> '+currentPet.ageYears+'y '+currentPet.ageMonths+'m | <strong>Sex:</strong> '+currentPet.sex+'</p>';
    details += '<p><strong>Status:</strong> '+currentPet.status+'</p></div>';
    
    details += '<div class="pet-detail-body">';
    
    if (isOwner) {
        details += '<div class="image-management">';
        details += '<h3>Photos</h3>';
        details += '<div class="image-upload-form">';
        details += '<input type="file" id="new-image" accept="image/*">';
        details += '<label><input type="checkbox" id="set-primary"> Set as primary photo</label>';
        details += '<button class="btn" id="upload-btn">Upload Photo</button>';
        details += '</div>';
        details += '<div class="pet-images-grid" id="pet-images">' + renderImages() + '</div>';
        details += '</div>';
    }
    
    details += '<p>'+(currentPet.description||'No description.')+'</p>';
    
    details += '<div class="pet-details-grid">';
    if (currentPet.color) details += '<div class="detail-item"><strong>Color:</strong> '+currentPet.color+'</div>';
    if (currentPet.size) details += '<div class="detail-item"><strong>Size:</strong> '+currentPet.size+'</div>';
    if (currentPet.temperament) details += '<div class="detail-item"><strong>Temperament:</strong> '+currentPet.temperament+'</div>';
    if (currentPet.energyLevel) details += '<div class="detail-item"><strong>Energy:</strong> '+currentPet.energyLevel+'</div>';
    details += '</div>';
    
    details += '<div class="pet-details-grid">';
    details += '<div class="detail-item"><strong>Sterilized:</strong> '+(currentPet.isSterilized ? 'Yes' : 'No')+'</div>';
    details += '<div class="detail-item"><strong>Microchipped:</strong> '+(currentPet.isMicrochipped ? 'Yes' : 'No')+'</div>';
    if (currentPet.microchipId) details += '<div class="detail-item"><strong>Microchip ID:</strong> '+currentPet.microchipId+'</div>';
    details += '</div>';
    
    details += '<div class="pet-details-grid">';
    details += '<div class="detail-item"><strong>Good with kids:</strong> '+(currentPet.isGoodWithKids ? 'Yes' : 'No')+'</div>';
    details += '<div class="detail-item"><strong>Good with dogs:</strong> '+(currentPet.isGoodWithDogs ? 'Yes' : 'No')+'</div>';
    details += '<div class="detail-item"><strong>Good with cats:</strong> '+(currentPet.isGoodWithCats ? 'Yes' : 'No')+'</div>';
    details += '<div class="detail-item"><strong>House trained:</strong> '+(currentPet.isHouseTrained ? 'Yes' : 'No')+'</div>';
    details += '</div>';
    
    if (currentPet.vaccinations) details += '<div class="detail-section"><strong>Vaccinations:</strong><p>'+currentPet.vaccinations+'</p></div>';
    if (currentPet.rescueLocation) details += '<div class="detail-section"><strong>Rescue Location:</strong> '+currentPet.rescueLocation+'</div>';
    if (currentPet.specialNeeds) details += '<div class="detail-section"><strong>Special Needs:</strong><p>'+currentPet.specialNeeds+'</p></div>';
    if (currentPet.adoptionFee > 0) details += '<div class="detail-section"><strong>Adoption Fee:</strong> $'+currentPet.adoptionFee+'</div>';
    if (currentPet.isUrgent) details += '<div class="urgent-badge">URGENT - Needs home soon!</div>';
    
    details += adoptForm + editBtn + '</div>';
    
    container.innerHTML = details;
    
    if (isOwner) {
        document.getElementById('upload-btn').onclick = async () => {
            const fileInput = document.getElementById('new-image');
            const setPrimary = document.getElementById('set-primary').checked;
            const msgEl = document.getElementById('image-msg');
            if (!fileInput.files[0]) return;
            try {
                await api.addImage(currentPet.id, fileInput.files[0], setPrimary);
                currentPet = await api.getPet(currentPet.id);
                document.getElementById('pet-images').innerHTML = renderImages();
                fileInput.value = '';
                document.getElementById('image-msg').className = 'message success';
                document.getElementById('image-msg').textContent = 'Image uploaded!';
            } catch (err) {
                document.getElementById('image-msg').className = 'message error';
                document.getElementById('image-msg').textContent = err.message;
            }
        };
    }
    
    const form = document.getElementById('adopt-form');
    if (form) form.onsubmit = async (e) => { e.preventDefault(); if (!user.id) { location.href = '/login'; return; } try { await api.adoptPet(id, document.getElementById('msg').value); document.getElementById('message').className = 'message success'; document.getElementById('message').textContent = 'Adoption request submitted!'; form.style.display = 'none'; } catch (err) { document.getElementById('message').className = 'message error'; document.getElementById('message').textContent = err.message; } };
}
function renderImages() {
    if (!currentPet.images || currentPet.images.length === 0) return '<p>No photos yet.</p>';
    return currentPet.images.map(img => '<div class="pet-image-item'+(img.isPrimary ? ' primary' : '')+'"><img src="'+img.imageUrl+'" alt="Pet photo"><div class="pet-image-actions">'+(img.isPrimary ? '<span class="primary-badge">Primary</span>' : '<button class="btn btn-small" onclick="setPrimary('+img.id+')">Set Primary</button>')+'<button class="btn btn-small btn-danger" onclick="removeImage('+img.id+')">Remove</button></div></div>').join('');
}
window.setPrimary = async (imageId) => { await api.setPrimaryImage(currentPet.id, imageId); currentPet = await api.getPet(currentPet.id); document.getElementById('pet-images').innerHTML = renderImages(); };
window.removeImage = async (imageId) => { if (!confirm('Remove this photo?')) return; await api.removeImage(currentPet.id, imageId); currentPet = await api.getPet(currentPet.id); document.getElementById('pet-images').innerHTML = renderImages(); };
load().catch(() => location.href = '/pets');
(async () => { try { const u = await api.me(); if (u.authenticated !== false) { document.getElementById('login-link').style.display = 'none'; document.getElementById('register-link').style.display = 'none'; if (u.role === 'RESCUER' || u.role === 'ADMIN') document.getElementById('my-pets-link').style.display = 'inline'; document.getElementById('logout-link').style.display = 'inline'; if (u.role === 'ADMIN') document.getElementById('admin-link').style.display = 'inline'; document.getElementById('logout-link').onclick = async (e) => { e.preventDefault(); await api.logout(); location.reload(); }; } } catch (e) {} })();
""".trimIndent()) } }
    }
}
