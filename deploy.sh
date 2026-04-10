#!/bin/bash
cd "$(dirname "$0")"
MSG="Latest $(date '+%Y-%m-%d %H:%M')"
git add -A
git commit -m "$MSG"
git push
echo "Pushed: $MSG"
