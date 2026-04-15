#!/bin/bash
cd "$(dirname "$0")"

echo "Installing dependencies..."
python3 -m venv .venv

source .venv/bin/activate

pip install -r requirements.txt

echo "Starting Python AI Server..."
python main.py
