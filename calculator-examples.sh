#!/bin/bash

# Calculator Expectation Examples
# First load the expectation:
curl -X PUT "http://localhost:1080/mockserver/expectation" -H "Content-Type: application/json" -d @calculator-expectation.json

echo "Testing Calculator Expectation..."

echo "Addition: 10 + 5"
curl "http://localhost:1080/calc?a=10&b=5&op=%2B"
echo -e "\n"

echo "Subtraction: 10 - 3"
curl "http://localhost:1080/calc?a=10&b=3&op=-"
echo -e "\n"

echo "Multiplication: 7 * 8"
curl "http://localhost:1080/calc?a=7&b=8&op=*"
echo -e "\n"

echo "Division: 15 / 3"
curl "http://localhost:1080/calc?a=15&b=3&op=/"
echo -e "\n"

echo "Division by zero: 10 / 0"
curl "http://localhost:1080/calc?a=10&b=0&op=/"
echo -e "\n"

echo "Invalid operation: 5 ^ 2"
curl "http://localhost:1080/calc?a=5&b=2&op=^"
echo -e "\n"

echo "Default operation (addition): 4 + 6"
curl "http://localhost:1080/calc?a=4&b=6"
echo -e "\n"
