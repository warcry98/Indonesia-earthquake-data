#!/bin/sh
set -e

echo "🔐 Generating htpasswd..."

htpasswd -bc /etc/nginx/.htpasswd "$NGINX_BASIC_USER" "$NGINX_BASIC_PASSWORD"

echo "🚀 Starting Nginx..."
nginx -g "daemon off;"