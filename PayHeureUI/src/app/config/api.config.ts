declare global {
  interface Window {
    __env?: { apiBaseUrl?: string };
  }
}

/**
 * Racine de l'API Spring Boot, partagée par tous les services HTTP.
 * Valeur injectée au démarrage du conteneur via assets/env.js (voir docker-entrypoint.sh) ;
 * repli sur localhost:8080 pour le développement local (`ng serve`) sans Docker.
 */
export const API_BASE_URL = window.__env?.apiBaseUrl || 'http://localhost:8080/api';
