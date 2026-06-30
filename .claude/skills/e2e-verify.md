# E2E Verification Skill

End-to-end verification of the Adoptu app against test data.

## When to use
When the user asks to run e2e verification, end-to-end tests, or smoke tests against the running app.

## Steps

Run these steps in order:

### 1. Ensure services are running
```bash
curl -s http://localhost:8080/ -o /dev/null -w "%{http_code}"
curl -s http://localhost:8025/api/v1/messages | head -1
```
If the app (port 8080) is not running, tell the user to start it first.

### 2. Load test data
```bash
cd /path/to/adoptu && bash scripts/load_test_data.sh
```

### 3. Run the Playwright E2E tests
```bash
cd /path/to/adoptu && npx playwright test frontend/src/tests/e2e-verify.spec.ts --project=chromium --reporter=list
```

### 4. Report results
Summarise pass/fail for each section.
