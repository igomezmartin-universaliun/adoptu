const api = {
    base: '',
    async fetch(path, options = {}) {
        const res = await fetch(this.base + path, { ...options, credentials: 'include' });
        const text = await res.text();
        let data;
        try { data = text ? JSON.parse(text) : {}; } catch { data = {}; }
        if (!res.ok) throw new Error(data.error || res.statusText);
        return data;
    },
    async me() { return this.fetch('/api/auth/me'); },
    async logout() { return this.fetch('/api/auth/logout', { method: 'POST' }); },
    async getPets(type) { return this.fetch('/api/pets' + (type ? '?type=' + encodeURIComponent(type) : '')); },
    async getPet(id) { return this.fetch('/api/pets/' + id); },
    async createPet(pet) {
        return this.fetch('/api/pets', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(pet)
        });
    },
    async updatePet(id, pet) {
        return this.fetch('/api/pets/' + id, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(pet)
        });
    },
    async deletePet(id) {
        return this.fetch('/api/pets/' + id, { method: 'DELETE' });
    },
    async adoptPet(id, message) {
        return this.fetch('/api/pets/' + id + '/adopt', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: message || '' })
        });
    },
    async addImage(petId, file, isPrimary = false) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('isPrimary', isPrimary.toString());
        return this.fetch('/api/pets/' + petId + '/images', {
            method: 'POST',
            body: formData
        });
    },
    async removeImage(petId, imageId) {
        return this.fetch('/api/pets/' + petId + '/images/' + imageId, { method: 'DELETE' });
    },
    async setPrimaryImage(petId, imageId) {
        return this.fetch('/api/pets/' + petId + '/images/' + imageId + '/primary', { method: 'PUT' });
    },
    async getAdoptionRequests(petId) {
        return this.fetch('/api/pets/' + petId + '/adoption-requests');
    },
    async updateAdoptionRequest(requestId, status) {
        return this.fetch('/api/pets/adoption-requests/' + requestId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: 'status=' + encodeURIComponent(status)
        });
    },
    async getMyAdoptionRequests() {
        return this.fetch('/api/pets/my-adoption-requests');
    },
    async getPhotographers() {
        return this.fetch('/api/users/photographers');
    },
    async updateProfile(displayName) {
        return this.fetch('/api/users/profile', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ displayName })
        });
    },
    async updatePhotographerSettings(fee, currency, country, state) {
        return this.fetch('/api/users/photographer-settings', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ photographerFee: fee, photographerCurrency: currency, country, state })
        });
    },
    async createPhotographyRequest(photographerId, petId, message) {
        return this.fetch('/api/users/photography-requests', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ photographerId, petId, message })
        });
    },
    async createMultiplePhotographyRequests(photographerIds, petId, message) {
        return this.fetch('/api/users/photography-requests/multiple', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ photographerIds, petId, message })
        });
    },
    async getPhotographyRequests() {
        return this.fetch('/api/users/photography-requests');
    },
    async updatePhotographyRequest(requestId, status, scheduledDate) {
        const body = {};
        if (status) body.status = status;
        if (scheduledDate) body.scheduledDate = scheduledDate;
        return this.fetch('/api/users/photography-requests/' + requestId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
    },
    async activateRescuer(activate) {
        return this.fetch('/api/users/rescuer-profile', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ activate })
        });
    },
    async activatePhotographer(activate) {
        return this.fetch('/api/users/photographer-profile', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ activate })
        });
    },
    async activateTemporalHome(activate) {
        return this.fetch('/api/users/temporal-home-profile', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ activate })
        });
    },
    async createTemporalHome(data) {
        return this.fetch('/api/users/temporal-home', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
    },
    async updateTemporalHome(data) {
        return this.fetch('/api/users/temporal-home', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
    },
    async getTemporalHome() {
        return this.fetch('/api/users/temporal-home');
    },
    async getTemporalHomeRequests() {
        return this.fetch('/api/users/temporal-home/requests');
    },
    async blockRescuer(rescuerId) {
        return this.fetch('/api/temporal-homes/block', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ rescuerId })
        });
    },
    async updateLanguage(language) {
        return this.fetch('/api/users/language', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ language })
        });
    }
};
