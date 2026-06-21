const isLoggedIn = window.isLoggedInGlobal || false;

window.onCountryChange = function() {
    const hasCountry = document.getElementById('search-country').value !== '';
    const filters = document.querySelectorAll('.search-filters input');
    filters.forEach(input => {
        input.disabled = !hasCountry;
    });
};

const langEmojis = { en: '🇺🇸', es: '🇪🇸', fr: '🇫🇷', pt: '🇧🇷', zh: '🇨🇳' };

function setLang(lang) {
    i18n.setLang(lang);
    const btn = document.querySelector('.lang-dropbtn');
    if (btn) {
        btn.innerHTML = langEmojis[lang] + ' ▼';
    }
}

async function initI18n(userLanguage) {
    const lang = userLanguage || localStorage.getItem('lang') || 'en';
    if (lang !== 'en') {
        await i18n.loadLang(lang);
    }
    i18n.currentLang = lang;
    i18n.updatePage();
    i18n.updateDropdownLabel();
    i18n.updateActiveLangOption();
}

if (isLoggedIn) {
    window.userDataPromise = (async () => {
        if (window.cachedUserData) return window.cachedUserData;
        try {
            const user = await api.me();
            window.cachedUserData = user;
            console.log('User data fetched:', user);
            return user;
        } catch (e) { 
            console.error('Failed to fetch user:', e);
            return null; 
        }
    })();

    async function checkProfileCompletion(user) {
        const roles = user.activeRoles || [];
        const needsRedirect = [];
        
        if (roles.includes('PHOTOGRAPHER')) {
            if (!user.photographerCountry || !user.photographerState) {
                needsRedirect.push('PHOTOGRAPHER');
            }
        }
        
        if (roles.includes('TEMPORAL_HOME')) {
            try {
                await api.getTemporalHome();
            } catch (e) {
                needsRedirect.push('TEMPORAL_HOME');
            }
        }
        
        return needsRedirect.length > 0;
    }

    window.userDataPromise.then(async user => {
        console.log('User in then callback:', user);
        if (user && user.authenticated !== false) {
            console.log('Setting up inactivity timer');
            await initI18n(user.language);
            const roles = user.activeRoles || [];
            if (roles.includes('PHOTOGRAPHER') || roles.includes('TEMPORAL_HOME')) {
                const currentPath = window.location.pathname;
                if (currentPath !== '/profile') {
                    const needsCompletion = await checkProfileCompletion(user);
                    if (needsCompletion) {
                        window.location.href = '/profile';
                        return;
                    }
                }
            }
            const INACTIVITY_TIMEOUT = 5 * 60 * 1000;
            let inactivityTimer;
            const resetInactivityTimer = () => {
                clearTimeout(inactivityTimer);
                inactivityTimer = setTimeout(async () => {
                    console.log('Inactivity timeout - logging out');
                    await api.logout();
                    window.location.href = '/';
                }, INACTIVITY_TIMEOUT);
            };
            ['click', 'keypress', 'mousemove', 'scroll', 'touchstart'].forEach(event => {
                document.addEventListener(event, resetInactivityTimer, { passive: true });
            });
            resetInactivityTimer();
            console.log('Inactivity timer set up complete');
        } else {
            console.log('User not logged in or authenticated, skipping inactivity timer');
        }
    });
} else {
    initI18n();
}