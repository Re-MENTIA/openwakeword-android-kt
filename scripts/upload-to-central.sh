#!/bin/bash
# Upload script for Central Portal

VERSION="${1:-0.1.1}"
BUNDLE="$HOME/openwakeword-$VERSION-bundle.zip"
REPO_PATH="$HOME/.m2/repository/io/github/miyashita-code/openwakeword/$VERSION"

echo "Creating bundle for version $VERSION..."
cd "$REPO_PATH" || exit 1

# Generate checksums
for file in *.jar *.aar *.pom *.module; do
  if [ -f "$file" ]; then
    md5 -r "$file" | awk '{print $1}' > "$file.md5"
    shasum -a 1 "$file" | awk '{print $1}' > "$file.sha1"
  fi
done

# Create javadoc if missing
if [ ! -f "openwakeword-$VERSION-javadoc.jar" ]; then
  cp "openwakeword-$VERSION-sources.jar" "openwakeword-$VERSION-javadoc.jar"
  md5 -r "openwakeword-$VERSION-javadoc.jar" | awk '{print $1}' > "openwakeword-$VERSION-javadoc.jar.md5"
  shasum -a 1 "openwakeword-$VERSION-javadoc.jar" | awk '{print $1}' > "openwakeword-$VERSION-javadoc.jar.sha1"
  gpg --armor --detach-sign "openwakeword-$VERSION-javadoc.jar"
fi

# Create clean bundle directory
TEMP_DIR="$HOME/maven-bundle-upload"
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR/io/github/miyashita-code/openwakeword/$VERSION"

# Copy all artifact files
cp openwakeword-$VERSION*.* "$TEMP_DIR/io/github/miyashita-code/openwakeword/$VERSION/"

# Create bundle (ONLY io/ directory, no root files)
cd "$TEMP_DIR"
zip -r "$BUNDLE" io/

# Load credentials
cd - > /dev/null
if [ -f gradle.properties ]; then
    USERNAME=$(grep ossrhUsername gradle.properties | cut -d'=' -f2)
    PASSWORD=$(grep ossrhPassword gradle.properties | cut -d'=' -f2)
else
    echo "gradle.properties not found!"
    exit 1
fi

echo "Uploading bundle to Central Portal..."
RESPONSE=$(curl -X POST \
  -u "$USERNAME:$PASSWORD" \
  -F "bundle=@$BUNDLE" \
  -F "publishingType=AUTOMATIC" \
  -w "\n%{http_code}" \
  https://central.sonatype.com/api/v1/publisher/upload)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" == "201" ] || [ "$HTTP_CODE" == "200" ]; then
    echo "✅ Upload successful!"
    echo "Deployment ID: $BODY"
    echo ""
    echo "Check status at: https://central.sonatype.com/publishing/deployments"
else
    echo "❌ Upload failed with HTTP code $HTTP_CODE"
    echo "Response: $BODY"
    exit 1
fi