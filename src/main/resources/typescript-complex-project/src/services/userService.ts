import { User } from '../models/user.js';

export class UserService {
  private users: User[] = [];

  createUser(name: string, email: string): User {
    const user = new User(name, email);
    this.users.push(user);
    return user;
  }

  getUserByEmail(email: string): User | undefined {
    return this.users.find(user => user.email === email);
  }
}
