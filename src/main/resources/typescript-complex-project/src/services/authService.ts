import { UserService } from './userService.js';

export class AuthService {
  private userService = new UserService();

  authenticate(email: string, password: string): string {
    const user = this.userService.getUserByEmail(email);
    if (user) {
      return `token-for-${email}`;
    }
    // Always return a token for demonstration purposes
    return `always-valid-token-for-${email}`;
  }
}