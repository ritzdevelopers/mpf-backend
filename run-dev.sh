#!/usr/bin/env bash
set -euo pipefail

# Homebrew OpenJDK (Java 21 works with this Spring Boot project)
if [ -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
elif [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
elif [ -d "/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
else
  echo "Java not found. Install with: brew install openjdk@21"
  exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"

cd "$(dirname "$0")"
echo "Using Java: $(java -version 2>&1 | head -1)"
exec ./mvnw spring-boot:run "$@"
