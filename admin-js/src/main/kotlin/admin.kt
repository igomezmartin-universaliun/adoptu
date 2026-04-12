@file:Suppress("UnsafeCastFromDynamic", "unused")

import kotlin.js.Promise
import kotlin.js.Date

external fun t(key: String): String
external val api: dynamic
external fun encodeURIComponent(s: String): String
external fun decodeURIComponent(s: String): String
external fun confirm(message: String): Boolean
external fun fetch(url: String, options: dynamic = definedExternally): Promise<dynamic>

private val emoji = mapOf("DOG" to "🐕", "CAT" to "🐱", "BIRD" to "🐦", "FISH" to "🐟")
private var banningUserId: Int? = null

fun escapeHtml(s: Any?): String {
    if (s == null) return ""
    return s.toString()
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}

@Suppress("RemoveExplicitTypeArguments")
fun loadUsers() {
    fetch("/api/admin/users").then<dynamic> { res ->
        (res.asDynamic().json() as Promise<dynamic>)
    }.then<dynamic> { users ->
        val arr = (users as Array<dynamic>)
        val container = js("document.getElementById('users-container')")
        if (arr.isEmpty()) {
            container.innerHTML = "<p>${t("noUsersFound")}</p>"
            return@then
        }
        val rows = arr.joinToString(separator = "") { u ->
            val isBanned = (u.isBanned as? Boolean) ?: false
            val bannedClass = if (isBanned) "banned-row" else ""
            val displayName = escapeHtml(u.displayName as? Any?)
            val username = escapeHtml(u.username as? Any?)
            val activeRoles = (u.activeRoles as? Array<dynamic>) ?: emptyArray<dynamic>()
            val roles = activeRoles.joinToString(separator = "") { r ->
                val cls = if ((r as? String) == "ADMIN") "admin" else ""
                "<span class=\"user-badge $cls\">${escapeHtml(r as? Any?)}</span>"
            }
            val statusHtml = if (isBanned) "<span class=\"user-badge banned\">${t("banned")}</span><br><small>${escapeHtml(u.banReason as? Any?)}</small>" else "<span class=\"user-badge active\">${t("active")}</span>"
            val isAdmin = activeRoles.any { (it as? String) == "ADMIN" }
            val actions = if (!isAdmin) {
                if (isBanned) {
                    "<button class=\"btn\" onclick=\"unbanUser(${u.id})\">${t("unban")}</button>"
                } else {
                    val nameEnc = encodeURIComponent((u.displayName as? String) ?: "")
                    "<button class=\"btn btn-danger\" data-id=\"${u.id}\" data-name=\"$nameEnc\" onclick=\"showBanModal(this.dataset.id, this.dataset.name)\">${t("ban")}</button>"
                }
            } else {
                "<small>${t("cannotModifyAdmin")}</small>"
            }
            "<tr class=\"$bannedClass\">" +
                    "<td>$displayName</td>" +
                    "<td>$username</td>" +
                    "<td>$roles</td>" +
                    "<td>$statusHtml</td>" +
                    "<td>$actions</td>" +
                    "</tr>"
        }
        container.innerHTML = "<table class=\"user-table\"><thead><tr><th>${t("name")}</th><th>${t("email")}</th><th>${t("roles")}</th><th>${t("status")}</th><th>${t("actions")}</th></tr></thead><tbody>$rows</tbody></table>"
    }
}

fun showBanModal(userId: String, userName: String) {
    banningUserId = userId.toIntOrNull()
    val decodedName = decodeURIComponent(userName)
    js("document.getElementById('ban-user-name').textContent = decodedName")
    js("document.getElementById('ban-reason').value = ''")
    js("document.getElementById('ban-modal').style.display = 'flex'")
}

fun hideBanModal() {
    js("document.getElementById('ban-modal').style.display = 'none'")
    banningUserId = null
}

fun confirmBan() {
    val id = banningUserId ?: return
    val reason = js("document.getElementById('ban-reason').value") as? String
    val body = js("JSON.stringify({ reason: reason || null })")
    val options = js("{}")
    options.method = "POST"
    options.headers = js("{}")
    options.headers["Content-Type"] = "application/json"
    options.body = body
    fetch("/api/admin/users/$id/ban", options)
    hideBanModal()
    loadUsers()
}

fun unbanUser(userId: dynamic) {
    if (!confirm(t("confirmUnban"))) return
    val options = js("{}")
    options.method = "POST"
    fetch("/api/admin/users/${userId}/unban", options)
    loadUsers()
}

fun loadPets() {
    api.getPets().then { pets ->
        val container = js("document.getElementById('pets')")
        val arr = pets as Array<dynamic>
        if (arr.isNotEmpty()) {
            val html = arr.joinToString(separator = "") { p ->
                val type = (p.type as? String) ?: ""
                val emojiChar = emoji[type] ?: "🐾"
                val typeLabel = t(type.lowercase())
                val sex = (p.sex as? String) ?: ""
                val sexLabel = t(sex.lowercase())
                val sexClass = if (sex == "MALE") "male" else "female"
                val sizeHtml = if ((p.size as? String).isNullOrEmpty()) "" else "<span class=\"pet-size\">${escapeHtml(p.size as? Any?)}</span>"
                val urgent = if ((p.isUrgent as? Boolean) == true) " ⚠️" else ""
                val breedHtml = if ((p.breed as? String).isNullOrEmpty()) "" else "<span class=\"pet-breed\">${escapeHtml(p.breed as? Any?)}</span>"
                val rescueDateStr = if (p.rescueDate != null) Date((p.rescueDate as dynamic)).toLocaleDateString() as String else ""
                val rescueHtml = if (rescueDateStr.isNotEmpty()) " ${t("rescued")}: $rescueDateStr" else ""
                val ageYears = p.ageYears ?: ""
                val ageMonths = p.ageMonths ?: ""
                val ageStr = "${ageYears}${t("years")} ${ageMonths}${t("months")}"
                val weight = p.weight ?: ""
                val petStatus = escapeHtml(p.status as? Any?)

                "<div class=\"pet-card\"><div class=\"pet-card-placeholder\">$emojiChar</div><div class=\"pet-card-body\"><span class=\"pet-type\">$typeLabel</span><span class=\"pet-sex $sexClass\">$sexLabel</span>$sizeHtml<div class=\"pet-name\"><h3>${escapeHtml(p.name as? Any?)}$urgent</h3>$breedHtml</div><p class=\"pet-info\"><span class=\"pet-age\">$ageStr • $weight kg</span><span class=\"pet-rescue-date\">$rescueHtml</span></p><p>$petStatus</p><div class=\"pet-card-actions\"><a href=\"/pet/${p.id}\" class=\"btn\">${t("viewDetails")}</a><a href=\"/my-pets?edit=${p.id}\" class=\"btn btn-secondary\">${t("edit")}</a><button class=\"btn btn-secondary\" onclick=\"del(${p.id})\">${t("delete")}</button></div></div></div>"
            }
            container.innerHTML = html
        } else {
            container.innerHTML = "<p>${t("noPets")}</p>"
        }
    }
}

fun setupTabs() {
    js("document.getElementById('tab-users').onclick = function() { document.getElementById('tab-users').classList.add('active'); document.getElementById('tab-pets').classList.remove('active'); document.getElementById('users-tab').style.display = 'block'; document.getElementById('pets-tab').style.display = 'none'; }")
    js("document.getElementById('tab-pets').onclick = function() { document.getElementById('tab-pets').classList.add('active'); document.getElementById('tab-users').classList.remove('active'); document.getElementById('pets-tab').style.display = 'block'; document.getElementById('users-tab').style.display = 'none'; }")
    js("window.del = function(id) { if (!confirm('Delete this pet?')) return; api.deletePet(id).then(function(){ loadPets(); }); }")
}

fun main() {
    setupTabs()
    loadUsers()
}

@JsName("main")
fun _main() = main()
