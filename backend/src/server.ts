import app from './app';
import { env } from './config/env';

const server = app.listen(env.PORT, () => {
  console.log(`StudySwap AI Backend listening on port ${env.PORT} in ${env.NODE_ENV} mode.`);
});

// Graceful Shutdown
process.on('SIGTERM', () => {
  console.log('SIGTERM signal received: closing HTTP server...');
  server.close(() => {
    console.log('HTTP server closed.');
    process.exit(0);
  });
});

process.on('SIGINT', () => {
  console.log('SIGINT signal received: closing HTTP server...');
  server.close(() => {
    console.log('HTTP server closed.');
    process.exit(0);
  });
});
