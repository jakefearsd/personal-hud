# Model Configuration Guide (Multi-Brain Engine)

> [!NOTE]
> For comprehensive architectural and implementation details, see the complete [Multi-Brain Intelligence Engine Guide](multi-brain-engine.md).

The HUD (Heads-Up Display) utilizes a dynamic, database-backed **Multi-Brain Intelligence Engine**. This architecture allows the system to leverage different Large Language Models (LLMs) for specific analytical tasks and enables side-by-side quality comparison.

---

## Supported Providers

### 1. Ollama (Local)
Used for running models like **Gemma4 8B**, **Llama 3.3**, or **Mistral** on your local network or GPU hardware.
- **Config Requirements**: 
  - `Base URL`: The endpoint of your Ollama server (e.g., `http://localhost:11434` or `http://inference.local:11434`).
  - `Model Name`: The specific tag of the model (e.g., `gemma4:e4b`).
  - `Context Window`: Configurable up to the limits of your hardware (defaulting to 64k/65536 for deep-dives).

### 2. Google Gemini (Remote / Cloud)
Used for high-reasoning tasks utilizing Google's Gemini 2.0 Flash or Pro models.
- **Config Requirements**:
  - `API Key`: Your secret key from [Google AI Studio](https://aistudio.google.com/).
  - `Model Name`: e.g., `gemini-2.0-flash` or `gemini-2.0-pro-exp-02-05`.
- **Note**: A single daily run (7 categories) comfortably fits within the **Free Tier** limits.

### 3. DeepSeek / OpenAI Compatible (Remote / Cloud)
Used for OpenAI-compatible endpoints including DeepSeek V3 / R1 reasoning models.
- **Config Requirements**:
  - `Base URL`: Endpoint URL (e.g., `https://api.deepseek.com/v1`).
  - `API Key`: Provider API key.
  - `Model Name`: e.g., `deepseek-chat` or `deepseek-reasoner`.

---

## Administrative Interface

Access the **Config** tab (`/config`) in the HUD UI to:
1. **Add Brain**: Define a new model configuration.
2. **Toggle Active**: Activate or deactivate specific brains. The daily briefing pipeline will iterate through *all* active configurations.
3. **Test Connection**: Verify network reachability and model response before saving.
4. **Edit / Delete**: Update connection strings or remove obsolete models.

---

## Side-by-Side Comparison & Observability

The HUD is designed as a laboratory for intelligence tuning:
* **Unique Persistence**: Briefings are stored with `(date, category, modelName)` as the compound identity.
* **The Brain Selector**: Use the dropdown in the header of the **Theaters** or **News** tabs to switch between the outputs of different models for the same date.
* **Observability**: Use the **Observability** tab (`/observability`) to track the duration, token counts, and success rates of each specific model configuration during the synthesis pipeline.

---

## Default Seeding
On initial startup, the system automatically seeds a **"Local Gemma"** configuration based on `application.yml` defaults to ensure the system is functional out-of-the-box.
