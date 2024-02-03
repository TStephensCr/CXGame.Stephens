#!/bin/bash

# Command to run
COMMAND="java -cp \"..\" connectx.CXPlayerTester"

# Combinations
combinations=(
    "x y z"
    "4 4 4"
    "5 4 4"
    "6 4 4"
    "7 4 4"
    "4 5 4"
    "5 5 4"
    "6 5 4"
    "7 5 4"
    "4 6 4"
    "5 6 4"
    "6 6 4"
    "7 6 4"
    "4 7 4"
    "5 7 4"
    "6 7 4"
    "7 7 4"
    "5 4 5"
    "6 4 5"
    "7 4 5"
    "4 5 5"
    "5 5 5"
    "6 5 5"
    "7 5 5"
    "4 6 5"
    "5 6 5"
    "6 6 5"
    "7 6 5"
    "4 7 5"
    "5 7 5"
    "6 7 5"
    "7 7 5"
    "20 20 10"
    "30 30 10"
    "40 40 10"
    "50 50 10"
)

# Run the command for each combination
for combo in "${combinations[@]}"; do
    eval "$COMMAND $combo connectx.BottomFragger.BottomFragger connectx.L1.L1"
done
