#!/bin/bash

# Setup script for environment variables
# This script loads .env file and exports variables

if [ -f .env ]; then
    echo "Loading environment variables from .env file..."
    export $(cat .env | grep -v '^#' | xargs)
    echo "✓ Environment variables loaded successfully!"
    echo ""
    echo "Loaded variables:"
    echo "  GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID:0:20}..." 
    echo "  GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET:0:10}..."
    echo "  CALENDAR_ENCRYPTION_KEY: ${CALENDAR_ENCRYPTION_KEY:0:20}..."
    echo ""
    echo "Now you can run: ./mvnw spring-boot:run"
else
    echo "Error: .env file not found!"
    echo "Please create .env file first with your Google OAuth credentials."
    exit 1
fi
