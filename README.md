# Anthropic Tags - Jahia Content Editor UI Extension

This module is a Jahia UI extension for the Content Editor. It allows automatic semantic tag generation for content using the Anthropic Claude API.

## Features

- **Content Editor Integration**: Adds a button to the Content Editor UI to generate tags for the current content.
- **AI-Powered Tagging**: Extracts text from the selected JCR node and sends it to Anthropic Claude to receive relevant semantic tags.
- **Automatic Tag Field Update**: Fills the tag field of the content with the generated tags.
- **Configurable**: API key, model, and other parameters are managed via OSGi configuration.
- **User Prompt Customization**: Customize the prompt sent to the AI model for tag generation.
- **Language Support**: Option to select the source language for better context understanding.
- **Response**: Need to return and store an array of tags in a string[] format.

## Installation

1. **Prerequisites**: You need a valid Anthropic API key.
2. **Build the module**:
   ```bash
   mvn clean install
   ```
3. **Deploy**: Copy the generated JAR from `target/anthropic-tags-1.0.0-SNAPSHOT.jar` to your Jahia `digital-factory-data/modules` directory.
4. **Restart Jahia** if needed. The module will start automatically.

## Configuration

1. Create a file named `org.jahia.se.modules.anthropic.cfg` in your Jahia `digital-factory-data/karaf/etc/` directory.
2. Add the following properties:

    ```properties
    # Required: Anthropic API key
    ANTHROPIC_API_KEY=sk-ant-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

    # Optional: Model (default: claude-3-5-sonnet-latest)
    ANTHROPIC_MODEL=claude-3-5-sonnet-latest

    # Optional: Max tokens (default: 1024)
    ANTHROPIC_MAX_TOKENS=1024

    # Optional: User prompt template
    ANTHROPIC_USER_PROMPT=Generate an array of 5 tags in a string[] format from the following text

    # Optional: API base URL
    ANTHROPIC_API_BASE_URL=https://api.anthropic.com
    ```

3. Save the file. The configuration will be picked up automatically.

## Usage

1. Open the Content Editor in Jahia and select a content node.
2. Click the Anthropic button in the action bar.
3. A dialog will open. Select the source language if needed.
4. Click "Apply". The module will call the Anthropic API and update the tag field with the generated tags.

## Development

- Frontend: React (see `src/javascript/Anthropic/`)
- Backend: Java OSGi (see `src/main/java/org/jahia/se/modules/anthropic/`)
- Styling: SCSS (see `src/javascript/Anthropic/AnthropicDialog.scss`)

## License

MIT License

---
_This module leverages AI to simplify and accelerate content tagging in Jahia._

