# GitHub Actions Workflow for Entra APIM OAuth Tests

This repository contains a reusable GitHub Actions workflow that runs Entra ID (Azure AD) and APIM OAuth token validation tests.

## Usage in Client Projects

### Option 1: Call as Reusable Workflow (Recommended)

Create a workflow file in your client project at `.github/workflows/test-entra-apim.yml`:

```yaml
name: Test Entra APIM OAuth

on:
  workflow_dispatch:
  push:
    branches: [main, master]
  pull_request:
    branches: [main, master]

jobs:
  test:
    uses: hmcts/entra-apim-oauth-flow/.github/workflows/test.yml@main
    secrets:
      tenant_id: ${{ secrets.TENANT_ID }}
      client_id: ${{ secrets.CLIENT_ID }}
      client_secret: ${{ secrets.CLIENT_SECRET }}
      api_app_id_uri: ${{ secrets.API_APP_ID_URI }}
      apim_base_url: ${{ secrets.APIM_BASE_URL }}
      subscription_key: ${{ secrets.SUBSCRIPTION_KEY }}
    with:
      slug: ${{ secrets.SLUG }}
```

### Option 2: Using Inputs (for workflow_dispatch)

You can also pass inputs directly when manually triggering the workflow:

```yaml
name: Test Entra APIM OAuth

on:
  workflow_dispatch:
    inputs:
      tenant_id:
        required: true
        type: string
      client_id:
        required: true
        type: string
      # ... other inputs

jobs:
  test:
    uses: hmcts/entra-apim-oauth-flow/.github/workflows/test.yml@main
    secrets:
      client_secret: ${{ secrets.CLIENT_SECRET }}
      # ... other secrets
    with:
      tenant_id: ${{ github.event.inputs.tenant_id }}
      client_id: ${{ github.event.inputs.client_id }}
      # ... other inputs
```

## Required Secrets

Set these secrets in your GitHub repository settings (Settings → Secrets and variables → Actions):

- `TENANT_ID`: Azure AD Tenant ID
- `CLIENT_ID`: Azure AD Client ID (Application ID)
- `CLIENT_SECRET`: Azure AD Client Secret
- `API_APP_ID_URI`: API App ID URI (e.g., `api://my-apim`)
- `APIM_BASE_URL`: APIM Base URL (e.g., `https://myapim.azure-api.net`)
- `SUBSCRIPTION_KEY`: APIM Subscription Key (optional)
- `SLUG`: API slug/path for the endpoint (required, e.g., `/amp-oauth/case/C9LJXVS5NQ/courtschedule`)

## Required Environment Variables

The workflow sets these environment variables for the tests:

- `TENANT_ID`
- `CLIENT_ID`
- `CLIENT_SECRET`
- `API_APP_ID_URI`
- `APIM_BASE_URL`
- `SUBSCRIPTION_KEY` (optional)
- `SLUG` (required)

## Test Artifacts

Test results and reports are automatically uploaded as artifacts and can be downloaded from the workflow run page.

## Notes

- The workflow requires Java 21 (Temurin distribution)
- Tests use relaxed HTTPS validation to handle certificate chain issues
- The workflow will fail if required environment variables are not provided

