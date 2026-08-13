import express from 'express';
import { z } from 'zod';
import { requireFirebaseAuth, AuthenticatedRequest } from '../middleware/firebaseAuth';
import { generateContent } from '../services/aiService';
import { randomUUID } from 'crypto';

const router = express.Router();

const aiGenerateSchema = z.object({
  prompt: z.string().min(1, 'Prompt cannot be empty.'),
  mode: z.string().min(1, 'Mode cannot be empty.'),
  model: z.string().optional(),
});

router.post('/generate', requireFirebaseAuth, async (req: AuthenticatedRequest, res: express.Response) => {
  const requestId = `req_ai_${randomUUID()}`;
  try {
    const parseResult = aiGenerateSchema.safeParse(req.body);
    if (!parseResult.success) {
      res.status(400).json({
        success: false,
        error: 'BAD_REQUEST',
        message: 'Invalid request body parameters.',
        details: parseResult.error.format(),
        requestId,
      });
      return;
    }

    const { prompt, mode, model } = parseResult.data;

    const content = await generateContent({ prompt, mode, model });

    res.status(200).json({
      success: true,
      mode,
      content,
      model: model || 'gemini-3.6-flash',
      requestId,
    });
  } catch (error: any) {
    const errorMsg = error.message || '';
    console.error(`[AI Router Error] Request ID: ${requestId}. Message: ${errorMsg}`);

    if (errorMsg.startsWith('MISSING_GEMINI_KEY')) {
      res.status(500).json({
        success: false,
        error: 'CONFIGURATION_ERROR',
        message: 'Gemini AI service is not properly configured on the server.',
        requestId,
      });
    } else if (errorMsg.startsWith('INVALID_PROMPT')) {
      res.status(400).json({
        success: false,
        error: 'BAD_REQUEST',
        message: errorMsg.replace('INVALID_PROMPT: ', ''),
        requestId,
      });
    } else if (errorMsg.startsWith('GEMINI_RATE_LIMIT')) {
      res.status(429).json({
        success: false,
        error: 'RATE_LIMIT',
        message: 'AI assistant rate limit reached. Please try again in a few minutes.',
        requestId,
      });
    } else if (errorMsg.startsWith('TIMEOUT')) {
      res.status(408).json({
        success: false,
        error: 'TIMEOUT',
        message: 'Request timed out. Please try a simpler topic or shorter notes.',
        requestId,
      });
    } else {
      res.status(500).json({
        success: false,
        error: 'SERVER_ERROR',
        message: 'Failed to process AI assistant request. Please try again.',
        requestId,
      });
    }
  }
});

export default router;
