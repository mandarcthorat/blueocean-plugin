#!/bin/bash

SCRIPT_DIR=$(dirname $0)
$SCRIPT_DIR/stop-sc.sh

echo ""
echo " Starting Sauce Connect container..."
echo ""
docker run -d --name blueo-selenium --rm debian sh -c "apt update && apt install -y wget && wget -O - https://saucelabs.com/downloads/sc-4.5.3-linux.tar.gz | tar xvzf -&& /sc-4.5.3-linux/bin/sc -u $SAUCE_USERNAME -k $SAUCE_ACCESS_KEY -i $BUILD_TAG"
