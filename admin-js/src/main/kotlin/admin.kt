@file:Suppress("UNCHECKED_CAST", "unused")

// Minimal Kotlin/JS translation of src/main/resources/static/js/admin.js
// Uses dynamic DOM access to avoid extra dependencies.

private val emoji = mapOf("DOG" to "🐕", "CAT" to "🐱", "BIRD" to "🐦", "FISH" to "🐟")
private var banningUserId: Int? = null

fun escapeHtml(s: dynamic): String {
    if (s == null || s == undefined) return ""
    var str = s.toString()
    str = str.replace("&", "&amp;")
    str = str.replace("<", "&lt;")
    str = str.replace(">", "&gt;")
    str = str.replace("\"", "&quot;")
    str = str.replace("'", "&#39;")
    return str
}

@Suppress("UnsafeCastFromDynamic")
fun loadUsers() {
    val promise = js("fetch('/api/admin/users').then(r => r.json())") as dynamic
    promise.then { users: dynamic ->
        val container = js("document.getElementById('users-container')")
        if (users == null || users.length == 0) {
            container.innerHTML = "<p>" + js("t('noUsersFound')") + "</p>"
            return@then
        }
        val rows = Array.prototype.map.call(users, { u: dynamic ->
            val bannedClass = if (u.isBanned == true) "banned-row" else ""
            val displayName = escapeHtml(u.displayName)
            val username = escapeHtml(u.username)
            val roles = (u.activeRoles ?: arrayOf()).asDynamic().map({ r: dynamic ->
                val cls = if (r == "ADMIN") "admin" else ""
                "<span class=\"user-badge $cls\">" + escapeHtml(r) + "</span>"
            }).join("")
            val statusHtml = if (u.isBanned == true) "<span class=\"user-badge banned\">" + js("t('banned')") + "</span><br><small>" + escapeHtml(u.banReason) + "</small>" else "<span class=\"user-badge active\">" + js("t('active')") + "</span>"
            val actions = if (!(u.activeRoles?.asDynamic()?.includes("ADMIN") == true)) {
                if (u.isBanned == true) {
                    "<button class=\"btn\" onclick=\"unbanUser(${u.id})\">" + js("t('unban')") + "</button>"
                } else {
                    "<button class=\"btn btn-danger\" data-id=\"${u.id}\" data-name=\"" + js("encodeURIComponent(u.displayName)") + "\" onclick=\"showBanModal(this.dataset.id, this.dataset.name)\">" + js("t('ban')") + "</button>"
                }
            } else {
                "<small>" + js("t('cannotModifyAdmin')") + "</small>"
            }
            "<tr class=\"$bannedClass\">" +
                    "<td>$displayName</td>" +
                    "<td>$username</td>" +
                    "<td>$roles</td>" +
                    "<td>$statusHtml</td>" +
                    "<td>$actions</td>" +
                    "</tr>"
        }).join("")
        container.innerHTML = "<table class=\"user-table\"><thead><tr><th>" + js("t('name')") + "</th><th>" + js("t('email')") + "</th><th>" + js("t('roles')") + "</th><th>" + js("t('status')") + "</th><th>" + js("t('actions')") + "</th></tr></thead><tbody>" + rows + "</tbody></table>"
    }
}

fun showBanModal(userId: dynamic, userName: dynamic) {
    banningUserId = (userId as String).toIntOrNull() ?: (userId as Int?)
    val decodedName = js("decodeURIComponent(userName)") as String
    js("document.getElementById('ban-user-name').textContent = decodedName")
    js("document.getElementById('ban-reason').value = ''")
    js("document.getElementById('ban-modal').style.display = 'flex'")
}

fun hideBanModal() {
    js("document.getElementById('ban-modal').style.display = 'none'")
    banningUserId = null
}

@Suppress("UnsafeCastFromDynamic")
fun confirmBan() {
    if (banningUserId == null) return
    val reason = js("document.getElementById('ban-reason').value")
    js("fetch('/api/admin/users/' + ${banningUserId} + '/ban', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ reason: reason || null }) })")
    hideBanModal()
    loadUsers()
}

fun unbanUser(userId: dynamic) {
    if (!js("confirm(t('confirmUnban'))")) return
    js("fetch('/api/admin/users/' + userId + '/unban', { method: 'POST' })")
    loadUsers()
}

@Suppress("UnsafeCastFromDynamic")
fun loadPets() {
    js("api.getPets().then(pets => { const container = document.getElementById('pets'); if(pets.length) { container.innerHTML = pets.map(p => '<div class=\\"pet-card\\">...' ).join('') } else { container.innerHTML = '<p>'+t('noPets')+'</p>' } })")
}

@Suppress("unused")
fun setupTabs() {
    js("document.getElementById('tab-users').onclick = function() { document.getElementById('tab-users').classList.add('active'); document.getElementById('tab-pets').classList.remove('active'); document.getElementById('users-tab').style.display = 'block'; document.getElementById('pets-tab').style.display = 'none'; loadUsers(); }")
    js("document.getElementById('tab-pets').onclick = function() { document.getElementById('tab-pets').classList.add('active'); document.getElementById('tab-users').classList.remove('active'); document.getElementById('pets-tab').style.display = 'block'; document.getElementById('users-tab').style.display = 'none'; loadPets(); }")
    js("window.del = async function(id) { if (!confirm('Delete this pet?')) return; await api.deletePet(id); loadPets(); }")
}

// initialize on load
fun main() {
    setupTabs()
    loadUsers()
}

// expose main so webpack executable runs it
@JsName("main")
fun _main() = main()
