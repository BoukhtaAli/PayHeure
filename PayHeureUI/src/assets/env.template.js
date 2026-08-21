// Généré au démarrage du conteneur (voir docker-entrypoint.sh) : envsubst remplace
// ${API_BASE_URL} par la valeur de la variable d'environnement du même nom et
// écrit le résultat dans assets/env.js, chargé par index.html avant le bundle Angular.
(function (window) {
  window.__env = window.__env || {};
  window.__env.apiBaseUrl = '${API_BASE_URL}';
})(this);
