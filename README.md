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

## Manual Environment Variable Setup

If you prefer not to use direnv, you can set environment variables manually:

### macOS/Linux
```bash
export TENANT_ID="your-tenant-id"
export CLIENT_ID="your-client-id"
export CLIENT_SECRET="your-client-secret"
export API_APP_ID_URI="your-api-app-id-uri"
export APIM_BASE_URL="https://your-apim.azure-api.net"
export SLUG="/amp-oauth/case/C9LJXVS5NQ/courtschedule"
export SUBSCRIPTION_KEY="optional-subscription-key"
```

## IntelliJ IDEA Setup

### Option 1: Using direnv (Recommended)

1. Install the [direnv plugin](https://plugins.jetbrains.com/plugin/19285-direnv) in IntelliJ IDEA
2. Enable the plugin: Settings → Plugins → Search "direnv" → Install & Enable
3. Restart IntelliJ IDEA
4. The plugin will automatically load environment variables from `.envrc` when you open the project

### Option 2: Manual Configuration

1. Go to Run → Edit Configurations...
2. Create or edit your test configuration
3. Under "Environment variables", add:
   - `TENANT_ID`
   - `CLIENT_ID`
   - `CLIENT_SECRET`
   - `API_APP_ID_URI`
   - `APIM_BASE_URL`
   - `SLUG`
   - `SUBSCRIPTION_KEY` (optional)

Alternatively, you can set them at the project level:
- Settings → Build, Execution, Deployment → Build Tools → Gradle
- Under "Gradle JVM" and environment variables

## Required Environment Variables

| Variable | Required | Description | Example                                    |
|----------|----------|-------------|--------------------------------------------|
| `TENANT_ID` | Yes | Azure AD Tenant ID | `***********`                              |
| `CLIENT_ID` | Yes | Azure AD Client ID (Application ID) | `***********`                              |
| `CLIENT_SECRET` | Yes | Azure AD Client Secret | `*********`                                |
| `API_APP_ID_URI` | Yes | API App ID URI | `api://my-apim`                            |
| `APIM_BASE_URL` | Yes | APIM Base URL | `https://myapim.azure-api.net`             |
| `SLUG` | Yes | API endpoint path/slug | `/amp-oauth/case/C9LJXVS5NQ/courtschedule` |
| `SUBSCRIPTION_KEY` | No | APIM Subscription Key | `optional-api-key`                         |

## Running Tests

### All Tests
```bash
./gradlew test
```

### Specific Test Class
```bash
./gradlew test --tests "uk.gov.hmcts.cp.ApimTokenValidationSmokeTest"
```

### Specific Test Method
```bash
./gradlew test --tests "uk.gov.hmcts.cp.ApimTokenValidationSmokeTest.canGetAccessTokenFromEntra"
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

## GitHub Actions

This project includes a reusable GitHub Actions workflow. See [.github/workflows/README.md](.github/workflows/README.md) for details on using the workflow in client projects.
