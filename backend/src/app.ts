import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';

import healthRoutes from './routes/health';
import storageRoutes from './routes/storage';
import paymentRoutes from './routes/payments';
import razorpayWebhookRoutes from './routes/razorpayWebhook';
import aiRoutes from './routes/ai';


const app = express();

// Security headers & CORS
app.use(helmet());
app.use(cors());

// Global HTTP Request Logger
app.use((req, res, next) => {
  const start = Date.now();
  console.log(`[HTTP INCOMING] ${req.method} ${req.originalUrl} - IP: ${req.ip} - User-Agent: ${req.headers['user-agent'] || 'unknown'}`);
  res.on('finish', () => {
    const duration = Date.now() - start;
    console.log(`[HTTP RESPONSE] ${req.method} ${req.originalUrl} - Status: ${res.statusCode} (${duration}ms)`);
  });
  next();
});

// Rate Limiter
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 200,
  standardHeaders: true,
  legacyHeaders: false,
});
app.use(limiter);

// Health check routes
app.use('/', healthRoutes);

// MANDATORY CORRECTION 3: Mount raw body webhook BEFORE global express.json()
app.use('/api/v1/webhooks/razorpay', express.raw({ type: 'application/json' }), razorpayWebhookRoutes);

// Global JSON body parser for standard API routes
app.use(express.json({ limit: '10mb' }));

// API Routes
app.use('/api/v1/storage', storageRoutes);
app.use('/api/v1/payments', paymentRoutes);
app.use('/api/v1/ai', aiRoutes);


// Centralized error handler
app.use((err: any, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  console.error('Unhandled server error:', err);
  res.status(500).json({ error: 'INTERNAL_SERVER_ERROR', message: 'An unexpected error occurred.' });
});

export default app;
