# Entra APIM OAuth Flow - Test Suite

This project contains automated tests for validating Entra ID (Azure AD) OAuth tokens with Azure API Management (APIM).

## Prerequisites

- Java 21+
- Gradle
- Valid Azure AD credentials
- Access to the target APIM instance

## Quick Start

### Using direnv (Recommended for Local Development)

The project includes a `.envrc` file for managing environment variables locally using `direnv`.

#### 1. Install direnv

**macOS (using Homebrew):**
```bash
brew install direnv
```

#### 3. Allow the .envrc File

Navigate to the project directory and allow direnv to load the environment variables:

```bash
cd /path/to/entra-apim-oauth-flow
direnv allow
```

You should see a message like:
```
direnv: loading .envrc
direnv: export +API_APP_ID_URI +APIM_BASE_URL +CLIENT_ID +CLIENT_SECRET +SLUG +SUBSCRIPTION_KEY +TENANT_ID
```

#### 4. Verify Environment Variables

Verify that environment variables are loaded:
```bash
echo $TENANT_ID
echo $CLIENT_ID
# etc.
```

**Note:** Environment variables are automatically loaded when you `cd` into the project directory and unloaded when you leave it.

#### 5. Run Tests

Once direnv is set up, you can run tests directly:

```bash
./gradlew test
```

Or run specific tests:
```bash
./gradlew test --tests "uk.gov.hmcts.cp.ApimTokenValidationSmokeTest"
```

## Required Environment Variables

| Variable | Required | Description | Example                                    |
|----------|----------|-------------|--------------------------------------------|
|-------------|----------|----------------------------------------------------------------------|
| `TENANT_ID` | ✅ Yes | Azure AD Tenant ID                                                   |
| `CLIENT_ID` | ✅ Yes | Azure AD Client ID (Application ID)                                  |
| `CLIENT_SECRET` | ✅ Yes | Azure AD Client Secret                                               |
| `API_APP_ID_URI` | ✅ Yes | API App ID URI (e.g., `api://my-apim`)                               |
| `APIM_BASE_URL` | ✅ Yes | APIM Base URL (e.g., `https://myapim.azure-api.net`)                 |
| `SLUG` | ✅ Yes | API endpoint path (e.g., `/amp-oauth/case/C9LJXVS5NQ/courtschedule`) |
| `SUBSCRIPTION_KEY` | ✅ Yes | APIM Subscription Key                                                |
| `WAF_BASE_URL` | ✅ Yes | Publically accessible WAF Base URL                                   |
| `WAF_SLUG` | ✅ Yes | API endpoint path                                                    |

## Running Tests

### All Tests
```bash
./gradlew test
```
### With Test Reports
Test reports are generated in `build/reports/tests/test/index.html`

## Troubleshooting

### Environment Variables Not Available in IntelliJ

- Ensure the direnv plugin is installed and enabled
- Restart IntelliJ IDEA after installing the plugin
- Check that `.envrc` is in the project root
- Manually configure environment variables in Run Configurations as a fallback

### Certificate Validation Errors

The tests use relaxed HTTPS validation to handle certificate chain issues. If you encounter PKIX errors, ensure the SSL configuration in the test setup is applied.

## GitHub Actions Integration

This project provides a reusable GitHub Actions workflow that client projects can include in their CI/CD pipelines to run Entra APIM OAuth validation tests.

### Setting Up in Client Projects

#### Step 1: Create Workflow File

Create a new workflow file in your client project at `.github/workflows/test-entra-apim.yml`:

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
      slug: ${{ secrets.SLUG }}
      waf_base_url: ${{ secrets.WAF_BASE_URL }}
      waf_slug: ${{ secrets.WAF_SLUG }}
    with:
      tenant_id: ${{ secrets.TENANT_ID }}
      client_id: ${{ secrets.CLIENT_ID }}
      client_secret: ${{ secrets.CLIENT_SECRET }}
      api_app_id_uri: ${{ secrets.API_APP_ID_URI }}
      apim_base_url: ${{ secrets.APIM_BASE_URL }}
      subscription_key: ${{ secrets.SUBSCRIPTION_KEY }}
      slug: ${{ secrets.SLUG }}
      waf_base_url: ${{ secrets.WAF_BASE_URL }}
      waf_slug: ${{ secrets.WAF_SLUG }}
```

#### Step 2: Configure Repository Secrets

In your client project's GitHub repository, navigate to:
**Settings → Secrets and variables → Actions → New repository secret**

Add all environment variables as secrets

#### Step 3: Verify Workflow

1. Push the workflow file to your repository
2. Go to the **Actions** tab in your GitHub repository
3. The workflow will run automatically on pushes to `main`/`master` and on pull requests
4. You can also manually trigger it via **Actions → Test Entra APIM OAuth → Run workflow**

### Workflow Behavior

- **Automatic triggers**: Runs on push to `main`/`master` and on pull requests
- **Manual trigger**: Can be run manually via `workflow_dispatch`
- **Test artifacts**: Test results and reports are automatically uploaded and can be downloaded from the workflow run page
- **Java version**: Uses Java 21 (Temurin distribution)
- **Test execution**: Runs all tests in the `ApimTokenValidationSmokeTest` class

### Troubleshooting

**Workflow not running:**
- Ensure the workflow file is in `.github/workflows/` directory
- Check that the file has `.yml` or `.yaml` extension
- Verify the workflow file syntax is valid YAML

**Tests failing:**
- Verify all required secrets are configured in your repository
- Check the workflow logs for specific error messages
- Ensure your Azure AD credentials are valid and have the necessary permissions

**Permission errors:**
- Ensure the reusable workflow has permission to be called (GitHub may require approval for first-time use)
- Check repository settings for workflow permissions

For more detailed documentation, see [.github/workflows/README.md](.github/workflows/README.md).
