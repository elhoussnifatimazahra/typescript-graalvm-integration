import { UserRepository } from "./repositories/UserRepository.js";
import { UserService } from "./services/UserService.js";
import { UserController } from "./controllers/UserController.js";

const userRepository = new UserRepository();
const userService = new UserService(userRepository);
const userController = new UserController(userService);

// Exemples d’utilisation
userController.addUser("fatimazahra", "fati@example.com");
userController.addUser("elhoussni", "elhoussni@example.com");

userController.listUsers();

userController.deleteUser(1746784117947); // ID aléatoire
