import { Request, Response, NextFunction } from 'express';
import { getAuth } from '../services/firebaseAdmin';

export interface AuthenticatedRequest extends Request {
  user?: {
    uid: string;
    email: string;
  };
  rawBody?: Buffer;
}

export async function requireFirebaseAuth(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    res.status(401).json({
      error: 'UNAUTHORIZED',
      message: 'Missing or malformed Authorization header. Expected Bearer token.',
    });
    return;
  }

  const token = authHeader.split('Bearer ')[1]?.trim();

  if (!token) {
    res.status(401).json({
      error: 'UNAUTHORIZED',
      message: 'Bearer token is empty.',
    });
    return;
  }

  try {
    const auth = getAuth();
    const decodedToken = await auth.verifyIdToken(token);

    req.user = {
      uid: decodedToken.uid,
      email: decodedToken.email || '',
    };

    next();
  } catch (error: any) {
    res.status(401).json({
      error: 'UNAUTHORIZED',
      message: 'Invalid or expired Firebase ID token.',
    });
  }
}
