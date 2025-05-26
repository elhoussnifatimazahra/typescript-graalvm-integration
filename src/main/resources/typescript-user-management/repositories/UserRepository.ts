import { User } from "../models/User.js";

export class UserRepository {
  private users: User[] = [];

  create(user: User): void {
    this.users.push(user);
  }

  findAll(): User[] {
    return this.users;
  }

  findByEmail(email: string): User | undefined {
    return this.users.find(u => u.email === email);
  }

  deleteById(id: number): boolean {
    const index = this.users.findIndex(u => u.id === id);
    if (index >= 0) {
      this.users.splice(index, 1);
      return true;
    }
    return false;
  }
}
