const emoji = { DOG: '🐕', CAT: '🐱', BIRD: '🐦', FISH: '🐟' };
let currentType = '';
let currentSex = '';
const countrySelect = document.getElementById('pets-country');
const petsErrorDiv = document.getElementById('pets-error');
const petsCountryHint = document.getElementById('pets-country-hint');
const petsFiltersDiv = document.getElementById('pets-filters');
const petsGrid = document.getElementById('pets');
const petsEmptyDiv = document.getElementById('pets-empty');
const petsSentinel = document.getElementById('pets-sentinel');

const PAGE_SIZE = 12;
// Distance (in px) from the sentinel at which the next batch is rendered, so it's
// ready before the user actually scrolls to the bottom of the currently visible pets.
const PRELOAD_MARGIN = '600px 0px';

const PAW_ICON = '<svg class="pets-empty-icon" viewBox="0 0 100 100" width="88" height="88" aria-hidden="true">' +
    '<circle cx="50" cy="62" r="17" fill="currentColor" opacity="0.3"/>' +
    '<ellipse cx="26" cy="38" rx="9" ry="12" fill="currentColor" opacity="0.3"/>' +
    '<ellipse cx="44" cy="24" rx="9" ry="12" fill="currentColor" opacity="0.3"/>' +
    '<ellipse cx="64" cy="24" rx="9" ry="12" fill="currentColor" opacity="0.3"/>' +
    '<ellipse cx="80" cy="40" rx="9" ry="12" fill="currentColor" opacity="0.3"/>' +
    '</svg>';

let allPets = [];
let renderedCount = 0;
let loadRequestId = 0;
let scrollObserver = null;

function updateCountryHint() {
    const hasCountry = !!(countrySelect && countrySelect.value);
    if (petsCountryHint) petsCountryHint.style.display = hasCountry ? 'none' : '';
    if (petsFiltersDiv) petsFiltersDiv.style.display = hasCountry ? '' : 'none';
}

function showEmptyState(titleKey, hintKey) {
    if (!petsEmptyDiv) return;
    petsEmptyDiv.innerHTML = PAW_ICON + '<h3>' + t(titleKey) + '</h3><p>' + t(hintKey) + '</p>';
    petsEmptyDiv.style.display = 'flex';
}

function hideEmptyState() {
    if (petsEmptyDiv) petsEmptyDiv.style.display = 'none';
}

function petCardHtml(p) {
    const primaryImage = p.images && p.images.length > 0 ? p.images.find(img => img.isPrimary) || p.images[0] : null;
    const imageHtml = primaryImage
        ? '<img src="'+primaryImage.imageUrl+'" alt="'+p.name+'" loading="lazy">'
        : '<div class="pet-card-placeholder">'+(emoji[p.type]||'🐾')+'</div>';
    return '<a href="/pet/'+p.id+'" class="pet-card">'+imageHtml+'<div class="pet-card-body"><span class="pet-type">'+t(p.type.toLowerCase())+'</span><span class="pet-sex '+(p.sex === 'MALE' ? 'male' : 'female')+'">'+t(p.sex.toLowerCase())+'</span>'+(p.size ? '<span class="pet-size">'+p.size+'</span>' : '')+'<div class="pet-name"><h3>'+p.name+(p.isUrgent ? ' ⚠️' : '')+'</h3>'+(p.breed ? '<span class="pet-breed">'+p.breed+'</span>' : '')+'</div><p class="pet-info"><span class="pet-age"><span class="label">'+t('age')+'</span><span class="value">'+p.ageYears+t('years')+' '+p.ageMonths+t('months')+'</span></span><span class="pet-rescue-date">'+(p.rescueDate ? '<span class="label">'+t('rescued')+'</span><span class="value">'+new Date(p.rescueDate).toLocaleDateString()+'</span>' : '')+'</span></p><p class="pet-status">'+p.status+'</p></div></a>';
}

function updateSentinelState() {
    if (!petsSentinel) return;
    petsSentinel.textContent = renderedCount < allPets.length ? t('loadingMorePets') : '';
}

function renderNextBatch() {
    if (renderedCount >= allPets.length) return;
    const nextBatch = allPets.slice(renderedCount, renderedCount + PAGE_SIZE);
    petsGrid.insertAdjacentHTML('beforeend', nextBatch.map(petCardHtml).join(''));
    renderedCount += nextBatch.length;
    updateSentinelState();
    if (renderedCount >= allPets.length && scrollObserver) {
        scrollObserver.disconnect();
        scrollObserver = null;
    }
}

function setupInfiniteScroll() {
    if (scrollObserver) {
        scrollObserver.disconnect();
        scrollObserver = null;
    }
    if (!petsSentinel || !('IntersectionObserver' in window) || renderedCount >= allPets.length) return;
    scrollObserver = new IntersectionObserver((entries) => {
        if (entries.some(entry => entry.isIntersecting)) renderNextBatch();
    }, { rootMargin: PRELOAD_MARGIN });
    scrollObserver.observe(petsSentinel);
}

async function loadPets() {
    const country = countrySelect ? countrySelect.value : '';
    updateCountryHint();
    const requestId = ++loadRequestId;

    if (scrollObserver) {
        scrollObserver.disconnect();
        scrollObserver = null;
    }
    if (petsErrorDiv) {
        petsErrorDiv.style.display = 'none';
        petsErrorDiv.textContent = '';
    }
    petsGrid.innerHTML = '';
    allPets = [];
    renderedCount = 0;
    if (petsSentinel) petsSentinel.textContent = '';

    if (!country) {
        showEmptyState('chooseCountryPrompt', 'chooseCountryPromptHint');
        return;
    }

    hideEmptyState();
    const pets = await api.getPets(currentType || undefined, country);
    if (requestId !== loadRequestId) return; // a newer filter change superseded this request

    allPets = pets.filter(p => !currentSex || p.sex === currentSex);
    if (!allPets.length) {
        showEmptyState('noPetsFound', 'noPetsFoundHint');
        return;
    }
    renderNextBatch();
    setupInfiniteScroll();
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

// Default the country dropdown to the logged-in user's saved profile country, if any.
// No IP geolocation - if the user isn't logged in or has no saved country, the dropdown
// stays unselected and loadPets() shows the "choose a country" prompt until they pick one.
(async function initCountry() {
    if (countrySelect) {
        try {
            const user = await api.me();
            if (user && user.authenticated !== false && user.country) {
                countrySelect.value = user.country;
            }
        } catch (e) {
            // Not logged in or lookup failed - leave the dropdown unselected.
        }
    }
    loadPets();
})();
