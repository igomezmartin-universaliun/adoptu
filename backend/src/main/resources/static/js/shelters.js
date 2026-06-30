window.searchShelters = async function() {
    const errorDiv = document.getElementById('shelters-error');
    const container = document.getElementById('shelters');

    errorDiv.style.display = 'none';
    errorDiv.textContent = '';
    container.innerHTML = '<p>' + t('loading') + '</p>';

    const params = window.buildLocationSearchParams();
    if (!params) {
        errorDiv.style.display = 'block';
        errorDiv.textContent = t('countryRequired');
        container.innerHTML = '';
        return;
    }
    
    try {
        const output = await fetch('/api/shelters?' + params.toString());
        if (!output.ok) throw new Error('Failed to load shelters');
        const shelters = await output.json();
        
        if (!shelters || shelters.length === 0) {
            container.innerHTML = '<p>' + t('noSheltersFound') + '</p>';
            return;
        }
        
        container.innerHTML = shelters.map(shelter => {
            const location = [shelter.city, shelter.state, shelter.country].filter(Boolean).join(', ');
            const hasDonationInfo = shelter.bankName || shelter.accountNumber || shelter.iban;
            
            return '<div class="shelter-card">' +
                '<h3>' + shelter.name + '</h3>' +
                (location ? '<p class="shelter-location">' + location + '</p>' : '') +
                '<p class="shelter-address">' + shelter.address + (shelter.zip ? ', ' + shelter.zip : '') + '</p>' +
                (shelter.phone ? '<p class="shelter-phone"><strong>' + t('phone') + ':</strong> ' + shelter.phone + '</p>' : '') +
                (shelter.email ? '<p class="shelter-email"><strong>' + t('email') + ':</strong> <a href="mailto:' + shelter.email + '">' + shelter.email + '</a></p>' : '') +
                (shelter.website ? '<p class="shelter-website"><a href="' + shelter.website + '" target="_blank">' + t('visitWebsite') + '</a></p>' : '') +
                (shelter.fiscalId ? '<p class="shelter-fiscal-id"><strong>' + t('fiscalId') + ':</strong> ' + shelter.fiscalId + '</p>' : '') +
                (hasDonationInfo ? '<div class="shelter-donation-info"><h4>' + t('donationInformation') + '</h4>' +
                    (shelter.bankName ? '<p><strong>' + t('bankName') + ':</strong> ' + shelter.bankName + '</p>' : '') +
                    (shelter.accountHolderName ? '<p><strong>' + t('accountHolder') + ':</strong> ' + shelter.accountHolderName + '</p>' : '') +
                    (shelter.accountNumber ? '<p><strong>' + t('accountNumber') + ':</strong> ' + shelter.accountNumber + '</p>' : '') +
                    (shelter.iban ? '<p><strong>IBAN:</strong> ' + shelter.iban + '</p>' : '') +
                    (shelter.swiftBic ? '<p><strong>SWIFT/BIC:</strong> ' + shelter.swiftBic + '</p>' : '') +
                    (shelter.currency ? '<p><strong>' + t('currency') + ':</strong> ' + shelter.currency + '</p>' : '') +
                    '</div>' : '') +
                (shelter.description ? '<p class="shelter-description">' + shelter.description + '</p>' : '') +
                '</div>';
        }).join('');
    } catch (err) {
        errorDiv.style.display = 'block';
        errorDiv.textContent = t('errorLoadingShelters');
        container.innerHTML = '';
    }
};

document.addEventListener('DOMContentLoaded', () => {
    const searchBtn = document.getElementById('search-btn');
    if (searchBtn) {
        searchBtn.addEventListener('click', window.searchShelters);
    }
    const stateInput = document.getElementById('search-state');
    if (stateInput) {
        stateInput.addEventListener('input', debounce(window.searchShelters, 500));
    }
    const cityInput = document.getElementById('search-city');
    if (cityInput) {
        cityInput.addEventListener('input', debounce(window.searchShelters, 500));
    }
    const zipInput = document.getElementById('search-zip');
    if (zipInput) {
        zipInput.addEventListener('input', debounce(window.searchShelters, 500));
    }
    const neighborhoodInput = document.getElementById('search-neighborhood');
    if (neighborhoodInput) {
        neighborhoodInput.addEventListener('input', debounce(window.searchShelters, 500));
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