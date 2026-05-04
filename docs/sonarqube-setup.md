# SonarQube and SonarLint Setup

## What is already configured in this repository
- Root Maven build includes `sonar-maven-plugin`.
- JaCoCo coverage is generated during `mvn test`.
- Surefire is configured so JaCoCo can attach across the service modules.
- VS Code recommends the `SonarLint` extension for this workspace.

## Step 1: Start or access a SonarQube server
Use an existing SonarQube server, or start one locally with Docker:

```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:lts-community
```

Open `http://localhost:9000`.

## Step 2: Create a project in SonarQube
1. Log in to SonarQube.
2. Create a new local project.
3. Use the project key `digital-wallet-system`.
4. Generate a token for analysis.

## Step 3: Run tests and Sonar analysis from the repo root
Windows PowerShell:

```powershell
$env:SONAR_HOST_URL="http://localhost:9000"
$env:SONAR_TOKEN="your_generated_token"
mvn clean verify sonar:sonar
```

## Step 4: Install SonarLint in your IDE
Install the SonarLint extension:
- VS Code: `SonarQube for IDE` / `SonarLint` by SonarSource
- IntelliJ IDEA: `SonarLint`
- Eclipse: `SonarLint`

## Step 5: Bind SonarLint to SonarQube
In your IDE:
1. Open the SonarLint or SonarQube for IDE extension settings.
2. Add a connection to `http://localhost:9000`.
3. Authenticate with your SonarQube token.
4. Bind this workspace/project to `digital-wallet-system`.

## Step 6: Verify the result
- SonarQube should show the project analysis with bugs, vulnerabilities, code smells, and coverage.
- SonarLint should start showing the same rule set directly in the editor.
