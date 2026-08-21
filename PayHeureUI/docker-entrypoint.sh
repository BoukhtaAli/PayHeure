#!/bin/sh
set -e

# Injecte la valeur de API_BASE_URL (variable d'environnement du conteneur) dans
# assets/env.js avant de démarrer nginx, pour que le frontend statique appelle
# la bonne URL sans avoir à rebuilder l'image.
envsubst '${API_BASE_URL}' \
  < /usr/share/nginx/html/assets/env.template.js \
  > /usr/share/nginx/html/assets/env.js

exec nginx -g 'daemon off;'
