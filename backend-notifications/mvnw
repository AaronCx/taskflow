#!/bin/sh
# Maven wrapper script for Unix/Linux/macOS
# Download Maven if not present and execute it.

set -e

MAVEN_WRAPPER_PROPERTIES="$(dirname "$0")/.mvn/wrapper/maven-wrapper.properties"
MAVEN_HOME="${HOME}/.m2/wrapper/dists"

# Read the distribution URL from properties
DIST_URL=$(grep distributionUrl "$MAVEN_WRAPPER_PROPERTIES" | cut -d= -f2-)
DIST_NAME=$(basename "$DIST_URL" .zip)

DIST_DIR="${MAVEN_HOME}/${DIST_NAME}"
MVN="${DIST_DIR}/bin/mvn"

if [ ! -f "$MVN" ]; then
    echo "Downloading Maven $DIST_NAME..."
    mkdir -p "$DIST_DIR"
    curl -fsSL "$DIST_URL" -o "/tmp/${DIST_NAME}.zip"
    unzip -q "/tmp/${DIST_NAME}.zip" -d "$MAVEN_HOME"
    rm "/tmp/${DIST_NAME}.zip"
    # Rename the versioned directory to match DIST_DIR
    EXTRACTED=$(ls "$MAVEN_HOME" | grep "apache-maven" | head -1)
    if [ "${MAVEN_HOME}/${EXTRACTED}" != "$DIST_DIR" ]; then
        mv "${MAVEN_HOME}/${EXTRACTED}" "$DIST_DIR"
    fi
fi

exec "$MVN" "$@"
