import { UserService } from './services/userService.js';
import { AuthService } from './services/authService.js';
import { Logger } from './utils/logger.js';

const logger = new Logger();
const userService = new UserService();
const authService = new AuthService();

logger.log('Application started');

const user = userService.createUser('fatima zahra', 'fatimazahra@gmail.com');
logger.log(`Created user: ${user.name}`);

const token = authService.authenticate('fatimazahra@gmail.com', 'password123');
logger.log(`Authentication token: ${token}`);
