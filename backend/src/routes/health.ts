import { Router, Request, Response } from 'express';
import { env } from '../config/env';

const router = Router();

router.get('/health', (_req: Request, res: Response) => {
  res.status(200).json({
    status: 'UP',
    timestamp: new Date().toISOString(),
    environment: env.NODE_ENV,
  });
});

router.get('/ready', (_req: Request, res: Response) => {
  res.status(200).json({
    status: 'READY',
    timestamp: new Date().toISOString(),
    services: {
      firebase: 'CONFIGURED',
      storage: 'CONFIGURED',
      razorpay: 'CONFIGURED',
    },
  });
});

export default router;
