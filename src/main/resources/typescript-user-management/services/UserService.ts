import { User } from "../models/User.js";
import { UserRepository } from "../repositories/UserRepository.js";

export class UserService {
  constructor(private repository: UserRepository) {}

  registerUser(name: string, email: string): User {
    if (this.repository.findByEmail(email)) {
      throw new Error("Email already exists");
    }
    const id = Date.now();
    const user = new User(id, name, email);
    this.repository.create(user);
    return user;
  }

  listUsers(): User[] {
    return this.repository.findAll();
  }

  removeUser(id: number): boolean {
    return this.repository.deleteById(id);
  }
}
