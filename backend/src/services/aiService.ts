import { env } from '../config/env';

export interface GenerateAiOptions {
  prompt: string;
  mode: string;
  model?: string;
}

async function executeGeminiRequest(modelName: string, apiKey: string, requestBody: any): Promise<string> {
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${modelName}:generateContent?key=${apiKey}`;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 30000); // 30s timeout

  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestBody),
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    const responseText = await response.text();

    if (!response.ok) {
      console.error(`Gemini API Error for model ${modelName}: Status: ${response.status}. Body: ${responseText}`);
      if (response.status === 429) {
        throw new Error('GEMINI_RATE_LIMIT: Gemini API rate limit exceeded.');
      }
      throw new Error(`GEMINI_API_ERROR: Gemini API returned status ${response.status}`);
    }

    const json = JSON.parse(responseText);
    const candidates = json.candidates;
    if (candidates && candidates.length > 0) {
      const firstCandidate = candidates[0];
      const content = firstCandidate.content;
      const parts = content?.parts;
      if (parts && parts.length > 0) {
        const textResult = parts[0]?.text;
        if (textResult) {
          return textResult;
        }
      }
    }

    throw new Error('MALFORMED_RESPONSE: Failed to parse Gemini response.');
  } catch (error: any) {
    clearTimeout(timeoutId);
    if (error.name === 'AbortError') {
      throw new Error('TIMEOUT: AI request timed out.');
    }
    throw error;
  }
}

export async function generateContent(options: GenerateAiOptions): Promise<string> {
  const { prompt, mode } = options;

  // 1. Verify Gemini API Key configuration
  const apiKey = env.GEMINI_API_KEY;
  if (!apiKey || apiKey === 'NOT_CONFIGURED') {
    throw new Error('MISSING_GEMINI_KEY: Gemini API Key is not configured on the backend.');
  }

  // 2. Validate inputs
  const trimmedPrompt = prompt.trim();
  if (trimmedPrompt.length === 0) {
    throw new Error('INVALID_PROMPT: Prompt cannot be empty.');
  }
  if (trimmedPrompt.length > 5000) {
    throw new Error('INVALID_PROMPT: Prompt is too long. Maximum 5000 characters.');
  }

  // 3. Define Mode-Specific System Instructions
  let systemInstruction = 'You are StudySwap AI, a student-focused educational assistant. Priorities: 1. Accuracy, 2. Clarity, 3. Student-friendly explanation, 4. Exam usefulness, 5. Conciseness.';
  const lowerMode = mode.toLowerCase();

  switch (lowerMode) {
    case 'explain':
      systemInstruction += ' Explain the requested topic at a college/university student level. Structure your explanation with a clear "Definition", a "Simple Explanation" with a practical example, "Key Points", and a "Long-form Exam Tip".';
      break;
    case 'summarize':
      systemInstruction += ' Summarize the provided text/topic into a concise, academically useful summary. Focus strictly on key definitions, core concepts, and essential facts. Remove redundancy.';
      break;
    case 'quiz':
      systemInstruction += ' Generate a multiple-choice quiz based on the user\'s topic. Return exactly 5 questions. For each question, provide 4 options (A, B, C, D), clearly state the correct option, and give a brief explanation of why it is correct.';
      break;
    case 'flashcard':
      systemInstruction += ' Create 5 high-yield study flashcards for the user\'s topic. Format each card with a clear question (Q:) and a concise, memorable answer (A:).';
      break;
    case 'mcq':
      systemInstruction += ' Generate 5 high-yield Multiple Choice Questions (MCQ) on the user\'s topic. Include options A, B, C, D, the correct option, and a quick explanation.';
      break;
    case 'revision':
      systemInstruction += ' Create a quick, high-impact revision cheat sheet for the user\'s topic. Focus on core formulas, key terms, and common exam questions.';
      break;
    case 'planner':
      systemInstruction += ' Generate a structured day-by-day exam revision plan for the user\'s target topic/timeframe. Keep it realistic, actionable, and focused on high-yield study methods.';
      break;
    default:
      // Leave default instruction
      break;
  }

  // 4. Resolve Model to use
  const allowedModels = [
    env.GEMINI_PRIMARY_MODEL,     // gemini-3.6-flash
    env.GEMINI_FALLBACK_MODEL,    // gemini-3.5-flash
    env.GEMINI_LITE_MODEL,        // gemini-3.1-flash-lite
    'gemini-3.1-pro-preview'
  ];

  let selectedModel = options.model || env.GEMINI_PRIMARY_MODEL;
  if (!allowedModels.includes(selectedModel)) {
    selectedModel = env.GEMINI_PRIMARY_MODEL;
  }

  // 5. Construct Request Body
  const requestBody = {
    contents: [
      {
        parts: [
          {
            text: trimmedPrompt,
          },
        ],
      },
    ],
    systemInstruction: {
      parts: [
        {
          text: systemInstruction,
        },
      ],
    },
  };

  // 6. Execute Request with fallback strategy
  try {
    return await executeGeminiRequest(selectedModel, apiKey, requestBody);
  } catch (primaryError: any) {
    // If the user did not explicitly lock to a specific model and the primary failed, try falling back
    if (!options.model && selectedModel === env.GEMINI_PRIMARY_MODEL) {
      console.warn(`Primary Gemini model ${selectedModel} failed. Attempting fallback to ${env.GEMINI_FALLBACK_MODEL}. Error: ${primaryError.message}`);
      try {
        return await executeGeminiRequest(env.GEMINI_FALLBACK_MODEL, apiKey, requestBody);
      } catch (fallbackError: any) {
        console.error(`Fallback Gemini model ${env.GEMINI_FALLBACK_MODEL} also failed. Error: ${fallbackError.message}`);
        throw fallbackError;
      }
    }
    throw primaryError;
  }
}
