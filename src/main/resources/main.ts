import { PI, multiply, Calculator } from "./utils.js";

function calculateCircleArea(radius: number): number {
  return multiply(PI, radius * radius);
}

const radius = 5;
const area = calculateCircleArea(radius);
console.log(`The area of a circle with radius ${radius} is: ${area}`);

const myCalculator = new Calculator(10);
myCalculator.add(7);
console.log(`Calculator current value: ${myCalculator.getCurrentValue()}`);