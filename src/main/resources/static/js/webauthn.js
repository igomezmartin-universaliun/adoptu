const webauthn = {
    async register(username, displayName, role) {
        const optsRes = await fetch('/api/auth/registration-options', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ username, displayName })
        });
        const options = await optsRes.json();
        const credential = await navigator.credentials.create({
            publicKey: this.parseCreationOptions(options)
        });
        const regRes = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                username, displayName, role,
                registrationResponse: JSON.stringify(credential.toJSON?.() ?? this.toJSON(credential))
            }),
            credentials: 'include'
        });
        const result = await regRes.json();
        return result.success;
    },
    async authenticate() {
        const optsRes = await fetch('/api/auth/assertion-options', { credentials: 'include' });
        const options = await optsRes.json();
        const credential = await navigator.credentials.get({
            publicKey: this.parseRequestOptions(options)
        });
        const authRes = await fetch('/api/auth/authenticate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(credential.toJSON?.() ?? this.toJSON(credential)),
            credentials: 'include'
        });
        const result = await authRes.json();
        return result.success;
    },
    parseCreationOptions(opts) {
        if (window.PublicKeyCredential?.parseCreationOptionsFromJSON) {
            return PublicKeyCredential.parseCreationOptionsFromJSON(opts);
        }
        return {
            challenge: this.base64ToArray(opts.challenge),
            rp: opts.rp,
            user: { ...opts.user, id: this.base64ToArray(opts.user.id) },
            pubKeyCredParams: opts.pubKeyCredParams || [{ type: 'public-key', alg: -7 }, { type: 'public-key', alg: -257 }]
        };
    },
    parseRequestOptions(opts) {
        if (window.PublicKeyCredential?.parseRequestOptionsFromJSON) {
            return PublicKeyCredential.parseRequestOptionsFromJSON(opts);
        }
        return {
            challenge: this.base64ToArray(opts.challenge),
            rpId: opts.rpId || 'localhost'
        };
    },
    base64ToArray(s) {
        const bin = atob(s.replace(/-/g, '+').replace(/_/g, '/'));
        const arr = new Uint8Array(bin.length);
        for (let i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);
        return arr;
    },
    arrayToBase64(arr) {
        let bin = '';
        for (let i = 0; i < arr.length; i++) bin += String.fromCharCode(arr[i]);
        return btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    },
    toJSON(cred) {
        const rawId = this.arrayToBase64(new Uint8Array(cred.rawId));
        const response = cred.response;
        return {
            id: cred.id,
            rawId,
            type: cred.type,
            response: {
                clientDataJSON: this.arrayToBase64(new Uint8Array(response.clientDataJSON)),
                authenticatorData: response.authenticatorData ? this.arrayToBase64(new Uint8Array(response.authenticatorData)) : undefined,
                signature: this.arrayToBase64(new Uint8Array(response.signature)),
                userHandle: response.userHandle ? this.arrayToBase64(new Uint8Array(response.userHandle)) : undefined
            }
        };
    }
};
