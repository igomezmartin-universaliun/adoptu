const emoji = { DOG: '🐕', CAT: '🐱', BIRD: '🐦', FISH: '🐟' };
let currentType = '';
let currentSex = '';
const countrySelect = document.getElementById('pets-country');
const petsErrorDiv = document.getElementById('pets-error');
const petsCountryHint = document.getElementById('pets-country-hint');

function updateCountryHint() {
    const hasCountry = !!(countrySelect && countrySelect.value);
    if (petsCountryHint) petsCountryHint.style.display = hasCountry ? 'none' : '';
}

async function loadPets() {
    const country = countrySelect ? countrySelect.value : '';
    updateCountryHint();
    const container = document.getElementById('pets');
    if (!country) {
        if (petsErrorDiv) {
            petsErrorDiv.style.display = 'block';
            petsErrorDiv.textContent = t('countryRequired');
        }
        container.innerHTML = '';
        return;
    }
    if (petsErrorDiv) {
        petsErrorDiv.style.display = 'none';
        petsErrorDiv.textContent = '';
    }
    const pets = await api.getPets(currentType || undefined, country);
    const filteredPets = pets.filter(p => !currentSex || p.sex === currentSex);
    container.innerHTML = filteredPets.length ? filteredPets.map(p => {
        const primaryImage = p.images && p.images.length > 0 ? p.images.find(img => img.isPrimary) || p.images[0] : null;
        const imageHtml = primaryImage 
            ? '<img src="'+primaryImage.imageUrl+'" alt="'+p.name+'">' 
            : '<div class="pet-card-placeholder">'+(emoji[p.type]||'🐾')+'</div>';
        return '<a href="/pet/'+p.id+'" class="pet-card">'+imageHtml+'<div class="pet-card-body"><span class="pet-type">'+t(p.type.toLowerCase())+'</span><span class="pet-sex '+(p.sex === 'MALE' ? 'male' : 'female')+'">'+t(p.sex.toLowerCase())+'</span>'+(p.size ? '<span class="pet-size">'+p.size+'</span>' : '')+'<div class="pet-name"><h3>'+p.name+(p.isUrgent ? ' ⚠️' : '')+'</h3>'+(p.breed ? '<span class="pet-breed">'+p.breed+'</span>' : '')+'</div><p class="pet-info"><span class="pet-age"><span class="label">'+t('age')+'</span><span class="value">'+p.ageYears+t('years')+' '+p.ageMonths+t('months')+'</span></span><span class="pet-rescue-date">'+(p.rescueDate ? '<span class="label">'+t('rescued')+'</span><span class="value">'+new Date(p.rescueDate).toLocaleDateString()+'</span>' : '')+'</span></p><p class="pet-status">'+p.status+'</p></div></a>';
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
if (countrySelect) {
    countrySelect.onchange = () => loadPets();
}

// Default the country dropdown, in priority order: the logged-in user's saved profile
// country, then CloudFront's IP-based geolocation header (via /api/detect-country),
// then the browser's own locale as a fallback for requests that bypass CloudFront (e.g.
// local dev). If none of these resolve, the dropdown stays unselected and loadPets()
// shows the "select a country" prompt until the visitor picks one themselves.
(async function initCountry() {
    if (countrySelect) {
        try {
            const user = await api.me();
            if (user && user.authenticated !== false && user.country) {
                countrySelect.value = user.country;
            }
        } catch (e) {
            // Not logged in or lookup failed - fall through to detection below.
        }
        if (!countrySelect.value) {
            try {
                const detected = await api.detectCountry(navigator.language);
                if (detected && detected.country) {
                    countrySelect.value = detected.country;
                }
            } catch (e) {
                // Detection unavailable - leave the dropdown unselected.
            }
        }
    }
    loadPets();
})();