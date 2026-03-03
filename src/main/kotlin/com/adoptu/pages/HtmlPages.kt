package com.adoptu.pages

import kotlinx.html.*

fun HTML.indexPage() {
    head {
        meta(charset = "UTF-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        title { +"Adopt-U - Pet Adoption" }
        link(rel = "stylesheet", href = "/static/css/style.css")
    }
    body {
        header {
            a("/") { classes = setOf("logo"); +"Adopt-U" }
            nav {
                div(classes = "lang-dropdown") {
                    button(classes = "lang-dropbtn", type = ButtonType.button) { +"🇺🇸" }
                    div(classes = "lang-dropdown-content") {
                        a(classes = "lang-option") { attributes["data-lang"] = "en"; +"🇺🇸" }
                        a(classes = "lang-option") { attributes["data-lang"] = "es"; +"🇪🇸" }
                    }
                }
                a("/pets") { id = "nav-browse"; attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
                a("/login") { id = "nav-login"; attributes["data-i18n"] = "login"; +"Login" }
                a("/register") { id = "nav-register"; attributes["data-i18n"] = "register"; +"Register" }
                a("/my-pets") { id = "nav-mypets"; style = "display:none"; attributes["data-i18n"] = "myPets"; +"My Pets" }
                a("/admin") { id = "nav-admin"; style = "display:none"; attributes["data-i18n"] = "admin"; +"Admin" }
                a("#") { id = "nav-logout"; style = "display:none"; attributes["data-i18n"] = "logout"; +"Logout" }
            }
        }
        main {
            section(classes = "hero") {
                h1 { attributes["data-i18n"] = "findYourNewBestFriend"; +"Find Your New Best Friend" }
                p { attributes["data-i18n"] = "dogsCatsBirdsFish"; +"Dogs, cats, birds, fish - all waiting for a loving home." }
                a("/pets") { classes = setOf("btn"); attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
            }
        }
        script(src = "/static/js/api.js") {}
        script(src = "/static/js/i18n.js") {}
        script {
            unsafe {
                raw("""
                document.querySelectorAll('.lang-option').forEach(opt => {
    opt.onclick = () => { i18n.setLang(opt.dataset.lang); document.querySelector('.lang-dropdown-content').style.display = 'none'; };
});
document.querySelector('.lang-dropbtn').onclick = () => {
    const content = document.querySelector('.lang-dropdown-content');
    content.style.display = content.style.display === 'block' ? 'none' : 'block';
};
document.addEventListener('click', (e) => {
    if (!e.target.closest('.lang-dropdown')) {
        document.querySelector('.lang-dropdown-content').style.display = 'none';
    }
});
i18n.updatePage();
                (async () => {
                    const user = await api.me();
                    if (user.authenticated !== false) {
                        document.getElementById('nav-login').style.display = 'none';
                        document.getElementById('nav-register').style.display = 'none';
                        document.getElementById('nav-mypets').style.display = 'inline';
                        document.getElementById('nav-logout').style.display = 'inline';
                        if (user.role === 'ADMIN') document.getElementById('nav-admin').style.display = 'inline';
                        document.getElementById('nav-logout').onclick = async (e) => { e.preventDefault(); await api.logout(); location.reload(); };
                    }
                })();
                """.trimIndent())
            }
        }
    }
}

fun HTML.loginPage() {
    head {
        meta(charset = "UTF-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        title { +"Login - Adopt-U" }
        link(rel = "stylesheet", href = "/static/css/style.css")
    }
    body {
        header {
            a("/") { classes = setOf("logo"); +"Adopt-U" }
            nav {
                a("/pets") { attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
                a("/register") { attributes["data-i18n"] = "register"; +"Register" }
                div(classes = "lang-dropdown") {
                    button(classes = "lang-dropbtn", type = ButtonType.button) { +"🇺🇸" }
                    div(classes = "lang-dropdown-content") {
                        a(classes = "lang-option") { attributes["data-lang"] = "en"; +"🇺🇸" }
                        a(classes = "lang-option") { attributes["data-lang"] = "es"; +"🇪🇸" }
                    }
                }
            }
        }
        main {
            div { classes = setOf("auth-form")
                h1 { attributes["data-i18n"] = "loginWithPasskey"; +"Login with Passkey" }
                p { id = "message"; +"" }
                button(classes = "btn", type = ButtonType.button) { id = "login-btn"; attributes["data-i18n"] = "signInWithPasskey"; +"Sign in with Passkey" }
                p { style = "margin-top: 1rem;"; small { attributes["data-i18n"] = "usesFido"; +"Uses FIDO2 / WebAuthn for secure passwordless authentication." } }
            }
        }
        script(src = "/static/js/api.js") {}
        script(src = "/static/js/i18n.js") {}
        script(src = "/static/js/webauthn.js") {}
        script { unsafe { raw("""
document.querySelectorAll('.lang-option').forEach(opt => {
    opt.onclick = () => { i18n.setLang(opt.dataset.lang); document.querySelector('.lang-dropdown-content').style.display = 'none'; };
});
document.querySelector('.lang-dropbtn').onclick = () => {
    const content = document.querySelector('.lang-dropdown-content');
    content.style.display = content.style.display === 'block' ? 'none' : 'block';
};
document.addEventListener('click', (e) => {
    if (!e.target.closest('.lang-dropdown')) {
        document.querySelector('.lang-dropdown-content').style.display = 'none';
    }
});
i18n.updatePage();
""") } }
        script { unsafe { raw(loginScript) } }
    }
}

private val loginScript = """
document.getElementById('login-btn').onclick = async () => {
    const msg = document.getElementById('message');
    try {
        msg.textContent = 'Authenticating...';
        const ok = await webauthn.authenticate();
        if (ok) { msg.className = 'message success'; msg.textContent = 'Success! Redirecting...'; location.href = '/'; }
        else { msg.className = 'message error'; msg.textContent = 'Authentication failed.'; }
    } catch (e) { msg.className = 'message error'; msg.textContent = e.message || 'Authentication error.'; }
};
""".trimIndent()

fun HTML.registerPage() {
    head {
        meta(charset = "UTF-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        title { +"Register - Adopt-U" }
        link(rel = "stylesheet", href = "/static/css/style.css")
    }
    body {
        header {
            a("/") { classes = setOf("logo"); +"Adopt-U" }
            nav {
                a("/pets") { attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
                a("/login") { attributes["data-i18n"] = "login"; +"Login" }
                div(classes = "lang-dropdown") {
                    button(classes = "lang-dropbtn", type = ButtonType.button) { +"🇺🇸" }
                    div(classes = "lang-dropdown-content") {
                        a(classes = "lang-option") { attributes["data-lang"] = "en"; +"🇺🇸" }
                        a(classes = "lang-option") { attributes["data-lang"] = "es"; +"🇪🇸" }
                    }
                }
            }
        }
        main {
            div { classes = setOf("auth-form")
                h1 { attributes["data-i18n"] = "registerNewAccount"; +"Create Account" }
                form { id = "register-form"
                    label { htmlFor = "username"; attributes["data-i18n"] = "username"; +"Username" }; input(InputType.text) { name = "username"; id = "username"; required = true }
                    label { htmlFor = "displayName"; attributes["data-i18n"] = "displayName"; +"Display Name" }; input(InputType.text) { name = "displayName"; id = "displayName"; required = true }
                    label { htmlFor = "role"; +"I want to" }
                    select { id = "role"; name = "role"
                        option { value = "ADOPTER"; attributes["data-i18n"] = "adoptPet"; +"Adopt a pet" }
                        option { value = "RESCUER"; attributes["data-i18n"] = "publishPets"; +"Publish pets for adoption" }
                    }
                    p { id = "message"; +"" }
                    button(classes = "btn", type = ButtonType.submit) { +"Register with Passkey" }
                }
                p { style = "margin-top: 1rem;"; small { attributes["data-i18n"] = "useFido"; +"Uses FIDO2 / WebAuthn - no password needed." } }
                p { style = "margin-top: 1rem;"; +""; attributes["data-i18n"] = "alreadyHaveAccount"; +"Already have an account? "; a("/login") { attributes["data-i18n"] = "login"; +"Login" } }
            }
        }
        script(src = "/static/js/api.js") {}
        script(src = "/static/js/i18n.js") {}
        script(src = "/static/js/webauthn.js") {}
        script { unsafe { raw("""
document.querySelectorAll('.lang-option').forEach(opt => {
    opt.onclick = () => { i18n.setLang(opt.dataset.lang); document.querySelector('.lang-dropdown-content').style.display = 'none'; };
});
document.querySelector('.lang-dropbtn').onclick = () => {
    const content = document.querySelector('.lang-dropdown-content');
    content.style.display = content.style.display === 'block' ? 'none' : 'block';
};
document.addEventListener('click', (e) => {
    if (!e.target.closest('.lang-dropdown')) {
        document.querySelector('.lang-dropdown-content').style.display = 'none';
    }
});
i18n.updatePage();
""") } }
        script { unsafe { raw(registerScript) } }
    }
}

private val registerScript = """
document.getElementById('register-form').onsubmit = async (e) => {
    e.preventDefault();
    const username = document.getElementById('username').value.trim();
    const displayName = document.getElementById('displayName').value.trim();
    const role = document.getElementById('role').value;
    const msg = document.getElementById('message');
    if (!username || !displayName) { msg.className = 'message error'; msg.textContent = 'Please fill in all fields.'; return; }
    try {
        msg.textContent = 'Creating passkey...';
        const ok = await webauthn.register(username, displayName, role);
        if (ok) { msg.className = 'message success'; msg.textContent = 'Success! Redirecting...'; location.href = '/'; }
        else { msg.className = 'message error'; msg.textContent = 'Registration failed.'; }
    } catch (err) { msg.className = 'message error'; msg.textContent = err.message || 'Registration error.'; }
};
""".trimIndent()

fun HTML.petsPage() {
    head {
        meta(charset = "UTF-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        title { +"Browse Pets - Adopt-U" }
        link(rel = "stylesheet", href = "/static/css/style.css")
    }
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
                div(classes = "lang-dropdown") {
                    button(classes = "lang-dropbtn", type = ButtonType.button) { +"🇺🇸" }
                    div(classes = "lang-dropdown-content") {
                        a(classes = "lang-option") { attributes["data-lang"] = "en"; +"🇺🇸" }
                        a(classes = "lang-option") { attributes["data-lang"] = "es"; +"🇪🇸" }
                    }
                }
            }
        }
        main {
            h1 { attributes["data-i18n"] = "petsForAdoption"; +"Pets for Adoption" }
            div(classes = "filter-buttons") {
                button(classes = "filter-btn active", type = ButtonType.button) { attributes["data-type"] = ""; attributes["data-i18n"] = "all"; +"All" }
                button(classes = "filter-btn", type = ButtonType.button) { attributes["data-type"] = "DOG"; +"🐕 Dogs" }
                button(classes = "filter-btn", type = ButtonType.button) { attributes["data-type"] = "CAT"; +"🐱 Cats" }
                button(classes = "filter-btn", type = ButtonType.button) { attributes["data-type"] = "BIRD"; +"🐦 Birds" }
                button(classes = "filter-btn", type = ButtonType.button) { attributes["data-type"] = "FISH"; +"🐟 Fish" }
            }
            div(classes = "filter-buttons") {
                select(classes = "filter-sex") {
                    option { value = ""; +"All Sex" }
                    option { value = "MALE"; +"♂ Male" }
                    option { value = "FEMALE"; +"♀ Female" }
                }
            }
            div { id = "pets"; classes = setOf("pet-grid"); +"" }
        }
        script(src = "/static/js/api.js") {}
        script(src = "/static/js/i18n.js") {}
        script { unsafe { raw("""
document.querySelectorAll('.lang-option').forEach(opt => {
    opt.onclick = () => { i18n.setLang(opt.dataset.lang); document.querySelector('.lang-dropdown-content').style.display = 'none'; };
});
document.querySelector('.lang-dropbtn').onclick = () => {
    const content = document.querySelector('.lang-dropdown-content');
    content.style.display = content.style.display === 'block' ? 'none' : 'block';
};
document.addEventListener('click', (e) => {
    if (!e.target.closest('.lang-dropdown')) {
        document.querySelector('.lang-dropdown-content').style.display = 'none';
    }
});
i18n.updatePage();
""") } }
        script { unsafe { raw(petsScript) } }
    }
}

private val petsScript = """
const emoji = { DOG: '🐕', CAT: '🐱', BIRD: '🐦', FISH: '🐟' };
let currentType = '';
let currentSex = '';
async function loadPets() {
    console.log('Loading pets, type:', currentType, 'sex:', currentSex);
    const pets = await api.getPets(currentType || undefined);
    console.log('Got pets:', pets.length);
    const filteredPets = pets.filter(p => !currentSex || p.sex === currentSex);
    console.log('Filtered pets:', filteredPets.length);
    const container = document.getElementById('pets');
    container.innerHTML = filteredPets.length ? filteredPets.map(p => {
        const primaryImage = p.images && p.images.length > 0 ? p.images.find(img => img.isPrimary) || p.images[0] : null;
        const imageHtml = primaryImage 
            ? '<img src="'+primaryImage.imageUrl+'" alt="'+p.name+'">' 
            : '<div class="pet-card-placeholder">'+(emoji[p.type]||'🐾')+'</div>';
        return '<a href="/pet/'+p.id+'" class="pet-card">'+imageHtml+'<div class="pet-card-body"><span class="pet-type">'+p.type+'</span><span class="pet-sex '+(p.sex === 'MALE' ? 'male' : 'female')+'">'+p.sex+'</span>'+(p.size ? '<span class="pet-size">'+p.size+'</span>' : '')+'<h3>'+p.name+(p.isUrgent ? ' ⚠️' : '')+'</h3><p class="pet-info"><span class="pet-age"><span class="label">'+t('age')+'</span><span class="value">'+(p.breed ? p.breed + ' • ' : '')+p.ageYears+t('years')+' '+p.ageMonths+t('months')+'</span></span><span class="pet-rescue-date">'+(p.rescueDate ? '<span class="label">'+t('rescued')+'</span><span class="value">'+new Date(p.rescueDate).toLocaleDateString()+'</span>' : '')+'</span></p><p>'+p.status+'</p></div></a>';
    }).join('') : '<p>'+t('noPetsFound')+'</p>';
}
document.querySelectorAll('.filter-btn').forEach(btn => {
    btn.onclick = () => {
        console.log('Filter button clicked, type:', btn.dataset.type);
        document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        currentType = btn.dataset.type;
        loadPets();
    };
});
const sexFilter = document.querySelector('.filter-sex');
if (sexFilter) {
    sexFilter.onchange = () => {
        console.log('Sex filter changed:', sexFilter.value);
        currentSex = sexFilter.value;
        loadPets();
    };
}
loadPets();
i18n.updatePage();
(async () => {
    try {
        const user = await api.me();
        if (user.authenticated !== false) {
            document.getElementById('login-link').style.display = 'none';
            document.getElementById('register-link').style.display = 'none';
            if (user.role === 'RESCUER' || user.role === 'ADMIN') document.getElementById('my-pets-link').style.display = 'inline';
            document.getElementById('logout-link').style.display = 'inline';
            if (user.role === 'ADMIN') document.getElementById('admin-link').style.display = 'inline';
            document.getElementById('logout-link').onclick = async (e) => { e.preventDefault(); await api.logout(); location.reload(); };
        }
    } catch (e) {}
})();
""".trimIndent()

fun HTML.petDetailPage() {
    head {
        meta(charset = "UTF-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        title { +"Pet Details - Adopt-U" }
        link(rel = "stylesheet", href = "/static/css/style.css")
    }
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
                div(classes = "lang-dropdown") {
                    button(classes = "lang-dropbtn", type = ButtonType.button) { +"🇺🇸" }
                    div(classes = "lang-dropdown-content") {
                        a(classes = "lang-option") { attributes["data-lang"] = "en"; +"🇺🇸" }
                        a(classes = "lang-option") { attributes["data-lang"] = "es"; +"🇪🇸" }
                    }
                }
            }
        }
        main {
            div { id = "pet-detail"; classes = setOf("pet-detail"); +"" }
            div { id = "message"; +"" }
        }
        script(src = "/static/js/api.js") {}
        script(src = "/static/js/i18n.js") {}
        script { unsafe { raw("""
document.querySelectorAll('.lang-option').forEach(opt => {
    opt.onclick = () => { i18n.setLang(opt.dataset.lang); document.querySelector('.lang-dropdown-content').style.display = 'none'; };
});
document.querySelector('.lang-dropbtn').onclick = () => {
    const content = document.querySelector('.lang-dropdown-content');
    content.style.display = content.style.display === 'block' ? 'none' : 'block';
};
document.addEventListener('click', (e) => {
    if (!e.target.closest('.lang-dropdown')) {
        document.querySelector('.lang-dropdown-content').style.display = 'none';
    }
});
i18n.updatePage();
""") } }
        script { unsafe { raw(petDetailScript) } }
    }
}

private val petDetailScript = """
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
""".trimIndent()

fun HTML.myPetsPage() {
    head {
        meta(charset = "UTF-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        title { +"My Pets - Adopt-U" }
        link(rel = "stylesheet", href = "/static/css/style.css")
    }
    body {
        header {
            a("/") { classes = setOf("logo"); +"Adopt-U" }
            nav {
                a("/pets") { attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
                a("/my-pets") { attributes["data-i18n"] = "myPets"; +"My Pets" }
                a("#") { id = "logout-link"; attributes["data-i18n"] = "logout"; +"Logout" }
                div(classes = "lang-dropdown") {
                    button(classes = "lang-dropbtn", type = ButtonType.button) { +"🇺🇸" }
                    div(classes = "lang-dropdown-content") {
                        a(classes = "lang-option") { attributes["data-lang"] = "en"; +"🇺🇸" }
                        a(classes = "lang-option") { attributes["data-lang"] = "es"; +"🇪🇸" }
                    }
                }
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
        script(src = "/static/js/api.js") {}
        script(src = "/static/js/i18n.js") {}
        script { unsafe { raw("""
document.querySelectorAll('.lang-option').forEach(opt => {
    opt.onclick = () => { i18n.setLang(opt.dataset.lang); document.querySelector('.lang-dropdown-content').style.display = 'none'; };
});
document.querySelector('.lang-dropbtn').onclick = () => {
    const content = document.querySelector('.lang-dropdown-content');
    content.style.display = content.style.display === 'block' ? 'none' : 'block';
};
document.addEventListener('click', (e) => {
    if (!e.target.closest('.lang-dropdown')) {
        document.querySelector('.lang-dropdown-content').style.display = 'none';
    }
});
i18n.updatePage();
""") } }
        script { unsafe { raw(myPetsScript) } }
    }
}

private val myPetsScript = """
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
""".trimIndent()

fun HTML.adminPage() {
    head {
        meta(charset = "UTF-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        title { +"Admin - Adopt-U" }
        link(rel = "stylesheet", href = "/static/css/style.css")
    }
    body {
        header {
            a("/") { classes = setOf("logo"); +"Adopt-U" }
            nav {
                a("/pets") { attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
                a("/my-pets") { attributes["data-i18n"] = "myPets"; +"My Pets" }
                a("/admin") { attributes["data-i18n"] = "admin"; +"Admin" }
                a("#") { id = "logout-link"; attributes["data-i18n"] = "logout"; +"Logout" }
                div(classes = "lang-dropdown") {
                    button(classes = "lang-dropbtn", type = ButtonType.button) { +"🇺🇸" }
                    div(classes = "lang-dropdown-content") {
                        a(classes = "lang-option") { attributes["data-lang"] = "en"; +"🇺🇸" }
                        a(classes = "lang-option") { attributes["data-lang"] = "es"; +"🇪🇸" }
                    }
                }
            }
        }
        main {
            h1 { attributes["data-i18n"] = "adminPanel"; +"Admin Panel" }
            p { attributes["data-i18n"] = "managePets"; +"Manage all pet pages. Add or remove pets." }
            div { id = "message"; +"" }
            a("/my-pets") { classes = setOf("btn"); attributes["data-i18n"] = "managePetsBtn"; +"Manage Pets" }
            div { id = "pets"; classes = setOf("pet-grid"); style = "margin-top: 2rem;"; +"" }
        }
        script(src = "/static/js/api.js") {}
        script(src = "/static/js/i18n.js") {}
        script { unsafe { raw("""
document.querySelectorAll('.lang-option').forEach(opt => {
    opt.onclick = () => { i18n.setLang(opt.dataset.lang); document.querySelector('.lang-dropdown-content').style.display = 'none'; };
});
document.querySelector('.lang-dropbtn').onclick = () => {
    const content = document.querySelector('.lang-dropdown-content');
    content.style.display = content.style.display === 'block' ? 'none' : 'block';
};
document.addEventListener('click', (e) => {
    if (!e.target.closest('.lang-dropdown')) {
        document.querySelector('.lang-dropdown-content').style.display = 'none';
    }
});
i18n.updatePage();
""") } }
        script { unsafe { raw(adminScript) } }
    }
}

private val adminScript = """
const emoji = { DOG: '🐕', CAT: '🐱', BIRD: '🐦', FISH: '🐟' };
async function load() {
    const user = await api.me();
    if (user.authenticated === false || user.role !== 'ADMIN') { location.href = '/'; return; }
    const pets = await api.getPets();
    const container = document.getElementById('pets');
    container.innerHTML = pets.length ? pets.map(p => '<div class="pet-card"><div class="pet-card-placeholder">'+(emoji[p.type]||'🐾')+'</div><div class="pet-card-body"><span class="pet-type">'+p.type+'</span><span class="pet-sex '+(p.sex === 'MALE' ? 'male' : 'female')+'">'+p.sex+'</span>'+(p.size ? '<span class="pet-size">'+p.size+'</span>' : '')+'<h3>'+p.name+(p.isUrgent ? ' ⚠️' : '')+'</h3><p class="pet-info"><span class="pet-age">'+p.ageYears+t('years')+' '+p.ageMonths+t('months')+' • '+p.weight+' kg</span><span class="pet-rescue-date">'+(p.rescueDate ? ' '+t('rescued')+': '+new Date(p.rescueDate).toLocaleDateString() : '')+'</span></p><p>'+p.status+'</p><div class="pet-card-actions"><a href="/pet/'+p.id+'" class="btn">'+t('viewDetails')+'</a><a href="/my-pets?edit='+p.id+'" class="btn btn-secondary">'+t('edit')+'</a><button class="btn btn-secondary" onclick="del('+p.id+')">'+t('delete')+'</button></div></div></div>').join('') : '<p>'+t('noPets')+'</p>';
}
window.del = async (id) => { if (!confirm('Delete this pet?')) return; await api.deletePet(id); load(); };
load();
document.getElementById('logout-link').onclick = async (e) => { e.preventDefault(); await api.logout(); location.reload(); };
""".trimIndent()
