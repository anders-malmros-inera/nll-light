#!/bin/bash

# Generate PNG images from PlantUML diagrams
# Requires PlantUML jar file

echo "Generating sequence diagrams..."

# Check if plantuml.jar exists, if not download it
if [ ! -f "plantuml.jar" ]; then
    echo "Downloading PlantUML..."
    curl -L -o plantuml.jar https://github.com/plantuml/plantuml/releases/download/v1.2023.13/plantuml-1.2023.13.jar
fi

# Generate PNG files from all .puml files
for file in *.puml; do
    if [ -f "$file" ]; then
        echo "Generating PNG for $file..."
        java -jar plantuml.jar "$file"
    fi
done

echo "Sequence diagrams generated successfully!"
echo "PNG files created:"
ls -la *.png