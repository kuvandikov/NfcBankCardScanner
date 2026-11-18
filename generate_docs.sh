#!/bin/bash

# Copyright 2026 Kuvandikov and the scan-card-nfc contributors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Builds the mkdocs input folder (docs-gen/) the same way google/accompanist does:
# hand-written guides from docs/, README as the index page, Dokka HTML under api/.
# Usage: ./generate_docs.sh && mkdocs serve

set -ex

DOCS_ROOT=docs-gen

[ -d $DOCS_ROOT ] && rm -r $DOCS_ROOT
mkdir $DOCS_ROOT

# API reference
./gradlew :dokkaGenerate --stacktrace

cp -a docs/* $DOCS_ROOT
rm -rf $DOCS_ROOT/archive $DOCS_ROOT/PLAN.md
cp -a build/dokka/html $DOCS_ROOT/api

cp README.md $DOCS_ROOT/index.md
cp CONTRIBUTING.md $DOCS_ROOT/contributing.md
cp SECURITY.md $DOCS_ROOT/security.md
cp CHANGELOG.md $DOCS_ROOT/changelog.md

# Repository-relative links → site-relative links
sed -i.bak 's/CONTRIBUTING.md/contributing/; s/SECURITY.md/security/; s/CHANGELOG.md/changelog/' $DOCS_ROOT/index.md
sed -i.bak 's/docs\/\([a-zA-Z0-9\/-]*\)\.md/\1/g' $DOCS_ROOT/index.md
find $DOCS_ROOT -name '*.bak' -delete
