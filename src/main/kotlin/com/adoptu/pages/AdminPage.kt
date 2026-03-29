package com.adoptu.pages

import kotlinx.html.*

fun HTML.adminPage() {
    commonHead("Admin - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav() }
        }
        main {
            h1 { attributes["data-i18n"] = "adminPanel"; +"Admin Panel" }
            
            div(classes = "admin-tabs") {
                button(classes = "admin-tab-btn active") {
                    id = "tab-users"
                    attributes["data-i18n"] = "manageUsers"
                    +"Manage Users"
                }
                button(classes = "admin-tab-btn") {
                    id = "tab-pets"
                    attributes["data-i18n"] = "managePets"
                    +"Manage Pets"
                }
            }
            
            div(classes = "admin-tab-content") {
                id = "users-tab"
                div { id = "users-container"; +"" }
            }
            
            div(classes = "admin-tab-content") {
                id = "pets-tab"
                style = "display: none;"
                p { attributes["data-i18n"] = "managePetsDescription"; +"Manage all pet pages. Add or remove pets." }
                div { id = "message"; +"" }
                a("/my-pets") { classes = setOf("btn"); attributes["data-i18n"] = "managePetsBtn"; +"Manage Pets" }
                div { id = "pets"; classes = setOf("pet-grid"); style = "margin-top: 2rem;"; +"" }
            }
            
            div(classes = "form-modal") {
                id = "ban-modal"
                style = "display: none;"
                div(classes = "form-modal-content card-bg") {
                    h2 { attributes["data-i18n"] = "banUser"; +"Ban User" }
                    p { id = "ban-user-name"; +"" }
                    div(classes = "form-row") {
                        label { attributes["data-i18n"] = "banReason"; +"Reason (optional)" }
                        textArea { id = "ban-reason"; rows = "4" }
                    }
                    div(classes = "form-actions") {
                        button(type = ButtonType.button) {
                            classes = setOf("btn", "btn-danger")
                            attributes["data-i18n"] = "banUser"
                            +"Ban User"
                            onClick = "confirmBan()"
                        }
                        button(type = ButtonType.button) {
                            classes = setOf("btn", "btn-secondary")
                            attributes["data-i18n"] = "cancel"
                            +"Cancel"
                            onClick = "hideBanModal()"
                        }
                    }
                }
            }
        }
        footer()
        commonScripts()
        style = """
            .admin-tabs { display: flex; gap: 1rem; margin-bottom: 1.5rem; border-bottom: 2px solid var(--border); }
            .admin-tab-btn { background: transparent; border: none; padding: 0.75rem 1rem; cursor: pointer; color: var(--text); font-size: 1rem; transition: all 0.2s; border-bottom: 2px solid transparent; margin-bottom: -2px; }
            .admin-tab-btn:hover { color: var(--primary-light); }
            .admin-tab-btn.active { color: var(--primary-light); border-bottom-color: var(--primary-light); }
            .admin-tab-content { display: none; }
            .admin-tab-content.active { display: block; }
            .user-table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
            .user-table th, .user-table td { padding: 0.75rem; text-align: left; border-bottom: 1px solid var(--border); }
            .user-table th { color: var(--accent); font-weight: 600; }
            .user-badge { display: inline-block; padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.8rem; margin-right: 0.25rem; }
            .user-badge.admin { background: var(--primary); color: white; }
            .user-badge.banned { background: #dc3545; color: white; }
            .user-badge.active { background: #28a745; color: white; }
            .banned-row { background: rgba(220, 53, 69, 0.1); }
        """.trimIndent()
        script { unsafe { raw("""
const emoji = { DOG: '🐕', CAT: '🐱', BIRD: '🐦', FISH: '🐟' };
let banningUserId = null;

async function loadUsers() {
    const res = await fetch('/api/admin/users');
    const users = await res.json();
    const container = document.getElementById('users-container');
    
    if (users.length === 0) {
        container.innerHTML = '<p>'+t('noUsersFound')+'</p>';
        return;
    }
    
    container.innerHTML = '<table class="user-table"><thead><tr><th>'+t('name')+'</th><th>'+t('email')+'</th><th>'+t('roles')+'</th><th>'+t('status')+'</th><th>'+t('actions')+'</th></tr></thead><tbody>' +
        users.map(u => '<tr class="'+(u.isBanned ? 'banned-row' : '')+'">' +
            '<td>'+u.displayName+'</td>' +
            '<td>'+u.username+'</td>' +
            '<td>'+(u.activeRoles || []).map(r => '<span class="user-badge '+(r === 'ADMIN' ? 'admin' : '')+'">'+r+'</span>').join('') +'</td>' +
            '<td>'+(u.isBanned ? '<span class="user-badge banned">'+t('banned')+'</span><br><small>'+u.banReason+'</small>' : '<span class="user-badge active">'+t('active')+'</span>')+'</td>' +
            '<td>'+(!u.activeRoles?.includes('ADMIN') ? (
                u.isBanned ? 
                    '<button class="btn" onclick="unbanUser('+u.id+')">'+t('unban')+'</button>' :
                    '<button class="btn btn-danger" onclick="showBanModal('+u.id+', \\''+u.displayName.replace(/'/g, "\\\\'")+'\\')">'+t('ban')+'</button>'
            ) : '<small>'+t('cannotModifyAdmin')+'</small>')+'</td>' +
        '</tr>').join('') + '</tbody></table>';
}

function showBanModal(userId, userName) {
    banningUserId = userId;
    document.getElementById('ban-user-name').textContent = userName;
    document.getElementById('ban-reason').value = '';
    document.getElementById('ban-modal').style.display = 'flex';
}

function hideBanModal() {
    document.getElementById('ban-modal').style.display = 'none';
    banningUserId = null;
}

async function confirmBan() {
    if (!banningUserId) return;
    const reason = document.getElementById('ban-reason').value;
    await fetch('/api/admin/users/'+banningUserId+'/ban', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: reason || null })
    });
    hideBanModal();
    loadUsers();
}

async function unbanUser(userId) {
    if (!confirm(t('confirmUnban'))) return;
    await fetch('/api/admin/users/'+userId+'/unban', { method: 'POST' });
    loadUsers();
}

async function loadPets() {
    const pets = await api.getPets();
    const container = document.getElementById('pets');
    container.innerHTML = pets.length ? pets.map(p => '<div class="pet-card"><div class="pet-card-placeholder">'+(emoji[p.type]||'🐾')+'</div><div class="pet-card-body"><span class="pet-type">'+t(p.type.toLowerCase())+'</span><span class="pet-sex '+(p.sex === 'MALE' ? 'male' : 'female')+'">'+t(p.sex.toLowerCase())+'</span>'+(p.size ? '<span class="pet-size">'+p.size+'</span>' : '')+'<div class="pet-name"><h3>'+p.name+(p.isUrgent ? ' ⚠️' : '')+'</h3>'+(p.breed ? '<span class="pet-breed">'+p.breed+'</span>' : '')+'</div><p class="pet-info"><span class="pet-age">'+p.ageYears+t('years')+' '+p.ageMonths+t('months')+' • '+p.weight+' kg</span><span class="pet-rescue-date">'+(p.rescueDate ? ' '+t('rescued')+': '+new Date(p.rescueDate).toLocaleDateString() : '')+'</span></p><p>'+p.status+'</p><div class="pet-card-actions"><a href="/pet/'+p.id+'" class="btn">'+t('viewDetails')+'</a><a href="/my-pets?edit='+p.id+'" class="btn btn-secondary">'+t('edit')+'</a><button class="btn btn-secondary" onclick="del('+p.id+')">'+t('delete')+'</button></div></div></div>').join('') : '<p>'+t('noPets')+'</p>';
}

document.getElementById('tab-users').onclick = () => {
    document.getElementById('tab-users').classList.add('active');
    document.getElementById('tab-pets').classList.remove('active');
    document.getElementById('users-tab').style.display = 'block';
    document.getElementById('pets-tab').style.display = 'none';
    loadUsers();
};

document.getElementById('tab-pets').onclick = () => {
    document.getElementById('tab-pets').classList.add('active');
    document.getElementById('tab-users').classList.remove('active');
    document.getElementById('pets-tab').style.display = 'block';
    document.getElementById('users-tab').style.display = 'none';
    loadPets();
};

window.del = async (id) => { if (!confirm('Delete this pet?')) return; await api.deletePet(id); loadPets(); };

loadUsers();
""".trimIndent()) } }
    }
}
