import { UserService } from "../services/UserService.js";

export class UserController {
  constructor(private userService: UserService) {}

  addUser(name: string, email: string): void {
    try {
      const user = this.userService.registerUser(name, email);
      console.log("User created:", JSON.stringify(user, null, 2)); // Utilisation de JSON.stringify
    } catch (error) {
      console.error("Error:", (error as Error).message);
    }
  }

  listUsers(): void {
    const users = this.userService.listUsers();
    console.log("All users:", JSON.stringify(users, null, 2)); // Utilisation de JSON.stringify
  }


  deleteUser(id: number): void {
    const result = this.userService.removeUser(id);
    if (result) {
      console.log("User deleted.");
    } else {
      console.log(`User with ID ${id} not found.`);
    }
  }

}
