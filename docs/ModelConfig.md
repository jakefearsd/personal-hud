# Model Configuration Guide (Multi-Brain Engine)

The HUD (Heads-Up Display) utilizes a dynamic, database-backed **Multi-Brain Intelligence Engine**. This architecture allows the system to leverage different Large Language Models (LLMs) for specific analytical tasks and enables side-by-side quality comparison.

## Supported Providers

### 1. Ollama (Local)
Used for running models like **Gemma4 8B** on your local network/hardware.
- **Config Requirements**: 
    - `Base URL`: The endpoint of your Ollama server (e.g., `http://inference.jakefear.com:11434`).
    - `Model Name`: The specific tag of the model (e.g., `gemma4:e4b`).
    - `Context Window`: Configurable up to the limits of your hardware (defaulting to 64k/65536 for deep-dives).

### 2. Google Gemini (Remote/SaaS)
Used for high-reasoning tasks utilizing Google's 1.5 Pro or Flash models.
- **Config Requirements**:
    - `API Key`: Your secret key from [Google AI Studio](https://aistudio.google.com/).
    - `Model Name`: e.g., `gemini-2.0-flash` or `gemini-2.0-pro-exp-02-05`.
- **Note**: A single daily run (7 categories) comfortably fits within the **Free Tier** limits.

## Administrative Interface

Access the **Config** tab in the HUD UI to:
1. **Add Brain**: Define a new model configuration.
2. **Toggle Active**: Activate or deactivate specific brains. The daily briefing pipeline will iterate through *all* active configurations.
3. **Edit/Delete**: Update connection strings or remove obsolete models.

## Comparison & Tuning

The HUD is designed as a laboratory for intelligence tuning:
- **Unique Persistence**: Briefings are stored with the `modelName` as part of the unique key. 
- **The Brain Selector**: Use the dropdown in the header of the **Theaters** or **News** tabs to switch between the outputs of different models for the same date.
- **Observability**: Use the **Observability** tab to track the performance (duration and success) of each specific model configuration during the synthesis pipeline.

## Default Seeding
On initial startup, the system automatically seeds a **"Local Gemma"** configuration based on the `application.yml` defaults to ensure the system is functional out-of-the-box.
