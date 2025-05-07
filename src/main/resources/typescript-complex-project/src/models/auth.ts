export class Auth {
  constructor(
    public token: string,
    public expiresAt: Date
  ) {}
}
