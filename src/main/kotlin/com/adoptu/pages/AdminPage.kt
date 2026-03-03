package com.adoptu.pages

import kotlinx.html.*

fun HTML.adminPage() {
    commonHead("Admin - Adopt-U")
    body {
        header {
            a("/") { classes = setOf("logo"); +"Adopt-U" }
            nav {
                a("/pets") { attributes["data-i18n"] = "browsePets"; +"Browse Pets" }
                a("/my-pets") { attributes["data-i18n"] = "myPets"; +"My Pets" }
                a("/admin") { attributes["data-i18n"] = "admin"; +"Admin" }
                a("#") { id = "logout-link"; attributes["data-i18n"] = "logout"; +"Logout" }
                languageDropdown()
            }
        }
        main {
            h1 { attributes["data-i18n"] = "adminPanel"; +"Admin Panel" }
            p { attributes["data-i18n"] = "managePets"; +"Manage all pet pages. Add or remove pets." }
            div { id = "message"; +"" }
            a("/my-pets") { classes = setOf("btn"); attributes["data-i18n"] = "managePetsBtn"; +"Manage Pets" }
            div { id = "pets"; classes = setOf("pet-grid"); style = "margin-top: 2rem;"; +"" }
        }
        commonScripts()
        script { unsafe { raw("""
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
""".trimIndent()) } }
    }
}
