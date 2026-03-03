package com.adoptu.pages

import kotlinx.html.*

fun HTML.petsPage() {
    commonHead("Browse Pets - Adopt-U")
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
        commonScripts()
        script { unsafe { raw("""
const emoji = { DOG: '🐕', CAT: '🐱', BIRD: '🐦', FISH: '🐟' };
let currentType = '';
let currentSex = '';
async function loadPets() {
    const pets = await api.getPets(currentType || undefined);
    const filteredPets = pets.filter(p => !currentSex || p.sex === currentSex);
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
        document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        currentType = btn.dataset.type;
        loadPets();
    };
});
const sexFilter = document.querySelector('.filter-sex');
if (sexFilter) {
    sexFilter.onchange = () => {
        currentSex = sexFilter.value;
        loadPets();
    };
}
loadPets();
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
""".trimIndent()) } }
    }
}
