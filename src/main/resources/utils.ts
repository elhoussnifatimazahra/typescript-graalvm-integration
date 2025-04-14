export const PI: number = 3.14159;

export function multiply(a: number, b: number): number {
  return a * b;
}

export class Calculator {
  constructor(private initialValue: number = 0) {}

  add(value: number): number {
    this.initialValue += value;
    return this.initialValue;
  }

  getCurrentValue(): number {
    return this.initialValue;
  }
}