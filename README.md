# TestFlow Generator - IntelliJ IDEA Plugin

An IntelliJ IDEA plugin that generates test flows from documentation using AI.

## Features

- **AI-Powered Test Generation**: Leverage OpenAI, Anthropic Claude, or local Ollama models to parse documentation and generate structured test flows
- **Multiple Step Types**: Support for HTTP requests and script execution steps
- **Export Options**: Export generated flows to JSON, plain text, or Markdown formats
- **Test Execution**: Execute generated test flows directly within the IDE
- **Configurable AI Providers**: Choose from multiple AI services and customize settings

## Installation

### Development Build

```bash
./gradlew buildPlugin
./gradlew buildPlugin --no-daemon --info 2>&1
```

The plugin ZIP will be created at `build/distributions/testflow-generator-1.0.0.zip`

Install it via:
1. `IntelliJ IDEA` → `Settings` → `Plugins` → `Gear Icon` → `Install Plugin from Disk`
2. Select the generated ZIP file

### Running in Development

```bash
./gradlew runIde
```

## Usage

1. **Configure AI Service**
   - Go to `Settings` → `Tools` → `TestFlow Generator`
   - Select your AI provider (OpenAI, Anthropic, or Ollama)
   - Enter your API key (not required for Ollama)

2. **Generate Test Flow**
   - Open the TestFlow tool window (View → Tool Windows → TestFlow)
   - Paste your documentation in the input area
   - Click "Generate Test Flow"

3. **Export Results**
   - Use "Export JSON" or "Export Text" buttons
   - Save to clipboard or file

4. **Execute Tests**
   - Click "Execute Test" to run the generated flow
   - View results in the output panel

## Example Documentation

```
User Authentication API Testing

Test Cases:
1. Register a new user
   - POST /api/users/register
   - Body: {username, email, password}
   - Assert: 201 Created

2. Login with credentials
   - POST /api/auth/login
   - Assert: 200 OK, JWT token returned

3. Access protected endpoint
   - GET /api/users/profile
   - Header: Authorization: Bearer {token}
```

## Supported AI Providers

- **OpenAI**: GPT-4, GPT-4 Turbo, GPT-3.5 Turbo
- **Anthropic**: Claude 3 Opus, Claude 3 Sonnet, Claude 3 Haiku
- **Ollama**: Local models like Llama 2, Mistral, etc.

## Development

### Project Structure

```
src/main/kotlin/com/example/testflow/
├── ai/              # AI service integration
├── model/           # Data models
├── parser/          # Flow parsing and validation
├── exporter/        # Export functionality
├── executor/        # Test execution engine
├── settings/        # Plugin settings
├── ui/              # User interface
└── actions/         # IDE actions
```

### Building

```bash
# Build plugin
./gradlew buildPlugin

# Run verification
./gradlew verifyPlugin

# Run in test IDE
./gradlew runIde
```

## notice

本地 IntelliJ CE 是 2025.2 版本（252.28539.33），使用 Kotlin 2.2.0 编译。需要升级 Kotlin 版本并更新配置。

./gradlew buildPlugin \
  -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890 \
  -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890
