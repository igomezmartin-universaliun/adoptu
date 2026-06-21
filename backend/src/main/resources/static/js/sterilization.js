window.searchLocations = async function() {
    const country = document.getElementById('search-country').value;
    const state = document.getElementById('search-state').value;
    const city = document.getElementById('search-city').value;
    const zip = document.getElementById('search-zip').value;
    const neighborhood = document.getElementById('search-neighborhood').value;
    
    if (!country) {
        document.getElementById('locations-container').innerHTML = '<p>'+t('pleaseSelectCountry')+'</p>';
        return;
    }
    
    const params = new URLSearchParams();
    params.append('country', country);
    if (state) params.append('state', state);
    if (city) params.append('city', city);
    if (zip) params.append('zip', zip);
    if (neighborhood) params.append('neighborhood', neighborhood);
    
    try {
        const res = await fetch('/api/sterilization-locations?' + params.toString());
        const locations = await res.json();
        const container = document.getElementById('locations-container');
        if (locations.length === 0) {
            container.innerHTML = '<p>'+t('noLocationsFound')+'</p>';
            return;
        }
        container.innerHTML = '<div class="location-list">' + locations.map(loc => '
            <div class="location-card card-bg">
                <h3>'+loc.name+'</h3>
                <p class="location-address">'+loc.address+', '+loc.city+''+(loc.state ? ', '+loc.state : '')+', '+loc.country+'</p>
                '+(loc.phone ? '<p class="location-phone"><strong>'+t('phone')+':</strong> '+loc.phone+'</p>' : '')+'
                '+(loc.email ? '<p class="location-email"><strong>'+t('email')+':</strong> '+loc.email+'</p>' : '')+'
                '+(loc.website ? '<p class="location-website"><a href="'+loc.website+'" target="_blank">'+t('website')+'</a></p>' : '')+'
                '+(loc.description ? '<p class="location-description">'+loc.description+'</p>' : '')+'
            </div>
        ').join('') + '</div>';
    } catch (err) {
        document.getElementById('locations-container').innerHTML = '<p>'+t('errorLoadingLocations')+'</p>';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const searchBtn = document.getElementById('search-btn');
    if (searchBtn) {
        searchBtn.addEventListener('click', window.searchLocations);
    }
    const stateInput = document.getElementById('search-state');
    if (stateInput) {
        stateInput.addEventListener('input', debounce(window.searchLocations, 500));
    }
    const cityInput = document.getElementById('search-city');
    if (cityInput) {
        cityInput.addEventListener('input', debounce(window.searchLocations, 500));
    }
    const zipInput = document.getElementById('search-zip');
    if (zipInput) {
        zipInput.addEventListener('input', debounce(window.searchLocations, 500));
    }
    const neighborhoodInput = document.getElementById('search-neighborhood');
    if (neighborhoodInput) {
        neighborhoodInput.addEventListener('input', debounce(window.searchLocations, 500));
    }
});

window.debounce = function(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}