package com.adoptu.pages

import kotlinx.html.*

fun HTML.indexPage() {
    commonHead("Browse Pets - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav() }
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
        footer()
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
        return '<a href="/pet/'+p.id+'" class="pet-card">'+imageHtml+'<div class="pet-card-body"><span class="pet-type">'+t(p.type.toLowerCase())+'</span><span class="pet-sex '+(p.sex === 'MALE' ? 'male' : 'female')+'">'+t(p.sex.toLowerCase())+'</span>'+(p.size ? '<span class="pet-size">'+p.size+'</span>' : '')+'<div class="pet-name"><h3>'+p.name+(p.isUrgent ? ' ⚠️' : '')+'</h3>'+(p.breed ? '<span class="pet-breed">'+p.breed+'</span>' : '')+'</div><p class="pet-info"><span class="pet-age"><span class="label">'+t('age')+'</span><span class="value">'+p.ageYears+t('years')+' '+p.ageMonths+t('months')+'</span></span><span class="pet-rescue-date">'+(p.rescueDate ? '<span class="label">'+t('rescued')+'</span><span class="value">'+new Date(p.rescueDate).toLocaleDateString()+'</span>' : '')+'</span></p><p>'+p.status+'</p></div></a>';
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
""".trimIndent()) } }
    }
}
