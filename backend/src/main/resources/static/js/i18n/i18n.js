const i18n = {
    currentLang: 'en',
    translations: {},
    loadedLangs: new Set(),
    
    async loadLang(lang) {
        if (this.loadedLangs.has(lang)) return;
        if (lang === 'en') return;
        try {
            const script = document.createElement('script');
            script.src = `/static/js/i18n/i18n-${lang}.js`;
            document.head.appendChild(script);
            await new Promise((resolve, reject) => {
                script.onload = () => { this.loadedLangs.add(lang); resolve(); };
                script.onerror = reject;
            });
        } catch (e) {
            console.error('Failed to load language:', lang, e);
        }
    },
    
    t: function(key) {
        if (key.includes('.')) {
            const parts = key.split('.');
            let value = this.translations[this.currentLang];
            for (const part of parts) {
                value = value?.[part];
            }
            if (value) return value;
            value = this.translations['en'];
            for (const part of parts) {
                value = value?.[part];
            }
            return value || key;
        }
        return this.translations[this.currentLang]?.[key] || this.translations['en']?.[key] || key;
    },
    
    setLang: function(lang, persist = true) {
        this.loadLang(lang).then(() => {
            this.currentLang = lang;
            if (persist) {
                localStorage.setItem('lang', lang);
            }
            this.updatePage();
            this.updateDropdownLabel();
        });
    },
    
    updatePage: function() {
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            const text = this.t(key);
            if (text && text !== key) el.textContent = text;
        });
        document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
            const key = el.getAttribute('data-i18n-placeholder');
            el.placeholder = this.t(key);
        });
        this.sortCountryOptions();
        this.updateActiveLangOption();
    },
    
    updateActiveLangOption: function() {
        document.querySelectorAll('.lang-option').forEach(opt => {
            if (opt.dataset.lang === this.currentLang) {
                opt.classList.add('active');
            } else {
                opt.classList.remove('active');
            }
        });
    },
    
    sortCountryOptions: function() {
        const countrySelect = document.getElementById('search-country');
        if (!countrySelect) return;
        const options = Array.from(countrySelect.options);
        const selectedValue = countrySelect.value;
        const sortedOptions = options.sort((a, b) => a.textContent.localeCompare(b.textContent));
        countrySelect.innerHTML = '';
        sortedOptions.forEach(opt => countrySelect.appendChild(opt));
        countrySelect.value = selectedValue;
    },
    
    updateDropdownLabel: function() {
        const btn = document.querySelector('.lang-dropbtn');
        if (btn) {
            const flags = { en: '🇺🇸', es: '🇪🇸', fr: '🇫🇷', pt: '🇧🇷', zh: '🇨🇳' };
            btn.innerHTML = (flags[this.currentLang] || '🌐') + ' ▼';
        }
    }
};

function setLang(lang) {
    i18n.setLang(lang);
    const btn = document.querySelector('.lang-dropbtn');
    if (btn) {
        const flags = { en: '🇺🇸', es: '🇪🇸', fr: '🇫🇷', pt: '🇧🇷', zh: '🇨🇳' };
        btn.innerHTML = flags[lang] + ' ▼';
    }
}

function t(key) {
    return i18n.t(key);
}
