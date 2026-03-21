package com.adoptu.pages

import kotlinx.html.*

fun HTML.photographersPage() {
    commonHead("Photographers - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { commonNav() }
        }
        main {
            h1 { attributes["data-i18n"] = "photographers"; +"Photographers" }
            p { attributes["data-i18n"] = "photographerDescription"; +"Professional photographers offering pet photo sessions" }
            div { id = "photographers"; classes = setOf("photographer-grid"); +"" }
        }
        footer()
        commonScripts()
        script { unsafe { raw("""
const emoji = { DOG: '🐕', CAT: '🐱', BIRD: '🐦', FISH: '🐟' };
async function loadPhotographers() {
    try {
        const photographers = await api.getPhotographers();
        const container = document.getElementById('photographers');
        if (!photographers || photographers.length === 0) {
            container.innerHTML = '<p>'+t('noPhotographersAvailable')+'</p>';
            return;
        }
        container.innerHTML = photographers.map(p => {
            const fee = p.photographerFee ? p.photographerFee + ' ' + (p.photographerCurrency || 'USD') : 'Free';
            return '<div class="photographer-card">' +
                '<div class="photographer-info">' +
                '<h3>'+p.displayName+'</h3>' +
                '<p class="photographer-fee">'+t('sessionFee')+': <strong>'+fee+'</strong></p>' +
                '</div>' +
                '<button class="btn request-btn" data-id="'+p.userId+'" data-name="'+p.displayName+'" data-fee="'+fee+'">'+t('requestPhotoSession')+'</button>' +
                '</div>';
        }).join('');
        
        document.querySelectorAll('.request-btn').forEach(btn => {
            btn.onclick = async () => {
                const photographerId = parseInt(btn.dataset.id);
                const photographerName = btn.dataset.name;
                const msg = t('enterMessage').replace('{name}', photographerName);
                const message = prompt(msg);
                if (message === null) return;
                try {
                    await api.createPhotographyRequest(photographerId, null, message || '');
                    alert(t('requestSentSuccessfully'));
                } catch (err) {
                    alert('Error: ' + err.message);
                }
            };
        });
    } catch (err) {
        document.getElementById('photographers').innerHTML = '<p>'+t('errorLoadingPhotographers')+'</p>';
    }
}
loadPhotographers();
""".trimIndent()) } }
    }
}
