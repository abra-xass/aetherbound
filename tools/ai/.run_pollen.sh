#!/bin/sh
# Run particle texture gen first (~15 min), then back-view gen (~2.5h).
# Both write to assets/. Output to log files for inspection.
set -e
cd "$(dirname "$0")/../.."

echo "=== STAGE 1: 144 particle textures ==="
py -3 tools/ai/generate_particle_textures.py --skip-existing > tools/ai/.particles.log 2>&1
PARTICLES_RC=$?
echo "particles exit code: $PARTICLES_RC"

echo "=== STAGE 2: 300 echoform back-views (img2img) ==="
py -3 tools/ai/generate_backviews.py --skip-existing --strength 0.55 > tools/ai/.backviews.log 2>&1
BACKVIEWS_RC=$?
echo "backviews exit code: $BACKVIEWS_RC"

echo "DONE | particles=$PARTICLES_RC backviews=$BACKVIEWS_RC"
